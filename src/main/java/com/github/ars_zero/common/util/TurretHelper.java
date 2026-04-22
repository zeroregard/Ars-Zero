package com.github.ars_zero.common.util;

import com.hollingsworth.arsnouveau.common.block.BasicSpellTurret;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class TurretHelper {

    public record TurretAim(BlockPos turretPos, Direction facing) {}

    public static TurretAim findTurretAim(ServerLevel level, BlockPos playerBlock) {
        BlockState atPlayer = level.getBlockState(playerBlock);
        if (atPlayer.getBlock() instanceof BasicSpellTurret) {
            return new TurretAim(playerBlock, atPlayer.getValue(BasicSpellTurret.FACING));
        }
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = playerBlock.relative(dir);
            BlockState state = level.getBlockState(neighbor);
            if (state.getBlock() instanceof BasicSpellTurret) {
                Direction facing = state.getValue(BasicSpellTurret.FACING);
                if (neighbor.relative(facing).equals(playerBlock)) {
                    return new TurretAim(neighbor, facing);
                }
            }
        }
        return null;
    }

    public static Vec3 getTurretLookDir(Level world, BlockPos turretPos, Direction turretFacing) {
        Vec3 lookVec = new Vec3(turretFacing.getStepX(), turretFacing.getStepY(), turretFacing.getStepZ()).normalize();
        if(SableCompanion.INSTANCE.getContaining(world, turretPos) instanceof SubLevelAccess subLevel) {
            return subLevel.logicalPose().transformNormal(lookVec).normalize();
        }
        return lookVec;
    }
}
