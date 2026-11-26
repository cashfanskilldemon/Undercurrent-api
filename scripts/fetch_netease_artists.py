#!/usr/bin/env python3
"""
抓网易云音乐的「流派代表歌手」+ 头像，生成 artist 种子数据。

数据来源（都是网页端公开接口，不需要登录）：
- /api/playlist/list?cat=<标签>   某个风格标签下的热门歌单
- /api/search/get?s=<关键词>&type=1000  按关键词搜歌单（标签太老派时用这个，能抓到当代艺人）
- /api/playlist/detail?id=<id>    歌单曲目（匿名只返回前 10 首，用来统计歌手出现频次）
- /api/artist/<id>                歌手详情（头像 picUrl、歌曲数、专辑数）

思路：流派 → 网易云歌单（标签 / 关键词）→ 歌单里出现次数最多的歌手 = 这个流派的代表艺人。
      频次比关键词搜歌手靠得住（搜「techno」会搜出一堆名字就叫 Techno 的路人）。

筛选：`scripts/artist-exclude.txt` 里列的是人工剔除的名单（太古早 / 太土 / 合集号），
      命名命中就跳过，同样保证每个流派仍取够 --per 位。

用法：
  python scripts/fetch_netease_artists.py                 # 抓 + 下载头像 + 生成 SQL
  python scripts/fetch_netease_artists.py --select-only   # 只按排除名单重新选一遍，不联网
  python scripts/fetch_netease_artists.py --per 60        # 每类取 60 位
  python scripts/fetch_netease_artists.py --dry-run       # 只打印名单
"""
from __future__ import annotations

import argparse
import json
import random
import sys
import time
from collections import Counter
from pathlib import Path
from typing import Dict, List, Tuple

import requests

ROOT = Path(__file__).resolve().parent.parent
IMG_DIR = ROOT / "src" / "main" / "resources" / "static" / "artists"
# 迁移脚本按日期归档，改数据时一眼就能看出版本
SQL_OUT = ROOT / "src" / "main" / "resources" / "db" / "2026-11-26" / "artist-seed.sql"
CAND_OUT = ROOT / "scripts" / "out" / "candidates.json"
EXCLUDE_FILE = ROOT / "scripts" / "artist-exclude.txt"

HEADERS = {
    "User-Agent": (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
        "(KHTML, like Gecko) Chrome/124.0 Safari/537.36"
    ),
    "Referer": "https://music.163.com/",
    "Cookie": "appver=2.0.2; os=pc",
}

# 我们的 8 个流派 → 歌单来源：("tag", 网易云风格标签) 或 ("kw", 歌单搜索关键词)
# 标签抓的是「风格榜」，老派流派（爵士/轻音乐）标签下全是上古大神和钢琴翻弹，
# 所以这几类改用关键词搜当代歌单（neosoul / lofi / world music）。
CATEGORIES: List[Tuple[str, List[Tuple[str, str]]]] = [
    ("嘻哈 / 说唱", [("tag", "说唱")]),
    ("低音 / 俱乐部", [("tag", "舞曲")]),
    ("电子 / 舞曲", [("tag", "电子")]),
    ("氛围 / 慢速", [("kw", "lofi"), ("kw", "ambient"), ("kw", "chillout")]),
    ("爵士 / 灵魂", [("kw", "neosoul"), ("kw", "jazzhiphop"), ("kw", "citypop"), ("kw", "现代爵士")]),
    ("摇滚 / 独立", [("tag", "摇滚"), ("tag", "另类/独立")]),
    ("民族 / 世界", [("tag", "世界音乐"), ("kw", "afrobeat"), ("kw", "world music")]),
    ("实验 / 前卫", [("tag", "后摇")]),
]

PLAYLISTS_PER_SOURCE = 20       # 每个标签/关键词抓多少个热门歌单（每个歌单 10 首）
MIN_MUSIC = 20                  # 歌曲数下限，过滤掉路人小号
MIN_ALBUM = 2                   # 专辑数下限，过滤掉只传过一两首的
IMG_SIZE = 160                  # 头像边长（卡片显示 52px，2x 屏够用）
SLEEP = 0.18                    # 请求间隔，别把人家接口打疼

# 兜底黑名单：合集 / 厂牌账号，不是具体的音乐人
BLOCK_NAMES = {"群星", "Various Artists", "V.A.", "未知艺术家", "佚名", "网络歌手"}


def sleep_a_bit() -> None:
    time.sleep(SLEEP + random.random() * 0.12)


def get_json(url: str, tries: int = 3) -> dict:
    last: Exception | None = None
    for _ in range(tries):
        try:
            r = requests.get(url, headers=HEADERS, timeout=20)
            r.raise_for_status()
            return r.json()
        except Exception as exc:  # noqa: BLE001 - 网络抖动就重试一次
            last = exc
            time.sleep(1.2)
    raise RuntimeError(f"请求失败：{url}（{last}）")


def playlists_of(source: Tuple[str, str]) -> List[dict]:
    kind, value = source
    if kind == "tag":
        url = (
            "https://music.163.com/api/playlist/list"
            f"?cat={requests.utils.quote(value)}&order=hot&limit={PLAYLISTS_PER_SOURCE}&offset=0&total=true"
        )
        data = get_json(url)
        if data.get("code") != 200:
            print(f"  ! 标签「{value}」返回 {data.get('code')}，跳过")
            return []
        return [p for p in (data.get("playlists") or []) if p.get("id")]

    url = (
        "https://music.163.com/api/search/get"
        f"?s={requests.utils.quote(value)}&type=1000&limit={PLAYLISTS_PER_SOURCE}&offset=0"
    )
    data = get_json(url)
    return [p for p in ((data.get("result") or {}).get("playlists") or []) if p.get("id")]


def artists_of_playlist(pid: int) -> List[dict]:
    data = get_json(f"https://music.163.com/api/playlist/detail?id={pid}")
    tracks = ((data.get("result") or {}).get("tracks")) or []
    out: List[dict] = []
    for track in tracks:
        for artist in track.get("artists") or []:
            if artist.get("id") and artist.get("name"):
                out.append(artist)
    return out


def artist_detail(artist_id: int) -> dict:
    data = get_json(f"https://music.163.com/api/artist/{artist_id}")
    return data.get("artist") or {}


def avatar_url(pic_url: str | None) -> str | None:
    if not pic_url:
        return None
    return pic_url.replace("http://", "https://") + f"?param={IMG_SIZE}y{IMG_SIZE}"


def download(url: str, dest: Path) -> bool:
    if dest.exists() and dest.stat().st_size > 1200:
        return True
    try:
        r = requests.get(url, headers=HEADERS, timeout=25)
        if r.status_code != 200 or len(r.content) < 1200:
            return False
        dest.write_bytes(r.content)
        return True
    except Exception:  # noqa: BLE001
        return False


def load_exclude() -> set[str]:
    if not EXCLUDE_FILE.exists():
        return set()
    names = set()
    for line in EXCLUDE_FILE.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if line and not line.startswith("#"):
            names.add(line)
    return names


def collect(depth: int) -> List[dict]:
    """联网抓候选：返回 [{netease_id, name, ..., category, rank}]（一位艺人多流派就多行）"""
    rows: List[dict] = []
    for cat, sources in CATEGORIES:
        counter: Counter[int] = Counter()
        names: Dict[int, str] = {}
        seen_playlists: set[int] = set()
        for source in sources:
            pls = playlists_of(source)
            print(f"[{cat}] {source[0]}「{source[1]}」歌单 {len(pls)} 个")
            for pl in pls:
                if pl["id"] in seen_playlists:
                    continue
                seen_playlists.add(pl["id"])
                for artist in artists_of_playlist(pl["id"]):
                    counter[artist["id"]] += 1
                    names[artist["id"]] = artist["name"]
                sleep_a_bit()
        print(f"[{cat}] 候选歌手 {len(counter)} 位")

        rank = 0
        for artist_id, _hits in counter.most_common(depth * 3):
            if rank >= depth:
                break
            detail = artist_detail(artist_id)
            music = int(detail.get("musicSize") or 0)
            album = int(detail.get("albumSize") or 0)
            name = detail.get("name") or names.get(artist_id) or ""
            sleep_a_bit()
            if not name or name in BLOCK_NAMES or music < MIN_MUSIC or album < MIN_ALBUM:
                continue
            rank += 1
            rows.append(
                {
                    "netease_id": artist_id,
                    "name": name,
                    "alias": ",".join(detail.get("alias") or [])[:200],
                    "bio": (detail.get("briefDesc") or "").replace("\n", " ")[:500],
                    "music": music,
                    "album": album,
                    "pic": detail.get("picUrl") or "",
                    "category": cat,
                    "rank": rank,
                }
            )
            print(f"   {rank:>2}. {name}")
    CAND_OUT.parent.mkdir(parents=True, exist_ok=True)
    CAND_OUT.write_text(json.dumps(rows, ensure_ascii=False, indent=2), encoding="utf-8")
    return rows


def select(cand: List[dict], per: int, exclude: set[str]) -> List[dict]:
    """按流派排名挑人，跳过排除名单；同一位艺人多流派只留一条主记录"""
    by_cat: Dict[str, List[dict]] = {}
    for row in cand:
        by_cat.setdefault(row["category"], []).append(row)

    chosen: Dict[int, dict] = {}
    for cat, _ in CATEGORIES:
        rank = 0
        for row in sorted(by_cat.get(cat, []), key=lambda r: r["rank"]):
            if rank >= per:
                break
            if row["name"] in exclude:
                continue
            rank += 1
            entry = chosen.get(row["netease_id"])
            if entry is None:
                entry = dict(row)
                entry["categories"] = []
                chosen[row["netease_id"]] = entry
            entry["categories"].append((cat, rank))

    cat_index = {c: i for i, (c, _) in enumerate(CATEGORIES)}
    rows = sorted(chosen.values(), key=lambda e: min((cat_index[c], r) for c, r in e["categories"]))
    for i, row in enumerate(rows, start=1):
        row["id"] = i
    return rows


def sql_str(value: str | None) -> str:
    if value is None or value == "":
        return "NULL"
    return "'" + str(value).replace("\\", "\\\\").replace("'", "''") + "'"


def write_sql(rows: List[dict]) -> None:
    lines: List[str] = []
    lines.append("-- 本文件由 scripts/fetch_netease_artists.py 自动生成，请勿手改")
    lines.append("-- 数据来源：网易云音乐公开接口（流派歌单 → 歌手出现频次，人工剔除过老/过土的）")
    lines.append(f"-- 艺人 {len(rows)} 位，分类 {len(CATEGORIES)} 个，头像见 static/artists/")
    lines.append("")
    lines.append("DELETE FROM artist_genre;")
    lines.append("DELETE FROM artist;")
    lines.append("")
    lines.append("INSERT INTO artist (id, netease_id, name, alias, avatar, bio, music_size, album_size) VALUES")
    values = []
    for row in rows:
        avatar = f"/artists/{row['netease_id']}.jpg" if row.get("has_image") else None
        values.append(
            "  ({}, {}, {}, {}, {}, {}, {}, {})".format(
                row["id"],
                row["netease_id"],
                sql_str(row["name"]),
                sql_str(row.get("alias")),
                sql_str(avatar),
                sql_str(row.get("bio")),
                row["music"],
                row["album"],
            )
        )
    lines.append(",\n".join(values) + ";")
    lines.append("")
    lines.append("INSERT INTO artist_genre (artist_id, category, sort_order) VALUES")
    pairs = []
    for row in rows:
        for cat, rank in sorted(row["categories"], key=lambda pair: (pair[0], pair[1])):
            pairs.append(f"  ({row['id']}, {sql_str(cat)}, {rank})")
    lines.append(",\n".join(pairs) + ";")
    lines.append("")
    SQL_OUT.parent.mkdir(parents=True, exist_ok=True)
    SQL_OUT.write_text("\n".join(lines), encoding="utf-8")
    print(f"\nSQL 已写入 {SQL_OUT.relative_to(ROOT)}")


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--per", type=int, default=50, help="每个流派最终取几位艺人")
    ap.add_argument("--depth", type=int, default=0, help="每类抓多少候选（默认 per*1.6）")
    ap.add_argument("--no-images", action="store_true", help="不下载头像")
    ap.add_argument("--select-only", action="store_true", help="复用 candidates.json，不联网")
    ap.add_argument("--images-only", action="store_true", help="按已生成的 SQL 补齐缺失头像")
    ap.add_argument("--dry-run", action="store_true", help="只打印名单，不写文件")
    args = ap.parse_args()

    exclude = load_exclude()
    depth = args.depth or int(args.per * 1.6)

    if args.images_only:
        # 只补头像：从生成好的 SQL 里读出 netease_id，逐个下载还没落盘的
        import re

        text = SQL_OUT.read_text(encoding="utf-8")
        ids = [int(m.group(1)) for m in re.finditer(r"\(\d+, (\d+), '", text)]
        cand = json.loads(CAND_OUT.read_text(encoding="utf-8")) if CAND_OUT.exists() else []
        pics = {str(c["netease_id"]): c.get("pic") for c in cand}
        IMG_DIR.mkdir(parents=True, exist_ok=True)
        ok = 0
        for netease_id in ids:
            dest = IMG_DIR / f"{netease_id}.jpg"
            if dest.exists() and dest.stat().st_size > 1200:
                continue
            try:
                url = avatar_url(pics.get(str(netease_id)))
                if not url:
                    url = avatar_url(artist_detail(netease_id).get("picUrl"))
                if url and download(url, dest):
                    ok += 1
            except Exception as exc:  # noqa: BLE001
                print(f"  ! {netease_id} 头像失败：{exc}")
            sleep_a_bit()
        print(f"补齐头像 {ok} 张，共 {len(ids)} 位需要图片")
        return 0

    if args.select_only:
        cand = json.loads(CAND_OUT.read_text(encoding="utf-8"))
        # 复用上一轮的下载结果：头像文件在不在
        for row in cand:
            row["has_image"] = (IMG_DIR / f"{row['netease_id']}.jpg").exists()
    else:
        cand = collect(depth)

    rows = select(cand, args.per, exclude)

    if not args.no_images and not args.dry_run and not args.select_only:
        IMG_DIR.mkdir(parents=True, exist_ok=True)
        ok = 0
        for row in rows:
            row["has_image"] = False
            url = avatar_url(row.get("pic"))
            if url and download(url, IMG_DIR / f"{row['netease_id']}.jpg"):
                row["has_image"] = True
                ok += 1
            sleep_a_bit()
        print(f"\n头像下载完成：{ok}/{len(rows)} 张 → {IMG_DIR.relative_to(ROOT)}")

    if args.dry_run:
        for row in rows:
            cats = "、".join(c for c in [row["category"]])
            print(f"  {row['name']:<24} {cats}")
        return 0

    if args.select_only:
        for row in rows:
            row.setdefault("has_image", (IMG_DIR / f"{row['netease_id']}.jpg").exists())

    write_sql(rows)
    print("\n各分类数量：")
    for cat, _ in CATEGORIES:
        n = sum(1 for r in rows if any(c == cat for c, _ in r["categories"]))
        print(f"  {cat:<10} {n}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
