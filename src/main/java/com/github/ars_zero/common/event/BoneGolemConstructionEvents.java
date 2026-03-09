package com.github.ars_zero.common.event;

import com.github.ars_zero.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Handles the Bone Golem construction mechanic.
 *
 * Construction pattern (axis-independent, checked for both X and Z arms):
 *
 *        [skull]       ← placed block (y)
 *        [bone]        ← y-1, center  (upper body)
 *   [bone][bone][bone] ← y-1, left / center / right  (arms + body)
 *        [bone]        ← y-2, center  (lower body)
 *
 * In other words, 4 bone blocks form a T/cross shape one block below the
 * skull, and the skull sits on top.
 */
public class BoneGolemConstructionEvents {

    private static final Block BONE_BLOCK = Blocks.BONE_BLOCK;

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        Block placed = event.getState().getBlock();
        if (placed != Blocks.SKELETON_SKULL
                && placed != Blocks.SKELETON_WALL_SKULL
                && placed != Blocks.WITHER_SKELETON_SKULL
                && placed != Blocks.WITHER_SKELETON_WALL_SKULL) {
            return;
        }

        Level level = (Level) event.getLevel();
        if (level.isClientSide()) return;

        BlockPos skullPos = event.getPos();
        trySpawnBoneGolem(level, skullPos);
    }

    /**
     * Attempts to spawn a Bone Golem centred at {@code skullPos}.
     *
     * The four bone blocks must exist in the following positions relative to
     * the skull:
     * <ul>
     *   <li>Upper body:  (0, -1, 0)</li>
     *   <li>Left arm:    (-1, -1, 0) or (0, -1, -1)</li>
     *   <li>Right arm:   (+1, -1, 0) or (0, -1, +1)</li>
     *   <li>Lower body:  (0, -2, 0)</li>
     * </ul>
     * Arms are checked on both the X axis and the Z axis.
     */
    private static void trySpawnBoneGolem(Level level, BlockPos skullPos) {
        // The four bone block positions to check (relative to skull, per axis)
        BlockPos upperBody = skullPos.below(1);
        BlockPos lowerBody = skullPos.below(2);

        if (!isBoneBlock(level, upperBody) || !isBoneBlock(level, lowerBody)) {
            return;
        }

        // Check arms along X axis
        boolean xAxis = isBoneBlock(level, upperBody.west()) && isBoneBlock(level, upperBody.east());
        // Check arms along Z axis
        boolean zAxis = isBoneBlock(level, upperBody.north()) && isBoneBlock(level, upperBody.south());

        if (!xAxis && !zAxis) {
            return;
        }

        // All blocks confirmed – consume them and spawn the golem.
        level.removeBlock(skullPos, false);   // skull
        level.removeBlock(upperBody, false);
        level.removeBlock(lowerBody, false);
        if (xAxis) {
            level.removeBlock(upperBody.west(), false);
            level.removeBlock(upperBody.east(), false);
        } else {
            level.removeBlock(upperBody.north(), false);
            level.removeBlock(upperBody.south(), false);
        }

        // Spawn at the centre of the lower-body block, standing on the ground.
        var golem = ModEntities.BONE_GOLEM.get().create(level);
        if (golem == null) return;

        // Position golem feet at the lower body block position.
        golem.moveTo(lowerBody.getX() + 0.5, lowerBody.getY(), lowerBody.getZ() + 0.5, 0.0F, 0.0F);
        level.addFreshEntity(golem);
    }

    private static boolean isBoneBlock(Level level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() == BONE_BLOCK;
    }
}
