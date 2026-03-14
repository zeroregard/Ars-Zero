---
name: Voxel entities
description: Rules for editing voxel entity classes and their interactions
paths:
  - src/main/java/com/github/ars_zero/common/entity/voxel/**
  - src/main/java/com/github/ars_zero/common/entity/BaseVoxelEntity.java
  - src/main/java/com/github/ars_zero/registry/VoxelInteractionRegistry.java
---

# Voxel Entity Rules

## Structure

All voxel entities extend `BaseVoxelEntity` (which extends `Projectile`).
8 elemental types: Arcane, Fire, Ice, Lightning, Water, Stone, Wind, Blight.

## Adding a new voxel type

1. Extend `BaseVoxelEntity` in `common/entity/voxel/`
2. Register entity type in `ModEntities`
3. Create renderer extending the base voxel renderer in `client/renderer/`
4. Register element interactions in `VoxelInteractionRegistry` (currently 21 interaction types)
5. Add GeckoLib animation controller if the voxel needs custom animation

## Interaction registry

Cross-element interactions are data-driven in `VoxelInteractionRegistry`. When two voxels collide, their element pair is looked up here. Always register both directions (A→B and B→A) unless the interaction is asymmetric by design.

## BlightVoxelEntity

Special case — causes terrain corruption on block hit. Does not follow the standard damage model. Check `BlightVoxelEntity` before assuming voxel behavior is uniform.
