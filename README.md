This mod adds a new caster item to Ars Nouveau; the Spell Staff. Unlike the Spell Book, the Staff allows you to continously cast spells as you hold down 'use'. This opens up for new scenarios that were previously not possible. To support such new scenarios, it's necessary to add a new cast method 'Temporal Context Form' and new effects that work in this context.

## Items

*   **Creative/Archmage/Mage/Novice Spell Staff:** Has 3 spell phases (begin, tick, end) each with 10 glyph slots. Dyable.
*   **Psion's Circlet:** A Tier 3 wearable circlet (Curios head slot) with 3 spell phases (begin, tick, end) each with 10 glyph slots. Cast spells by holding the channel key while wearing it.

## Glyphs

*   **Temporal Context Form:** Resolve spell at previous spell phase output. Only usable in the spell staff's 'tick' or 'end' phases
*   **Near Form**: Cast 1 block ahead of your look direction
*   **Push:** Works just like pull, but in the opposite direction. Useful for pushing away objects to where you look
*   **Select**: Selects blocks/entities - mostly used to 'mark' objects for Temporal Context Form, in the begin phase.
*   **Conjure Voxel**: Create a small block-like entity of mana. Has alternative elemental forms depending on which effects come directly after it (see Voxels section below)
*   **Anchor**: Moves entities/blocks in relation to the caster
*   **Remove Gravity**: Sets a (living) entity to have no gravity for a duration of time.

## Voxels

Voxels are small block-like entities created by the **Conjure Voxel** glyph. They can be created in different elemental variants by placing specific glyphs directly after Conjure Voxel:

*   **Arcane Voxel** (default): Created with just Conjure Voxel. Can carry a spell resolver to cast spells when it hits targets.
*   **Fire Voxel**: Created with Conjure Voxel + Ignite. Interacts with fire-related blocks and entities. Vulnerable to water and rain.
*   **Water Voxel**: Created with Conjure Voxel + Conjure Water. Interacts with water-related blocks and can be collected. Vulnerable to hot environments.
*   **Wind Voxel**: Created with Conjure Voxel + Wind Shear. Floats and pushes entities. Creates explosive reactions with fire.
*   **Stone Voxel**: Created with Conjure Voxel + Conjure Terrain. Breaks fragile blocks and deals impact damage.
*   **Ice Voxel**: Created with Conjure Voxel + Cold Snap. Freezes water and breaks fragile blocks.
*   **Lightning Voxel**: Created with Conjure Voxel + Discharge. Deals electrical damage and effects.

### Voxel Interactions

When voxels collide with each other, they produce elemental interactions. Opposing elements cancel each other out, while compatible elements may merge or combine.

### Elemental Power

Your **Elemental Power** affects how voxels interact with environmental hazards. Higher power in a voxel's element increases its resistance to opposing forces.
