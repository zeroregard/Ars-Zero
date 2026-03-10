package com.github.ars_zero.common.world.structure;

import com.github.ars_zero.registry.ModWorldgen;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;

import java.util.Optional;

public class BlightDungeonStructure extends Structure {

    public static final MapCodec<BlightDungeonStructure> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(settingsCodec(instance),
                    StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(s -> s.startPool),
                    ResourceLocation.CODEC.optionalFieldOf("start_jigsaw_name").forGetter(s -> s.startJigsawName),
                    Codec.intRange(0, 128).fieldOf("size").forGetter(s -> s.size),
                    HeightProvider.CODEC.fieldOf("start_height").forGetter(s -> s.startHeight),
                    Heightmap.Types.CODEC.optionalFieldOf("project_start_to_heightmap").forGetter(s -> s.projectStartToHeightmap),
                    Codec.intRange(1, 128).fieldOf("max_distance_from_center").forGetter(s -> s.maxDistanceFromCenter),
                    Codec.BOOL.fieldOf("use_expansion_hack").forGetter(s -> s.useExpansionHack)
            ).apply(instance, BlightDungeonStructure::new));

    private final Holder<StructureTemplatePool> startPool;
    private final Optional<ResourceLocation> startJigsawName;
    private final int size;
    private final HeightProvider startHeight;
    private final Optional<Heightmap.Types> projectStartToHeightmap;
    private final int maxDistanceFromCenter;
    private final boolean useExpansionHack;

    public BlightDungeonStructure(Structure.StructureSettings config,
                                 Holder<StructureTemplatePool> startPool,
                                 Optional<ResourceLocation> startJigsawName,
                                 int size,
                                 HeightProvider startHeight,
                                 Optional<Heightmap.Types> projectStartToHeightmap,
                                 int maxDistanceFromCenter,
                                 boolean useExpansionHack) {
        super(config);
        this.startPool = startPool;
        this.startJigsawName = startJigsawName;
        this.size = size;
        this.startHeight = startHeight;
        this.projectStartToHeightmap = projectStartToHeightmap;
        this.maxDistanceFromCenter = maxDistanceFromCenter;
        this.useExpansionHack = useExpansionHack;
    }

    @Override
    public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        int x = chunkPos.getMiddleBlockX();
        int z = chunkPos.getMiddleBlockZ();

        // Reject placement on water — check ocean floor vs world surface; if they differ, it's liquid
        int oceanFloor = context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
        int worldSurface = context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
        if (worldSurface > oceanFloor) {
            return Optional.empty();
        }

        // The entrance jigsaw is at y=0 (bottom) of the 12-tall staircase piece.
        // Place it so the top of the piece (y=11) is flush with the surface.
        // Jigsaw anchors the named connector to blockPos.y, so offset = surfaceY - 11.
        BlockPos blockPos = new BlockPos(chunkPos.getMinBlockX(), worldSurface - 11, chunkPos.getMinBlockZ());

        // Dynamically size: enough staircase segments to reach Y=30, plus fixed budget for dungeon rooms.
        int staircaseSegments = Math.max(1, (int) Math.ceil((worldSurface - 30) / 12.0));
        int dynamicSize = staircaseSegments + 20; // 20 extra slots for dungeon rooms/hallways

        return JigsawPlacement.addPieces(
                context,
                this.startPool,
                this.startJigsawName,
                dynamicSize,
                blockPos,
                this.useExpansionHack,
                this.projectStartToHeightmap,
                this.maxDistanceFromCenter,
                PoolAliasLookup.EMPTY,
                DimensionPadding.ZERO,
                LiquidSettings.IGNORE_WATERLOGGING);
    }

    @Override
    public StructureType<?> type() {
        return ModWorldgen.NECROPOLIS_STRUCTURE.get();
    }
}
