package com.github.ars_zero.common.datagen;

import com.google.common.hash.Hashing;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

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
            CompletableFuture<?> singleBlock = writeStructure(cachedOutput, "single_block", buildSingleBlockStructure());
            CompletableFuture<?> simpleRoom = writeStructure(cachedOutput, "simple_room", buildSimpleRoomStructure());
            return CompletableFuture.allOf(singleBlock, simpleRoom);
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

    /**
     * Build NBT for the single_block structure (3x3x3 stone cube).
     * Format must match StructureTemplate.load():
     * - "size": ListTag of 3 Int (not IntArray)
     * - "blocks": ListTag of compounds at ROOT; each has "pos" = ListTag of 3 Int, "state" = Int
     * - "palettes": ListTag of ListTags (each inner list = one palette = block state compounds)
     * - "entities": ListTag
     */
    private static CompoundTag buildSingleBlockStructure() {
        BlockState stone = Blocks.STONE.defaultBlockState();
        CompoundTag blockStateTag = NbtUtils.writeBlockState(stone);

        ListTag paletteList = new ListTag();
        paletteList.add(blockStateTag);

        ListTag blocksAtRoot = new ListTag();
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                for (int z = 0; z < 3; z++) {
                    CompoundTag entry = new CompoundTag();
                    entry.put("pos", newIntegerList(x, y, z));
                    entry.putInt("state", 0);
                    blocksAtRoot.add(entry);
                }
            }
        }

        ListTag palettes = new ListTag();
        palettes.add(paletteList);

        CompoundTag root = new CompoundTag();
        root.put("size", newIntegerList(3, 3, 3));
        root.put("blocks", blocksAtRoot);
        root.put("palettes", palettes);
        root.put("entities", new ListTag());
        return root;
    }

    /**
     * Build NBT for a simple room: 5x4x5 box, stone brick floor/walls/ceiling, hollow inside.
     */
    private static CompoundTag buildSimpleRoomStructure() {
        BlockState stoneBricks = Blocks.STONE_BRICKS.defaultBlockState();
        CompoundTag blockStateTag = NbtUtils.writeBlockState(stoneBricks);

        ListTag paletteList = new ListTag();
        paletteList.add(blockStateTag);

        int sizeX = 5, sizeY = 4, sizeZ = 5;
        ListTag blocksAtRoot = new ListTag();
        for (int x = 0; x < sizeX; x++) {
            for (int z = 0; z < sizeZ; z++) {
                blocksAtRoot.add(blockEntry(0, x, 0, z));           // floor (y=0)
                blocksAtRoot.add(blockEntry(0, x, sizeY - 1, z));   // ceiling (y=3)
            }
        }
        for (int y = 1; y < sizeY - 1; y++) {
            for (int x = 0; x < sizeX; x++) {
                blocksAtRoot.add(blockEntry(0, x, y, 0));           // wall z=0
                if (sizeZ > 1) blocksAtRoot.add(blockEntry(0, x, y, sizeZ - 1)); // wall z=4
            }
            for (int z = 1; z < sizeZ - 1; z++) {
                blocksAtRoot.add(blockEntry(0, 0, y, z));           // wall x=0
                if (sizeX > 1) blocksAtRoot.add(blockEntry(0, sizeX - 1, y, z)); // wall x=4
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

    private static CompoundTag blockEntry(int stateId, int x, int y, int z) {
        CompoundTag entry = new CompoundTag();
        entry.put("pos", newIntegerList(x, y, z));
        entry.putInt("state", stateId);
        return entry;
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
