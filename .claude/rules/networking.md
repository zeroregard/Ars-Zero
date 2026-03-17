---
name: Networking
description: Rules for editing packet classes and network registration
paths:
  - src/main/java/com/github/ars_zero/common/network/**
---

# Networking Rules

## Overview

23 packet types in `common/network/`. Categories:
- Staff slot selection & sound configuration
- Multi-phase device scroll / slot setting
- Entity movement & cancellation
- Mana drain visualization
- Explosion effects & sound
- Curio casting input
- Clipboard and parchment operations

## Adding a new packet

1. Create class in `common/network/` — named `Packet<Feature>`
2. Register in the network registration setup
3. Handle on **both** sides (client handler AND server handler) — never assume one side only

## Conventions

- Packets are named `Packet<Feature>` (e.g. `PacketStaffSlotSelect`)
- Keep packet payloads minimal — send IDs/primitives, not full objects
- Server-bound packets must validate all data — never trust client input for game logic
