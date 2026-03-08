#!/usr/bin/env python3
"""
Patch the pool field of jigsaw blocks in an NBT file.
Useful for fixing jigsaw connections without rebuilding in-game.

Usage:
    python3 scripts/claude/patch_jigsaw_pool.py <nbt_name> <old_pool> <new_pool>
    python3 scripts/claude/patch_jigsaw_pool.py entrance_staircase ars_zero:necropolis/rooms_2 ars_zero:necropolis/stairs
"""

import sys, shutil
import nbtlib
from pathlib import Path

NBT_DIR = Path(__file__).resolve().parent.parent.parent / "src/main/resources/data/ars_zero/structure/necropolis"
WORLD_DIR = Path(__file__).resolve().parent.parent.parent / "run/saves/Structure Layout/generated/ars_zero/structures/necropolis"

if len(sys.argv) != 4:
    print("Usage: patch_jigsaw_pool.py <nbt_name> <old_pool> <new_pool>")
    sys.exit(1)

nbt_name, old_pool, new_pool = sys.argv[1], sys.argv[2], sys.argv[3]
f = NBT_DIR / (nbt_name if nbt_name.endswith(".nbt") else nbt_name + ".nbt")
if not f.exists():
    print(f"Not found: {f}")
    sys.exit(1)

nbt = nbtlib.load(f)
fixed = 0
for b in nbt.get("blocks", []):
    if "nbt" in b:
        pool = b["nbt"].get("pool")
        if pool is not None and str(pool) == old_pool:
            b["nbt"]["pool"] = nbtlib.String(new_pool)
            fixed += 1

print(f"Fixed {fixed} jigsaw(s) in {f.name}")
nbt.save(f)

world_copy = WORLD_DIR / f.name
if world_copy.exists():
    shutil.copy2(f, world_copy)
    print(f"Updated world copy")
