package com.github.ars_zero.common.glyph;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.entity.BaseVoxelEntity;
import com.github.ars_zero.common.gravity.GravitySuppression;
import com.github.ars_zero.registry.ModMobEffects;
import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractEffect;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.SpellSchool;
import com.hollingsworth.arsnouveau.api.spell.SpellSchools;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentDurationDown;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentExtendTime;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.NotNull;

public class ZeroGravityEffect extends AbstractEffect {
    public static final String ID = "zero_gravity_effect";
    public static final ZeroGravityEffect INSTANCE = new ZeroGravityEffect();

    public ZeroGravityEffect() {
        super(ArsZero.prefix(ID), "Remove Gravity");
    }

    @Override
    public void onResolveEntity(EntityHitResult rayTraceResult, Level world, @NotNull LivingEntity shooter, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        if (world.isClientSide) {
            return;
        }
        int duration = getDuration(spellStats);
        if (rayTraceResult.getEntity() instanceof LivingEntity living && living.isAlive()) {
            living.addEffect(new MobEffectInstance(ModMobEffects.ZERO_GRAVITY, duration, 0, false, true, true));
        }
    }

    @Override
    public void onResolveBlock(BlockHitResult rayTraceResult, Level world, @NotNull LivingEntity shooter, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        ArsZero.LOGGER.debug("RemoveGravityEffect: Block hit ignored");
    }

    @Override
    public int getDefaultManaCost() {
        return 35;
    }

    @NotNull
    @Override
    public Set<AbstractAugment> getCompatibleAugments() {
        return Set.of(AugmentExtendTime.INSTANCE, AugmentDurationDown.INSTANCE);
    }

    @Override
    public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
        super.addAugmentDescriptions(map);
        map.put(AugmentExtendTime.INSTANCE, "Extends the gravity suppression duration");
        map.put(AugmentDurationDown.INSTANCE, "Reduces the gravity suppression duration");
    }

    @Override
    public String getBookDescription() {
        return "Temporarily removes gravity from the target, allowing it to remain suspended until the effect ends or the target is removed.";
    }

    @Override
    public SpellTier defaultTier() {
        return SpellTier.TWO;
    }

    @NotNull
    @Override
    public Set<SpellSchool> getSchools() {
        return Set.of(SpellSchools.MANIPULATION);
    }

    private int getDuration(SpellStats spellStats) {
        double multiplier = spellStats.getDurationMultiplier();
        if (multiplier <= 0) {
            multiplier = 1.0;
        }
        return (int) Math.round(BaseVoxelEntity.DEFAULT_LIFETIME_TICKS * multiplier);
    }
}

