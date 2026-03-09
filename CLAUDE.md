# CLAUDE.md — Ars Zero

This file provides guidance for AI assistants working in this repository.

## Project Overview

**Ars Zero** is a Minecraft NeoForge mod (version 1.11.1) that extends the [Ars Nouveau](https://www.curseforge.com/minecraft/mc-mods/ars-nouveau) spellcasting mod. It adds staff-based spellcasting with temporal context, elemental voxel entities, element-interaction mechanics, new glyphs, mob effects, and multiblock structures.

- **Minecraft:** 1.21.1
- **NeoForge:** 21.1.219
- **Java:** 21
- **Mod ID:** `ars_zero`
- **Root package:** `com.github.ars_zero`

## Build System

The project uses **Gradle** with the NeoForge ModDev plugin. Always use the Gradle wrapper:

```bash
./gradlew build              # Compile and package the mod JAR
./gradlew checkstyleMain     # Run code style checks (must pass before committing)
./gradlew runData            # Re-generate data (recipes, tags, etc.) into src/generated/
```

Key Gradle properties live in `gradle.properties`. All dependency versions are defined there — do not hardcode version strings in `build.gradle`.

The final artifact is versioned as `ars_zero-${minecraft_version}-${mod_version}.jar`.

## Running & Testing

### Dev environments

```bash
./gradlew runClient          # Launch a dev Minecraft client
./gradlew runServer          # Launch a dev Minecraft server (headless)
./gradlew runBotClient       # Bot testing client (runs in run-bot/)
```

### Game tests (primary test mechanism)

NeoForge GameTest is the test framework. Tests run inside the Minecraft server environment.

```bash
./gradlew runGameTestServer                                  # Run all tests
./gradlew runGameTestServer -Dars_zero.testFilter=TestName   # Run a specific test
```

Test source: `src/test/java/` — entry point is `ArsZeroTestsMod.java`.

Test structures (NBT/SNBT) live in `src/test/resources/data/ars_zero/structure/`. The `runGameTestServer` task automatically copies structures to `run/gameteststructures/ars_zero/` before executing.

There are no standard JUnit tests; all behavioural testing is done via GameTest.

## Code Style & Conventions

### Checkstyle

Only one rule is enforced: **no unused imports** (`config/checkstyle/checkstyle.xml`).

Run `./gradlew checkstyleMain` and fix all reported violations before committing. The build will fail if checkstyle reports errors.

### General conventions

- **Encoding:** UTF-8 everywhere.
- **Line endings:** LF (Unix) — enforced via `.gitattributes`.
- **Java version:** Use Java 21 language features where appropriate.
- **Client/server separation:** Client-only code lives under `com.github.ars_zero.client`. Never reference client classes from `common` or top-level packages without a dist-check (`FMLEnvironment.dist.isClient()`).
- **No star imports.** All imports must be explicit (checkstyle will catch unused ones).
- **Registry pattern:** All registerable content (blocks, items, entities, glyphs, effects, etc.) must be declared in the corresponding `registry/Mod*.java` class and registered in `ArsZero.java` constructor via `DeferredRegister`.
- **Resource locations:** Use `ArsZero.prefix("path")` to create `ResourceLocation` values namespaced under `ars_zero`.

## Architecture

### Package layout

```
com.github.ars_zero/
├── ArsZero.java              # @Mod entry point — registers everything
├── client/                   # Client-only: rendering, GUI, particles, input, JEI, sounds
├── common/
│   ├── entity/               # Voxel entities + their interaction handlers
│   │   └── interaction/      # VoxelInteractionRegistry + per-pair interaction classes
│   ├── block/                # Custom blocks and tile entities
│   ├── item/                 # Custom items (staves, multi-block items)
│   ├── glyph/                # Spell glyphs (augments, geometrize, convergence)
│   ├── effect/               # Mob effects
│   ├── gravity/              # Zero-gravity system
│   ├── event/                # Server-side event handlers (anchor, gravity, discount)
│   ├── spell/ & casting/     # Spell mechanics
│   ├── crafting/recipes/     # Custom recipe types
│   ├── network/              # Networking.java — packet registration
│   ├── config/               # ServerConfig.java — server-side config
│   ├── fluid/                # Custom fluids
│   ├── particle/timeline/    # Particle timeline animations
│   ├── util/                 # Utility classes
│   ├── structure/            # Convergence structure helpers
│   └── datagen/              # Data generators (recipes)
├── event/                    # Top-level event handlers (Curios integration, resolver events)
├── registry/                 # Mod*.java — all DeferredRegister declarations
└── mixin/                    # Mixin classes (6 client, 2 shared)
```

### Voxel entity system

Voxel entities (Fire, Water, Ice, Wind, Stone, Lightning, Arcane, Blight) are the central gameplay mechanic. Each extends a base voxel entity class and is registered in `ModEntities`.

Interactions between two voxel types are handled by `VoxelInteractionRegistry`. To add a new interaction:
1. Create a class implementing the interaction interface in `common/entity/interaction/`.
2. Register it in `ArsZero.registerVoxelInteractions()` with the two entity classes as keys.
3. Add a corresponding test in `src/test/java/`.

### Glyph system

Glyphs are registered in `ModGlyphs.java` via `ArsNouveauAPI`. Optional augment compatibility is added in `ModGlyphs.addOptionalAugmentCompatibility()`, called during `FMLCommonSetupEvent`.

### Event buses

- **`modEventBus`** — mod lifecycle events (`FMLCommonSetupEvent`, `GatherDataEvent`, `RegisterEvent`, etc.). Listeners are registered in the `ArsZero` constructor.
- **`NeoForge.EVENT_BUS`** — game events. Listeners are registered via `NeoForge.EVENT_BUS.register(...)` in the constructor.

### Mixins

Mixin config: `src/main/resources/ars_zero.mixins.json`. All mixin classes live in `com.github.ars_zero.mixin`. Client-only mixins must be annotated appropriately and guarded by environment.

### Data generation

Run `./gradlew runData` to regenerate resources. Output goes to `src/generated/resources/`, which is included in the main source set. Commit generated files when they change.

## Dependencies & Repositories

All versions are in `gradle.properties`. Key runtime dependencies:

| Dependency | Role |
|---|---|
| `ars_nouveau` | Core spellcasting API (mandatory) |
| `ars_elemental` | Elemental magic extensions (transitive disabled) |
| `sauce` | Utility library (jar-jar embedded) |
| `geckolib` | Entity animations |
| `curios` | Curio slots (mandatory) |
| `jei` | Recipe viewer (compileOnly + runtimeOnly) |
| `patchouli` | In-game documentation book (runtimeOnly) |
| `nuggets` | Utility (compileOnly) |

`sauce` is jar-jar embedded — no separate installation required for users.

## Adding New Content

### New voxel entity
1. Create entity class in `common/entity/` extending the appropriate base.
2. Register in `registry/ModEntities.java`.
3. Add renderer in `client/renderer/entity/`.
4. Register interactions in `ArsZero.registerVoxelInteractions()`.
5. Add spawner block if needed in `common/block/` and `registry/ModBlocks.java`.
6. Write GameTests in `src/test/java/`.

### New glyph
1. Create glyph class in `common/glyph/` (augment, geometrize, or convergence).
2. Register in `registry/ModGlyphs.java`.
3. Add recipe via datagen in `common/datagen/GlyphRecipeDatagen.java` and re-run `runData`.
4. Add Patchouli documentation entry in `src/main/resources/assets/ars_zero/patchouli_books/worn_notebook/en_us/entries/glyphs_1/`.

### New item or block
1. Declare in the appropriate `registry/Mod*.java`.
2. Register the `DeferredRegister` in `ArsZero.java` constructor (already done for existing registries).
3. Add model JSON in `src/main/resources/assets/ars_zero/models/`.
4. Add recipe via datagen and re-run `runData`.

## Configuration

Server-side config is defined in `common/config/ServerConfig.java` using NeoForge's `ModConfigSpec`. It is registered in the `ArsZero` constructor:

```java
modContainer.registerConfig(ModConfig.Type.SERVER, ServerConfig.SERVER_CONFIG);
```

## Versioning

Version format: `<minecraft_version>-<mod_version>` (e.g. `1.21.1-1.11.1`).

To bump the mod version, edit `mod_version` in `gradle.properties`. Do not change `minecraft_version` unless actually updating the MC target.

## Common Pitfalls

- **Unused imports** will fail the checkstyle task — always clean imports.
- **Client code in common** will crash dedicated servers. Always guard with `FMLEnvironment.dist.isClient()` or use `DistExecutor`.
- **`src/generated/resources/`** is auto-generated — do not manually edit files there. Re-run `runData` instead.
- **Test structures** must be present at `run/gameteststructures/ars_zero/` before tests run. The Gradle task handles this automatically, but manual test runs from an IDE require copying them manually.
- **`sauce` is jar-jar embedded** — do not add it as a separate mod dependency in test environments; it is included in the mod JAR.
- The Gradle daemon is disabled (`org.gradle.daemon=false`) intentionally — do not re-enable it.
