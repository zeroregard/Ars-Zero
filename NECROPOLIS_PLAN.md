# Necropolis Structure Plan

## Overview
Underground jigsaw dungeon accessed via a vertical staircase shaft.
Built manually as NBT pieces in-game, wired together via template pools.

---

## Workflow (per piece)
1. Build piece in-game, save as NBT to project root
2. Drop NBT in `/Users/mathiasprivate/Documents/Mods/Ars-Zero/`
3. Claude patches jigsaw data + deploys to `structure/necropolis/`
4. Update relevant template pool JSON if needed
5. Relaunch client, test with `/place structure ars_zero:necropolis ~ ~ ~`

---

## Rooms

### ✅ entrance_staircase
- Vertical shaft, 11×12×11, repeating downward via jigsaw chain
- Bottom jigsaw: `name=necropolis/entrance`, `target=necropolis/connector`, `pool=necropolis/rooms`
- Top jigsaw: `name=necropolis/connector`, `target=necropolis/entrance`, `pool=minecraft:empty`
- Pool: `necropolis/rooms` → chains more staircases

---

### 🔲 entrance_surface
- The above-ground cap sitting at the top of the staircase shaft
- Decorative exterior (archway, ruined stonework, etc.)
- One jigsaw pointing down into the first staircase

### ✅ entrance_bottom
- 19×8×19 antechamber at the base of the staircase chain
- Top jigsaw: `name=necropolis/connector`, `target=necropolis/entrance`, `pool=minecraft:empty`
- 4 side jigsaws (N/S/E/W): `name=necropolis/passage`, `target=necropolis/passage`, `pool=necropolis/hallways`

### ✅ hallway
- Straight connecting corridor between rooms
- Jigsaws at both ends, cobweb processor applied

### ✅ small_room_1
- 19×14×19, loot chest (necropolis_small), cobweb placeholders
- 4 side jigsaws → necropolis/hallways pool

### 🔲 medium_room_1 (WIP)
- 21×13×19, in progress
- 1 entrance jigsaw (passage_medium), reached via small_to_medium connector

### 🔲 large_room
- Big open chamber, ~17×9×17
- 3–4 exits, centrepiece (altar, pit, pillar arrangement)
- Elite mob encounter

### 🔲 treasure_room
- Dead end, no exits beyond entrance
- Loot chests, trapped, well-decorated
- Rare, low weight in pool

### 🔲 staff_room
- Themed around Ars Nouveau — bookshelves, spell-writing desk, source jars
- Contains a guaranteed staff loot chest or pedestal
- Dead end or 1 exit

---

## Template Pools

| Pool | Purpose |
|------|---------|
| `necropolis/start` | Start piece → entrance_staircase |
| `necropolis/rooms` | Chaining pool → entrance_staircase + entrance_bottom |
| `necropolis/hallways` | hallway, small_room_1, dead_end_small, small_to_medium |
| `necropolis/medium_rooms` | medium_room_1 (more to come) |

---

## Structure JSON
- Type: `ars_zero:necropolis`
- Start pool: `ars_zero:necropolis/start`
- Start jigsaw: `ars_zero:necropolis/entrance`
- Size: 20
- Biome tag: `#ars_zero:has_structure/necropolis` → `ars_zero:blight_forest`
