#!/usr/bin/env python3
"""
Generate GZIP-compressed structure template NBT files for the Blight Dungeon.
Produces entrance.nbt, room_small.nbt, repurpose_room.nbt as placeholder
5x4x5 boxes of ars_nouveau:sourcestone_large_bricks.

Requires: pip install nbtlib

Usage:
  python scripts/generate_blight_dungeon_nbt.py [output_dir]
  Default output_dir: src/main/resources/data/ars_zero/structures/blight_dungeon
"""

import os
import sys

try:
    import nbtlib
    from nbtlib import Compound, List, Int, String
except ImportError:
    print("Error: nbtlib is required. Install with: pip install nbtlib", file=sys.stderr)
    sys.exit(1)


# Structure template layout expected by Minecraft StructureTemplate.load() (1.21):
# Single-palette format: root has "size" (ListTag of 3 TAG_Int), "blocks", "palette" (one list of block state compounds), "entities".
# Block compound: "pos" (ListTag of 3 TAG_Int), "state" (TAG_Int = palette index).
# Minecraft uses tag.getList("size", 3) and getList("pos", 3) — List of Int, NOT IntArray.
# DataVersion (3955 for 1.21) avoids data fixer altering the NBT.
BLOCK_ID = "ars_nouveau:sourcestone_large_bricks"
DATA_VERSION = 3955  # SharedConstants.WORLD_VERSION for 1.21


def make_structure_nbt(width: int, height: int, depth: int) -> Compound:
    """Root: size, blocks, palette, entities, DataVersion."""
    palette_entries = List[Compound]([Compound({"Name": String(BLOCK_ID)})])
    blocks_list = List[Compound]()
    for x in range(width):
        for y in range(height):
            for z in range(depth):
                blocks_list.append(Compound({
                    "pos": List[Int]([Int(x), Int(y), Int(z)]),
                    "state": Int(0),
                }))
    return Compound({
        "size": List[Int]([Int(width), Int(height), Int(depth)]),
        "blocks": blocks_list,
        "palette": palette_entries,
        "entities": List[Compound](),
        "DataVersion": Int(DATA_VERSION),
    })


def main() -> None:
    script_dir = os.path.dirname(os.path.abspath(__file__))
    project_root = os.path.dirname(script_dir)
    default_out = os.path.join(
        project_root,
        "src", "main", "resources", "data", "ars_zero", "structures", "blight_dungeon",
    )
    out_dir = sys.argv[1] if len(sys.argv) > 1 else default_out
    os.makedirs(out_dir, exist_ok=True)

    # 5x4x5 box per plan/README
    w, h, d = 5, 4, 5
    root = make_structure_nbt(w, h, d)

    names = ["entrance", "room_small", "repurpose_room"]
    for name in names:
        path = os.path.join(out_dir, f"{name}.nbt")
        # nbtlib.File wraps a compound and can save gzipped
        nbt_file = nbtlib.File(root, gzipped=True)
        nbt_file.filename = path
        nbt_file.save(path, gzipped=True)
        print(f"Wrote {path}")

    # Minimal 3x3x3 cube for format validation (game test: ars_zero:test/simple_3x3)
    test_dir = os.path.join(project_root, "src", "main", "resources", "data", "ars_zero", "structures", "test")
    os.makedirs(test_dir, exist_ok=True)
    simple_root = make_structure_nbt(3, 3, 3)
    simple_path = os.path.join(test_dir, "simple_3x3.nbt")
    nbtlib.File(simple_root, gzipped=True).save(simple_path, gzipped=True)
    print(f"Wrote {simple_path}")

    print("Done. Rebuild the mod (e.g. ./gradlew build) and test in a new world.")


if __name__ == "__main__":
    main()
