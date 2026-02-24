package com.github.ars_zero.common.world.tree;

import com.github.ars_zero.registry.ModWorldgen;
import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerType;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * Larger variant of dead archwood: taller trunk (via config), longer branches, same 2x2 trunk and roots.
 */
public class BigDeadArchwoodTrunkPlacer extends TrunkPlacer {

    public BigDeadArchwoodTrunkPlacer(int baseHeight, int heightRandA, int heightRandB) {
        super(baseHeight, heightRandA, heightRandB);
    }

    @Override
    protected TrunkPlacerType<?> type() {
        return ModWorldgen.BIG_DEAD_ARCHWOOD_TRUNK_PLACER.get();
    }

    public static final MapCodec<BigDeadArchwoodTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(builder ->
        builder.group(
            Codec.intRange(0, 32).fieldOf("base_height").forGetter(placer -> placer.baseHeight),
            Codec.intRange(0, 24).fieldOf("height_rand_a").forGetter(placer -> placer.heightRandA),
            Codec.intRange(0, 24).fieldOf("height_rand_b").forGetter(placer -> placer.heightRandB)
        ).apply(builder, BigDeadArchwoodTrunkPlacer::new));

    protected static void setDirtAt(LevelSimulatedReader level, BiConsumer<BlockPos, BlockState> blockSetter,
                                    RandomSource random, BlockPos pos, TreeConfiguration config, boolean isTreeOrigin) {
        if (isTreeOrigin || level.isStateAtPosition(pos, state -> state.is(BlockTags.DIRT) || state.is(Blocks.FARMLAND))) {
            TrunkPlacer.setDirtAt(level, blockSetter, random, pos, config);
        }
    }

    @Override
    public List<FoliagePlacer.FoliageAttachment> placeTrunk(LevelSimulatedReader world, BiConsumer<BlockPos, BlockState> consumer,
                                                          RandomSource rand, int foliageHeight, BlockPos pos, TreeConfiguration config) {
        List<FoliagePlacer.FoliageAttachment> list = Lists.newArrayList();
        BlockPos blockpos = pos.below();
        setDirtAt(world, consumer, rand, blockpos, config, true);
        setDirtAt(world, consumer, rand, blockpos.east(), config, false);
        setDirtAt(world, consumer, rand, blockpos.south(), config, false);
        setDirtAt(world, consumer, rand, blockpos.south().east(), config, false);
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        int yOffset = y + foliageHeight - 1;

        int curX = x;
        int curZ = z;
        final int driftInterval = 4;
        final double driftChance = 0.4;

        int numBranches = 0;
        int lastBranch = 0;
        double branchChance = 0.5;
        boolean northB = rand.nextFloat() >= branchChance;
        boolean southB = rand.nextFloat() >= branchChance;
        boolean eastB = rand.nextFloat() >= branchChance;
        boolean westB = rand.nextFloat() >= branchChance;

        for (int i = 0; i < foliageHeight; ++i) {
            if (i > 0 && i % driftInterval == 0 && rand.nextDouble() < driftChance) {
                int dx = rand.nextInt(3) - 1;
                int dz = rand.nextInt(3) - 1;
                int nextX = Math.max(x - 1, Math.min(x + 1, curX + dx));
                int nextZ = Math.max(z - 1, Math.min(z + 1, curZ + dz));
                if (nextX != curX || nextZ != curZ) {
                    curX = nextX;
                    curZ = nextZ;
                }
            }

            int j2 = y + i;
            BlockPos blockpos1 = new BlockPos(curX, j2, curZ);
            if (TreeFeature.isAirOrLeaves(world, blockpos1)) {
                placeTrunkLogWithVariation(world, consumer, rand, blockpos1, config);
                placeTrunkLogWithVariation(world, consumer, rand, blockpos1.east(), config);
                placeTrunkLogWithVariation(world, consumer, rand, blockpos1.south(), config);
                placeTrunkLogWithVariation(world, consumer, rand, blockpos1.east().south(), config);
            }

            if (i == 0) {
                BlockPos abovePos = pos.above(i);
                addRoots(world, rand, abovePos.west(), consumer, config, new Direction[]{Direction.NORTH, Direction.WEST});
                addRoots(world, rand, abovePos.south(2), consumer, config, new Direction[]{Direction.SOUTH, Direction.WEST});
                addRoots(world, rand, abovePos.south().west(), consumer, config, new Direction[]{Direction.WEST});
                addRoots(world, rand, abovePos.south(2).east(), consumer, config, new Direction[]{Direction.EAST, Direction.SOUTH});
                addRoots(world, rand, abovePos.east(2), consumer, config, new Direction[]{Direction.EAST, Direction.NORTH});
                addRoots(world, rand, abovePos.east(2).south(), consumer, config, new Direction[]{Direction.EAST});
                addRoots(world, rand, abovePos.east().north(), consumer, config, new Direction[]{Direction.NORTH});
                addRoots(world, rand, abovePos.north(), consumer, config, new Direction[]{Direction.NORTH, Direction.EAST});
            }

            if (i > 1 && i > lastBranch) {
                BlockPos trunkAtHeight = new BlockPos(curX, y, curZ);
                if (northB) {
                    addBranchLogsOnlyBig(world, trunkAtHeight, i, Direction.NORTH, rand, config, consumer);
                    lastBranch = i;
                    numBranches++;
                    northB = false;
                } else if (southB) {
                    addBranchLogsOnlyBig(world, trunkAtHeight.relative(Direction.SOUTH), i, Direction.SOUTH, rand, config, consumer);
                    lastBranch = i;
                    numBranches++;
                    southB = false;
                } else if (eastB) {
                    addBranchLogsOnlyBig(world, trunkAtHeight.relative(Direction.EAST).south(), i, Direction.EAST, rand, config, consumer);
                    lastBranch = i;
                    numBranches++;
                    eastB = false;
                } else if (westB) {
                    addBranchLogsOnlyBig(world, trunkAtHeight, i, Direction.WEST, rand, config, consumer);
                    lastBranch = i;
                    numBranches++;
                    westB = false;
                } else if (numBranches == 0) {
                    addBranchLogsOnlyBig(world, trunkAtHeight, i, Direction.NORTH, rand, config, consumer);
                    lastBranch = i;
                    numBranches++;
                    addBranchLogsOnlyBig(world, trunkAtHeight, i, Direction.SOUTH, rand, config, consumer);
                    numBranches++;
                }
            }
        }

        list.add(new FoliagePlacer.FoliageAttachment(new BlockPos(curX, yOffset, curZ), 0, true));
        return list;
    }

    private void placeTrunkLogWithVariation(LevelSimulatedReader world, BiConsumer<BlockPos, BlockState> consumer,
                                           RandomSource random, BlockPos pos, TreeConfiguration config) {
        BlockState state = config.trunkProvider.getState(random, pos);
        if (state.hasProperty(RotatedPillarBlock.AXIS) && random.nextFloat() < 0.12f) {
            state = state.setValue(RotatedPillarBlock.AXIS, random.nextBoolean() ? Direction.Axis.X : Direction.Axis.Z);
        }
        if (TreeFeature.isAirOrLeaves(world, pos)) {
            consumer.accept(pos, state);
        }
    }

    /** Longer branches for big trees: more horizontal and vertical extent. */
    private void addBranchLogsOnlyBig(LevelSimulatedReader world, BlockPos pos, int height, Direction d,
                                      RandomSource random, TreeConfiguration config, BiConsumer<BlockPos, BlockState> consumer) {
        pos = pos.above(height);
        addLog(world, pos.relative(d), random, config, consumer);
        addLog(world, pos.relative(d).above(1), random, config, consumer);
        addLog(world, pos.relative(d).above(2), random, config, consumer);
        addLog(world, pos.relative(d, 2).above(2), random, config, consumer);
        addLog(world, pos.relative(d, 3).above(2), random, config, consumer);
        addLog(world, pos.relative(d, 3).above(3), random, config, consumer);
        addLog(world, pos.relative(d, 4).above(3), random, config, consumer);
        addLog(world, pos.relative(d, 4).above(2), random, config, consumer);
        addLog(world, pos.relative(d, 5).above(2), random, config, consumer);
        addLog(world, pos.relative(d, 5).above(1), random, config, consumer);
    }

    private boolean addLog(LevelSimulatedReader world, BlockPos pos, RandomSource random, TreeConfiguration config,
                          BiConsumer<BlockPos, BlockState> consumer) {
        return addBlock(world, pos, config.trunkProvider.getState(random, pos), consumer);
    }

    private boolean addBlock(LevelSimulatedReader world, BlockPos pos, BlockState state, BiConsumer<BlockPos, BlockState> consumer) {
        if (TreeFeature.validTreePos(world, pos)) {
            consumer.accept(pos, state);
            return true;
        }
        return false;
    }

    private void addRoots(LevelSimulatedReader world, RandomSource rand, BlockPos pos, BiConsumer<BlockPos, BlockState> consumer,
                         TreeConfiguration config, Direction[] extendedDirs) {
        BlockState state = config.trunkProvider.getState(rand, pos);
        if (rand.nextDouble() < 0.75 && TreeFeature.validTreePos(world, pos)) {
            consumer.accept(pos.immutable(), state);
            for (int i = 0; i < 2; i++) {
                if (TreeFeature.validTreePos(world, pos.below())) {
                    pos = pos.below();
                    consumer.accept(pos.immutable(), state);
                } else {
                    break;
                }
            }
            for (Direction d : extendedDirs) {
                placeRotatedRoot(world, rand, pos.below().relative(d), consumer, config, d);
            }
        }
    }

    private boolean placeRotatedRoot(LevelSimulatedReader world, RandomSource rand, BlockPos pos,
                                    BiConsumer<BlockPos, BlockState> consumer, TreeConfiguration config, Direction direction) {
        BlockState state = config.trunkProvider.getState(rand, pos);
        if (state.hasProperty(RotatedPillarBlock.AXIS)) {
            state = state.setValue(RotatedPillarBlock.AXIS, direction.getAxis());
        }
        if (rand.nextDouble() < 0.6 && validForExtendedRoot(world, pos)) {
            consumer.accept(pos.immutable(), state);
            int count = 0;
            while (rand.nextDouble() < 0.8 - count * 0.3) {
                count++;
                if (rand.nextDouble() < 0.7) {
                    direction = rand.nextDouble() < 0.5 ? direction.getClockWise() : direction.getCounterClockWise();
                    state = state.setValue(RotatedPillarBlock.AXIS, direction.getAxis());
                }
                pos = pos.relative(direction);
                if (TreeFeature.validTreePos(world, pos.below())) {
                    pos = pos.below();
                }
                if (validForExtendedRoot(world, pos)) {
                    consumer.accept(pos.immutable(), state);
                } else {
                    break;
                }
            }
            return true;
        }
        return false;
    }

    private static boolean validForExtendedRoot(LevelSimulatedReader world, BlockPos pos) {
        return TreeFeature.validTreePos(world, pos)
            || world.isStateAtPosition(pos, s -> s.getBlock() == Blocks.DIRT || s.getBlock() == Blocks.GRASS_BLOCK);
    }
}
