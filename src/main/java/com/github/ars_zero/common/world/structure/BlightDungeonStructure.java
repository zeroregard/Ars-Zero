package com.github.ars_zero.common.world.structure;

import com.github.ars_zero.registry.ModWorldgen;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
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
                    Codec.intRange(0, 30).fieldOf("size").forGetter(s -> s.size),
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
        int startY = this.startHeight.sample(context.random(), new WorldGenerationContext(context.chunkGenerator(), context.heightAccessor()));
        BlockPos blockPos = new BlockPos(chunkPos.getMinBlockX(), startY, chunkPos.getMinBlockZ());

        return JigsawPlacement.addPieces(
                context,
                this.startPool,
                this.startJigsawName,
                this.size,
                blockPos,
                this.useExpansionHack,
                this.projectStartToHeightmap,
                this.maxDistanceFromCenter,
                PoolAliasLookup.EMPTY,
                DimensionPadding.ZERO,
                LiquidSettings.APPLY_WATERLOGGING);
    }

    @Override
    public void afterPlace(WorldGenLevel level, StructureManager manager, ChunkGenerator chunkGenerator, RandomSource random,
                          BoundingBox box, ChunkPos chunkPos, PiecesContainer pieces) {
        super.afterPlace(level, manager, chunkGenerator, random, box, chunkPos, pieces);
        if (pieces.pieces().isEmpty()) return;

        // Start piece is the first (room_connector 9x6x9). West face is at minX.
        BoundingBox startBox = pieces.pieces().get(0).getBoundingBox();
        int baseX = startBox.minX() - 1;
        int baseY = startBox.minY();
        int baseZ = startBox.minZ();

        // Sample height from a position guaranteed to be within the accessible WorldGenRegion
        int sampleX = Math.max(baseX, box.minX());
        int sampleZ = Math.max(baseZ, box.minZ());
        int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, sampleX, sampleZ);
        int height = Math.max(3, Math.min(120, surfaceY - baseY));

        BlockState wallBlock = level.registryAccess().registry(Registries.BLOCK)
                .flatMap(r -> r.getOptional(ResourceLocation.parse("ars_nouveau:sourcestone_large_bricks")))
                .map(b -> b.defaultBlockState())
                .orElse(Blocks.STONE_BRICKS.defaultBlockState());
        BlockState stairBlock = Blocks.STONE_STAIRS.defaultBlockState()
                .setValue(BlockStateProperties.HALF, Half.BOTTOM)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, net.minecraft.core.Direction.WEST);

        final int sizeX = 5;
        final int sizeZ = 3;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < sizeX; x++) {
                BlockPos posN = new BlockPos(baseX + x, baseY + y, baseZ);
                BlockPos posS = new BlockPos(baseX + x, baseY + y, baseZ + sizeZ - 1);
                if (box.isInside(posN)) level.setBlock(posN, wallBlock, 2);
                if (box.isInside(posS)) level.setBlock(posS, wallBlock, 2);
            }
            int stairX = y % sizeX;
            if (y >= 1) {
                BlockPos stairPos = new BlockPos(baseX + stairX, baseY + y, baseZ + 1);
                if (box.isInside(stairPos)) level.setBlock(stairPos, stairBlock, 2);
            }
            for (int x = 0; x < sizeX; x++) {
                if (x != stairX || y < 1) {
                    BlockPos airPos = new BlockPos(baseX + x, baseY + y, baseZ + 1);
                    if (box.isInside(airPos)) level.setBlock(airPos, Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
    }

    @Override
    public StructureType<?> type() {
        return ModWorldgen.BLIGHT_DUNGEON_STRUCTURE.get();
    }
}
