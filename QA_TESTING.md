# QA Testing – Temporal Context Refactor

Manual test checklist for effects impacted by the temporal context refactor. For each item, verify that **Temporal Context Form** correctly targets the entity/block created in the Begin phase when used in Tick or End phases.

---

## Effects That Record Temporal Context

- [ ] **EffectBeam** – Single beam
  - Begin: Touch + Beam | Tick: Temporal Context Form + (effect)
- [ ] **EffectBeam** – Split beams (Augment Split)
  - Begin: Touch + Beam + Split | Tick: Temporal Context Form + (effect)
- [ ] **ConjureVoxelEffect** – Single voxel (Arcane, Water, Fire, etc.)
  - Begin: Touch + Conjure Voxel + variant | Tick: Temporal Context Form + (effect)
- [ ] **ConjureVoxelEffect** – Split voxels (Augment Split)
  - Begin: Touch + Conjure Voxel + Split + variant | Tick: Temporal Context Form + (effect)
- [ ] **EffectGeometrize + Conjure Terrain** – Geometry terrain entity
  - Begin: Touch + Geometrize + Conjure Terrain | Tick: Temporal Context Form + (effect)
- [ ] **EffectGeometrize + Mage Block** – Geometry entity (Break-less)
  - Begin: Touch + Geometrize | Tick: Temporal Context Form + (effect)
- [ ] **EffectGeometrize + Break** – Geometry break entity
  - Begin: Touch + Geometrize + Break | Tick: Temporal Context Form + (effect)
- [ ] **EffectConvergence + Explosion** – Explosion controller entity
  - Begin: Touch + Convergence + Explosion | Tick: Temporal Context Form + (effect)
- [ ] **EffectConvergence + Conjure Water** – Water convergence entity
  - Begin: Touch + Convergence + Conjure Water | Tick: Temporal Context Form + (effect)
- [ ] **EffectConvergence + Player (entity target)** – Player charger entity
  - Begin: Touch + entity (Player) + Convergence | Tick: Temporal Context Form + (effect)
- [ ] **EffectConvergence + Block (Source Jar)** – Source jar charger entity
  - Begin: Touch + block (Source Jar) + Convergence | Tick: Temporal Context Form + (effect)
- [ ] **SelectEffect** – Block group entity
  - Begin: Touch + Select (blocks) | Tick: Temporal Context Form + (effect)

---

## Block Destroy + Anchor Flow (BlockGroup creation)

- [ ] **Break + Anchor** – BlockGroup creation for temporal anchor
  - Begin: Touch + Break (blocks) | Tick/End: Temporal Context Form + Anchor
  - Verify BlockGroup is created and Anchor uses it

---

## Multi-Target / Chaining (Event-per-target)

- [ ] **EffectBurst + effect** – Multiple targets from one Begin
  - Begin: Touch + Burst + (e.g. Break/effect)
  - Verify each burst target is recorded and usable in Tick/End with Temporal Context Form (if applicable)

---

## Phase Coverage

- [ ] **Tick phase** – Temporal Context Form in Tick slot
- [ ] **End phase** – Temporal Context Form in End slot
