package com.github.ars_zero.mixin;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.registry.ModMobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityGravityTravelMixin {
    @Unique
    private double ars_zero$prevYVelocity;

    @Inject(method = "travel", at = @At("HEAD"))
    private void ars_zero$capturePrevVelocity(Vec3 travelVector, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        this.ars_zero$prevYVelocity = self.getDeltaMovement().y;
    }

    @Inject(method = "travel", at = @At("TAIL"))
    private void ars_zero$cancelDownwardGravityWhenZeroG(Vec3 travelVector, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        MobEffectInstance inst = self.getActiveEffects().stream()
            .filter(i -> i.getEffect().is(ModMobEffects.ZERO_GRAVITY.getKey()))
            .findFirst()
            .orElse(null);
        if (inst == null || inst.getDuration() <= 0) {
            return;
        }
        Vec3 motion = self.getDeltaMovement();
        double gravity = self.getGravity();
        double expectedAfterGravity = this.ars_zero$prevYVelocity - gravity;
        if (motion.y <= expectedAfterGravity + 1.0E-6 && this.ars_zero$prevYVelocity - motion.y > 0.0) {
            double negatedY = motion.y + gravity;
            self.setDeltaMovement(motion.x, negatedY, motion.z);
            self.fallDistance = 0.0f;
            ArsZero.LOGGER.info("ZeroG travel: negated gravity {}, y {} -> {} for {}", gravity, motion.y, negatedY, self.getName().getString());
        }
    }
}


