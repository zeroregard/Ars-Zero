# Making TerraBlender an Optional Dependency

## Problem

TerraBlender is effectively a **hard dependency** right now, even though there is a
runtime guard in `ArsZero.java`:

```java
if (ModList.get().isLoaded("terrablender") && weight > 0) { ... }
```

The guard never fires. `BlightForestRegion` directly extends `terrablender.api.Region`
and imports three TerraBlender classes. The JVM tries to resolve those at class-load
time — before any mod-list check — and throws `NoClassDefFoundError` if the jar is
absent. `build.gradle` also uses `implementation`, which signals to NeoForge that the
dep is required at runtime.

## Goal

Match how Ars Nouveau itself handles TerraBlender: biome works when the mod is present,
world generates without it when it's not.

## Suggested approach

### 1. Gradle — switch to `compileOnly`

```gradle
// build.gradle
compileOnly('com.github.glitchfiend:TerraBlender-neoforge:1.21.1-4.1.0.8')
```

This keeps the classes available at compile time but does **not** add TerraBlender to
the mod's required dependency list.

### 2. Isolate the TerraBlender classes behind a compatibility wrapper

Because the JVM resolves class hierarchies eagerly, `BlightForestRegion` (which extends
`Region`) cannot live in the normal classpath if the dep is optional. The standard
pattern is a **compat class** that is only referenced after the mod-list guard:

```
common/world/biome/
    BlightForestBiome.java          ← keep, no TerraBlender imports
    terrablender/
        TerraBlenderCompat.java     ← new; contains BlightForestRegion inline or calls it
```

`TerraBlenderCompat` is the only class that imports TerraBlender types. It is
**never referenced at the top level** — only inside the `isLoaded` branch:

```java
// ArsZero.java (or a dedicated setup method)
if (ModList.get().isLoaded("terrablender") && weight > 0) {
    TerraBlenderCompat.register(weight);   // class loaded here, not before
    blightForestRegionRegistered = true;
}
```

Because `TerraBlenderCompat` is only touched inside the guard, the JVM won't attempt
to resolve `terrablender.api.Region` unless TerraBlender is actually present.

### 3. `neoforge.mods.toml` — declare as optional

```toml
[[dependencies.ars_zero]]
    modId       = "terrablender"
    type        = "optional"
    versionRange = "[1.21.1-4.1.0.8,)"
    ordering    = "NONE"
    side        = "BOTH"
```

This stops NeoForge / mod launchers from demanding TerraBlender before the game loads.

## Files to touch

| File | Change |
|------|--------|
| `build.gradle` | `implementation` → `compileOnly` |
| `src/main/resources/META-INF/neoforge.mods.toml` | add optional dependency block |
| `src/main/java/…/common/world/biome/BlightForestRegion.java` | move into `terrablender/` compat package (or inline into `TerraBlenderCompat`) |
| `src/main/java/…/ArsZero.java` | reference compat class, not `BlightForestRegion` directly |

## Reference

Ars Nouveau uses exactly this pattern for its own optional TerraBlender support —
worth checking how they split the compat class there for a concrete example.
