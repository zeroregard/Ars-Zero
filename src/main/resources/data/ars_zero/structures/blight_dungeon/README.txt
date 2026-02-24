Blight Dungeon structure templates (NBT)
========================================

These structure pieces are used by the Blight Dungeon jigsaw structure in the
Blight Forest biome. Create them in-game with Structure Blocks and export
the .nbt files here.

Required files (referenced by template pools):
- entrance.nbt   Start piece: surface ruin + staircase/shaft down; add Jigsaw
                 blocks at the bottom connecting to "ars_zero:blight_dungeon/rooms".
- room_small.nbt Small dungeon cell: walls/floor/ceiling in SourceStone only;
                 optional spawner/chest; Jigsaw for rooms or repurpose.
- repurpose_room.nbt Room reserved for future use (no end portal). Same palette;
                 leave center/platform for a later mechanic.

Block palette (SourceStone only):
  Use only Ars Nouveau SourceStone blocks so the structure can later be
  swapped to custom blocks in one go (e.g. via a structure processor or
  by rebuilding NBTs). Do not use Blackstone or other non-SourceStone blocks.
- ars_nouveau:sourcestone_large_bricks
- ars_nouveau:sourcestone
- ars_nouveau:smooth_sourcestone_large_bricks
- ars_nouveau:sourcestone_small_bricks (and other ars_nouveau:sourcestone_*
  variants from LibBlockNames as needed for variety)

Future: The entire palette may be replaced with new custom blocks (e.g. 
ars_zero:dungeon_brick). Keeping a single, documented block set now makes
that replacement straightforward later.

Jigsaw names: use pool "ars_zero:blight_dungeon/rooms" (and optionally
"ars_zero:blight_dungeon/start") as needed so pieces connect. Keep depth small
for now (entrance + 1–2 room types); add hallways/rooms later by extending pools.

Placeholder NBT files (entrance.nbt, room_small.nbt, repurpose_room.nbt) are
included: each is a small 5x4x5 box of sourcestone_large_bricks so the
structure generates visibly at /locate. Replace them with full pieces (staircase,
rooms with Jigsaw blocks, etc.) built in creative with Structure Blocks; save
and copy the .nbt into this folder (or data/ars_zero/structure/blight_dungeon/
if your version uses the singular path).

Troubleshooting (structure not found / nothing at /locate coords):
- Rebuild the mod (./gradlew build) so the NBT files are in the jar or run
  resources; then use a NEW world or travel to chunks that have never been
  generated (so the structure can place).
- The structure generates only in ars_zero:blight_forest and minecraft:forest.
  Use /locate structure ars_zero:blight_dungeon then go to the coords; the
  entrance is a 5x4x5 SourceStone box at the surface.
- If /locate says "no matching structure", the structure set may not be
  registered for your dimension (e.g. some dedicated server setups).
