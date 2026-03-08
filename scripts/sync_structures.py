#!/usr/bin/env python3
"""
Copy structure NBTs saved in-game back to the mod's resource folder.

After saving a structure block in-game, Minecraft writes the NBT to:
  run/saves/Structure Layout/generated/ars_zero/structures/necropolis/<name>.nbt

This copies them to:
  src/main/resources/data/ars_zero/structure/necropolis/<name>.nbt

TNT blocks in the source NBT are replaced with structure_void so that
boundary markers placed while building don't overwrite world geometry
when the structure is placed in-game.

Usage:
    python3 scripts/sync_structures.py
    ./gradlew syncStructures
"""

import gzip
import shutil
from pathlib import Path

import nbtlib

PROJECT_ROOT = Path(__file__).resolve().parent.parent

SRC  = PROJECT_ROOT / "run/saves/Structure Layout/generated/ars_zero/structures/necropolis"
DEST = PROJECT_ROOT / "src/main/resources/data/ars_zero/structure/necropolis"

TNT_NAME  = "minecraft:tnt"
VOID_NAME = "minecraft:structure_void"


def replace_tnt_with_void(src: Path, dst: Path) -> int:
    """Copy src NBT to dst, replacing TNT palette entries with structure_void.
    Returns the number of palette entries replaced."""
    nbt = nbtlib.load(src)

    palette = nbt.get("palette") or nbt.get("Palette")
    replaced = 0
    if palette:
        for entry in palette:
            name_tag = entry.get("Name") or entry.get("name")
            if name_tag is not None and str(name_tag) == TNT_NAME:
                if "Name" in entry:
                    entry["Name"] = nbtlib.String(VOID_NAME)
                else:
                    entry["name"] = nbtlib.String(VOID_NAME)
                replaced += 1

    nbt.save(dst)
    return replaced


def main():
    if not SRC.exists():
        print(f"Source not found: {SRC}")
        print("Save a structure block in-game first.")
        return 1

    files = sorted(SRC.glob("*.nbt"))
    if not files:
        print(f"No .nbt files in {SRC}")
        return 1

    DEST.mkdir(parents=True, exist_ok=True)
    for f in files:
        dst = DEST / f.name
        replaced = replace_tnt_with_void(f, dst)
        # Also overwrite the source so /place template sees the processed version
        if replaced:
            shutil.copy2(dst, f)
        suffix = f"  ({replaced} TNT → structure_void)" if replaced else ""
        print(f"  {f.name}{suffix}")

    print(f"\n{len(files)} file(s) synced to {DEST}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
