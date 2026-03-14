---
name: Necropolis structures
description: Rules for editing necropolis NBT pieces, template pools, and worldgen structure config
paths:
  - src/main/resources/data/ars_zero/structure/**
  - src/main/resources/data/ars_zero/worldgen/template_pool/**
  - src/main/resources/data/ars_zero/worldgen/structure/**
---

# Necropolis Structure Rules

Full design doc: `.claude/rules/necropolis.md`

## Critical: NBT pieces are hand-built, not datagen

All `.nbt` files under `structure/necropolis/` are built in-game and synced via:
```bash
./gradlew syncStructures
```
**Never** generate or overwrite them with `runData`. `StructureDatagen.java` has been deleted.

## Jigsaw connection rules

- All connectors in the hallways pool use `name`/`target` = `ars_zero:necropolis/passage`
- Entry jigsaw on a piece has `pool=minecraft:empty` (terminal — does not spawn children)
- Exit jigsaws have `pool=ars_zero:necropolis/<pool_name>`
- Corridor opening dimensions must match across connected pieces — currently all hallway pieces use **3-wide corridors** (hallway_straight is an exception at 5-wide; fix is pending)

## Debugging jigsaw issues

Use scripts in `.claude/scripts/`:

```bash
# Inspect all jigsaw blocks in every necropolis piece
python3 .claude/scripts/inspect_jigsaws.py

# Check block palette and counts for a specific piece
python3 .claude/scripts/inspect_palette.py <piece_name>

# Patch a jigsaw pool field without rebuilding in-game
python3 .claude/scripts/patch_jigsaw_pool.py <piece_name> <old_pool> <new_pool>
```

Requires `nbtlib`: `pip install nbtlib`

## Template pool fallback

All pools use `fallback: minecraft:empty` — a dead end spawns when the pool is exhausted or the jigsaw system can't fit any piece. This is intentional.

## Spawn overrides

`necropolis.json` uses `bounding_box: "piece"` so each piece rolls its own spawn group independently. Mobs require `smooth_corrupted_sourcestone*` floor blocks — the dungeon floor must use these or mobs won't spawn.
