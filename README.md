# Undercurrent 后端

地下嘻哈 / 电子音乐社区的后端服务，对应前端 `Undercurrent-app`。

**技术栈**：Java 21 · Spring Boot 3.4.2 · MyBatis 3.0.4 · MySQL（开发库 5.7.36，8.0 同样兼容）

---

## 一、建库（两条 SQL，按顺序执行）

```bash
# 1. 建库建表（脚本内含 DROP TABLE，会清空重建）
mysql -h 127.0.0.1 -u root -p --default-character-set=utf8mb4 < src/main/resources/db/schema.sql

# 2. 灌入初始数据（10 位音乐人 / 8 首作品 / 8 个插件 / 7 篇帖子 / 11 条回复）
mysql -h 127.0.0.1 -u root -p --default-character-set=utf8mb4 < src/main/resources/db/seed.sql
```

`seed.sql` 就是前端现在看到的那份演示内容，字段一一对应。相对时间（"2 小时前"）已转成基于 `NOW()` 的绝对时间。

管理员账号（`admin` / `cashfans03`）已包含在 `seed.sql` 里；已有库想单独补，执行 `src/main/resources/db/migration-auth-admin.sql` 即可（幂等）。

> 想重新灌数据直接重跑 `seed.sql`，开头有 `TRUNCATE`。
>
> `--default-character-set=utf8mb4` 别省，否则中文会变问号。用 Navicat / DataGrip 的话直接打开 SQL 文件执行即可。

**关于 MySQL 5.7**：`schema.sql` 已按 5.7 兼容写（`utf8mb4_unicode_ci`，不用 8.0 才有的 `utf8mb4_0900_ai_ci`）。索引里的 `DESC` 在 5.7 会被解析但忽略，不影响功能，升到 8.0 后自动生效。

---

## 二、配置与启动

仓库里**不含任何数据库凭据**。`src/main/resources/application.yml` 的默认值是「本地 3306 / root / 空密码」，全部可被环境变量覆盖：

| 环境变量 | 默认值 |
|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/Undercurrent_test?...` |
| `DB_USERNAME` | `root` |
| `DB_PASSWORD` | 空 |

本地开发任选一种方式填真实连接信息：

**方式一：外置配置文件（推荐，改完不用重启 IDE 配置）**

```bash
cp config/application.yml.example config/application.yml   # 再填 url / username / password
```

`config/` 在项目根目录时优先级高于 classpath，里面的值会覆盖 `application.yml` 的默认值；该文件已被 `.gitignore` 忽略，不会进仓库。

**方式二：环境变量**

```bash
export DB_URL='jdbc:mysql://127.0.0.1:3306/Undercurrent_test?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai'
export DB_USERNAME=root
export DB_PASSWORD='你的密码'
```

> 库名 `Undercurrent_test` 首字母大写，Linux 上 MySQL 区分库名大小写，别写成 `undercurrent_test`。
> 密码含 `$ < + ( )` 等特殊字符时，YAML 里要用单引号包住，否则 `$` 会被当变量。

启动：

```bash
mvn spring-boot:run
# 或
mvn clean package -DskipTests && java -jar target/undercurrent-api-1.0.0.jar
```

访问 `http://localhost:8080/api/meta` 能看到 JSON 就说明通了。

> **需要 JDK 21**。如果你本机还是 17，把 `pom.xml` 的 `<java.version>` 改成 17 也能跑（代码没用到 21 专属语法，实测 17 编译通过）。

改完代码想确认「接口 × 数据库」整条链路没坏，跑一遍冒烟：

```bash
node scripts/smoke.mjs                          # 默认 http://localhost:8080
node scripts/smoke.mjs http://127.0.0.1:8081    # 指定其它地址
```

11 项检查，覆盖元数据 / 帖子列表 / 详情 / 频道过滤 / 点赞收藏开关 / 空正文校验 / 作品排序 / spec 透传 / 插件分类过滤。点赞收藏是成对调用的，跑完不留下脏数据；最后一行给出通过/失败数量，失败时退出码为 1。

> 注意：业务异常（参数校验失败等）HTTP 状态仍是 200，失败信息在响应体 `code` 里——这是本项目约定的统一响应体风格，前端拦截器应判断 `data.code` 而不是 HTTP status。

---

## 三、API 清单

统一响应体：`{ "code": 0, "message": "ok", "data": ... }`，`code != 0` 即失败。

分页结构：`{ list: [], total, page, size, hasMore }`

### 讨论

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/threads?channel=&keyword=&tag=&page=1&size=20` | 列表。`channel` 传"全部"或省略表示不过滤 |
| GET | `/api/threads/{id}` | 详情，含 `replies` 数组 |
| POST | `/api/threads` | 发帖 |
| POST | `/api/threads/{id}/like` | 点赞 / 取消，返回 `{liked, likes}` |
| POST | `/api/threads/{id}/save` | 收藏 / 取消，返回 `{saved}` |

发帖请求体：

```json
{
  "channel": "作品反馈",
  "body": "正文内容",
  "tags": ["求反馈", "boombap"],
  "attachmentKind": "track",
  "attachmentTrackId": 1
}
```

### 作品

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/tracks?genre=&keyword=&sort=latest\|plays\|likes&page=1&size=20` | 列表 |
| GET | `/api/tracks/{id}` | 详情，`spec` 原样透传 JSON |
| POST | `/api/tracks/{id}/play` | 播放计数 +1 |
| POST | `/api/tracks/{id}/like` | 点赞切换 |

### 插件 / 元数据

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/plugins?category=&keyword=&page=1&size=20` | 插件列表 |
| GET | `/api/plugins/{id}` | 插件详情 |
| GET | `/api/meta` | 一次拿到流派 / 频道 / 插件分类 / 热门标签 |

### 鉴权

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/auth/login` | 账号密码登录，body `{ "account": "admin", "password": "..." }`，成功返回用户资料；失败返回 `code: 401` |

- 账号即 `app_user.handle`（大小写不敏感），密码存在 `password_hash` 列。
- 哈希算法：**PBKDF2-HMAC-SHA256**，120000 轮、16 字节随机盐，存储格式 `pbkdf2$迭代次数$盐(base64)$哈希(base64)`，实现见 `common/PasswordHasher`。
- 演示账号：`admin` / `cashfans03`，由 `db/migration-auth-admin.sql` 写入（seed.sql 里也带了一份，重灌数据不会丢）。
- 目前**不发 token**，登录成功后前端把返回的 `id` 继续放进 `X-User-Id` 头；接 JWT 时替换 `AuthController` 的返回体即可，业务代码不用动。

### 未登录怎么办

`userId` 从请求头 `X-User-Id` 取：

```bash
curl -H "X-User-Id: 1" -X POST http://localhost:8080/api/threads/1/like
```

不带该头时，`liked` / `saved` 恒为 `false`，点赞、通知等需要身份的接口返回 401。这是给前端联调用的临时方案，正式接入登录后换成 JWT 解析即可（改 `Controller` 里取 userId 的方式）。

---

## 四、前端要改的地方

现在前端读的是 `data/mock-data.json`，换成接口后有几处必须同步：

**1. ID 类型变了**

mock 里是 `"a1"` `"t1"` `"th1"`，数据库自增主键是 `1` `2` `3`。前端所有 `authorId === artist.id` 这类比对要改成数字比较。

**2. 作者信息已内嵌**

以前是 `thread.authorId` 再去 `artists` 数组里 lookup，现在接口直接返回：

```json
{
  "id": 1,
  "author": { "id": 1, "name": "阿鬼", "handle": "ghostwire", "hue": 122, "verified": true },
  "channel": "作品反馈",
  "body": "...",
  "tags": ["求反馈", "boombap"],
  "likes": 286,
  "reposts": 41,
  "repliesCount": 3,
  "pinned": true,
  "attachment": { "kind": "track", "trackId": 1, "track": {...} }
}
```

不用再拼接。

**3. 时间变了**

原来是 `"2 小时前"` 这种字符串，现在是 `"2026-11-23 14:30:00"`。前端需要一个相对时间格式化函数（`dayjs.fromNow()` 或自己写）。

**4. 开发期跨域**

后端已开 CORS（`localhost` / `127.0.0.1` 任意端口）。也可以在 `server.mjs` 或 vite 里配代理：

```js
proxy: { '/api': { target: 'http://localhost:8080', changeOrigin: true } }
```

---

## 五、目录结构

```
undercurrent-api/
├── pom.xml
├── scripts/gen-seed.mjs              # mock-data.json → seed.sql
├── scripts/smoke.mjs                 # 接口冒烟测试（11 项）
└── src/main/
    ├── java/com/undercurrent/
    │   ├── UndercurrentApplication.java
    │   ├── common/                   # 统一响应 / 分页 / 异常 / CSV 转 List
    │   ├── config/WebConfig.java     # CORS
    │   ├── domain/                   # 数据库实体
    │   ├── vo/                       # 返回给前端的视图对象
    │   ├── dto/                      # 入参
    │   ├── mapper/                   # MyBatis 接口
    │   ├── service/                  # 业务
    │   └── controller/               # REST
    └── resources/
        ├── application.yml
        ├── mapper/*.xml              # SQL
        └── db/{schema,seed}.sql
```

---

## 六、数据模型速览

| 表 | 说明 |
|---|---|
| `app_user` | 用户，`role` 区分音乐人/制作人/插件作者/听众 |
| `thread` | 讨论帖，`pinned` 置顶，`attachment_*` 挂作品 |
| `thread_reply` | 回复，`parent_id` 支持楼中楼 |
| `track` | 作品，`spec_json` 存鼓机序列（JSON 列） |
| `plugin` | 插件，`formats` / `os` 逗号分隔 |
| `thread_like` / `thread_save` / `track_like` / `reply_like` | 互动关系，联合主键天然去重 |
| `user_follow` | 关注关系 |
| `genre` / `channel` / `plugin_category` | 字典表 |
| `tag_stat` | 标签帖子数，用于右栏推荐 |

计数（`likes_count` / `replies_count`）做了冗余，点赞时同事务更新，避免列表页 COUNT 子查询。

**没有建物理外键**，只建索引——关联完整性由应用层保证，避免删用户时级联阻塞。如果团队习惯物理外键，可以按注释里的关联关系补上。
