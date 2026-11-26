#!/usr/bin/env python3
"""
把 scripts/curated-artists.txt 里的人工名单解析成网易云歌手，插到候选缓存最前排。

用途：Spotify 上那种辨识度很强、但不一定出现在「热门歌单频次榜」里的艺人
      （Lil Skies / Don Toliver / SOPHIE / Jamie xx …），手动指定后优先入选。

用法：
  python scripts/add_curated_artists.py             # 写入 scripts/out/candidates.json
  python scripts/fetch_netease_artists.py --select-only
  python scripts/fetch_netease_artists.py --images-only
  python scripts/fetch_netease_artists.py --select-only
"""
from __future__ import annotations

import json
import time
import urllib.parse
from pathlib import Path

import requests

ROOT = Path(__file__).resolve().parent.parent
CURATED = ROOT / "scripts" / "curated-artists.txt"
CAND_OUT = ROOT / "scripts" / "out" / "candidates.json"
HEADERS = {"User-Agent": "Mozilla/5.0", "Referer": "https://music.163.com/", "Cookie": "appver=2.0.2; os=pc"}


def get_json(url: str, tries: int = 3) -> dict:
    last = None
    for _ in range(tries):
        try:
            return requests.get(url, headers=HEADERS, timeout=20).json()
        except Exception as exc:  # noqa: BLE001
            last = exc
            time.sleep(1)
    raise RuntimeError(f"请求失败 {url}: {last}")


def find_artist(name: str) -> dict | None:
    url = "https://music.163.com/api/search/get?" + urllib.parse.urlencode(
        {"s": name, "type": 100, "limit": 8, "offset": 0}
    )
    data = get_json(url)
    artists = ((data.get("result") or {}).get("artists")) or []
    exact = [a for a in artists if (a.get("name") or "").strip().lower() == name.strip().lower()]
    if not exact:
        return None
    # 同名时优先有头像、歌曲数多的那个
    return sorted(exact, key=lambda a: (bool(a.get("picUrl")), a.get("musicSize") or 0), reverse=True)[0]


def main() -> int:
    pairs = []
    for line in CURATED.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if line and not line.startswith("#") and "|" in line:
            category, name = (part.strip() for part in line.split("|", 1))
            pairs.append((category, name))

    cand = json.loads(CAND_OUT.read_text(encoding="utf-8"))
    index = {(row["netease_id"], row["category"]): row for row in cand}
    added = 0
    bumped = 0
    missed = []

    for i, (category, name) in enumerate(pairs):
        hit = find_artist(name)
        time.sleep(0.18)
        if not hit:
            missed.append(name)
            print(f"  ! 没找到精确匹配：{name}")
            continue
        artist_id = hit["id"]
        key = (artist_id, category)
        row = index.get(key)
        if row is None:
            detail = get_json(f"https://music.163.com/api/artist/{artist_id}").get("artist") or {}
            time.sleep(0.18)
            row = {
                "netease_id": artist_id,
                "name": detail.get("name") or hit["name"],
                "alias": ",".join(detail.get("alias") or [])[:200],
                "bio": (detail.get("briefDesc") or "").replace("\n", " ")[:500],
                "music": int(detail.get("musicSize") or hit.get("musicSize") or 0),
                "album": int(detail.get("albumSize") or hit.get("albumSize") or 0),
                "pic": detail.get("picUrl") or hit.get("picUrl") or "",
                "category": category,
                "rank": 0,
            }
            cand.append(row)
            index[key] = row
            added += 1
        row["rank"] = round(0.001 * (i + 1), 4)  # 0.001~0.23，始终排在歌单候选（1 起）前面
        bumped += 1
        print(f"  {category:<10} {row['name']:<22} id={artist_id}")

    CAND_OUT.write_text(json.dumps(cand, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"\n新增 {added} 位，提前 {bumped} 位，未找到 {len(missed)} 位：{missed}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
