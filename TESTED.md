## Tested

- [x] Multi phase parchment can be created from Ctrl+C w/ blank parchment
- [ ] Multi phase parchment can be applied to turret
- [x] Mult phase copy paste from one device to another, also with Cmd+C - Cmd+V
- [x] Adams AOE/Amp show up
- [x] Adams AOE/Amp work for sure in spells
- [x] Default cooldown config

---

## How to copy a multiphase slot onto a parchment

**You need:** Staff (or circlet equipped) in one hand, **Blank Parchment** or **Spell Parchment** (Ars Nouveau) in the **other** hand.

1. Hold the staff in main hand, and a **Blank Parchment** (or Spell Parchment) in off-hand.
2. Open the staff GUI (right-click with staff, or use the key that opens it).
3. Click the spell slot you want to save (one of the 10 slots on the right) so it’s selected.
4. **Copy** that slot:
   - Right-click the slot → **Copy**, or  
   - **Ctrl+C** (Cmd+C on Mac).
5. One parchment is consumed and you receive **one Multiphase Spell Parchment** with that slot’s data (begin/tick/end, name, delay, style).

If the other hand already has a **Multiphase Spell Parchment**, Copy writes the slot data onto that item instead (no conversion).

---

## Scribes table (Ars Nouveau): staff/parchment → multiphase parchment

Same result as above, but using the table so it matches how players put single-spell onto parchment in Ars Nouveau:

1. Put your **multiphase staff** (or circlet) **on the Scribes table** (place the item on the table).
2. Hold a **Blank Parchment** or **Spell Parchment** (Ars Nouveau) in your hand.
3. **Shift + right-click** the table.
4. One parchment is consumed; the table now has a **Multiphase Spell Parchment** with the **first non-empty slot** from the device. The staff (or circlet) is returned to your inventory (or dropped if full).

You can use either this Scribes table flow or the off-hand Copy flow; both are supported.

**Note:** In Ars Nouveau, putting a single-spell onto a parchment is done at the **Scribes table** (blank parchment + spell book there), not by copy-paste in the book GUI.
