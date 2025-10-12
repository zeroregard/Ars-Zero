package com.github.ars_zero.common.spell;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;

public class SpellResult {
    public final Entity targetEntity;
    public final BlockPos targetPosition;
    public final HitResult hitResult;
    public final SpellEffectType effectType;
    public final long timestamp;
    
    public SpellResult(Entity targetEntity, BlockPos targetPosition, HitResult hitResult, SpellEffectType effectType) {
        this.targetEntity = targetEntity;
        this.targetPosition = targetPosition;
        this.hitResult = hitResult;
        this.effectType = effectType;
        this.timestamp = System.currentTimeMillis();
    }
    
    public static SpellResult fromHitResult(HitResult hitResult, SpellEffectType effectType) {
        Entity entity = null;
        BlockPos blockPos = null;
        
        if (hitResult instanceof net.minecraft.world.phys.EntityHitResult entityHit) {
            entity = entityHit.getEntity();
        } else if (hitResult instanceof net.minecraft.world.phys.BlockHitResult blockHit) {
            blockPos = blockHit.getBlockPos();
        }
        
        return new SpellResult(entity, blockPos, hitResult, effectType);
    }
}
