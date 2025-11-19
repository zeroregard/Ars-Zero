This mod adds a new caster item to Ars Nouveau; the Spell Staff. Unlike the Spell Book, the Staff allows you to continously cast spells as you hold down 'use'. This opens up for new scenarios that were previously not possible. To support such new scenarios, it's necessary to add a new cast method 'Temporal Context Form' and new effects that work in this context.

*   Items:
    *   **Creative/Archmage/Mage/Novice Spell Staff:** Has 3 spell phases (begin, tick, end) each with 10 glyph slots. Dyable.
    *   **Psion's Circlet:** A wearable circlet (Curios head slot) with 3 spell phases (begin, tick, end) each with 10 glyph slots. Tier 3 (Archmage). Cast spells by holding the channel key while wearing it. Dyable.
*   Glyphs: 
    *   **Temporal Context Form:** Resolve spell at previous spell phase output. Only usable in the spell staff's 'tick' or 'end' phases
    *   **Near Form**: Cast 1 block ahead of your look direction
    *   **Push:** Works just like pull, but in the opposite direction. Useful for pushing away objects to where you look
    *   **Select**: Selects blocks/entities - mostly used to 'mark' objects for Temporal Context Form, in the begin phase.
    *   **Conjure Voxel**: Create a small block arcane block entity of mana. Has alternative forms (Fire, Water) depending on which effects come directly after \[Ignite, Conjure Water\]
    *   **Anchor**: Moves entities/blocks in relation to the caster
    *   **Remove Gravity**: Sets a (living) entity to have no gravity for a duration of time.

Voxels have different effects depending on your **Elemental Power** related to the school of a summoned voxel. For example, fire voxels are harder to extinguish when exposed to water, if you have more Fire Power.