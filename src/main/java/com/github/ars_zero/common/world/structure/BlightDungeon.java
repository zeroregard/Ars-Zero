package com.github.ars_zero.common.world.structure;

import com.github.ars_zero.registry.ModStructures;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;

import java.util.Optional;

/**
 * Dungeon-like jigsaw structure for the Blight Forest: surface entrance,
 * interior of SourceStone and Blackstone bricks, repurpose room (no end portal).
 */
public class BlightDungeon extends Structure {

    public static final MapCodec<BlightDungeon> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(BlightDungeon.settingsCodec(instance),
            StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(s -> s.startPool),
            ResourceLocation.CODEC.optionalFieldOf("start_jigsaw_name").forGetter(s -> s.startJigsawName),
            Codec.intRange(0, 30).fieldOf("size").forGetter(s -> s.size),
            HeightProvider.CODEC.fieldOf("start_height").forGetter(s -> s.startHeight),
            Heightmap.Types.CODEC.optionalFieldOf("project_start_to_heightmap").forGetter(s -> s.projectStartToHeightmap),
            Codec.intRange(1, 128).fieldOf("max_distance_from_center").forGetter(s -> s.maxDistanceFromCenter)
        ).apply(instance, BlightDungeon::new));

    public final Holder<StructureTemplatePool> startPool;
    public final Optional<ResourceLocation> startJigsawName;
    public final int size;
    public final HeightProvider startHeight;
    public final Optional<Heightmap.Types> projectStartToHeightmap;
    public final int maxDistanceFromCenter;
    public Optional<Integer> terrainHeightCheckRadius = Optional.empty();
    public Optional<Integer> allowedTerrainHeightRange = Optional.empty();
    public boolean cannotSpawnInLiquid = true;
    public Optional<Integer> minYAllowed = Optional.empty();
    public Optional<Integer> maxYAllowed = Optional.empty();

    public BlightDungeon(
        Structure.StructureSettings config,
        Holder<StructureTemplatePool> startPool,
        Optional<ResourceLocation> startJigsawName,
        int size,
        HeightProvider startHeight,
        Optional<Heightmap.Types> projectStartToHeightmap,
        int maxDistanceFromCenter
    ) {
        super(config);
        this.startPool = startPool;
        this.startJigsawName = startJigsawName;
        this.size = size;
        this.startHeight = startHeight;
        this.projectStartToHeightmap = projectStartToHeightmap;
        this.maxDistanceFromCenter = maxDistanceFromCenter;
        this.terrainHeightCheckRadius = Optional.of(1);
        this.allowedTerrainHeightRange = Optional.of(6);
    }

    @Override
    public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
        var chunkPos = context.chunkPos();
        if (this.cannotSpawnInLiquid) {
            BlockPos centerOfChunk = context.chunkPos().getMiddleBlockPosition(0);
            int landHeight = context.chunkGenerator().getFirstOccupiedHeight(
                centerOfChunk.getX(), centerOfChunk.getZ(),
                Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
            NoiseColumn columnOfBlocks = context.chunkGenerator().getBaseColumn(
                centerOfChunk.getX(), centerOfChunk.getZ(), context.heightAccessor(), context.randomState());
            BlockState topBlock = columnOfBlocks.getBlock(centerOfChunk.getY() + landHeight);
            if (!topBlock.getFluidState().isEmpty()) {
                return Optional.empty();
            }
        }

        if (this.terrainHeightCheckRadius.isPresent() &&
            (this.allowedTerrainHeightRange.isPresent() || this.minYAllowed.isPresent())) {
            int maxTerrainHeight = Integer.MIN_VALUE;
            int minTerrainHeight = Integer.MAX_VALUE;
            int terrainCheckRange = this.terrainHeightCheckRadius.get();
            for (int curChunkX = chunkPos.x - terrainCheckRange; curChunkX <= chunkPos.x + terrainCheckRange; curChunkX++) {
                for (int curChunkZ = chunkPos.z - terrainCheckRange; curChunkZ <= chunkPos.z + terrainCheckRange; curChunkZ++) {
                    int height = context.chunkGenerator().getBaseHeight(
                        (curChunkX << 4) + 7, (curChunkZ << 4) + 7,
                        this.projectStartToHeightmap.orElse(Heightmap.Types.WORLD_SURFACE_WG),
                        context.heightAccessor(), context.randomState());
                    maxTerrainHeight = Math.max(maxTerrainHeight, height);
                    minTerrainHeight = Math.min(minTerrainHeight, height);
                    if (this.minYAllowed.isPresent() && minTerrainHeight < this.minYAllowed.get()) {
                        return Optional.empty();
                    }
                    if (this.maxYAllowed.isPresent() && minTerrainHeight > this.maxYAllowed.get()) {
                        return Optional.empty();
                    }
                }
            }
            if (this.allowedTerrainHeightRange.isPresent() &&
                maxTerrainHeight - minTerrainHeight > this.allowedTerrainHeightRange.get()) {
                return Optional.empty();
            }
        }

        int startY = this.startHeight.sample(context.random(),
            new WorldGenerationContext(context.chunkGenerator(), context.heightAccessor()));
        BlockPos blockPos = new BlockPos(chunkPos.getMinBlockX(), startY, chunkPos.getMinBlockZ());

        return JigsawPlacement.addPieces(
            context,
            this.startPool,
            this.startJigsawName,
            this.size,
            blockPos,
            false,
            this.projectStartToHeightmap,
            this.maxDistanceFromCenter,
            PoolAliasLookup.EMPTY,
            DimensionPadding.ZERO,
            LiquidSettings.APPLY_WATERLOGGING
        );
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.BLIGHT_DUNGEON.get();
    }
}
