#!/usr/bin/env python3
"""
把本地 H2 文件库（.h2data/undercurrent）导出成可在公司 MySQL 执行的脚本。

用法：
  python scripts/export_h2_to_mysql.py                       # 导出结构 + 数据
  python scripts/export_h2_to_mysql.py --schema-only         # 只导结构
  python scripts/export_h2_to_mysql.py -o db/migration-local.sql

输出默认写到 src/main/resources/db/migration-YYYYMMDD-local.sql，
确认内容无误后提交 GitHub，公司那边用 mysql 客户端执行即可。
"""
import argparse
import re
import sys
from datetime import datetime
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DB_PATH = ROOT / ".h2data" / "undercurrent"
H2_JAR_DIR = Path.home() / ".m2" / "repository" / "com" / "h2database" / "h2"


def find_h2_jar() -> Path:
    if not H2_JAR_DIR.exists():
        sys.exit(f"找不到 H2 驱动，请先让 Maven/IDEA 下载依赖：{H2_JAR_DIR}")
    jars = sorted(H2_JAR_DIR.glob("*/h2-*.jar"))
    if not jars:
        sys.exit(f"H2 驱动目录为空：{H2_JAR_DIR}")
    return jars[-1]


def run_script(h2_jar: Path, out_sql: Path):
    import subprocess
    url = f"jdbc:h2:file:{DB_PATH};MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;ACCESS_MODE_DATA=r"
    cmd = [
        "java", "-cp", str(h2_jar), "org.h2.tools.Shell",
        "-url", url, "-user", "sa", "-password", "",
        "-sql", f"SCRIPT TO '{out_sql.as_posix()}';",
    ]
    p = subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8")
    if p.returncode != 0:
        sys.exit(f"H2 SCRIPT 导出失败：\n{p.stderr}")


def decode_u(text: str) -> str:
    """U&'\\XXXX' -> 真实中文"""
    def repl(m):
        body = m.group(1)
        return "'" + re.sub(r"\\([0-9a-fA-F]{4})", lambda x: chr(int(x.group(1), 16)), body) + "'"
    return re.sub(r"U&'((?:[^'\\]|\\.)*)'", repl, text)


TYPE_MAP = [
    (r"CHARACTER VARYING\((\d+)\)", lambda m: f"VARCHAR({m.group(1)})"),
    (r"VARCHAR\((\d+)\)", lambda m: f"VARCHAR({m.group(1)})"),
    (r"CHARACTER LARGE OBJECT|CLOB|TEXT", lambda m: "TEXT"),
    (r"BIGINT", lambda m: "BIGINT"),
    (r"INTEGER|INT", lambda m: "INT"),
    (r"SMALLINT", lambda m: "SMALLINT"),
    (r"TINYINT", lambda m: "TINYINT"),
    (r"BOOLEAN", lambda m: "TINYINT(1)"),
    (r"DECIMAL\((\d+),\s*(\d+)\)|NUMERIC\((\d+),\s*(\d+)\)",
     lambda m: f"DECIMAL({m.group(1) or m.group(3)},{m.group(2) or m.group(4)})"),
    (r"DOUBLE PRECISION", lambda m: "DOUBLE"),
    (r"REAL", lambda m: "FLOAT"),
    (r"TIMESTAMP\(?\d*\)?", lambda m: "DATETIME"),
    (r"DATE", lambda m: "DATE"),
    (r"TIME\(?\d*\)?", lambda m: "TIME"),
    (r"JSON", lambda m: "JSON"),
    (r"BINARY LARGE OBJECT|BLOB", lambda m: "BLOB"),
]


def map_type(raw: str) -> str:
    up = raw.upper()
    for pat, fn in TYPE_MAP:
        m = re.search(pat, up)
        if m:
            return fn(m)
    return up


def parse_create_table(line: str, lines: list[str], i: int):
    """解析 CREATE CACHED TABLE 块，返回 (表名, 列定义列表, 下一行索引)"""
    m = re.match(r'CREATE CACHED TABLE "public"\."(\w+)"', line)
    if not m:
        return None
    table = m.group(1)
    cols = []
    i += 1
    while i < len(lines) and not lines[i].strip().startswith(")"):
        cl = lines[i].strip().rstrip(",")
        cm = re.match(r'"(\w+)"\s+(.*?)\s*$', cl)
        if not cm:
            i += 1
            continue
        name, typ = cm.group(1), cm.group(2)
        # 去掉注释
        typ = re.sub(r"\s+COMMENT\s+'[^']*'", "", typ, flags=re.I)
        # 去掉 H2 自增声明
        typ = re.sub(r"\s+GENERATED\s+BY\s+DEFAULT\s+AS\s+IDENTITY\s*\([^)]*\)", "", typ, flags=re.I)
        typ = re.sub(r"\s+DEFAULT\s+ON\s+NULL", "", typ, flags=re.I)
        # 非空
        not_null = bool(re.search(r"\bNOT NULL\b", typ, re.I))
        typ = re.sub(r"\s+NOT\s+NULL", "", typ, flags=re.I)
        # 默认值（保留）
        dm = re.search(r"\bDEFAULT\s+('[^']*'|-?\d+|CURRENT_TIMESTAMP|NULL)\b", typ, re.I)
        default = dm.group(0) if dm else ""
        if dm:
            typ = typ[:dm.start()] + typ[dm.end():]
        # H2 的 ON UPDATE 保留
        on_update = " ON UPDATE CURRENT_TIMESTAMP" if re.search(r"ON UPDATE CURRENT_TIMESTAMP", typ, re.I) else ""
        typ = re.sub(r"\s+ON UPDATE CURRENT_TIMESTAMP", "", typ, flags=re.I)

        mysql_type = map_type(typ.strip())
        parts = [f"`{name}`", mysql_type]
        if default:
            parts.append(default)
        if not_null:
            parts.append("NOT NULL")
        if on_update:
            parts.append(on_update.strip())
        cols.append(" ".join(parts))
        i += 1
    return table, cols, i + 1


def convert(h2_sql: str, schema_only: bool) -> str:
    h2_sql = decode_u(h2_sql)
    out = [
        "-- ============================================================",
        "-- Undercurrent 本地 H2 开发变更 → MySQL 迁移脚本",
        f"-- 导出时间：{datetime.now():%Y-%m-%d %H:%M:%S}",
        "-- 由 scripts/export_h2_to_mysql.py 生成，提交前请人工核对",
        "-- 目标库：MySQL 5.7 / 8.0，字符集 utf8mb4",
        "-- 执行方式：mysql -u root -p < 本文件（在目标库已存在的前提下）",
        "-- ============================================================",
        "",
        "SET NAMES utf8mb4;",
        "",
    ]

    lines = h2_sql.splitlines()
    tables: dict[str, dict] = {}
    i = 0
    while i < len(lines):
        stripped = lines[i].strip()

        # CREATE TABLE
        if stripped.startswith("CREATE CACHED TABLE"):
            parsed = parse_create_table(stripped, lines, i)
            if parsed:
                table, cols, i = parsed
                tables[table] = {"cols": cols, "pk": [], "unique": [], "index": [], "rows": []}
                continue

        # PRIMARY KEY —— 兼容 "public"."CONSTRAINT_x"
        m = re.match(r'ALTER TABLE "public"\."(\w+)" ADD CONSTRAINT "[^"]+"\."[^"]+" PRIMARY KEY\((.+?)\);', stripped)
        if m and m.group(1) in tables:
            pks = [c.strip().strip('"') for c in m.group(2).split(",")]
            tables[m.group(1)]["pk"] = pks
            i += 1
            continue

        # UNIQUE —— CONSTRAINT 形式
        m = re.match(r'ALTER TABLE "public"\."(\w+)" ADD CONSTRAINT "[^"]+"\."[^"]+" UNIQUE\((.+?)\);', stripped)
        if m and m.group(1) in tables:
            tables[m.group(1)]["unique"].append(m.group(2))
            i += 1
            continue

        # INDEX
        m = re.match(r'CREATE INDEX "[^"]+"\."[^"]+" ON "public"\."(\w+)"\((.+?)\);', stripped)
        if m and m.group(1) in tables:
            tables[m.group(1)]["index"].append(m.group(2))
            i += 1
            continue

        # INSERT 数据段
        m = re.match(r'INSERT INTO "public"\."(\w+)" VALUES', stripped)
        if m and not schema_only:
            table = m.group(1)
            if table in tables:
                i += 1
                while i < len(lines):
                    v = lines[i].strip().rstrip(",").rstrip(";")
                    if not v.startswith("("):
                        break
                    v = re.sub(r"TIMESTAMP\s+'([^']+)'", r"'\1'", v)
                    v = re.sub(r"DATE\s+'([^']+)'", r"'\1'", v)
                    v = re.sub(r"\bTRUE\b", "1", v, flags=re.I)
                    v = re.sub(r"\bFALSE\b", "0", v, flags=re.I)
                    tables[table]["rows"].append(v)
                    i += 1
                continue

        i += 1

    # 输出建表语句
    for table, meta in tables.items():
        out.append(f"-- ---------- {table} ----------")
        cols = list(meta["cols"])
        pk = meta["pk"]
        if pk:
            pk_cols = "`" + "`, `".join(pk) + "`"
            # 单列整型主键加 AUTO_INCREMENT
            if len(pk) == 1:
                for idx, c in enumerate(cols):
                    if c.startswith(f"`{pk[0]}`") and re.search(r"\b(BIGINT|INT|SMALLINT|TINYINT)\b", c):
                        cols[idx] = c + " AUTO_INCREMENT"
            cols.append(f"PRIMARY KEY ({pk_cols})")
        def clean_col(expr: str) -> str:
            expr = expr.strip()
            # H2 写法："col" DESC NULLS LAST / "col" NULLS FIRST
            m = re.match(r'^"([^"]+)"\s*(DESC|ASC)?\s*(?:NULLS\s+(?:FIRST|LAST))?$', expr, re.I)
            if m:
                return f"`{m.group(1)}` {m.group(2) or ''}".strip()
            # 兜底：去掉所有引号和 NULLS 子句
            expr = re.sub(r"\s*NULLS\s+(FIRST|LAST)", "", expr, flags=re.I)
            expr = expr.replace('"', "`")
            return expr.strip()

        for u in meta["unique"]:
            u_cols = ", ".join(clean_col(c) for c in u.split(","))
            cols.append(f"UNIQUE KEY ({u_cols})")
        for idx_expr in meta["index"]:
            idx_cols = [clean_col(c) for c in idx_expr.split(",")]
            cols.append(f"KEY ({', '.join(idx_cols)})")

        out.append(f"CREATE TABLE IF NOT EXISTS `{table}` (")
        out.append(",\n".join("  " + c for c in cols))
        out.append(") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;")
        out.append("")

    # 输出数据
    if not schema_only:
        out.append("-- ---------- 数据 ----------")
        for table, meta in tables.items():
            for row in meta["rows"]:
                out.append(f"INSERT IGNORE INTO `{table}` VALUES {row};")
            if meta["rows"]:
                out.append("")

    # 管理员账号保底
    out.append("-- 管理员账号（幂等）")
    out.append("INSERT INTO app_user (handle, nickname, avatar_hue, city, role, bio, followers_count, following_count, verified, password_hash, status)")
    out.append("VALUES ('admin', '管理员', 122, '乌鲁木齐', '管理员', '站点管理员账号', 128, 12, 1,")
    out.append("  '$2a$12$3PIju6pvHQ2WVQA4Jh8YnexnyQsYw0HDh.AvKbcDVt8.50SGh76su', 1)")
    out.append("ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash), status = VALUES(status);")
    out.append("")
    return "\n".join(out)


def main():
    ap = argparse.ArgumentParser(description="H2 → MySQL 脚本导出器")
    ap.add_argument("--schema-only", action="store_true", help="只导出建表语句，不含数据")
    ap.add_argument("-o", "--output", help="输出文件路径（默认 db/migration-YYYYMMDD-local.sql）")
    args = ap.parse_args()

    if not (ROOT / ".h2data" / "undercurrent.mv.db").exists():
        sys.exit(f"找不到本地 H2 数据库文件：{ROOT / '.h2data' / 'undercurrent.mv.db'}\n请先以 local profile 启动一次后端。")

    h2_jar = find_h2_jar()
    tmp = Path(__file__).parent / ".h2-export-tmp.sql"
    run_script(h2_jar, tmp)
    raw = tmp.read_text(encoding="utf-8")
    tmp.unlink(missing_ok=True)

    result = convert(raw, args.schema_only)
    out = Path(args.output) if args.output else ROOT / "src" / "main" / "resources" / "db" / f"migration-{datetime.now():%Y%m%d}-local.sql"
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(result, encoding="utf-8")
    print(f"已导出：{out}")
    print(f"表数量：{len(re.findall(r'CREATE TABLE IF NOT EXISTS', result))}，数据行：{len(re.findall(r'INSERT IGNORE INTO', result))}")


if __name__ == "__main__":
    main()
