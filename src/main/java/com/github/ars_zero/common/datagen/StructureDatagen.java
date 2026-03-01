package com.github.ars_zero.common.datagen;

import com.google.common.hash.Hashing;
import net.minecraft.core.BlockPos;
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
            CompletableFuture<?> roomConnector = writeStructure(cachedOutput, "room_connector", buildRoomConnectorStructure(wallBlock));
            CompletableFuture<?> roomConnectorBack = writeStructure(cachedOutput, "room_connector_back", buildRoomConnectorBackStructure(wallBlock));
            return CompletableFuture.allOf(roomConnector, roomConnectorBack);
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

    private static final String JIGSAW_NAME = "ars_zero:room_connector";
    private static final String JIGSAW_POOL = "ars_zero:blight_dungeon/connector";
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
     * Room with jigsaw on east wall (x=4) at (4,1,2), facing east. Tunnel opening at (4,1,2) and (4,2,2).
     */
    private static CompoundTag buildRoomConnectorStructure(BlockState wallBlock) {
        BlockState jigsawEast = Blocks.JIGSAW.defaultBlockState()
                .setValue(BlockStateProperties.ORIENTATION, FrontAndTop.EAST_UP);

        ListTag paletteList = new ListTag();
        paletteList.add(NbtUtils.writeBlockState(wallBlock));
        paletteList.add(NbtUtils.writeBlockState(jigsawEast));

        int sizeX = 5, sizeY = 4, sizeZ = 5;
        ListTag blocksAtRoot = new ListTag();
        addRoomWallsAndFloorCeiling(blocksAtRoot, sizeX, sizeY, sizeZ, Set.of(new BlockPos(4, 1, 2), new BlockPos(4, 2, 2)));
        blocksAtRoot.add(blockEntry(1, 4, 1, 2, jigsawNbt(JIGSAW_NAME, JIGSAW_NAME, JIGSAW_POOL)));

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
     * Room with jigsaw on west wall (x=0) at (0,1,2), facing west. Tunnel opening at (0,1,2) and (0,2,2).
     */
    private static CompoundTag buildRoomConnectorBackStructure(BlockState wallBlock) {
        BlockState jigsawWest = Blocks.JIGSAW.defaultBlockState()
                .setValue(BlockStateProperties.ORIENTATION, FrontAndTop.WEST_UP);

        ListTag paletteList = new ListTag();
        paletteList.add(NbtUtils.writeBlockState(wallBlock));
        paletteList.add(NbtUtils.writeBlockState(jigsawWest));

        int sizeX = 5, sizeY = 4, sizeZ = 5;
        ListTag blocksAtRoot = new ListTag();
        addRoomWallsAndFloorCeiling(blocksAtRoot, sizeX, sizeY, sizeZ, Set.of(new BlockPos(0, 1, 2), new BlockPos(0, 2, 2)));
        blocksAtRoot.add(blockEntry(1, 0, 1, 2, jigsawNbt(JIGSAW_NAME, JIGSAW_NAME, JIGSAW_POOL)));

        ListTag palettes = new ListTag();
        palettes.add(paletteList);
        CompoundTag root = new CompoundTag();
        root.put("size", newIntegerList(sizeX, sizeY, sizeZ));
        root.put("blocks", blocksAtRoot);
        root.put("palettes", palettes);
        root.put("entities", new ListTag());
        return root;
    }

    private static void addRoomWallsAndFloorCeiling(ListTag blocksAtRoot, int sizeX, int sizeY, int sizeZ, Set<BlockPos> skipPositions) {
        for (int x = 0; x < sizeX; x++) {
            for (int z = 0; z < sizeZ; z++) {
                if (!skipPositions.contains(new BlockPos(x, 0, z))) blocksAtRoot.add(blockEntry(0, x, 0, z));
                if (!skipPositions.contains(new BlockPos(x, sizeY - 1, z))) blocksAtRoot.add(blockEntry(0, x, sizeY - 1, z));
            }
        }
        for (int y = 1; y < sizeY - 1; y++) {
            for (int x = 0; x < sizeX; x++) {
                if (!skipPositions.contains(new BlockPos(x, y, 0))) blocksAtRoot.add(blockEntry(0, x, y, 0));
                if (sizeZ > 1 && !skipPositions.contains(new BlockPos(x, y, sizeZ - 1))) blocksAtRoot.add(blockEntry(0, x, y, sizeZ - 1));
            }
            for (int z = 1; z < sizeZ - 1; z++) {
                if (!skipPositions.contains(new BlockPos(0, y, z))) blocksAtRoot.add(blockEntry(0, 0, y, z));
                if (sizeX > 1 && !skipPositions.contains(new BlockPos(sizeX - 1, y, z))) blocksAtRoot.add(blockEntry(0, sizeX - 1, y, z));
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
