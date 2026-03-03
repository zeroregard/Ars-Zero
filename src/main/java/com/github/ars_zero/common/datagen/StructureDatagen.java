package com.github.ars_zero.common.datagen;

import com.google.common.hash.Hashing;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.core.FrontAndTop;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/**
 * Generates structure NBT files at datagen time so we don't need Python scripts.
 * Uses the same NBT format the game expects (palettes + blocks).
 */
public class StructureDatagen implements DataProvider {
    private static final String MOD_ID = "ars_zero";
    private final PackOutput output;
    private final CompletableFuture<HolderLookup.Provider> lookupProvider;

    public StructureDatagen(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        this.output = output;
        this.lookupProvider = lookupProvider;
    }

    @Override
    @NotNull
    public CompletableFuture<?> run(@NotNull CachedOutput cachedOutput) {
        return lookupProvider.thenCompose(provider -> {
            BlockState wallBlock = provider.lookup(Registries.BLOCK)
                    .flatMap(reg -> reg.get(ResourceKey.create(Registries.BLOCK, ResourceLocation.parse("ars_nouveau:sourcestone_large_bricks"))))
                    .map(h -> h.value().defaultBlockState())
                    .orElse(Blocks.STONE_BRICKS.defaultBlockState());
            BlockState floorBlock = provider.lookup(Registries.BLOCK)
                    .flatMap(reg -> reg.get(ResourceKey.create(Registries.BLOCK, ResourceLocation.parse("ars_nouveau:smooth_sourcestone_small_bricks"))))
                    .map(h -> h.value().defaultBlockState())
                    .orElse(wallBlock);
            CompletableFuture<?> roomConnector = writeStructure(cachedOutput, "room_connector", buildRoomConnectorStructure(wallBlock, floorBlock));
            CompletableFuture<?> smallRoom = writeStructure(cachedOutput, "small_room", buildSmallRoomStructure(wallBlock, floorBlock));
            CompletableFuture<?> hallwayShort = writeStructure(cachedOutput, "hallway_short", buildHallwayShortStructure(wallBlock, floorBlock));
            CompletableFuture<?> hallwayLong = writeStructure(cachedOutput, "hallway_long", buildHallwayLongStructure(wallBlock, floorBlock));
            CompletableFuture<?> largeRoom = writeStructure(cachedOutput, "large_room", buildLargeRoomStructure(wallBlock, floorBlock));
            CompletableFuture<?> deadEnd = writeStructure(cachedOutput, "dead_end", buildDeadEndStructure(wallBlock, floorBlock));
            CompletableFuture<?> smallRoom2 = writeStructure(cachedOutput, "small_room_2", buildSmallRoom2Structure(wallBlock, floorBlock));
            CompletableFuture<?> largeRoom2 = writeStructure(cachedOutput, "large_room_2", buildLargeRoom2Structure(wallBlock, floorBlock));
            CompletableFuture<?> hallwayMedium = writeStructure(cachedOutput, "hallway_medium", buildHallwayMediumStructure(wallBlock, floorBlock));
            CompletableFuture<?> hallwayNsShort = writeStructure(cachedOutput, "hallway_ns_short", buildHallwayNsShortStructure(wallBlock, floorBlock));
            CompletableFuture<?> hallwayNsMedium = writeStructure(cachedOutput, "hallway_ns_medium", buildHallwayNsMediumStructure(wallBlock, floorBlock));
            CompletableFuture<?> hallwayNsLong = writeStructure(cachedOutput, "hallway_ns_long", buildHallwayNsLongStructure(wallBlock, floorBlock));
            CompletableFuture<?> entranceStairs = writeStructure(cachedOutput, "entrance_stairs", buildEntranceStairsStructure(wallBlock, floorBlock));
            CompletableFuture<?> spiralStaircase = writeStructure(cachedOutput, "spiral_staircase", buildSpiralStaircaseStructure(wallBlock));
            return CompletableFuture.allOf(roomConnector, smallRoom, hallwayShort, hallwayLong, largeRoom, deadEnd, smallRoom2, largeRoom2, hallwayMedium, hallwayNsShort, hallwayNsMedium, hallwayNsLong, entranceStairs, spiralStaircase);
        });
    }

    private CompletableFuture<?> writeStructure(CachedOutput cachedOutput, String name, CompoundTag root) {
        Path path = output.getOutputFolder()
                .resolve("data/" + MOD_ID + "/structure/" + name + ".nbt");
        byte[] bytes;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            NbtIo.writeCompressed(root, baos);
            bytes = baos.toByteArray();
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
        try {
            cachedOutput.writeIfNeeded(path, bytes, Hashing.sha1().hashBytes(bytes));
            return CompletableFuture.completedFuture(null);
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private static CompoundTag blockEntry(int stateId, int x, int y, int z) {
        return blockEntry(stateId, x, y, z, null);
    }

    private static CompoundTag blockEntry(int stateId, int x, int y, int z, CompoundTag nbt) {
        CompoundTag entry = new CompoundTag();
        entry.put("pos", newIntegerList(x, y, z));
        entry.putInt("state", stateId);
        if (nbt != null && !nbt.isEmpty()) {
            entry.put("nbt", nbt);
        }
        return entry;
    }

    /** Shared by all pieces so they can connect to each other. */
    private static final String JIGSAW_NAME = "ars_zero:necropolis";
    private static final String JIGSAW_TARGET = "ars_zero:necropolis";
    private static final String JIGSAW_POOL = "ars_zero:necropolis/passage";
    private static final String JIGSAW_POOL_ENTRANCE = "ars_zero:necropolis/entrance";
    private static final String JIGSAW_FINAL_STATE = "minecraft:air";

    private static CompoundTag jigsawNbt(String name, String target, String pool) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("name", name);
        nbt.putString("target", target);
        nbt.putString("pool", pool);
        nbt.putString("final_state", JIGSAW_FINAL_STATE);
        nbt.putString("joint", "aligned");
        return nbt;
    }

    /**
     * Start room 9x6x9 with jigsaws west (entrance), east, north, south. West points to entrance pool (staircase); others to passage.
     */
    private static CompoundTag buildRoomConnectorStructure(BlockState wallBlock, BlockState floorBlock) {
        BlockState jigsawWest = Blocks.JIGSAW.defaultBlockState()
                .setValue(BlockStateProperties.ORIENTATION, FrontAndTop.WEST_UP);
        BlockState jigsawEast = Blocks.JIGSAW.defaultBlockState()
                .setValue(BlockStateProperties.ORIENTATION, FrontAndTop.EAST_UP);
        BlockState jigsawNorth = Blocks.JIGSAW.defaultBlockState()
                .setValue(BlockStateProperties.ORIENTATION, FrontAndTop.NORTH_UP);
        BlockState jigsawSouth = Blocks.JIGSAW.defaultBlockState()
                .setValue(BlockStateProperties.ORIENTATION, FrontAndTop.SOUTH_UP);

        BlockState torchSouth = Blocks.SOUL_WALL_TORCH.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);
        BlockState torchNorth = Blocks.SOUL_WALL_TORCH.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);

        ListTag paletteList = new ListTag();
        paletteList.add(NbtUtils.writeBlockState(wallBlock));       // 0
        paletteList.add(NbtUtils.writeBlockState(floorBlock));      // 1
        paletteList.add(NbtUtils.writeBlockState(jigsawWest));      // 2
        paletteList.add(NbtUtils.writeBlockState(jigsawEast));      // 3
        paletteList.add(NbtUtils.writeBlockState(jigsawNorth));     // 4
        paletteList.add(NbtUtils.writeBlockState(jigsawSouth));     // 5
        paletteList.add(NbtUtils.writeBlockState(Blocks.AIR.defaultBlockState())); // 6
        paletteList.add(NbtUtils.writeBlockState(torchSouth));      // 7
        paletteList.add(NbtUtils.writeBlockState(torchNorth));      // 8

        int sizeX = 9, sizeY = 6, sizeZ = 9;
        Set<BlockPos> skips = Set.of(
                new BlockPos(0, 1, 4), new BlockPos(0, 2, 4), new BlockPos(0, 3, 4),
                new BlockPos(8, 1, 4), new BlockPos(8, 2, 4), new BlockPos(8, 3, 4),
                new BlockPos(4, 1, 0), new BlockPos(4, 2, 0), new BlockPos(4, 3, 0),
                new BlockPos(4, 1, 8), new BlockPos(4, 2, 8), new BlockPos(4, 3, 8));
        ListTag blocksAtRoot = new ListTag();
        addRoomWallsAndFloorCeiling(blocksAtRoot, sizeX, sizeY, sizeZ, skips, 0, 1);
        blocksAtRoot.add(blockEntry(2, 0, 1, 4, jigsawNbt(JIGSAW_NAME, JIGSAW_TARGET, JIGSAW_POOL_ENTRANCE)));
        blocksAtRoot.add(blockEntry(3, 8, 1, 4, jigsawNbt(JIGSAW_NAME, JIGSAW_TARGET, JIGSAW_POOL)));
        blocksAtRoot.add(blockEntry(4, 4, 1, 0, jigsawNbt(JIGSAW_NAME, JIGSAW_TARGET, JIGSAW_POOL)));
        blocksAtRoot.add(blockEntry(5, 4, 1, 8, jigsawNbt(JIGSAW_NAME, JIGSAW_TARGET, JIGSAW_POOL)));
        addInteriorAir(blocksAtRoot, sizeX, sizeY, sizeZ, 6);
        blocksAtRoot.add(blockEntry(7, 4, 3, 1));  // torch on north wall
        blocksAtRoot.add(blockEntry(8, 4, 3, 7));  // torch on south wall

        ListTag palettes = new ListTag();
        palettes.add(paletteList);
        CompoundTag root = new CompoundTag();
        root.put("size", newIntegerList(sizeX, sizeY, sizeZ));
        root.put("blocks", blocksAtRoot);
        root.put("palettes", palettes);
        root.put("entities", new ListTag());
        return root;
    }

    /**
     * Entrance staircase 5×100×3: diagonal stairs from dungeon (y=30) up toward surface. Reaches y=129 so it clears surface in most terrain.
     * East jigsaw at (0,0,1) connects to room_connector west. Entrance is always on the WEST side of the start room.
     */
    private static CompoundTag buildEntranceStairsStructure(BlockState wallBlock, BlockState floorBlock) {
        BlockState jigsawEast = Blocks.JIGSAW.defaultBlockState()
                .setValue(BlockStateProperties.ORIENTATION, FrontAndTop.EAST_UP);
        BlockState stairBlock = Blocks.STONE_STAIRS.defaultBlockState()
                .setValue(BlockStateProperties.HALF, Half.BOTTOM)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST);

        ListTag paletteList = new ListTag();
        paletteList.add(NbtUtils.writeBlockState(wallBlock));
        paletteList.add(NbtUtils.writeBlockState(stairBlock));
        paletteList.add(NbtUtils.writeBlockState(jigsawEast));
        paletteList.add(NbtUtils.writeBlockState(Blocks.AIR.defaultBlockState()));

        int sizeX = 5, sizeY = 100, sizeZ = 3;
        ListTag blocksAtRoot = new ListTag();
        // Walls at z=0 and z=2
        for (int y = 0; y < sizeY; y++) {
            for (int x = 0; x < sizeX; x++) {
                blocksAtRoot.add(blockEntry(0, x, y, 0));
                blocksAtRoot.add(blockEntry(0, x, y, 2));
            }
        }
        // East jigsaw at bottom (0,0,1); pool empty so no further pieces attach
        blocksAtRoot.add(blockEntry(2, 0, 0, 1, jigsawNbt(JIGSAW_NAME, JIGSAW_TARGET, "minecraft:empty")));
        // Diagonal stairs: at each y=1..59 place stair at (y % sizeX, y, 1)
        for (int y = 1; y < sizeY; y++) {
            int x = y % sizeX;
            blocksAtRoot.add(blockEntry(1, x, y, 1));
        }
        // Hollow corridor at z=1: air everywhere except jigsaw (0,0,1) and stairs (y%5, y, 1)
        for (int y = 0; y < sizeY; y++) {
            for (int x = 0; x < sizeX; x++) {
                if (y == 0 && x == 0) continue; // jigsaw
                if (y >= 1 && x == y % sizeX) continue; // stair
                blocksAtRoot.add(blockEntry(3, x, y, 1));
            }
        }

        ListTag palettes = new ListTag();
        palettes.add(paletteList);
        CompoundTag root = new CompoundTag();
        root.put("size", newIntegerList(sizeX, sizeY, sizeZ));
        root.put("blocks", blocksAtRoot);
        root.put("palettes", palettes);
        root.put("entities", new ListTag());
        return root;
    }

    /**
     * Small room 9x6x9 with jigsaws west (0,1,4), east (8,1,4), north (4,1,0), south (4,1,8). Four-way exits for maze branching.
     */
    private static CompoundTag buildSmallRoomStructure(BlockState wallBlock, BlockState floorBlock) {
        BlockState jigsawWest = Blocks.JIGSAW.defaultBlockState()
                .setValue(BlockStateProperties.ORIENTATION, FrontAndTop.WEST_UP);
        BlockState jigsawEast = Blocks.JIGSAW.defaultBlockState()
                .setValue(BlockStateProperties.ORIENTATION, FrontAndTop.EAST_UP);
        BlockState jigsawNorth = Blocks.JIGSAW.defaultBlockState()
                .setValue(BlockStateProperties.ORIENTATION, FrontAndTop.NORTH_UP);
        BlockState jigsawSouth = Blocks.JIGSAW.defaultBlockState()
                .setValue(BlockStateProperties.ORIENTATION, FrontAndTop.SOUTH_UP);

        BlockState torchSouth = Blocks.SOUL_WALL_TORCH.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);
        BlockState torchNorth = Blocks.SOUL_WALL_TORCH.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);

        ListTag paletteList = new ListTag();
        paletteList.add(NbtUtils.writeBlockState(wallBlock));       // 0
        paletteList.add(NbtUtils.writeBlockState(floorBlock));      // 1
        paletteList.add(NbtUtils.writeBlockState(jigsawWest));      // 2
        paletteList.add(NbtUtils.writeBlockState(jigsawEast));      // 3
        paletteList.add(NbtUtils.writeBlockState(jigsawNorth));     // 4
        paletteList.add(NbtUtils.writeBlockState(jigsawSouth));     // 5
        paletteList.add(NbtUtils.writeBlockState(Blocks.AIR.defaultBlockState())); // 6
        paletteList.add(NbtUtils.writeBlockState(torchSouth));      // 7
        paletteList.add(NbtUtils.writeBlockState(torchNorth));      // 8

        int sizeX = 9, sizeY = 6, sizeZ = 9;
        Set<BlockPos> skips = Set.of(
                new BlockPos(0, 1, 4), new BlockPos(0, 2, 4), new BlockPos(0, 3, 4),
                new BlockPos(8, 1, 4), new BlockPos(8, 2, 4), new BlockPos(8, 3, 4),
                new BlockPos(4, 1, 0), new BlockPos(4, 2, 0), new BlockPos(4, 3, 0),
                new BlockPos(4, 1, 8), new BlockPos(4, 2, 8), new BlockPos(4, 3, 8));
        ListTag blocksAtRoot = new ListTag();
        addRoomWallsAndFloorCeiling(blocksAtRoot, sizeX, sizeY, sizeZ, skips, 0, 1);
        blocksAtRoot.add(blockEntry(2, 0, 1, 4, jigsawNbt(JIGSAW_NAME, JIGSAW_TARGET, JIGSAW_POOL)));
        blocksAtRoot.add(blockEntry(3, 8, 1, 4, jigsawNbt(JIGSAW_NAME, JIGSAW_TARGET, JIGSAW_POOL)));
        blocksAtRoot.add(blockEntry(4, 4, 1, 0, jigsawNbt(JIGSAW_NAME, JIGSAW_TARGET, JIGSAW_POOL)));
        blocksAtRoot.add(blockEntry(5, 4, 1, 8, jigsawNbt(JIGSAW_NAME, JIGSAW_TARGET, JIGSAW_POOL)));
        addInteriorAir(blocksAtRoot, sizeX, sizeY, sizeZ, 6);
        blocksAtRoot.add(blockEntry(7, 4, 3, 1));  // torch on north wall
        blocksAtRoot.add(blockEntry(8, 4, 3, 7));  // torch on south wall

        ListTag palettes = new ListTag();
        palettes.add(paletteList);
        CompoundTag root = new CompoundTag();
        root.put("size", newIntegerList(sizeX, sizeY, sizeZ));
        root.put("blocks", blocksAtRoot);
        root.put("palettes", palettes);
        root.put("entities", new ListTag());
        return root;
    }

    /**
     * Small room variant (same 9x6x9, west + east jigsaws) for pool variance.
     */
    private static CompoundTag buildSmallRoom2Structure(BlockState wallBlock, BlockState floorBlock) {
        return buildSmallRoomStructure(wallBlock, floorBlock);
    }

    /**
     * Hallway 3x4x7 with jigsaws west (0,1,3), east (2,1,3).
     */
    private static CompoundTag buildHallwayShortStructure(BlockState wallBlock, BlockState floorBlock) {
        BlockState jigsawWest = Blocks.JIGSAW.defaultBlockState()
                .setValue(BlockStateProperties.ORIENTATION, FrontAndTop.WEST_UP);
        BlockState jigsawEast = Blocks.JIGSAW.defaultBlockState()
                .setValue(BlockStateProperties.ORIENTATION, FrontAndTop.EAST_UP);

        ListTag paletteList = new ListTag();
        paletteList.add(NbtUtils.writeBlockState(wallBlock));
        paletteList.add(NbtUtils.writeBlockState(floorBlock));
        paletteList.add(NbtUtils.writeBlockState(jigsawWest));
        paletteList.add(NbtUtils.writeBlockState(jigsawEast));
        paletteList.add(NbtUtils.writeBlockState(Blocks.AIR.defaultBlockState()));

        int sizeX = 5, sizeY = 5, sizeZ = 7;
        Set<BlockPos> skips = Set.of(
                new BlockPos(0, 1, 3), new BlockPos(0, 2, 3), new BlockPos(0, 3, 3),
                new BlockPos(4, 1, 3), new BlockPos(4, 2, 3), new BlockPos(4, 3, 3));
        ListTag blocksAtRoot = new ListTag();
        addRoomWallsAndFloorCeiling(blocksAtRoot, sizeX, sizeY, sizeZ, skips, 0, 1);
        blocksAtRoot.add(blockEntry(2, 0, 1, 3, jigsawNbt(JIGSAW_NAME, JIGSAW_TARGET, JIGSAW_POOL)));
        blocksAtRoot.add(blockEntry(3, 4, 1, 3, jigsawNbt(JIGSAW_NAME, JIGSAW_TARGET, JIGSAW_POOL)));
        addInteriorAir(blocksAtRoot, sizeX, sizeY, sizeZ, 4);

        ListTag palettes = new ListTag();
        palettes.add(paletteList);
        CompoundTag root = new CompoundTag();
        root.put("size", newIntegerList(sizeX, sizeY, sizeZ));
        root.put("blocks", blocksAtRoot);
        root.put("palettes", palettes);
        root.put("entities", new ListTag());
        return root;
    }

    /**
     * Hallway 3x4x11 with jigsaws west (0,1,5), east (2,1,5).
     */
    private static CompoundTag buildHallwayLongStructure(BlockState wallBlock, BlockState floorBlock) {
        BlockState jigsawWest = Blocks.JIGSAW.defaultBlockState()
                .setValue(BlockStateProperties.ORIENTATION, FrontAndTop.WEST_UP);
        BlockState jigsawEast = Blocks.JIGSAW.defaultBlockState()
                .setValue(BlockStateProperties.ORIENTATION, FrontAndTop.EAST_UP);

        ListTag paletteList = new ListTag();
        paletteList.add(NbtUtils.writeBlockState(wallBlock));
        paletteList.add(NbtUtils.writeBlockState(floorBlock));
        paletteList.add(NbtUtils.writeBlockState(jigsawWest));
        paletteList.add(NbtUtils.writeBlockState(jigsawEast));
        paletteList.add(NbtUtils.writeBlockState(Blocks.AIR.defaultBlockState()));

        int sizeX = 5, sizeY = 5, sizeZ = 11;
        Set<BlockPos> skips = Set.of(
                new BlockPos(0, 1, 5), new BlockPos(0, 2, 5), new BlockPos(0, 3, 5),
                new BlockPos(4, 1, 5), new BlockPos(4, 2, 5), new BlockPos(4, 3, 5));
        ListTag blocksAtRoot = new ListTag();
        addRoomWallsAndFloorCeiling(blocksAtRoot, sizeX, sizeY, sizeZ, skips, 0, 1);
        blocksAtRoot.add(blockEntry(2, 0, 1, 5, jigsawNbt(JIGSAW_NAME, JIGSAW_TARGET, JIGSAW_POOL)));
        blocksAtRoot.add(blockEntry(3, 4, 1, 5, jigsawNbt(JIGSAW_NAME, JIGSAW_TARGET, JIGSAW_POOL)));
        addInteriorAir(blocksAtRoot, sizeX, sizeY, sizeZ, 4);

        ListTag palettes = new ListTag();
        palettes.add(paletteList);
        CompoundTag root = new CompoundTag();
        root.put("size", newIntegerList(sizeX, sizeY, sizeZ));
        root.put("blocks", blocksAtRoot);
        root.put("palettes", palettes);
        root.put("entities", new ListTag());
        return root;
    }

    /**
     * Hallway 3x4x9 (medium length) with jigsaws west (0,1,4), east (2,1,4).
     */
    private static CompoundTag buildHallwayMediumStructure(BlockState wallBlock, BlockState floorBlock) {
        BlockState jigsawWest = Blocks.JIGSAW.defaultBlockState()
                .setValue(BlockStateProperties.ORIENTATION, FrontAndTop.WEST_UP);
        BlockState jigsawEast = Blocks.JIGSAW.defaultBlockState()
                .setValue(BlockStateProperties.ORIENTATION, FrontAndTop.EAST_UP);

        ListTag paletteList = new ListTag();
        paletteList.add(NbtUtils.writeBlockState(wallBlock));
        paletteList.add(NbtUtils.writeBlockState(floorBlock));
        paletteList.add(NbtUtils.writeBlockState(jigsawWest));
        paletteList.add(NbtUtils.writeBlockState(jigsawEast));
        paletteList.add(NbtUtils.writeBlockState(Blocks.AIR.defaultBlockState()));

        int sizeX = 5, sizeY = 5, sizeZ = 9;
        Set<BlockPos> skips = Set.of(
                new BlockPos(0, 1, 4), new BlockPos(0, 2, 4), new BlockPos(0, 3, 4),
                new BlockPos(4, 1, 4), new BlockPos(4, 2, 4), new BlockPos(4, 3, 4));
        ListTag blocksAtRoot = new ListTag();
        addRoomWallsAndFloorCeiling(blocksAtRoot, sizeX, sizeY, sizeZ, skips, 0, 1);
        blocksAtRoot.add(blockEntry(2, 0, 1, 4, jigsawNbt(JIGSAW_NAME, JIGSAW_TARGET, JIGSAW_POOL)));
        blocksAtRoot.add(blockEntry(3, 4, 1, 4, jigsawNbt(JIGSAW_NAME, JIGSAW_TARGET, JIGSAW_POOL)));
        addInteriorAir(blocksAtRoot, sizeX, sizeY, sizeZ, 4);

        ListTag palettes = new ListTag();
        palettes.add(paletteList);
        CompoundTag root = new CompoundTag();
        root.put("size", newIntegerList(sizeX, sizeY, sizeZ));
        root.put("blocks", blocksAtRoot);
        root.put("palettes", palettes);
        root.put("entities", new ListTag());
        return root;
    }

    /**
     * Hallway N-S 3x4x7 with jigsaws north (1,1,0), south (1,1,6). Runs along Z for maze turns.
     */
    private static CompoundTag buildHallwayNsShortStructure(BlockState wallBlock, BlockState floorBlock) {
        BlockState jigsawNorth = Blocks.JIGSAW.defaultBlockState()
                .setValue(BlockStateProperties.ORIENTATION, FrontAndTop.NORTH_UP);
        BlockState jigsawSouth = Blocks.JIGSAW.defaultBlockState()
                .setValue(BlockStateProperties.ORIENTATION, FrontAndTop.SOUTH_UP);

        ListTag paletteList = new ListTag();
        paletteList.add(NbtUtils.writeBlockState(wallBlock));
        paletteList.add(NbtUtils.writeBlockState(floorBlock));
        paletteList.add(NbtUtils.writeBlockState(jigsawNorth));
        paletteList.add(NbtUtils.writeBlockState(jigsawSouth));
        paletteList.add(NbtUtils.writeBlockState(Blocks.AIR.defaultBlockState()));

        int sizeX = 5, sizeY = 5, sizeZ = 7;
        Set<BlockPos> skips = Set.of(
                new BlockPos(2, 1, 0), new BlockPos(2, 2, 0), new BlockPos(2, 3, 0),
                new BlockPos(2, 1, 6), new BlockPos(2, 2, 6), new BlockPos(2, 3, 6));
        ListTag blocksAtRoot = new ListTag();
        addRoomWallsAndFloorCeiling(blocksAtRoot, sizeX, sizeY, sizeZ, skips, 0, 1);
        blocksAtRoot.add(blockEntry(2, 2, 1, 0, jigsawNbt(JIGSAW_NAME, JIGSAW_TARGET, JIGSAW_POOL)));
        blocksAtRoot.add(blockEntry(3, 2, 1, 6, jigsawNbt(JIGSAW_NAME, JIGSAW_TARGET, JIGSAW_POOL)));
        addInteriorAir(blocksAtRoot, sizeX, sizeY, sizeZ, 4);

        ListTag palettes = new ListTag();
        palettes.add(paletteList);
        CompoundTag root = new CompoundTag();
        root.put("size", newIntegerList(sizeX, sizeY, sizeZ));
        root.put("blocks", blocksAtRoot);
        root.put("palettes", palettes);
        root.put("entities", new ListTag());
        return root;
    }

    /**
     * Hallway N-S 3x4x9 with jigsaws north (1,1,0), south (1,1,8).
     */
    private static CompoundTag buildHallwayNsMediumStructure(BlockState wallBlock, BlockState floorBlock) {
        BlockState jigsawNorth = Blocks.JIGSAW.defaultBlockState()
                .setValue(BlockStateProperties.ORIENTATION, FrontAndTop.NORTH_UP);
        BlockState jigsawSouth = Blocks.JIGSAW.defaultBlockState()
                .setValue(BlockStateProperties.ORIENTATION, FrontAndTop.SOUTH_UP);

        ListTag paletteList = new ListTag();
        paletteList.add(NbtUtils.writeBlockState(wallBlock));
        paletteList.add(NbtUtils.writeBlockState(floorBlock));
        paletteList.add(NbtUtils.writeBlockState(jigsawNorth));
        paletteList.add(NbtUtils.writeBlockState(jigsawSouth));
        paletteList.add(NbtUtils.writeBlockState(Blocks.AIR.defaultBlockState()));

        int sizeX = 5, sizeY = 5, sizeZ = 9;
        Set<BlockPos> skips = Set.of(
                new BlockPos(2, 1, 0), new BlockPos(2, 2, 0), new BlockPos(2, 3, 0),
                new BlockPos(2, 1, 8), new BlockPos(2, 2, 8), new BlockPos(2, 3, 8));
        ListTag blocksAtRoot = new ListTag();
        addRoomWallsAndFloorCeiling(blocksAtRoot, sizeX, sizeY, sizeZ, skips, 0, 1);
        blocksAtRoot.add(blockEntry(2, 2, 1, 0, jigsawNbt(JIGSAW_NAME, JIGSAW_TARGET, JIGSAW_POOL)));
        blocksAtRoot.add(blockEntry(3, 2, 1, 8, jigsawNbt(JIGSAW_NAME, JIGSAW_TARGET, JIGSAW_POOL)));
        addInteriorAir(blocksAtRoot, sizeX, sizeY, sizeZ, 4);

        ListTag palettes = new ListTag();
        palettes.add(paletteList);
        CompoundTag root = new CompoundTag();
        root.put("size", newIntegerList(sizeX, sizeY, sizeZ));
        root.put("blocks", blocksAtRoot);
        root.put("palettes", palettes);
        root.put("entities", new ListTag());
        return root;
    }

    /**
     * Hallway N-S 3x4x11 with jigsaws north (1,1,0), south (1,1,10).
     */
    private static CompoundTag buildHallwayNsLongStructure(BlockState wallBlock, BlockState floorBlock) {
        BlockState jigsawNorth = Blocks.JIGSAW.defaultBlockState()
                .setValue(BlockStateProperties.ORIENTATION, FrontAndTop.NORTH_UP);
        BlockState jigsawSouth = Blocks.JIGSAW.defaultBlockState()
                .setValue(BlockStateProperties.ORIENTATION, FrontAndTop.SOUTH_UP);

        ListTag paletteList = new ListTag();
        paletteList.add(NbtUtils.writeBlockState(wallBlock));
        paletteList.add(NbtUtils.writeBlockState(floorBlock));
        paletteList.add(NbtUtils.writeBlockState(jigsawNorth));
        paletteList.add(NbtUtils.writeBlockState(jigsawSouth));
        paletteList.add(NbtUtils.writeBlockState(Blocks.AIR.defaultBlockState()));

        int sizeX = 5, sizeY = 5, sizeZ = 11;
        Set<BlockPos> skips = Set.of(
                new BlockPos(2, 1, 0), new BlockPos(2, 2, 0), new BlockPos(2, 3, 0),
                new BlockPos(2, 1, 10), new BlockPos(2, 2, 10), new BlockPos(2, 3, 10));
        ListTag blocksAtRoot = new ListTag();
        addRoomWallsAndFloorCeiling(blocksAtRoot, sizeX, sizeY, sizeZ, skips, 0, 1);
        blocksAtRoot.add(blockEntry(2, 2, 1, 0, jigsawNbt(JIGSAW_NAME, JIGSAW_TARGET, JIGSAW_POOL)));
        blocksAtRoot.add(blockEntry(3, 2, 1, 10, jigsawNbt(JIGSAW_NAME, JIGSAW_TARGET, JIGSAW_POOL)));
        addInteriorAir(blocksAtRoot, sizeX, sizeY, sizeZ, 4);

        ListTag palettes = new ListTag();
        palettes.add(paletteList);
        CompoundTag root = new CompoundTag();
        root.put("size", newIntegerList(sizeX, sizeY, sizeZ));
        root.put("blocks", blocksAtRoot);
        root.put("palettes", palettes);
        root.put("entities", new ListTag());
        return root;
    }

    /**
     * Large room 13x7x13 with jigsaws west (0,1,6), east (12,1,6), north (6,1,0), south (6,1,12). Four-way exits for maze branching.
     */
    private static CompoundTag buildLargeRoomStructure(BlockState wallBlock, BlockState floorBlock) {
        BlockState jigsawWest = Blocks.JIGSAW.defaultBlockState()
                .setValue(BlockStateProperties.ORIENTATION, FrontAndTop.WEST_UP);
        BlockState jigsawEast = Blocks.JIGSAW.defaultBlockState()
                .setValue(BlockStateProperties.ORIENTATION, FrontAndTop.EAST_UP);
        BlockState jigsawNorth = Blocks.JIGSAW.defaultBlockState()
                .setValue(BlockStateProperties.ORIENTATION, FrontAndTop.NORTH_UP);
        BlockState jigsawSouth = Blocks.JIGSAW.defaultBlockState()
                .setValue(BlockStateProperties.ORIENTATION, FrontAndTop.SOUTH_UP);

        BlockState torchSouth = Blocks.SOUL_WALL_TORCH.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);
        BlockState torchNorth = Blocks.SOUL_WALL_TORCH.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);

        ListTag paletteList = new ListTag();
        paletteList.add(NbtUtils.writeBlockState(wallBlock));       // 0
        paletteList.add(NbtUtils.writeBlockState(floorBlock));      // 1
        paletteList.add(NbtUtils.writeBlockState(jigsawWest));      // 2
        paletteList.add(NbtUtils.writeBlockState(jigsawEast));      // 3
        paletteList.add(NbtUtils.writeBlockState(jigsawNorth));     // 4
        paletteList.add(NbtUtils.writeBlockState(jigsawSouth));     // 5
        paletteList.add(NbtUtils.writeBlockState(Blocks.AIR.defaultBlockState())); // 6
        paletteList.add(NbtUtils.writeBlockState(torchSouth));      // 7
        paletteList.add(NbtUtils.writeBlockState(torchNorth));      // 8

        int sizeX = 13, sizeY = 7, sizeZ = 13;
        Set<BlockPos> skips = Set.of(
                new BlockPos(0, 1, 6), new BlockPos(0, 2, 6), new BlockPos(0, 3, 6),
                new BlockPos(12, 1, 6), new BlockPos(12, 2, 6), new BlockPos(12, 3, 6),
                new BlockPos(6, 1, 0), new BlockPos(6, 2, 0), new BlockPos(6, 3, 0),
                new BlockPos(6, 1, 12), new BlockPos(6, 2, 12), new BlockPos(6, 3, 12));
        ListTag blocksAtRoot = new ListTag();
        addRoomWallsAndFloorCeiling(blocksAtRoot, sizeX, sizeY, sizeZ, skips, 0, 1);
        blocksAtRoot.add(blockEntry(2, 0, 1, 6, jigsawNbt(JIGSAW_NAME, JIGSAW_TARGET, JIGSAW_POOL)));
        blocksAtRoot.add(blockEntry(3, 12, 1, 6, jigsawNbt(JIGSAW_NAME, JIGSAW_TARGET, JIGSAW_POOL)));
        blocksAtRoot.add(blockEntry(4, 6, 1, 0, jigsawNbt(JIGSAW_NAME, JIGSAW_TARGET, JIGSAW_POOL)));
        blocksAtRoot.add(blockEntry(5, 6, 1, 12, jigsawNbt(JIGSAW_NAME, JIGSAW_TARGET, JIGSAW_POOL)));
        addInteriorAir(blocksAtRoot, sizeX, sizeY, sizeZ, 6);
        blocksAtRoot.add(blockEntry(7, 6, 3, 1));   // torch on north wall
        blocksAtRoot.add(blockEntry(8, 6, 3, 11));  // torch on south wall

        ListTag palettes = new ListTag();
        palettes.add(paletteList);
        CompoundTag root = new CompoundTag();
        root.put("size", newIntegerList(sizeX, sizeY, sizeZ));
        root.put("blocks", blocksAtRoot);
        root.put("palettes", palettes);
        root.put("entities", new ListTag());
        return root;
    }

    /**
     * Large room variant (same 11x6x11) for pool variance.
     */
    private static CompoundTag buildLargeRoom2Structure(BlockState wallBlock, BlockState floorBlock) {
        return buildLargeRoomStructure(wallBlock, floorBlock);
    }

    /**
     * Dead-end room 9x6x9 with no jigsaw blocks (terminates a branch).
     */
    private static CompoundTag buildDeadEndStructure(BlockState wallBlock, BlockState floorBlock) {
        BlockState torchSouth = Blocks.SOUL_WALL_TORCH.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);
        BlockState torchNorth = Blocks.SOUL_WALL_TORCH.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);

        ListTag paletteList = new ListTag();
        paletteList.add(NbtUtils.writeBlockState(wallBlock));       // 0
        paletteList.add(NbtUtils.writeBlockState(floorBlock));      // 1
        paletteList.add(NbtUtils.writeBlockState(Blocks.AIR.defaultBlockState())); // 2
        paletteList.add(NbtUtils.writeBlockState(torchSouth));      // 3
        paletteList.add(NbtUtils.writeBlockState(torchNorth));      // 4

        int sizeX = 9, sizeY = 6, sizeZ = 9;
        ListTag blocksAtRoot = new ListTag();
        addRoomWallsAndFloorCeiling(blocksAtRoot, sizeX, sizeY, sizeZ, Set.of(), 0, 1);
        addInteriorAir(blocksAtRoot, sizeX, sizeY, sizeZ, 2);
        blocksAtRoot.add(blockEntry(3, 4, 3, 1));  // torch on north wall
        blocksAtRoot.add(blockEntry(4, 4, 3, 7));  // torch on south wall

        ListTag palettes = new ListTag();
        palettes.add(paletteList);
        CompoundTag root = new CompoundTag();
        root.put("size", newIntegerList(sizeX, sizeY, sizeZ));
        root.put("blocks", blocksAtRoot);
        root.put("palettes", palettes);
        root.put("entities", new ListTag());
        return root;
    }

    /**
     * Spiral staircase 5x90x5: stone-brick walls enclosing a 3x3 shaft with a center pillar and
     * rotating stairs. East jigsaw at (4,1,2) connects to room_connector's west entrance jigsaw.
     * No ceiling — the shaft exits open to the sky/terrain above.
     */
    private static CompoundTag buildSpiralStaircaseStructure(BlockState wallBlock) {
        BlockState jigsawEast = Blocks.JIGSAW.defaultBlockState()
                .setValue(BlockStateProperties.ORIENTATION, FrontAndTop.EAST_UP);
        BlockState stairEast = Blocks.STONE_BRICK_STAIRS.defaultBlockState()
                .setValue(BlockStateProperties.HALF, Half.BOTTOM)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, net.minecraft.core.Direction.EAST);
        BlockState stairSouth = Blocks.STONE_BRICK_STAIRS.defaultBlockState()
                .setValue(BlockStateProperties.HALF, Half.BOTTOM)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, net.minecraft.core.Direction.SOUTH);
        BlockState stairWest = Blocks.STONE_BRICK_STAIRS.defaultBlockState()
                .setValue(BlockStateProperties.HALF, Half.BOTTOM)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, net.minecraft.core.Direction.WEST);
        BlockState stairNorth = Blocks.STONE_BRICK_STAIRS.defaultBlockState()
                .setValue(BlockStateProperties.HALF, Half.BOTTOM)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, net.minecraft.core.Direction.NORTH);

        ListTag paletteList = new ListTag();
        paletteList.add(NbtUtils.writeBlockState(wallBlock));                        // 0: wall
        paletteList.add(NbtUtils.writeBlockState(stairEast));                        // 1
        paletteList.add(NbtUtils.writeBlockState(stairSouth));                       // 2
        paletteList.add(NbtUtils.writeBlockState(stairWest));                        // 3
        paletteList.add(NbtUtils.writeBlockState(stairNorth));                       // 4
        paletteList.add(NbtUtils.writeBlockState(jigsawEast));                       // 5
        paletteList.add(NbtUtils.writeBlockState(Blocks.AIR.defaultBlockState()));   // 6

        int sizeX = 5, sizeY = 90, sizeZ = 5;
        ListTag blocksAtRoot = new ListTag();

        // Floor (y=0)
        for (int x = 0; x < sizeX; x++) {
            for (int z = 0; z < sizeZ; z++) {
                blocksAtRoot.add(blockEntry(0, x, 0, z));
            }
        }
        // 4 exterior walls, y=1..sizeY-1 (no ceiling — open at top)
        for (int y = 1; y < sizeY; y++) {
            for (int x = 0; x < sizeX; x++) {
                blocksAtRoot.add(blockEntry(0, x, y, 0));           // N wall
                blocksAtRoot.add(blockEntry(0, x, y, sizeZ - 1));  // S wall
            }
            for (int z = 1; z < sizeZ - 1; z++) {
                blocksAtRoot.add(blockEntry(0, 0, y, z));           // W wall
                if (y == 1 && z == 2) continue;                     // jigsaw slot on E wall
                blocksAtRoot.add(blockEntry(0, sizeX - 1, y, z));  // E wall
            }
        }

        // East jigsaw at (4,1,2) — connects to room_connector's west entrance jigsaw
        blocksAtRoot.add(blockEntry(5, 4, 1, 2, jigsawNbt(JIGSAW_NAME, JIGSAW_TARGET, "minecraft:empty")));

        // Interior (x=1..3, y=1..sizeY-1, z=1..3): center pillar + clockwise spiral stairs + air
        // Spiral: phase 0=EAST(1,y,2), 1=SOUTH(2,y,1), 2=WEST(3,y,2), 3=NORTH(2,y,3)
        for (int y = 1; y < sizeY; y++) {
            blocksAtRoot.add(blockEntry(0, 2, y, 2));  // center pillar
            int phase = (y - 1) % 4;
            int sx, sz, ss;
            if      (phase == 0) { sx = 1; sz = 2; ss = 1; }
            else if (phase == 1) { sx = 2; sz = 1; ss = 2; }
            else if (phase == 2) { sx = 3; sz = 2; ss = 3; }
            else                 { sx = 2; sz = 3; ss = 4; }
            blocksAtRoot.add(blockEntry(ss, sx, y, sz));
            for (int x = 1; x < sizeX - 1; x++) {
                for (int z = 1; z < sizeZ - 1; z++) {
                    if (x == 2 && z == 2) continue;   // pillar
                    if (x == sx && z == sz) continue;  // stair
                    blocksAtRoot.add(blockEntry(6, x, y, z));
                }
            }
        }

        ListTag palettes = new ListTag();
        palettes.add(paletteList);
        CompoundTag root = new CompoundTag();
        root.put("size", newIntegerList(sizeX, sizeY, sizeZ));
        root.put("blocks", blocksAtRoot);
        root.put("palettes", palettes);
        root.put("entities", new ListTag());
        return root;
    }

    /** Fills interior (all positions not on the boundary) with air so rooms are hollow when placed in the world. */
    private static void addInteriorAir(ListTag blocksAtRoot, int sizeX, int sizeY, int sizeZ, int airStateId) {
        for (int x = 1; x < sizeX - 1; x++) {
            for (int y = 1; y < sizeY - 1; y++) {
                for (int z = 1; z < sizeZ - 1; z++) {
                    blocksAtRoot.add(blockEntry(airStateId, x, y, z));
                }
            }
        }
    }

    /** @param wallStateId palette index for walls and ceiling; @param floorStateId palette index for floor (y=0) */
    private static void addRoomWallsAndFloorCeiling(ListTag blocksAtRoot, int sizeX, int sizeY, int sizeZ, Set<BlockPos> skipPositions, int wallStateId, int floorStateId) {
        for (int x = 0; x < sizeX; x++) {
            for (int z = 0; z < sizeZ; z++) {
                if (!skipPositions.contains(new BlockPos(x, 0, z))) blocksAtRoot.add(blockEntry(floorStateId, x, 0, z));
                if (!skipPositions.contains(new BlockPos(x, sizeY - 1, z))) blocksAtRoot.add(blockEntry(wallStateId, x, sizeY - 1, z));
            }
        }
        for (int y = 1; y < sizeY - 1; y++) {
            for (int x = 0; x < sizeX; x++) {
                if (!skipPositions.contains(new BlockPos(x, y, 0))) blocksAtRoot.add(blockEntry(wallStateId, x, y, 0));
                if (sizeZ > 1 && !skipPositions.contains(new BlockPos(x, y, sizeZ - 1))) blocksAtRoot.add(blockEntry(wallStateId, x, y, sizeZ - 1));
            }
            for (int z = 1; z < sizeZ - 1; z++) {
                if (!skipPositions.contains(new BlockPos(0, y, z))) blocksAtRoot.add(blockEntry(wallStateId, 0, y, z));
                if (sizeX > 1 && !skipPositions.contains(new BlockPos(sizeX - 1, y, z))) blocksAtRoot.add(blockEntry(wallStateId, sizeX - 1, y, z));
            }
        }
    }

    private static ListTag newIntegerList(int... values) {
        ListTag list = new ListTag();
        for (int v : values) {
            list.add(IntTag.valueOf(v));
        }
        return list;
    }

    @Override
    @NotNull
    public String getName() {
        return "Ars Zero Structures";
    }
}
