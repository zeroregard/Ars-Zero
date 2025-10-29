package com.github.ars_zero.common.glyph;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.entity.CompressibleEntity;
import com.github.ars_zero.registry.ModSounds;
import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractEffect;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.SpellSchool;
import com.hollingsworth.arsnouveau.api.spell.SpellSchools;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentDampen;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;

public class PushEffect extends AbstractEffect {
    
    public static final String ID = "push_effect";
    public static final PushEffect INSTANCE = new PushEffect();

    public PushEffect() {
        super(ArsZero.prefix(ID), "Push");
    }

    @Override
    public void onResolveEntity(EntityHitResult rayTraceResult, Level world, @NotNull LivingEntity shooter, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        if (world.isClientSide) return;
        
        Entity target = rayTraceResult.getEntity();
        if (target == null) {
            return;
        }
        
        Vec3 lookVec = shooter.getLookAngle();
        
        double baseStrength = 1.5;
        double strength = baseStrength + spellStats.getAmpMultiplier() * 0.5;
        
        if (target instanceof CompressibleEntity compressible) {
            float compressionLevel = compressible.getCompressionLevel();
            strength *= (1.0 + compressionLevel * 2.0);
        }
        
        Vec3 velocity = lookVec.scale(strength);
        
        target.setDeltaMovement(velocity);
        target.hurtMarked = true;
        target.fallDistance = 0.0f;
        
        float pitch = 0.95f + (world.random.nextFloat() * 0.1f);
        world.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(), ModSounds.EFFECT_PUSH.get(), SoundSource.NEUTRAL, 1.0f, pitch);
    }

    @Override
    public int getDefaultManaCost() {
        return 25;
    }

    @NotNull
    @Override
    public Set<AbstractAugment> getCompatibleAugments() {
        return Set.of(AugmentAmplify.INSTANCE, AugmentDampen.INSTANCE);
    }

    @Override
    public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
        super.addAugmentDescriptions(map);
        map.put(AugmentAmplify.INSTANCE, "Increases the push strength");
        map.put(AugmentDampen.INSTANCE, "Decreases the push strength");
    }

    @Override
    public String getBookDescription() {
        return "Pushes the target entity in the direction you're looking. Unlike Knockback, this respects your full look direction including up and down. Perfect for launching entities carried with Translate in your END phase.";
    }

    @Override
    public SpellTier defaultTier() {
        return SpellTier.ONE;
    }

    @NotNull
    @Override
    public Set<SpellSchool> getSchools() {
        return Set.of(SpellSchools.ELEMENTAL_AIR);
    }
}

