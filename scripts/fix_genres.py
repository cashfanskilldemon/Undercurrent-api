#!/usr/bin/env python3
"""按 genre-override.txt 修正候选艺人的流派归属，并把旧名「实验 / 声音」统一成「实验 / 前卫」。"""
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CAND = ROOT / "scripts" / "out" / "candidates.json"
OVERRIDE = ROOT / "scripts" / "genre-override.txt"

rules = {}
for line in OVERRIDE.read_text(encoding="utf-8").splitlines():
    line = line.strip()
    if line and not line.startswith("#") and "|" in line:
        name, target = (part.strip() for part in line.split("|", 1))
        rules[name.lower()] = target

rows = json.loads(CAND.read_text(encoding="utf-8"))
out = []
moved = dropped = renamed = 0
for row in rows:
    if row["category"] == "实验 / 声音":
        row["category"] = "实验 / 前卫"
        renamed += 1
    target = rules.get(row["name"].lower())
    if target == "DROP":
        dropped += 1
        continue
    if target and target != row["category"]:
        row["category"] = target
        moved += 1
    out.append(row)

CAND.write_text(json.dumps(out, ensure_ascii=False, indent=2), encoding="utf-8")
print(f"改名 {renamed} 行，改流派 {moved} 行，剔除 {dropped} 行，剩余候选 {len(out)}")
