# 本地 H2 开发 ↔ 公司 MySQL 同步说明

## 一、本地开发

1. IDEA Run Configuration → VM options 加 `-Dspring.profiles.active=local`
2. 正常启动，走 H2 文件库，数据在 `.h2data/undercurrent.mv.db`（已 gitignore，重启不丢）
3. 建表脚本 `db/h2/schema.sql` 和种子 `db/h2/seed.sql` 是幂等的：
   - 每次启动自动补齐缺失的表和种子数据
   - **不会覆盖**你改过的数据（`INSERT IGNORE`）

## 二、把本地改动同步到公司

本地改完表结构 / 加了测试数据后：

```bash
python scripts/export_h2_to_mysql.py
```

默认生成 `src/main/resources/db/migration-YYYYMMDD-local.sql`，包含：
- 全部表结构（主键 / 自增 / 非空 / 默认值 / 索引 / 中文注释已转 MySQL 语法）
- 全部数据（INSERT IGNORE，可重复执行不冲突）

**提交前人工核对一次**，然后 commit & push。公司那边 `git pull` 后执行：

```bash
mysql -u root -p undercurrent_test < src/main/resources/db/migration-YYYYMMDD-local.sql
```

## 三、常用参数

```bash
python scripts/export_h2_to_mysql.py --schema-only              # 只导表结构
python scripts/export_h2_to_mysql.py -o db/migration-xxx.sql    # 指定输出路径
```

## 四、依赖

导出脚本需要 Java（调用 H2 驱动）和 Python 3。H2 驱动从 Maven 本地仓库读取，
只要用 IDEA 打开过项目、下载过依赖即可。若提示找不到驱动，先执行一次
`mvn dependency:go-offline` 或让 IDEA 重新加载 Maven 项目。

## 五、注意

- 导出的脚本是**全量**（当前 H2 库的所有表和数据），不是增量 diff。
  公司库若已有部分数据，`INSERT IGNORE` 会跳过主键冲突的行，不会覆盖。
- 本地 H2 不支持的 MySQL 特性（存储过程等）不会出现在导出脚本里。
- 表结构定稿后，建议同时把变更手工合并进 `db/schema.sql` 主脚本，保持单一事实源。
