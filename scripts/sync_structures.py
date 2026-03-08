#!/usr/bin/env python3
"""
Copy structure NBTs saved in-game back to the mod's resource folder.

After saving a structure block in-game, Minecraft writes the NBT to:
  run/saves/Structure Layout/generated/ars_zero/structures/necropolis/<name>.nbt

This copies them to:
  src/main/resources/data/ars_zero/structure/necropolis/<name>.nbt

Usage:
    python3 scripts/sync_structures.py
    ./gradlew syncStructures
"""

import shutil
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parent.parent

SRC  = PROJECT_ROOT / "run/saves/Structure Layout/generated/ars_zero/structures/necropolis"
DEST = PROJECT_ROOT / "src/main/resources/data/ars_zero/structure/necropolis"


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
        shutil.copy2(f, DEST / f.name)
        print(f"  {f.name}")

    print(f"\n{len(files)} file(s) synced to {DEST}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
