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

### 🔲 hallway
- Straight connecting corridor between rooms
- Short / medium / long variants
- Jigsaws at both ends

### 🔲 small_room
- Basic filler room, ~9×6×9
- 1–2 exits, minor loot chest, basic mob spawner

### 🔲 medium_room
- Mid-sized room, ~13×7×13
- 2–3 exits, more decoration, possible puzzle element

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

## Suggested Additional Rooms

### 🔲 barracks
- Long room with alcoves/bunks, many mob spawners
- Feels like a guard quarters

### 🔲 ritual_chamber
- Circular or octagonal, central altar with soul fire / candles
- Boss-adjacent or mini-boss spawn

### 🔲 flooded_crypt
- Partially waterlogged, coffin-style blocks lining walls
- Drowned / undead themed

### 🔲 library
- Tall room with bookshelves, ladders, a lectern
- Lore drop / spell scroll loot

### 🔲 collapsed_hall
- Partially destroyed hallway — rubble, exposed cave behind broken wall
- Connects optionally to a side cave or dead end

### 🔲 prison_cells
- Two rows of small cells along a corridor
- Mob spawners behind bars

---

## Template Pools

| Pool | Purpose |
|------|---------|
| `necropolis/start` | Start piece → entrance_staircase |
| `necropolis/rooms` | Chaining pool → entrance_staircase (expand later) |
| `necropolis/hallways` | Hallway variants |
| `necropolis/dead_ends` | Treasure room, staff room, flooded crypt, collapsed hall |
| `necropolis/large_rooms` | Large room, ritual chamber, barracks |

---

## Structure JSON
- Type: `ars_zero:necropolis`
- Start pool: `ars_zero:necropolis/start`
- Start jigsaw: `ars_zero:necropolis/entrance`
- Size: 20
- Biome tag: `#ars_zero:has_structure/necropolis` → `ars_zero:blight_forest`
