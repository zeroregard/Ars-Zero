package com.github.ars_zero.mixin;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.registry.ModMobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityGravityMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void ars_zero$tickLog(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self instanceof LivingEntity living) {
            long time = self.level().getGameTime();
            if (time % 40L == 0L) {
                boolean has = living.getActiveEffects().stream().anyMatch(i -> i.getEffect().is(ModMobEffects.ZERO_GRAVITY.getKey()));
                ArsZero.LOGGER.info("ZeroG tick: entity={}, hasEffect={}", living.getName().getString(), has);
            }
        }
    }

    @Inject(method = "baseTick", at = @At("HEAD"))
    private void ars_zero$baseTickLog(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self instanceof LivingEntity living) {
            long time = self.level().getGameTime();
            if (time % 40L == 0L) {
                boolean has = living.getActiveEffects().stream().anyMatch(i -> i.getEffect().is(ModMobEffects.ZERO_GRAVITY.getKey()));
                ArsZero.LOGGER.info("ZeroG baseTick: entity={}, hasEffect={}", living.getName().getString(), has);
            }
        }
    }

    @Inject(method = "applyGravity", at = @At("HEAD"), cancellable = true)
    private void ars_zero$logAndMaybeCancelGravity(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self instanceof LivingEntity living) {
            long time = self.level().getGameTime();
            if (time % 20L == 0L) {
                boolean has = living.getActiveEffects().stream().anyMatch(i -> i.getEffect().is(ModMobEffects.ZERO_GRAVITY.getKey()));
                ArsZero.LOGGER.info("ZeroG applyGravity check: entity={}, hasEffect={}", living.getName().getString(), has);
            }
            if (living.getActiveEffects().stream().anyMatch(i -> i.getEffect().is(ModMobEffects.ZERO_GRAVITY.getKey()))) {
                ArsZero.LOGGER.info("ZeroG applyGravity canceled for {}", living.getName().getString());
                ci.cancel();
            }
        }
    }

    @Inject(method = "getDefaultGravity", at = @At("HEAD"), cancellable = true)
    private void ars_zero$maybeZeroGravity(CallbackInfoReturnable<Double> cir) {
        Entity self = (Entity) (Object) this;
        if (self instanceof LivingEntity living) {
            long time = self.level().getGameTime();
            if (time % 20L == 0L) {
                boolean has = living.getActiveEffects().stream().anyMatch(i -> i.getEffect().is(ModMobEffects.ZERO_GRAVITY.getKey()));
                ArsZero.LOGGER.info("ZeroG check: entity={}, hasEffect={}", living.getName().getString(), has);
            }
            MobEffectInstance inst = living.getActiveEffects().stream()
                .filter(i -> i.getEffect().is(ModMobEffects.ZERO_GRAVITY.getKey()))
                .findFirst()
                .orElse(null);
            if (inst != null && inst.getDuration() > 0) {
                ArsZero.LOGGER.info("ZeroG active: zeroing gravity for {}", living.getName().getString());
                cir.setReturnValue(0.0D);
                return;
            }
        }
        // Non-living entities only get zero-g via their own physics (e.g., voxels).
        // We do not globally zero gravity for non-livings here.
        if (false) {
            cir.setReturnValue(0.0D);
        }
    }

    @Inject(method = "getGravity", at = @At("HEAD"), cancellable = true)
    private void ars_zero$maybeZeroGravityComputed(CallbackInfoReturnable<Double> cir) {
        Entity self = (Entity) (Object) this;
        if (self instanceof LivingEntity living) {
            MobEffectInstance inst = living.getActiveEffects().stream()
                .filter(i -> i.getEffect().is(ModMobEffects.ZERO_GRAVITY.getKey()))
                .findFirst()
                .orElse(null);
            if (inst != null && inst.getDuration() > 0) {
                ArsZero.LOGGER.info("ZeroG active(getGravity): zeroing gravity for {}", living.getName().getString());
                cir.setReturnValue(0.0D);
            }
        }
    }
}


