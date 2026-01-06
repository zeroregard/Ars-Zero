package com.github.ars_zero.common.entity;

import net.minecraft.world.phys.Vec3;

public interface ILerp {
    double getLerpValue();

    double getMaxDelta();

    Vec3 getTargetPosition();
}
