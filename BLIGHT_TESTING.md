# Blight Voxel & Fluid Testing Guide

## Blight Voxel Entity

### Block Interactions
- **Vegetation Destruction**: Destroys transparent vegetation (short grass, tall grass, ferns, large ferns, dandelions, poppies, sugar cane) ONLY when hitting top side, places fluid on top
  - Handles double-block plants (tall grass, large ferns) by destroying both halves
  - Destroys entire sugar cane columns
- **Leaves Destruction**: Destroys leaves from any side, no fluid placement, vaporization effects (smoke + blight splash particles + sound)
- **Grass to Dirt Conversion**: Converts grass blocks, mycelium, podzol, etc. to dirt ONLY when hitting top side, places fluid on top
  - Also destroys any vegetation above the grass block before converting
- **Mossy Block Conversion**: Converts mossy blocks (cobblestone, stone bricks, stairs, slabs, walls) to regular blocks from any side, places fluid only on top
- **Log Vaporization**: Vaporizes bark when hitting logs (smoke + blight splash particles + sound), places fluid only on top
- **Water Vaporization**: Vaporizes on contact with water blocks/fluid (smoke + blight splash particles + fire extinguish sound), discards in rain
- **Liquid Placement**: Only places blight fluid when hitting the TOP side of a block, never sides or bottom

### Entity Interactions
- **Living Entities**: 
  - Applies magic damage (2.0 * sizeScale)
  - Applies blight/poison effect (duration and amplifier scale with voxel size)
  - Vaporization effects (smoke + blight splash particles + fire extinguish sound)
  - Sets last hurt by mob/player for proper aggro behavior
- **Sheep**: 
  - Shears sheep (removes wool visually)
  - Applies damage and effect
  - Vaporization effects (smoke + blight splash particles + fire extinguish sound)
  - No item drop, no shear sound
- **Buckets**: Converts empty bucket item entities to blight fluid buckets (with bucket fill sound and blight splash particles)
- **Other Voxels**: Blight voxel discards when colliding with water voxel

### Particle & Sound Effects
- **Hit Particles**: 16 blight splash particles (green tinted) on block/entity hit
- **Vaporization Effects**: 12 smoke particles + 8 blight splash particles + fire extinguish sound
- **Custom Particle**: Uses `ModParticles.BLIGHT_SPLASH` with green color (0.42, 1.0, 0.47 RGB)

## Blight Fluid Block

### Properties
- **Flow**: Lava-like flow behavior (tickRate: 30, slopeFindDistance: 4)
- **Entity Effect**: Applies poison effect (100 ticks, level 0) to entities inside
- **No Auto-Conversion**: Does NOT automatically convert to water (no rain interaction, no adjacent water conversion)
- **Prevents Spread in Water**: Skips water blocks when checking adjacent blocks for interactions

### Block Interactions (via Interaction System)
- **Convert to Dirt**: Converts grass blocks, mycelium, podzol, etc. to dirt
- **Convert Mossy**: Converts mossy blocks to regular blocks
- **Destroy Flora**: Destroys vegetation (flowers, grass, ferns, saplings, vines, glow lichen, etc.)
- **Interaction Frequency**: Checks adjacent blocks randomly (1 in 8 chance per tick)
- **On Placement**: Immediately checks adjacent blocks when placed
- **Neighbor Changes**: Checks block below when neighbor changes

### Interactions
- **Water Voxel**: Converts to water when hit by water voxel (smoke particles + fire extinguish sound)
- **Water Blocks**: Prevents blight fluid from spreading into or affecting water blocks

## Water-Blight Interactions

- **Voxel Collision**: Blight voxel discards when colliding with water voxel
- **Fluid Conversion**: Water voxel converts blight fluid blocks to water on hit (smoke particles + fire extinguish sound)
- **Neutralization**: Water neutralizes blight - blight does not spread in water, and water voxels convert blight fluid to water

## Technical Details

### Damage Calculation
- Base damage: 2.0
- Scales with voxel size: `damage = 2.0 * (size / DEFAULT_BASE_SIZE)`

### Effect Application
- Effect type: Envenom (Ars Nouveau effect)
- Duration: 100 ticks (normal) or 160 ticks (burst), scaled by voxel size
- Amplifier: Scales with voxel size
- Applied to: Living entities hit by voxel, entities inside fluid

### Aggro Behavior
- Properly sets `setLastHurtByMob()` and `setLastHurtByPlayer()` to ensure mobs aggro correctly
- Uses stored caster reference for reliable owner tracking
