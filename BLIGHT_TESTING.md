# Blight Voxel & Fluid Testing Guide

## Blight Voxel Entity

### Block Interactions
- **Vegetation Destruction**: Destroys transparent vegetation (short grass, tall grass, ferns, large ferns, dandelions, poppies, sugar cane) ONLY when hitting top side, places fluid on top
- **Leaves Destruction**: Destroys leaves from any side, no fluid placement
- **Grass to Dirt Conversion**: Converts grass blocks, mycelium, podzol, etc. to dirt ONLY when hitting top side, places fluid on top
- **Mossy Block Conversion**: Converts mossy blocks to regular blocks from any side, places fluid only on top
- **Log Vaporization**: Vaporizes bark when hitting logs (smoke particles + sound), places fluid only on top
- **Water Vaporization**: Vaporizes on contact with water blocks/fluid (smoke particles + sound), discards in rain
- **Liquid Placement**: Only places blight fluid when hitting the TOP side of a block, never sides or bottom

### Entity Interactions
- **Living Entities**: Applies magic damage and blight effect, vaporization effects (smoke particles + sound)
- **Sheep**: Shears sheep (removes wool visually), applies damage and effect, vaporization effects (smoke + splash particles + sound), no item drop, no shear sound
- **Buckets**: Converts empty bucket item entities to blight fluid buckets (with sound and particles)
- **Other Voxels**: Blight voxel discards when hitting water voxel

## Blight Fluid Block

### Properties
- **Flow**: Lava-like flow behavior (tickRate: 30, slopeFindDistance: 4)
- **Entity Effect**: Applies poison effect to entities inside
- **No Auto-Conversion**: Does NOT automatically convert to water (no rain, no adjacent water conversion)

### Interactions
- **Water Voxel**: Converts to water when hit by water voxel (smoke particles + fire extinguish sound)
- **Block Interactions**: Affects adjacent blocks (dirt, mossy blocks, flora) via interaction system

## Water-Blight Interactions

- **Voxel Collision**: Blight voxel discards when colliding with water voxel
- **Fluid Conversion**: Water voxel converts blight fluid blocks to water on hit

