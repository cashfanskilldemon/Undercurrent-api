# Undercurrent API

地下嘻哈 / 电子音乐社区后端，为 [Undercurrent-app](https://github.com/cashfanskilldemon/Undercurrent-app) 提供接口：账号、讨论、作品、插件、搜索、通知与注册引导。

**技术栈**：Java 21 · Spring Boot 3.4.2 · MyBatis 3.0.4 · MySQL 5.7+ / H2（本地开发）

---

## 一、快速开始

### 方式一：本地 H2（不需要安装 MySQL）

项目内置 H2 文件库，激活 `local` profile 即可，建表与种子数据在启动时自动执行。

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

IDEA 用户在 Run Configuration 的 VM options 里加：

```
-Dspring.profiles.active=local
```

- 连接：`jdbc:h2:file:./.h2data/undercurrent;MODE=MySQL;...`，账号 `sa`、空密码
- 数据文件：项目根目录 `.h2data/undercurrent.mv.db`（已 gitignore，重启不丢）
- 建表脚本 `db/h2/schema.sql` 用 `CREATE TABLE IF NOT EXISTS`，种子 `db/h2/seed.sql` 用 `INSERT IGNORE`，每次启动只补齐缺失的表和数据，不覆盖已有行
- 配置在 `config/application-local.yml`，外置目录优先级高于 classpath

改动需要同步给团队时，用 `scripts/export_h2_to_mysql.py` 导出全量脚本，详见 `scripts/README-H2.md`。

### 方式二：MySQL

```bash
# 1. 建库建表（脚本内含 DROP TABLE，会清空重建）
mysql -h 127.0.0.1 -u root -p --default-character-set=utf8mb4 < src/main/resources/db/schema.sql

# 2. 灌入演示数据（用户 / 流派 / 作品 / 插件 / 帖子 / 回复）
mysql -h 127.0.0.1 -u root -p --default-character-set=utf8mb4 < src/main/resources/db/seed.sql
```

`schema.sql` 已包含当前全部表；老库升级按日期顺序执行 `db/migration-*.sql`（均为幂等脚本）：

| 顺序 | 脚本 | 内容 |
|---|---|---|
| 1 | `migration-20261124.sql` | 通知表、帖子配图字段、关注关系种子数据 |
| 2 | `migration-auth-phone.sql` | 手机号字段（仅实名认证使用） |
| 3 | `migration-auth-email.sql` | 邮箱注册 + 唯一索引 |
| 4 | `migration-auth-admin.sql` | 管理员账号 `admin` / `cashfans03` |
| 5 | `migration-genre-category.sql` | 流派扩容到 126 个并加分类 |
| 6 | `migration-onboarding.sql` | 引导字段与 `user_interest` 表 |
| 7 | `migration-social.sql` | 私信会话 / 消息 / 拉黑表 |

> 库名 `Undercurrent_test` 首字母大写，Linux 上 MySQL 区分大小写。
> `--default-character-set=utf8mb4` 不要省略，否则中文会变问号。

### 连接配置

仓库不含任何数据库凭据，`application.yml` 的默认值是「本地 3306 / root / 空密码」，可用环境变量覆盖：

| 环境变量 | 默认值 |
|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/Undercurrent_test?...` |
| `DB_USERNAME` | `root` |
| `DB_PASSWORD` | 空 |

也可以复制 `config/application.yml.example` 为 `config/application.yml` 后填写，该文件已被 gitignore。

### 启动与验证

```bash
mvn spring-boot:run
# 或
mvn clean package -DskipTests && java -jar target/undercurrent-api-1.0.0.jar
```

访问 `http://localhost:8080/api/meta` 返回 JSON 即启动成功。

启动时会执行一次 `SELECT 1` 预热连接池（`config/DataSourceWarmup`），避免首个请求承担建连耗时；日志打印「数据库连接预热完成」。

> JDK 21 为编译目标。本机只有 17 时把 `pom.xml` 的 `<java.version>` 改成 17 也能编译运行（未使用 21 专属语法）。

---

## 二、配置项

| 变量 | 默认值 | 说明 |
|---|---|---|
| `AUTH_SECRET` | `undercurrent-dev-secret-change-me` | token 签名密钥，**上线必须修改**，改了之后所有已签发 token 失效 |
| `AUTH_TTL_SECONDS` | `604800` | token 有效期（7 天） |
| `AUTH_ALLOW_HEADER_FALLBACK` | `false` | 是否信任手写的 `X-User-Id`，仅本地裸调接口时打开 |
| `AUTH_LOGIN_MAX_ATTEMPTS` | `5` | 连续失败锁定阈值 |
| `AUTH_LOGIN_LOCK_MINUTES` | `10` | 锁定时长 |
| `AUTH_LOGIN_FAIL_WINDOW_MINUTES` | `30` | 失败计数窗口 |
| `EMAIL_PROVIDER` | `dev` | `dev` 只打日志；`smtp` 走真实邮件 |
| `EMAIL_SMTP_HOST` / `PORT` / `USERNAME` / `PASSWORD` | `smtp.qq.com` / `465` / 空 | SMTP 连接信息 |
| `EMAIL_FROM` | `UNDERCURRENT <no-reply@undercurrent.local>` | 发件人，需与 SMTP 账号一致 |
| `SMS_PROVIDER` | `dev` | 短信通道，留给后续实名认证 |
| `CODE_EXPOSE_CODE` | `true` | dev 下把验证码回传前端，**上线必须设为 false** |

---

## 三、通用约定

统一响应体：

```json
{ "code": 0, "message": "ok", "data": {} }
```

`code != 0` 表示业务失败，HTTP 状态仍为 200，调用方应判断 `data.code` 或响应体 `code`。鉴权失败返回 `code: 401`，参数校验失败返回 `code: 400`，账号锁定返回 `code: 429`。

分页结构：

```json
{ "list": [], "total": 42, "page": 1, "size": 20, "hasMore": true }
```

`page` 从 1 开始，`size` 上限 100。

---

## 四、API

### 讨论

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/threads?channel=&keyword=&tag=&following=&page=1&size=20` | 列表；`channel` 传「全部」或省略表示不过滤，`following=true` 只看关注的人 |
| GET | `/api/threads/{id}` | 详情，含 `replies` |
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
  "attachmentTrackId": 1,
  "image": "cover-1.png"
}
```

`channel`、`body` 必填；`tags` 最多 5 个；`body` 最多 5000 字。

### 作品

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/tracks?genre=&keyword=&sort=latest\|plays\|likes&page=1&size=20` | 列表 |
| GET | `/api/tracks/{id}` | 详情，`spec` 原样透传为对象 |
| POST | `/api/tracks` | 上传作品（只存元数据，不接收音频文件） |
| POST | `/api/tracks/{id}/play` | 播放计数 +1 |
| POST | `/api/tracks/{id}/like` | 点赞切换 |

上传请求体：

```json
{
  "title": "地铁末班",
  "genre": "Boom Bap",
  "bpm": 88,
  "musicalKey": "F 小调",
  "blurb": "录在末班车上的环境声",
  "cover": "cover-1.png",
  "tags": ["采样"]
}
```

`title` 必填，`bpm` 范围 40–220，`blurb` 最多 500 字。

### 插件与元数据

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/plugins?category=&keyword=&page=1&size=20` | 插件列表 |
| GET | `/api/plugins/{id}` | 插件详情 |
| GET | `/api/meta` | 流派 / 频道 / 插件分类 / 热门标签 |

### 搜索

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/search?q=&limit=8` | 一次返回艺人、作品、帖子、插件四组结果与总数；`q` 为空时返回默认推荐内容 |

### 用户与关注

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/users/suggested?q=&limit=12` | 推荐关注 / 艺人搜索 |
| GET | `/api/users/{id}` | 用户资料 |
| GET | `/api/users/by-handle/{handle}` | 按 handle 查资料 |
| POST | `/api/users/{id}/follow` | 关注 / 取关切换 |

### 通知

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/notifications?page=1&size=50` | 通知列表，含未读数 |
| POST | `/api/notifications/{id}/read` | 单条已读 |
| POST | `/api/notifications/read-all` | 全部已读，返回 `{ok, changed}` |

### 注册后引导

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/onboarding` | 可选项 + 已选内容 + `needsOnboarding` |
| POST | `/api/onboarding` | 提交或跳过，覆盖式保存 |

```json
{
  "genres": ["Boom Bap", "Trap"],
  "artistIds": [1, 2],
  "role": "制作人",
  "ageGroup": "25-30",
  "city": "成都",
  "skipped": false
}
```

流派最多 12 个、艺人最多 30 个；`role` / `ageGroup` / `city` 必须是接口下发的选项之一。`skipped: true` 只标记引导已走过，不保存偏好。

### 鉴权

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/auth/login` | 账号密码登录，`{account, password}`，返回 `{token, expiresIn, user, needsOnboarding}` |
| POST | `/api/auth/register` | 注册，邮箱模式 `{email, emailCode, password, nickname?}`，或账号模式 `{account, password}` |
| POST | `/api/auth/email-code` | 发送邮箱验证码，`{email, scene: "register"\|"reset"}` |
| POST | `/api/auth/reset-password` | 邮箱 + 验证码重置密码 |
| POST | `/api/auth/sms-code` | 短信验证码，预留给实名认证 |
| POST | `/api/auth/forgot-password` | 不暴露账号是否存在，存在则给管理员写站内通知，由人工重置 |

---

## 五、鉴权说明

- 登录成功后前端带 `Authorization: Bearer <token>`；token 为 HMAC-SHA256 签名的 `base64url(uid.exp).base64url(签名)`，无状态不落库。
- `TokenAuthFilter` 解析 token 后注入 `X-User-Id`，Controller 无需感知鉴权细节。
- 不带 token 时 `X-User-Id` 默认不被信任（`allow-header-fallback=false`），本地裸调接口时用 `AUTH_ALLOW_HEADER_FALLBACK=true` 启动：

```bash
curl -H "X-User-Id: 1" -X POST http://localhost:8080/api/threads/1/like
```

- 密码使用 **BCrypt cost 12**（只引入 `spring-security-crypto`，不启用整套 Security 过滤链）；旧 `pbkdf2$...` 记录仍可校验，登录成功时自动升级。
- 防爆破：同一账号连续失败 5 次锁定 10 分钟，计数在内存（`LoginAttemptService`），多实例部署时应换成 Redis。
- 手机号不参与注册登录，`app_user.phone` 保留给实名认证。

### 验证码与邮件

`VerificationCodeService` 统一管理：5 分钟有效、一次性、错 5 次作废；同一目标 60 秒内不可重发、每天 10 条。

发送器可替换：

- 邮箱 `EmailSender`：默认 `DevEmailSender`（打日志 + 回传前端），`EMAIL_PROVIDER=smtp` 时用 `SmtpEmailSender`。
- 短信 `SmsSender`：默认 `DevSmsSender`，接阿里云 / 腾讯云时新增实现类即可。

开启真实发信（以腾讯企业邮为例）：

1. 在 `exmail.qq.com` 开通免费版，按提示配置 **MX** 与 **SPF** 记录
2. 创建成员账号，例如 `noreply@你的域名`，开启 SMTP 并生成客户端专用密码
3. 配置：

```yaml
spring:
  mail:
    host: smtp.exmail.qq.com
    port: 465
    username: noreply@你的域名
    password: 客户端专用密码
app:
  email:
    provider: smtp
    from: 'UNDERCURRENT <noreply@你的域名>'
```

QQ 个人邮箱把 host 换成 `smtp.qq.com`、密码换成授权码即可，代码无需改动。

---

## 六、数据模型

| 表 | 说明 |
|---|---|
| `app_user` | 用户，`role` 区分听众 / 制作人 / 音乐人 / 插件作者 |
| `user_interest` | 引导选择的流派与艺人偏好（`kind = genre` / `artist`） |
| `post` | 讨论帖，`pinned` 置顶，`attachment_*` 挂作品，`image` 配图 |
| `post_reply` | 回复，`parent_id` 支持楼中楼 |
| `track` | 作品，`spec_json` 存鼓机序列 |
| `plugin` | 插件，`formats` / `os` 逗号分隔 |
| `post_like` / `post_save` / `track_like` / `reply_like` | 互动关系，联合主键去重 |
| `user_follow` | 关注关系 |
| `notification` | 站内通知 |
| `conversation` / `direct_message` / `user_block` | 私信会话、消息、拉黑（表已建，接口待开发） |
| `genre` / `channel` / `plugin_category` / `tag_stat` | 字典与标签统计 |

计数（`likes_count` / `replies_count`）冗余在业务表，点赞时同事务更新，避免列表页 COUNT 子查询。

未建物理外键，只建索引，关联完整性由应用层保证。

---

## 七、脚本

| 脚本 | 用途 |
|---|---|
| `scripts/smoke.mjs` | 接口冒烟测试，覆盖元数据 / 帖子 / 作品 / 插件共 11 项检查，点赞收藏成对调用不留脏数据 |
| `scripts/gen-seed.mjs` | 从 mock 数据生成 `seed.sql` |
| `scripts/export_h2_to_mysql.py` | 把本地 H2 全量导出为 MySQL 可执行脚本，说明见 `scripts/README-H2.md` |

```bash
node scripts/smoke.mjs                          # 默认 http://localhost:8080
node scripts/smoke.mjs http://127.0.0.1:8081    # 指定地址
python scripts/export_h2_to_mysql.py            # 导出 H2 → MySQL
python scripts/export_h2_to_mysql.py --schema-only
```

---

## 八、目录结构

```
undercurrent-api/
├── pom.xml
├── config/
│   ├── application.yml.example    数据库模板（复制为 application.yml 使用）
│   └── application-local.yml      H2 本地开发配置
├── scripts/                       冒烟测试 / 种子生成 / H2 导出
└── src/main/
    ├── java/com/undercurrent/
    │   ├── common/                统一响应、分页、异常、验证码、token、密码、邮件/短信发送器
    │   ├── config/                CORS、连接池预热、邮件启动自检
    │   ├── domain/                数据库实体
    │   ├── dto/                   入参
    │   ├── vo/                    出参
    │   ├── mapper/                MyBatis 接口
    │   ├── service/               业务
    │   └── controller/            REST
    └── resources/
        ├── application.yml
        ├── mapper/*.xml           SQL
        └── db/
            ├── schema.sql         全量建表
            ├── seed.sql           演示数据
            ├── migration-*.sql    增量迁移
            └── h2/                H2 兼容版建表与种子
```

---

## License

MIT
