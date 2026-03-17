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
 * Huge variant of dead archwood: 3x3 hollow trunk (center is air), 2x1 entrance at bottom for spawns,
 * very tall (via config), very long branches, roots, and cobwebs.
 */
public class HugeDeadArchwoodTrunkPlacer extends TrunkPlacer {

    public HugeDeadArchwoodTrunkPlacer(int baseHeight, int heightRandA, int heightRandB) {
        super(baseHeight, heightRandA, heightRandB);
    }

    @Override
    protected TrunkPlacerType<?> type() {
        return ModWorldgen.HUGE_DEAD_ARCHWOOD_TRUNK_PLACER.get();
    }

    public static final MapCodec<HugeDeadArchwoodTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(builder ->
        builder.group(
            Codec.intRange(0, 32).fieldOf("base_height").forGetter(placer -> placer.baseHeight),
            Codec.intRange(0, 24).fieldOf("height_rand_a").forGetter(placer -> placer.heightRandA),
            Codec.intRange(0, 24).fieldOf("height_rand_b").forGetter(placer -> placer.heightRandB)
        ).apply(builder, HugeDeadArchwoodTrunkPlacer::new));

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
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        // 3x3 trunk centered on (x,z): min corner curX, curZ with curX+1, curZ+1 = center (hollow)
        int curX = x - 1;
        int curZ = z - 1;

        // Dirt under entire 3x3 footprint
        BlockPos ground = pos.below();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                setDirtAt(world, consumer, rand, ground.offset(dx, 0, dz), config, dx == 0 && dz == 0);
            }
        }

        int yOffset = y + foliageHeight - 1;
        final int driftInterval = 5;
        final double driftChance = 0.4;

        // 2x1 entrance at bottom: pick one of four sides, leave two adjacent blocks of the ring as air
        Direction entranceSide = Direction.Plane.HORIZONTAL.getRandomDirection(rand);

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
                int nextX = Math.max(x - 2, Math.min(x, curX + dx));
                int nextZ = Math.max(z - 2, Math.min(z, curZ + dz));
                if (nextX != curX || nextZ != curZ) {
                    curX = nextX;
                    curZ = nextZ;
                }
            }

            int j2 = y + i;
            BlockPos base = new BlockPos(curX, j2, curZ);
            boolean isEntranceLevel = (i == 0);

            // 3x3 ring (8 logs), center (base.east().south()) stays air = hollow
            placeHollowTrunkRing(world, consumer, rand, base, config, isEntranceLevel, entranceSide);

            // Cobwebs in the upper part of the tree (top half)
            if (i >= foliageHeight / 2 && rand.nextFloat() < 0.175f) {
                tryPlaceCobwebAround3x3(world, consumer, rand, base);
            }

            if (i == 0) {
                addRoots3x3(world, rand, curX, y, curZ, consumer, config);
            }

            // Branches only in the upper half of the tree (halfway up and above)
            if (i >= foliageHeight / 2 && i > lastBranch) {
                BlockPos northFace = new BlockPos(curX + 1, y, curZ);
                BlockPos southFace = new BlockPos(curX + 1, y, curZ + 2);
                BlockPos eastFace = new BlockPos(curX + 2, y, curZ + 1);
                BlockPos westFace = new BlockPos(curX, y, curZ + 1);
                if (northB) {
                    addBranchLogsOnlyHuge(world, northFace, i, Direction.NORTH, rand, config, consumer);
                    lastBranch = i;
                    numBranches++;
                    northB = false;
                } else if (southB) {
                    addBranchLogsOnlyHuge(world, southFace, i, Direction.SOUTH, rand, config, consumer);
                    lastBranch = i;
                    numBranches++;
                    southB = false;
                } else if (eastB) {
                    addBranchLogsOnlyHuge(world, eastFace, i, Direction.EAST, rand, config, consumer);
                    lastBranch = i;
                    numBranches++;
                    eastB = false;
                } else if (westB) {
                    addBranchLogsOnlyHuge(world, westFace, i, Direction.WEST, rand, config, consumer);
                    lastBranch = i;
                    numBranches++;
                    westB = false;
                } else if (numBranches == 0) {
                    addBranchLogsOnlyHuge(world, northFace, i, Direction.NORTH, rand, config, consumer);
                    lastBranch = i;
                    numBranches++;
                    addBranchLogsOnlyHuge(world, southFace, i, Direction.SOUTH, rand, config, consumer);
                    numBranches++;
                }
            }
        }

        list.add(new FoliagePlacer.FoliageAttachment(new BlockPos(curX + 1, yOffset, curZ + 1), 0, true));
        return list;
    }

    /** Places the 8 log blocks of a 3x3 ring; skips center and (at i==0) the 2x1 entrance on entranceSide. */
    private void placeHollowTrunkRing(LevelSimulatedReader world, BiConsumer<BlockPos, BlockState> consumer,
                                      RandomSource rand, BlockPos base, TreeConfiguration config,
                                      boolean isEntranceLevel, Direction entranceSide) {
        // Ring: (0,0),(1,0),(2,0), (0,1),(2,1), (0,2),(1,2),(2,2) relative to base. Skip (1,1) = center.
        for (int rx = 0; rx < 3; rx++) {
            for (int rz = 0; rz < 3; rz++) {
                if (rx == 1 && rz == 1) continue; // hollow center
                if (isEntranceLevel && isEntranceBlock(rx, rz, entranceSide)) continue; // 2x1 entrance
                BlockPos p = base.offset(rx, 0, rz);
                if (TreeFeature.isAirOrLeaves(world, p)) {
                    placeTrunkLogWithVariation(world, consumer, rand, p, config);
                }
            }
        }
    }

    /** True if (rx,rz) in 0..2 is one of the two blocks that form the 2x1 entrance on the given side. */
    private static boolean isEntranceBlock(int rx, int rz, Direction side) {
        return switch (side) {
            case NORTH -> rz == 0 && (rx == 1 || rx == 2); // front row, two blocks
            case SOUTH -> rz == 2 && (rx == 0 || rx == 1);
            case EAST -> rx == 2 && (rz == 1 || rz == 2);
            case WEST -> rx == 0 && (rz == 0 || rz == 1);
            default -> false;
        };
    }

    /** Roots around the 3x3 trunk footprint. */
    private void addRoots3x3(LevelSimulatedReader world, RandomSource rand, int curX, int y, int curZ,
                            BiConsumer<BlockPos, BlockState> consumer, TreeConfiguration config) {
        BlockPos center = new BlockPos(curX + 1, y, curZ + 1);
        addRoots(world, rand, center.west(2), consumer, config, new Direction[]{Direction.NORTH, Direction.WEST});
        addRoots(world, rand, center.west(2).south(), consumer, config, new Direction[]{Direction.WEST});
        addRoots(world, rand, center.west(2).south(2), consumer, config, new Direction[]{Direction.SOUTH, Direction.WEST});
        addRoots(world, rand, center.north(), consumer, config, new Direction[]{Direction.NORTH, Direction.EAST});
        addRoots(world, rand, center.north(2), consumer, config, new Direction[]{Direction.NORTH});
        addRoots(world, rand, center.east(2).north(), consumer, config, new Direction[]{Direction.EAST, Direction.NORTH});
        addRoots(world, rand, center.east(2), consumer, config, new Direction[]{Direction.EAST, Direction.SOUTH});
        addRoots(world, rand, center.east(2).south(), consumer, config, new Direction[]{Direction.EAST});
        addRoots(world, rand, center.south(2).east(), consumer, config, new Direction[]{Direction.SOUTH});
        addRoots(world, rand, center.south(2), consumer, config, new Direction[]{Direction.SOUTH, Direction.EAST});
        addRoots(world, rand, center.south(2).west(), consumer, config, new Direction[]{Direction.WEST});
        addRoots(world, rand, center.north().west(), consumer, config, new Direction[]{Direction.NORTH});
    }

    /** Cobweb candidates around the 3x3 trunk: hollow center above, or outside the ring (never in the log ring). */
    private void tryPlaceCobwebAround3x3(LevelSimulatedReader world, BiConsumer<BlockPos, BlockState> consumer,
                                         RandomSource random, BlockPos base) {
        BlockPos center = base.offset(1, 0, 1);
        // Hollow at next level is safe; same-level positions outside the 3x3 ring are safe
        BlockPos[] candidates = {
            center.above(),           // hollow at next level
            base.west(), base.north(), base.east(3), base.south(3),
            base.west().north(), base.east(3).north(), base.east(3).south(3), base.west().south(3)
        };
        for (int k = 0; k < 4; k++) {
            BlockPos p = candidates[random.nextInt(candidates.length)];
            if (TreeFeature.isAirOrLeaves(world, p)) {
                consumer.accept(p, Blocks.COBWEB.defaultBlockState());
                break;
            }
        }
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

    /** Very long branches for huge trees: extends further out and higher than big variant. */
    private void addBranchLogsOnlyHuge(LevelSimulatedReader world, BlockPos pos, int height, Direction d,
                                       RandomSource random, TreeConfiguration config, BiConsumer<BlockPos, BlockState> consumer) {
        pos = pos.above(height);
        addLog(world, pos.relative(d), random, config, consumer);
        addLog(world, pos.relative(d).above(1), random, config, consumer);
        addLog(world, pos.relative(d).above(2), random, config, consumer);
        addLog(world, pos.relative(d, 2).above(2), random, config, consumer);
        addLog(world, pos.relative(d, 3).above(2), random, config, consumer);
        addLog(world, pos.relative(d, 3).above(3), random, config, consumer);
        addLog(world, pos.relative(d, 4).above(3), random, config, consumer);
        addLog(world, pos.relative(d, 4).above(4), random, config, consumer);
        addLog(world, pos.relative(d, 5).above(4), random, config, consumer);
        addLog(world, pos.relative(d, 5).above(3), random, config, consumer);
        addLog(world, pos.relative(d, 6).above(3), random, config, consumer);
        addLog(world, pos.relative(d, 6).above(2), random, config, consumer);
        addLog(world, pos.relative(d, 7).above(2), random, config, consumer);
        addLog(world, pos.relative(d, 7).above(1), random, config, consumer);
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
