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
            Path path = output.getOutputFolder()
                    .resolve("data/" + MOD_ID + "/structure/single_block.nbt");
            CompoundTag root = buildSingleBlockStructure();
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
        });
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
