# Stone Voxel Interaction Notes

## Fire and Water Voxel Reference
- **Fire Voxel**: Floats without gravity, ignites or evaporates blocks it collides with, primes TNT, activates portals, and shrinks when drenched (see `FireVoxelEntity`).
- **Water Voxel**: Removes fire, hydrates farmland, fills cauldrons, leaves behind source blocks based on size, and evaporates in hot dimensions while respecting player water power (see `WaterVoxelEntity`).
- Both variants rely on `Conjure Voxel` + imbued effects (`Ignite` or `Conjure Water`) and respect spell stats such as Amplify, Split, and Extend Time.

## Stone Voxel Behaviour (implemented)
- Triggered by `Conjure Voxel` + the new `Conjure Terrain` effect.
- Heavier projectile that keeps gravity, reinforcing struck surfaces with stone/cobblestone and building a small buttress on entity impact.
- Applies kinetic damage when colliding with living targets; damage scales with voxel size and travel speed, rewarding launch-focused builds.

## Interaction Suggestions
### With Other Voxels
- **Fire + Stone**: Superheat the stone shell to drop magma blocks or temporarily set the Stone Voxel ablaze, trading durability for extra damage over time.
- **Water + Stone**: Quench the stone to generate mossy cobblestone or slow nearby entities via temporary mud/slick debuffs.
- **Arcane + Stone**: Allow Arcane Voxels to phase through and merge, transferring stored spell context into the Stone Voxel to trigger delayed explosions or scripted movements.

### With Blocks/Terrain
- Reinforce existing structures by granting temporary blast resistance bonuses to blocks touched by the Stone Voxel.
- When landing on sand or gravel, convert a wider patch into hardened sandstone, preventing cave-ins.
- Colliding with ore blocks could spawn loose shards (items) proportional to impact speed, giving builders a resource-harvest playstyle.

### With Entities
- High-velocity hits already inflict blunt damage; consider adding knockdown or stagger effects on large mobs.
- If the target is burning, have the stone shatter into basalt shards for bonus area damage.
- Shield-bearing entities could have their guard broken faster, encouraging Stone Voxels as anti-tank tools.
