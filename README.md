# Ars Noita

A mod focused on staff-based spellcasting with temporal context for Ars Nouveau.

## Overview

Ars Noita introduces a new staff-based spellcasting system that allows spells to be executed in different temporal phases:

- **Begin Phase**: Executes once when the staff is pressed
- **Tick Phase**: Executes every tick while the staff is held
- **End Phase**: Executes once when the staff is released

## Features

### ArsNoitaStaff

A staff item that provides temporal spellcasting capabilities:

- Left-click to begin casting (executes Begin phase spell)
- Hold to continue casting (executes Tick phase spell every tick)
- Release to end casting (executes End phase spell)
- Shift + Right-click to open staff configuration GUI

### TemporalContextForm Glyph

A special form glyph that resolves based on the current staff phase context:

- Can only be used in staff Tick or End phases
- Cannot be used in spellbooks
- Resolves differently based on the current temporal context

### Staff Configuration GUI

A GUI for configuring spells for each phase:

- Three rows for Begin, Tick, and End phases
- Row selection mechanism to choose which phase to configure
- Visual indicators for the currently selected phase

## Technical Details

- **Minecraft Version**: 1.21.1
- **NeoForge Version**: 21.1.200
- **Ars Nouveau Dependency**: 5.8.2+
- **Java Version**: 21

## Development Status

This is an Alpha MVP version focusing on the core staff-based spellcasting system. Future versions will include:

- Conjured entities
- Spell scaling and augments
- Advanced temporal mechanics
- Integration with other Ars mods

## Building

```bash
./gradlew build
```

## License

MIT License
