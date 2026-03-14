#!/usr/bin/env python3
"""
Print the block palette and block counts for a given NBT file.
Useful for checking what's in a structure piece.

Usage:
    python3 .claude/scripts/inspect_palette.py <nbt_name>
    python3 .claude/scripts/inspect_palette.py entrance_staircase
"""

import sys
from collections import Counter
import nbtlib
from pathlib import Path

NBT_DIR = Path(__file__).resolve().parent.parent.parent / "src/main/resources/data/ars_zero/structure/necropolis"

name = sys.argv[1] if len(sys.argv) > 1 else None
if not name:
    print("Usage: inspect_palette.py <nbt_name>")
    sys.exit(1)

f = NBT_DIR / (name if name.endswith(".nbt") else name + ".nbt")
if not f.exists():
    print(f"Not found: {f}")
    sys.exit(1)

nbt = nbtlib.load(f)
palette = nbt.get("palette", [])
blocks = nbt.get("blocks", [])
size = list(nbt.get("size", []))

print(f"{f.name}  size={size}  blocks={len(blocks)}")
counts = Counter(str(palette[int(b["state"])].get("Name", "?")) for b in blocks)
for block, count in counts.most_common():
    print(f"  {count:5d}  {block}")
