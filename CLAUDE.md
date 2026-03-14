# Ars Zero — Claude Code Reference

## Project Identity

| Field | Value |
|-------|-------|
| Mod name | Ars Zero |
| Mod ID | `ars_zero` |
| Version | 1.11.1 |
| Author | zeroregard |
| Minecraft | 1.21.1 / NeoForge 21.1.219+ / Java 21 |
| Root package | `com.github.ars_zero` |

## Dependencies

**Runtime:** Ars Nouveau `1.21.1-5.11.1.1289`, Ars Elemental `0.7.8.4.145`, Curios `9.2.2`, TerraBlender `1.21.1-4.1.0.8`, Sauce `0.0.18.57` (JarJar'd), Nuggets `1.1.0.48`

**Dev/optional:** GeckoLib `4.8.3`, Patchouli `87-NEOFORGE`, JEI `19.21.0.247` (compile-only)

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

**Multi-Phase Spellcasting** — central mechanic. Phases: `BEGIN`, `TICK`, `END` (`SpellPhase` enum). Key: `MultiPhaseCastContext`, `MultiPhaseCastContextMap`, `MultiPhaseCastContextRegistry`.

**Voxel Entity System** — 8 elemental types (`ArcaneVoxelEntity` … `BlightVoxelEntity`), all extend `BaseVoxelEntity` (extends Projectile). 21 cross-element interactions in `VoxelInteractionRegistry`. GeckoLib animations. See `.claude/rules/voxels.md`.

**Custom Glyphs (11)** — in `common/glyph/effect/`: `AnchorEffect`, `SustainEffect`, `ConjureVoxelEffect`, `EffectBeam`, `EffectConjureBlight`, `ZeroGravityEffect`, `PushEffect`, `SelectEffect`, `DiscardEffect`, `TemporalContextForm`, `NearForm`.

**Custom Augments (8)** — Shape: `AugmentHollow/Sphere/Cube/Flatten`. AOE: `AugmentAOETwo/Three`. Amplify: `AugmentAmplifyTwo/Three`.

**Staff System (4 tiers)** — `NoviceSpellStaff` → `ArchmageSpellStaff` → `CreativeSpellStaff`, all extend `AbstractSpellStaff`. GUI: `ArsZeroStaffGUI`. Sound: `StaffSoundManager`.

**Geometry System** — `GeometryEntity` base → geometry, break, terrain, process renderers.

**Explosion System** — `ExplosionControllerEntity` variants. Sound phases: charge → prime → activate → resolve → idle.

**World Generation** — Blight Forest biome, Blighted Cauldron, Voxel Spawner Block. See `.claude/rules/structures.md` and `.claude/necropolis.md` for the Necropolis dungeon.

## Registry Pattern

All registrations in `registry/` (register before use):
`ModEntities`, `ModBlocks`, `ModItems`, `ModGlyphs`, `ModEffects`, `ModFluids`, `ModSounds`

## Common Task Recipes

**Add a glyph/effect:** `common/glyph/effect/` → `ModGlyphs` → `GlyphRecipeDatagen` → `runData`

**Add a voxel type:** `common/entity/voxel/` → `ModEntities` → renderer → `VoxelInteractionRegistry`

**Add an augment:** `common/glyph/` → `ModGlyphs` → `GlyphRecipeDatagen`

**Add a packet:** `common/network/` → register → handle both sides. See `.claude/rules/networking.md`.

**Add/modify worldgen:** Edit `WorldgenProvider` in `common/datagen/` → `runData`

## Testing

NeoForge **GameTest** in `src/test/java/com/arszero/tests/` (18 test classes).
Helpers: `VoxelTestUtils`, `FriendlyTestReporter`, `TestRegistrationFilter`.
Structures: `src/test/resources/data/ars_zero/structure/` → copied to `run/gameteststructures/`.

## Mixin Notes

6 mixins, kept minimal intentionally:
- **Common:** `BlankParchmentItemMixin`, `ScribesBlockMixin`
- **Client:** `EnchantingApparatusRecipeCategoryMixin`, `GlyphButtonMixin`, `GuiSpellBookMixin`, `RecipeLayoutMixin`

Only add mixins when no other hook into Ars Nouveau behavior exists.

## Scoped Rules

Path-specific guidance lives in `.claude/rules/`:
- `necropolis.md` — living necropolis design doc (always loaded)
- `structures.md` — necropolis NBT pieces, template pools, jigsaw debugging (loads when editing structure files)
- `voxels.md` — voxel entity system
- `networking.md` — packet conventions

Scripts for structure inspection/patching: `.claude/scripts/`
