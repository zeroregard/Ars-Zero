#!/usr/bin/env python3
"""
Print all jigsaw block data from every necropolis NBT.
Useful for debugging jigsaw connection issues.

Usage:
    python3 .claude/scripts/inspect_jigsaws.py
"""

import nbtlib
from pathlib import Path

NBT_DIR = Path(__file__).resolve().parent.parent.parent / "src/main/resources/data/ars_zero/structure/necropolis"

for f in sorted(NBT_DIR.glob("*.nbt")):
    nbt = nbtlib.load(f)
    palette = nbt.get("palette", [])
    blocks = nbt.get("blocks", [])
    size = list(nbt.get("size", []))

    jigsaws = [
        (b, palette[int(b["state"])])
        for b in blocks
        if "jigsaw" in str(palette[int(b["state"])].get("Name", ""))
    ]

    if not jigsaws:
        continue

    print(f"\n=== {f.name} === size={size}")
    for b, entry in jigsaws:
        orientation = entry.get("Properties", {}).get("orientation", "?")
        pos = list(b["pos"])
        n = b.get("nbt", {})
        print(f"  pos={pos} orientation={orientation}")
        print(f"    name={n.get('name')}  target={n.get('target')}")
        print(f"    pool={n.get('pool')}  joint={n.get('joint')}")
