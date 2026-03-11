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

    /** Height of one staircase NBT piece in blocks. */
    private static final int STAIRCASE_HEIGHT = 12;
    /** Target Y level for the dungeon rooms floor. */
    private static final int DUNGEON_TARGET_Y = 30;

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

        // Reject placement on water — if ocean floor differs from world surface, it's liquid
        int oceanFloor = context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
        int worldSurface = context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
        if (worldSurface > oceanFloor) {
            return Optional.empty();
        }

        // Compute exactly how many staircase pieces are needed to descend from surface to DUNGEON_TARGET_Y.
        // Each piece is STAIRCASE_HEIGHT blocks tall. The top of piece 0 is at worldSurface.
        int staircaseSegments = Math.max(1, (int) Math.ceil((double)(worldSurface - DUNGEON_TARGET_Y) / STAIRCASE_HEIGHT));
        int dungeonY = worldSurface - (staircaseSegments * STAIRCASE_HEIGHT);

        // startX/Z for pieces: align to chunk min so the staircase entrance is predictably placed
        int startX = chunkPos.getMinBlockX();
        int startZ = chunkPos.getMinBlockZ();

        // Jigsaw start: place room_connector via start_pool=connector, start_jigsaw_name=connector.
        // Jigsaw subtracts the connector jigsaw local pos (8,7,8) from dungeonStart to get piece origin,
        // then processes all 4 passage exits automatically.
        // Staircase bottom jigsaw is at world (startX+4, dungeonY, startZ+4).
        // Connector jigsaw should sit at (startX+4, dungeonY-1, startZ+4) — one block below, faces touching.
        BlockPos dungeonStart = new BlockPos(startX + 4, dungeonY, startZ + 4);

        return Optional.of(new Structure.GenerationStub(dungeonStart, builder -> {
            // 1. Place exactly staircaseSegments staircase pieces, stacked downward from the surface.
            for (int i = 0; i < staircaseSegments; i++) {
                int pieceY = worldSurface - ((i + 1) * STAIRCASE_HEIGHT);
                BlockPos piecePos = new BlockPos(startX, pieceY, startZ);
                builder.addPiece(new NecropolisStaircasePiece(context.structureTemplateManager(), piecePos));
            }

            // 2. Jigsaw places room_connector first (start_pool=connector, start_jigsaw_name=connector),
            //    then automatically fans out through all 4 passage exits into hallways.
            JigsawPlacement.addPieces(
                    context,
                    this.startPool,
                    Optional.of(ResourceLocation.fromNamespaceAndPath("ars_zero", "necropolis/connector")),
                    15,
                    dungeonStart,
                    this.useExpansionHack,
                    this.projectStartToHeightmap,
                    this.maxDistanceFromCenter,
                    PoolAliasLookup.EMPTY,
                    DimensionPadding.ZERO,
                    LiquidSettings.IGNORE_WATERLOGGING)
                .ifPresent(stub -> stub.getPiecesBuilder().build().pieces().forEach(builder::addPiece));
        }));
    }

    @Override
    public StructureType<?> type() {
        return ModWorldgen.NECROPOLIS_STRUCTURE.get();
    }
}
