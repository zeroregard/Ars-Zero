# Ars Zero — Claude Code Reference

## Project Identity

| Field | Value |
|-------|-------|
| Mod name | Ars Zero |
| Mod ID | `ars_zero` |
| Version | 1.11.1 |
| Author | zeroregard |
| License | MIT |
| Minecraft | 1.21.1 |
| Modloader | NeoForge 21.1.219+ |
| Java | 21 |
| Root package | `com.github.ars_zero` |

## Tech Stack & Dependencies

**Required at runtime:**
- Ars Nouveau `1.21.1-5.11.1.1289` — primary magic system this mod extends
- Ars Elemental `0.7.8.4.145` — elemental magic extension
- Curios `9.2.2` — equipment/trinket slots
- TerraBlender `1.21.1-4.1.0.8` — biome blending
- Sauce `0.0.18.57` — utility library (JarJar'd into the jar)
- Nuggets `1.1.0.48` — shared utilities

**Dev/optional:**
- GeckoLib `4.8.3` — entity & item animations
- Patchouli `87-NEOFORGE` — in-game guide book
- JEI `19.21.0.247` — recipe browser (compile-only, optional at runtime)

**Mappings:** Parchment `2024.11.17`

## Build & Run Commands

```bash
./gradlew build                  # Produce release jar
./gradlew runClient              # Launch client dev env
./gradlew runServer              # Launch server dev env
./gradlew runGameTestServer      # Run all 18 GameTests
./gradlew runData                # Generate data (recipes, tags, worldgen)
./gradlew check                  # Run Checkstyle
./gradlew publishToMavenLocal    # Publish to local Maven
```

Data output lands in `src/generated/resources/`. Always run `runData` after changing datagen classes.

## Source Layout

```
src/main/java/com/github/ars_zero/
├── ArsZero.java               # Mod entry point, init hooks
├── api/                       # Public API for other mods
├── client/
│   ├── gui/                   # Staff GUI, radial menu, mana drain overlay
│   ├── renderer/              # Entity/block renderers (GeckoLib)
│   ├── particle/              # Custom particles & particle timelines
│   ├── sound/                 # Sound managers, looping sound instances
│   ├── shader/ & render/      # ArsZeroShaders, ArsZeroRenderTypes
│   └── jei/                   # JEI plugin integration
├── common/
│   ├── entity/                # All custom entities (voxels, geometry, explosions)
│   ├── glyph/                 # Effect glyphs & augments
│   ├── item/                  # Staves, circlets, orbs, parchment items
│   ├── block/                 # Custom blocks + block entities
│   ├── effect/                # Mob effects (ZeroGravity, etc.)
│   ├── spell/                 # Multi-phase cast context system
│   ├── world/                 # Biomes, features, structures, worldgen
│   ├── attachment/            # Entity data attachments
│   ├── network/               # 23 packet types
│   └── datagen/               # Data generators (recipes, tags, worldgen)
├── event/                     # Event handlers (resolver, gravity, discount, etc.)
├── mixin/                     # 6 mixins for Ars Nouveau UI compatibility
└── registry/                  # ModEntities, ModBlocks, ModItems, ModGlyphs, …
```

## Core Systems

### Multi-Phase Spellcasting
The central mechanic. Spells have three phases: `BEGIN`, `TICK`, `END` (enum `SpellPhase`).

Key classes:
- `MultiPhaseCastContext` — holds casting state for one active spell
- `MultiPhaseCastContextMap` / `MultiPhaseCastContextRegistry` — storage & lookup
- Sources: items, Curios equipment, turrets
- Network sync via dedicated packets

### Voxel Entity System (8 elemental types)
Physical projectile-style entities that interact with each other and the world.

| Class | Element | Color |
|-------|---------|-------|
| `ArcaneVoxelEntity` | Arcane | `0x8A2BE2` |
| `FireVoxelEntity` | Fire | — |
| `IceVoxelEntity` | Ice | — |
| `LightningVoxelEntity` | Lightning | — |
| `WaterVoxelEntity` | Water | — |
| `StoneVoxelEntity` | Stone | — |
| `WindVoxelEntity` | Wind | — |
| `BlightVoxelEntity` | Blight | special (terrain corruption) |

All extend `BaseVoxelEntity` (which extends Projectile). Interactions between elements are registered in `VoxelInteractionRegistry` (21 interaction types). GeckoLib drives animations.

### Custom Glyphs / Effects (11 types)
Located in `common/glyph/effect/`:
- `AnchorEffect` — keeps lifespan-based effects alive
- `SustainEffect` — alternative sustain
- `ConjureVoxelEffect` — spawns voxel entities
- `EffectBeam` — beam effect
- `EffectConjureBlight` — blight voxels
- `ZeroGravityEffect` — removes gravity from targets
- `PushEffect` — knockback
- `SelectEffect` — targeting
- `DiscardEffect` — disposal
- `TemporalContextForm` — form spell for temporal context
- `NearForm` — proximity form

### Custom Augments (8 types)
Shape: `AugmentHollow`, `AugmentSphere`, `AugmentCube`, `AugmentFlatten`
AOE: `AugmentAOETwo`, `AugmentAOEThree`
Amplify: `AugmentAmplifyTwo`, `AugmentAmplifyThree`

### Staff System (4 tiers)
All extend `AbstractSpellStaff` which extends Ars Nouveau's `AbstractStaff`:
- `NoviceSpellStaff` (T1)
- `MageSpellStaff` (T2)
- `ArchmageSpellStaff` (T3)
- `CreativeSpellStaff` (unlimited)

GUI: `ArsZeroStaffGUI`. Sound: `StaffSoundManager` with looping instances.

### Geometry System
`GeometryEntity` base → specialized renderers for geometry, break, terrain, and process variants.

### Explosion System
Multiple `ExplosionControllerEntity` variants. Sounds have phases: charge → prime → activate → resolve → idle.

### World Generation
- **Blight Forest biome** — custom terrain with blighted blocks
- **Blighted Cauldron** — interactive block with special recipes
- **Voxel Spawner Block** — spawns voxels for automation/testing
- Data driven via `WorldgenProvider` + `StructureDatagen`

## Registry Pattern

All registrations live in `registry/`:
- `ModEntities` — entity types
- `ModBlocks` — blocks + block entities
- `ModItems` — items
- `ModGlyphs` — glyphs / augments
- `ModEffects` — mob effects
- `ModFluids` — fluid types
- `ModSounds` — sound events

Register new features here before using them anywhere else.

## Code Conventions

- **Classes:** PascalCase (`ArcaneVoxelEntity`)
- **Constants:** `UPPER_SNAKE_CASE` (`DEFAULT_LIFETIME_TICKS`, `COLOR`)
- **Methods/fields:** camelCase
- **Packets:** named `Packet<Feature>` (e.g. `PacketStaffSlotSelect`)
- Annotations: `@Nullable` / `@NotNull` (Jetbrains) on all public APIs
- Checkstyle enforces **no unused imports** — always clean up imports
- Prefer composition via attachments over inheritance for entity data

## Networking (23 packet types)

All packets are in `common/network/`. Categories:
- Staff slot selection & sound configuration
- Multi-phase device scroll / slot setting
- Entity movement & cancellation
- Mana drain visualization
- Explosion effects & sound
- Curio casting input
- Clipboard and parchment operations

## Testing

Framework: NeoForge **GameTest** (`src/test/java/com/arszero/tests/`, 18 test classes).

Notable helpers:
- `VoxelTestUtils` — spawn & tick voxels in test environments
- `FriendlyTestReporter` — human-readable failure messages
- `TestRegistrationFilter` — run a subset of tests

Test structures live in `src/test/resources/data/ars_zero/structure/` and are copied to `run/gameteststructures/` at test time.

Run with: `./gradlew runGameTestServer`

## Common Task Recipes

### Add a new glyph/effect
1. Create class in `common/glyph/effect/` implementing the Ars Nouveau glyph interface
2. Register in `ModGlyphs`
3. Add recipe in `GlyphRecipeDatagen`
4. Run `./gradlew runData`

### Add a new voxel type
1. Extend `BaseVoxelEntity` in `common/entity/voxel/`
2. Register entity type in `ModEntities`
3. Create renderer extending the base voxel renderer in `client/renderer/`
4. Register interactions in `VoxelInteractionRegistry`
5. Add GeckoLib animation controller if needed

### Add a new augment
1. Create class in `common/glyph/` (shape or AOE category)
2. Register in `ModGlyphs`
3. Add recipe in `GlyphRecipeDatagen`

### Add a new packet
1. Create class in `common/network/`
2. Register it in the network registration setup
3. Handle on both sides (client handler / server handler)

### Add/modify world generation
1. Edit `WorldgenProvider` or `StructureDatagen` in `common/datagen/`
2. Run `./gradlew runData` to regenerate JSON files

## Mixin Notes

Only 6 mixins exist — kept minimal intentionally:
- **Common:** `BlankParchmentItemMixin`, `ScribesBlockMixin`
- **Client:** `EnchantingApparatusRecipeCategoryMixin`, `GlyphButtonMixin`, `GuiSpellBookMixin`, `RecipeLayoutMixin`

Only add mixins when there is no other way to hook into Ars Nouveau behavior.
