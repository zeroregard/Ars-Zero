#!/usr/bin/env python3
"""
Generate a flat Minecraft world with one command block per structure piece,
laid out in a row. Each command block is pre-filled with:
    /place structure ars_zero:necropolis/<nbt> ~ ~ ~

Usage:
    python3 scripts/generate_structure_world.py
    ./gradlew generateStructureWorld

Config: structure_layout.json (project root)
Output: run/saves/Structure Layout/
"""

import gzip
import json
import math
import os
import shutil
import struct
import zlib
from pathlib import Path

# ---------------------------------------------------------------------------
# Paths (relative to project root)
# ---------------------------------------------------------------------------
SCRIPT_DIR   = Path(__file__).resolve().parent
PROJECT_ROOT = SCRIPT_DIR.parent
CONFIG_FILE  = PROJECT_ROOT / "structure_layout.json"
NBT_DIR      = PROJECT_ROOT / "src/main/resources/data/ars_zero/structure/necropolis"
WORLD_DIR    = PROJECT_ROOT / "run/saves/Structure Layout"
WORLD_NAME   = "Structure Layout"
DONOR_WORLD  = PROJECT_ROOT / "run/saves/Building Blocks"   # existing world to borrow level.dat from

# Y levels
FLOOR_Y   = 63   # stone floor
CMD_Y     = 64   # command block / structure block row
BTN_Y     = 65   # button on top
SIGN_Y    = 65   # wall sign at same Y as button, on side face

# Gap between pieces (in blocks, added to the widest piece's X)
GAP = 8  # blocks between pieces (after the widest piece's X)

# ---------------------------------------------------------------------------
# Minimal NBT encoder (big-endian, no external deps)
# ---------------------------------------------------------------------------
TAG_END       = 0
TAG_BYTE      = 1
TAG_SHORT     = 2
TAG_INT       = 3
TAG_LONG      = 4
TAG_FLOAT     = 5
TAG_DOUBLE    = 6
TAG_BYTE_ARR  = 7
TAG_STRING    = 8
TAG_LIST      = 9
TAG_COMPOUND  = 10
TAG_INT_ARR   = 11
TAG_LONG_ARR  = 12


def _enc_str(s: str) -> bytes:
    b = s.encode("utf-8")
    return struct.pack(">H", len(b)) + b


def _enc_payload(tag_id: int, value) -> bytes:
    if tag_id == TAG_BYTE:
        return struct.pack(">b", value)
    if tag_id == TAG_SHORT:
        return struct.pack(">h", value)
    if tag_id == TAG_INT:
        return struct.pack(">i", value)
    if tag_id == TAG_LONG:
        return struct.pack(">q", value)
    if tag_id == TAG_FLOAT:
        return struct.pack(">f", value)
    if tag_id == TAG_DOUBLE:
        return struct.pack(">d", value)
    if tag_id == TAG_STRING:
        return _enc_str(value)
    if tag_id == TAG_BYTE_ARR:
        return struct.pack(">i", len(value)) + bytes(value)
    if tag_id == TAG_INT_ARR:
        return struct.pack(">i", len(value)) + struct.pack(f">{len(value)}i", *value)
    if tag_id == TAG_LONG_ARR:
        return struct.pack(">i", len(value)) + struct.pack(f">{len(value)}q", *value)
    if tag_id == TAG_LIST:
        elem_type, elems = value
        out = struct.pack(">bi", elem_type, len(elems))
        for e in elems:
            out += _enc_payload(elem_type, e)
        return out
    if tag_id == TAG_COMPOUND:
        out = b""
        for name, (tid, val) in value.items():
            out += struct.pack(">b", tid) + _enc_str(name) + _enc_payload(tid, val)
        out += struct.pack(">b", TAG_END)
        return out
    raise ValueError(f"Unknown tag {tag_id}")


def nbt_compound(name: str, fields: dict) -> bytes:
    """Encode a named TAG_Compound as root (with gzip wrapper for level.dat)."""
    payload = _enc_payload(TAG_COMPOUND, fields)
    return struct.pack(">b", TAG_COMPOUND) + _enc_str(name) + payload


def nbt_compound_raw(fields: dict) -> bytes:
    """Encode a TAG_Compound payload (no name prefix, no gzip)."""
    return _enc_payload(TAG_COMPOUND, fields)


# ---------------------------------------------------------------------------
# level.dat builder
# ---------------------------------------------------------------------------
def _patch_nbt_string(raw: bytes, key_name: str, new_value: str) -> bytes:
    """Find TAG_String key in raw NBT bytes and replace its string value."""
    key = b"\x08" + struct.pack(">H", len(key_name)) + key_name.encode("utf-8")
    idx = raw.find(key)
    if idx == -1:
        return raw  # key not present — leave unchanged
    val_offset = idx + len(key)
    old_len = struct.unpack(">H", raw[val_offset:val_offset + 2])[0]
    old_end = val_offset + 2 + old_len
    new_bytes = new_value.encode("utf-8")
    return raw[:val_offset] + struct.pack(">H", len(new_bytes)) + new_bytes + raw[old_end:]


def make_level_dat(world_name: str) -> bytes:
    """
    Copy level.dat from the donor world and patch:
      - LevelName → world_name
      - GameRules: doDaylightCycle=false, doWeatherCycle=false, doMobSpawning=false
    This avoids having to replicate the full 1.21.1 WorldGenSettings codec.
    """
    donor = DONOR_WORLD / "level.dat"
    if not donor.exists():
        raise FileNotFoundError(
            f"Donor level.dat not found: {donor}\n"
            "Edit DONOR_WORLD in the script to point at any working world in run/saves/."
        )
    raw = gzip.decompress(donor.read_bytes())

    raw = _patch_nbt_string(raw, "LevelName", world_name)
    raw = _patch_nbt_string(raw, "doDaylightCycle", "false")
    raw = _patch_nbt_string(raw, "doWeatherCycle", "false")
    raw = _patch_nbt_string(raw, "doMobSpawning", "false")

    return gzip.compress(raw)


# ---------------------------------------------------------------------------
# Anvil region / chunk builder
# ---------------------------------------------------------------------------
# Block state palette — we need: stone, air, command_block, oak_button, oak_sign

def _block_id(palette: list, block_name: str, props: dict = None) -> int:
    """Return index in palette, inserting if needed."""
    props = props or {}
    for i, entry in enumerate(palette):
        if entry["Name"] == block_name and entry.get("Properties", {}) == props:
            return i
    idx = len(palette)
    entry = {"Name": block_name}
    if props:
        entry["Properties"] = props
    palette.append(entry)
    return idx


def _encode_palette_entry(entry: dict) -> tuple:
    """Convert palette dict entry to NBT compound fields."""
    fields = {"Name": (TAG_STRING, entry["Name"])}
    if "Properties" in entry:
        prop_fields = {k: (TAG_STRING, v) for k, v in entry["Properties"].items()}
        fields["Properties"] = (TAG_COMPOUND, prop_fields)
    return fields


def _pack_block_states(indices: list, bits_per_entry: int) -> list:
    """Pack block state indices into 64-bit longs (Minecraft 1.16+ format)."""
    # Each long holds floor(64/bits_per_entry) values, no cross-long packing
    values_per_long = 64 // bits_per_entry
    longs = []
    for i in range(0, len(indices), values_per_long):
        chunk = indices[i:i + values_per_long]
        val = 0
        for j, idx in enumerate(chunk):
            val |= (idx & ((1 << bits_per_entry) - 1)) << (j * bits_per_entry)
        # Convert to signed long
        if val >= (1 << 63):
            val -= (1 << 64)
        longs.append(val)
    return longs


def make_chunk(chunk_x: int, chunk_z: int, blocks: dict) -> bytes:
    """
    blocks: dict of (local_x, y, local_z) -> (block_name, props_dict)
    Returns uncompressed chunk NBT bytes.
    Produces a minimal chunk with one section per 16-block Y range that has blocks.
    """
    # Group blocks by section (Y >> 4)
    sections_map = {}  # section_y -> {(lx, ly, lz): (name, props)}
    for (lx, y, lz), (name, props) in blocks.items():
        sec_y = y >> 4
        local_y = y & 15
        sections_map.setdefault(sec_y, {})[(lx, local_y, lz)] = (name, props)

    section_nbt_list = []
    for sec_y in sorted(sections_map.keys()):
        blk_map = sections_map[sec_y]
        palette = [{"Name": "minecraft:air"}]  # index 0 = air
        indices = [0] * 4096  # 16*16*16

        for (lx, ly, lz), (name, props) in blk_map.items():
            idx = _block_id(palette, name, props)
            flat = ly * 256 + lz * 16 + lx   # Minecraft Y*256 + Z*16 + X
            indices[flat] = idx

        bits = max(4, math.ceil(math.log2(max(len(palette), 2))))
        longs = _pack_block_states(indices, bits)

        palette_nbt = [_encode_palette_entry(e) for e in palette]

        section_fields = {
            "Y": (TAG_BYTE, sec_y),
            "block_states": (TAG_COMPOUND, {
                "palette": (TAG_LIST, (TAG_COMPOUND, palette_nbt)),
                "data":    (TAG_LONG_ARR, longs),
            }),
            "biomes": (TAG_COMPOUND, {
                "palette": (TAG_LIST, (TAG_STRING, ["minecraft:plains"])),
            }),
            "SkyLight":   (TAG_BYTE_ARR, bytes(2048)),
            "BlockLight": (TAG_BYTE_ARR, bytes(2048)),
        }
        section_nbt_list.append(section_fields)

    chunk_fields = {
        "DataVersion": (TAG_INT, 3953),
        "xPos":        (TAG_INT, chunk_x),
        "yPos":        (TAG_INT, -4),
        "zPos":        (TAG_INT, chunk_z),
        "Status":      (TAG_STRING, "minecraft:full"),
        "LastUpdate":  (TAG_LONG, 0),
        "sections":    (TAG_LIST, (TAG_COMPOUND, section_nbt_list)),
        "block_entities": (TAG_LIST, (TAG_COMPOUND, [])),
        "Heightmaps":  (TAG_COMPOUND, {}),
        "isLightOn":   (TAG_BYTE, 0),
        "InhabitedTime": (TAG_LONG, 0),
    }

    return nbt_compound("", chunk_fields)


def make_region(chunks: dict) -> bytes:
    """
    chunks: dict of (region_chunk_x, region_chunk_z) -> chunk_nbt_bytes
    Returns raw .mca region file bytes.
    """
    # Region file: 4KB header of offsets, 4KB header of timestamps, then chunk data
    SECTOR = 4096
    offsets  = bytearray(SECTOR)
    timestamps = bytearray(SECTOR)
    data_sectors = bytearray()

    current_sector = 2  # sectors 0 and 1 are headers
    for (rx, rz), chunk_nbt in chunks.items():
        compressed = zlib.compress(chunk_nbt)
        length = len(compressed) + 1  # +1 for compression type byte
        sector_count = math.ceil((length + 4) / SECTOR)  # +4 for length prefix

        # Build chunk entry: 4-byte length, 1-byte compression (2=zlib), data
        entry = struct.pack(">ib", length, 2) + compressed
        entry += b"\x00" * (sector_count * SECTOR - len(entry))
        data_sectors += entry

        # Write to header
        header_idx = 4 * (rx + rz * 32)
        packed = (current_sector << 8) | sector_count
        struct.pack_into(">I", offsets, header_idx, packed)

        current_sector += sector_count

    return bytes(offsets) + bytes(timestamps) + bytes(data_sectors)


# ---------------------------------------------------------------------------
# Block entity (command block, sign) builders
# ---------------------------------------------------------------------------

def _sign_fields(x: int, y: int, z: int, lines: list) -> dict:
    """Build block entity fields for a wall sign. lines = up to 4 strings."""
    while len(lines) < 4:
        lines.append("")
    messages = [json.dumps({"text": l}) for l in lines]
    return {
        "id": (TAG_STRING, "minecraft:sign"),
        "x":  (TAG_INT, x),
        "y":  (TAG_INT, y),
        "z":  (TAG_INT, z),
        "front_text": (TAG_COMPOUND, {
            "color":            (TAG_STRING, "black"),
            "has_glowing_text": (TAG_BYTE, 0),
            "messages": (TAG_LIST, (TAG_STRING, messages)),
        }),
        "back_text": (TAG_COMPOUND, {
            "color":            (TAG_STRING, "black"),
            "has_glowing_text": (TAG_BYTE, 0),
            "messages": (TAG_LIST, (TAG_STRING, [
                '{"text":""}', '{"text":""}', '{"text":""}', '{"text":""}',
            ])),
        }),
        "is_waxed": (TAG_BYTE, 0),
    }


def make_block_entities(pieces: list, x_positions: list) -> list:
    """
    Per piece, place at (bx, CMD_Y, 0):
      - LOAD command block: /place structure ars_zero:necropolis/<nbt> <bx> <CMD_Y> <0>
        (absolute coords so the structure lands inside the frame)
      - button on top (BTN_Y)
      - wall sign on its -Z face labelled "LOAD / <nbt>"
    At (bx - 2, CMD_Y, 0):
      - Structure block pre-configured in SAVE mode
      - button on top
      - wall sign labelled "SAVE / <nbt>"
    """
    entities = []
    for piece, bx in zip(pieces, x_positions):
        sx, sy, sz = piece["sizeXYZ"]
        nbt = piece["nbt"]
        full_name = f"ars_zero:necropolis/{nbt}"

        # --- LOAD command block (2 blocks left of frame) ---
        # Command uses absolute coords so structure spawns at the frame origin (bx, CMD_Y, 0)
        load_x, load_y, load_z = bx - 2, CMD_Y, 0
        cmd = f"/place template {full_name} {bx} {CMD_Y} {0} none none 1.0 0"
        entities.append({
            "pos": (load_x, load_y, load_z),
            "fields": {
                "id":        (TAG_STRING, "minecraft:command_block"),
                "x":         (TAG_INT, load_x),
                "y":         (TAG_INT, load_y),
                "z":         (TAG_INT, load_z),
                "Command":   (TAG_STRING, cmd),
                "auto":      (TAG_BYTE, 0),
                "powered":   (TAG_BYTE, 0),
                "conditionMet":          (TAG_BYTE, 0),
                "UpdateLastExecution":   (TAG_BYTE, 1),
                "LastExecution":         (TAG_LONG, 0),
                "SuccessCount":          (TAG_INT, 0),
                "LastOutput":            (TAG_STRING, ""),
                "TrackOutput":           (TAG_BYTE, 1),
                "CustomName":            (TAG_STRING, '""'),
            }
        })
        # Sign on -Z face of the LOAD block (facing south = towards +Z = player view)
        entities.append({
            "pos": (load_x, SIGN_Y, load_z - 1),
            "fields": _sign_fields(load_x, SIGN_Y, load_z - 1, ["LOAD", nbt, "", ""]),
        })

        # --- SAVE structure block (4 blocks to the -X side of frame) ---
        save_x, save_y, save_z = bx - 4, CMD_Y, 0
        entities.append({
            "pos": (save_x, save_y, save_z),
            "fields": {
                "id":    (TAG_STRING, "minecraft:structure_block"),
                "x":     (TAG_INT, save_x),
                "y":     (TAG_INT, save_y),
                "z":     (TAG_INT, save_z),
                "name":  (TAG_STRING, full_name),
                "mode":  (TAG_STRING, "SAVE"),
                "posX":  (TAG_INT, 4),   # structure block is 4 to the -X of frame origin (bx)
                "posY":  (TAG_INT, 0),
                "posZ":  (TAG_INT, 0),
                "sizeX": (TAG_INT, sx),
                "sizeY": (TAG_INT, sy),
                "sizeZ": (TAG_INT, sz),
                "integrity":   (TAG_FLOAT, 1.0),
                "mirror":      (TAG_STRING, "NONE"),
                "rotation":    (TAG_STRING, "NONE"),
                "ignoreEntities": (TAG_BYTE, 1),
                "showboundingbox": (TAG_BYTE, 1),
                "author":      (TAG_STRING, ""),
                "metadata":    (TAG_STRING, ""),
                "powered":     (TAG_BYTE, 0),
                "showair":     (TAG_BYTE, 0),
            }
        })
        # Sign on -Z face of the SAVE block
        entities.append({
            "pos": (save_x, SIGN_Y, save_z - 1),
            "fields": _sign_fields(save_x, SIGN_Y, save_z - 1, ["SAVE", nbt, "", ""]),
        })

    return entities


# ---------------------------------------------------------------------------
# World assembly
# ---------------------------------------------------------------------------

def build_world(pieces: list) -> None:
    max_size_x = max(p["sizeXYZ"][0] for p in pieces)
    step = max_size_x + GAP

    x_positions = [i * step for i in range(len(pieces))]
    total_width = x_positions[-1] + max_size_x if x_positions else 0

    max_size_z = max(p["sizeXYZ"][2] for p in pieces)
    print(f"Grid: {len(pieces)} pieces, step={step}, total X span={total_width}")

    # Collect all blocks: (world_x, world_y, world_z) -> (block_name, props)
    all_blocks: dict = {}

    # Stone floor: wide enough to cover all frames (Z 0..max_size_z) + cmd block area
    floor_z_min = -2
    floor_z_max = max_size_z + 1
    for wx in range(-3, total_width + 3):
        for wz in range(floor_z_min, floor_z_max + 1):
            all_blocks[(wx, FLOOR_Y, wz)] = ("minecraft:stone", {})

    for piece, bx in zip(pieces, x_positions):
        sx, sy, sz = piece["sizeXYZ"]
        # Structure occupies Y = CMD_Y .. CMD_Y + sy - 1
        # Bedrock frames sit just outside: one row below (CMD_Y - 1) and one row above (CMD_Y + sy)
        frame_y_bottom = CMD_Y - 1
        frame_y_top    = CMD_Y + sy

        # Bedrock frames: bottom and top — perimeter only (1 block thick)
        for frame_y in (frame_y_bottom, frame_y_top):
            for dx in range(sx):
                for dz in range(sz):
                    if dx == 0 or dx == sx - 1 or dz == 0 or dz == sz - 1:
                        all_blocks[(bx + dx, frame_y, dz)] = ("minecraft:bedrock", {})

        # LOAD command block — 2 blocks to the -X side of frame (outside, not touching the structure area)
        load_x = bx - 2
        all_blocks[(load_x, CMD_Y, 0)] = ("minecraft:command_block", {"facing": "up", "conditional": "false"})
        all_blocks[(load_x, BTN_Y, 0)] = ("minecraft:oak_button", {"face": "floor", "facing": "north", "powered": "false"})
        all_blocks[(load_x, SIGN_Y, -1)] = ("minecraft:oak_wall_sign", {"facing": "south", "waterlogged": "false"})

        # SAVE structure block — 4 blocks to -X of frame
        save_x = bx - 4
        all_blocks[(save_x, CMD_Y, 0)] = ("minecraft:structure_block", {"mode": "save"})
        all_blocks[(save_x, BTN_Y, 0)] = ("minecraft:oak_button", {"face": "floor", "facing": "north", "powered": "false"})
        all_blocks[(save_x, SIGN_Y, -1)] = ("minecraft:oak_wall_sign", {"facing": "south", "waterlogged": "false"})

    # Group blocks into chunks
    chunk_blocks: dict = {}  # (cx, cz) -> {(lx,y,lz): (name,props)}
    be_by_chunk: dict = {}   # (cx, cz) -> list of block entity fields

    for (wx, wy, wz), (name, props) in all_blocks.items():
        cx, cz = wx >> 4, wz >> 4
        lx, lz = wx & 15, wz & 15
        chunk_blocks.setdefault((cx, cz), {})[(lx, wy, lz)] = (name, props)

    block_entities = make_block_entities(pieces, x_positions)
    for be in block_entities:
        wx, wy, wz = be["pos"]
        cx, cz = wx >> 4, wz >> 4
        be_by_chunk.setdefault((cx, cz), []).append(be["fields"])

    # Build chunk NBT, injecting block entities
    chunk_nbt_map: dict = {}  # (region_x, region_z) indexed by world chunk coords
    all_chunk_coords = set(chunk_blocks.keys()) | set(be_by_chunk.keys())

    for (cx, cz) in all_chunk_coords:
        blocks = chunk_blocks.get((cx, cz), {})

        # Build sections
        sections_map: dict = {}
        for (lx, y, lz), (name, props) in blocks.items():
            sec_y = y >> 4
            local_y = y & 15
            sections_map.setdefault(sec_y, {})[(lx, local_y, lz)] = (name, props)

        section_nbt_list = []
        for sec_y in sorted(sections_map.keys()):
            blk_map = sections_map[sec_y]
            palette = [{"Name": "minecraft:air"}]
            indices = [0] * 4096

            for (lx, ly, lz), (name, props) in blk_map.items():
                idx = _block_id(palette, name, props)
                flat = ly * 256 + lz * 16 + lx
                indices[flat] = idx

            bits = max(4, math.ceil(math.log2(max(len(palette), 2))))
            longs = _pack_block_states(indices, bits)
            palette_nbt = [_encode_palette_entry(e) for e in palette]

            section_nbt_list.append({
                "Y": (TAG_BYTE, sec_y),
                "block_states": (TAG_COMPOUND, {
                    "palette": (TAG_LIST, (TAG_COMPOUND, palette_nbt)),
                    "data":    (TAG_LONG_ARR, longs),
                }),
                "biomes": (TAG_COMPOUND, {
                    "palette": (TAG_LIST, (TAG_STRING, ["minecraft:plains"])),
                }),
                "SkyLight":   (TAG_BYTE_ARR, bytes(2048)),
                "BlockLight": (TAG_BYTE_ARR, bytes(2048)),
            })

        be_list = be_by_chunk.get((cx, cz), [])

        chunk_fields = {
            "DataVersion":    (TAG_INT, 3953),
            "xPos":           (TAG_INT, cx),
            "yPos":           (TAG_INT, -4),
            "zPos":           (TAG_INT, cz),
            "Status":         (TAG_STRING, "minecraft:full"),
            "LastUpdate":     (TAG_LONG, 0),
            "sections":       (TAG_LIST, (TAG_COMPOUND, section_nbt_list)),
            "block_entities": (TAG_LIST, (TAG_COMPOUND, be_list)),
            "Heightmaps":     (TAG_COMPOUND, {}),
            "isLightOn":      (TAG_BYTE, 0),
            "InhabitedTime":  (TAG_LONG, 0),
        }
        chunk_nbt = nbt_compound("", chunk_fields)

        # Region coords
        rx, rz = cx & 31, cz & 31
        # For region file naming: region_file_x = cx >> 5
        region_file_x, region_file_z = cx >> 5, cz >> 5
        chunk_nbt_map.setdefault((region_file_x, region_file_z), {})[(rx, rz)] = chunk_nbt

    # Write world
    if WORLD_DIR.exists():
        shutil.rmtree(WORLD_DIR)
    region_dir = WORLD_DIR / "region"
    region_dir.mkdir(parents=True)

    for (rfx, rfz), chunks in chunk_nbt_map.items():
        region_bytes = make_region(chunks)
        region_path = region_dir / f"r.{rfx}.{rfz}.mca"
        region_path.write_bytes(region_bytes)
        print(f"  Wrote {region_path.name} ({len(chunks)} chunks)")

    level_dat_path = WORLD_DIR / "level.dat"
    level_dat_path.write_bytes(make_level_dat(WORLD_NAME))
    print(f"  Wrote level.dat")

    # Write icon (optional, skip if missing)
    # Write session.lock (required by Minecraft to claim the world)
    (WORLD_DIR / "session.lock").write_bytes(struct.pack(">q", 0))

    print(f"\nWorld written to: {WORLD_DIR}")
    print(f"Open 'Structure Layout' in runClient, then click each button to place structures.")


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def read_nbt_size(nbt_path: Path) -> list:
    """Read [sizeX, sizeY, sizeZ] directly from a gzip-compressed structure NBT file."""
    raw = gzip.decompress(nbt_path.read_bytes())
    key = b'\x09\x00\x04size'
    idx = raw.find(key)
    if idx == -1:
        raise ValueError(f"'size' tag not found in {nbt_path}")
    pos = idx + len(key)
    count = struct.unpack(">i", raw[pos + 1:pos + 5])[0]
    return [struct.unpack(">i", raw[pos + 5 + i * 4:pos + 9 + i * 4])[0] for i in range(count)]


def main():
    if not CONFIG_FILE.exists():
        print(f"ERROR: Config not found: {CONFIG_FILE}")
        return 1

    with open(CONFIG_FILE) as f:
        raw = json.load(f)

    pieces = []
    skipped = 0
    for entry in raw:
        nbt_name = entry["nbt"]
        nbt_path = NBT_DIR / f"{nbt_name}.nbt"
        if not nbt_path.exists():
            print(f"  WARN: NBT file not found, skipping: {nbt_path}")
            skipped += 1
            continue
        # Auto-read size from NBT; sizeXYZ in JSON is ignored (kept for reference only)
        size = read_nbt_size(nbt_path)
        pieces.append({"nbt": nbt_name, "sizeXYZ": size})

    print(f"Loaded {len(pieces)} pieces ({skipped} skipped — NBT missing)")

    if not pieces:
        print("No valid pieces found. Nothing to generate.")
        return 1

    build_world(pieces)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
