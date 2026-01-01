package com.github.ars_zero.common.glyph;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.entity.ILifespanExtendable;
import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractEffect;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import com.hollingsworth.arsnouveau.api.spell.SpellSchools;
import com.hollingsworth.arsnouveau.api.spell.SpellSchool;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentExtendTime;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;

public class SustainEffect extends AbstractEffect {
    
    public static final String ID = "sustain_effect";
    public static final SustainEffect INSTANCE = new SustainEffect();

    public SustainEffect() {
        super(ArsZero.prefix(ID), "Sustain");
    }

    @Override
    public void onResolveEntity(EntityHitResult rayTraceResult, Level world, @NotNull LivingEntity shooter, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        if (world.isClientSide) return;
        
        Entity target = rayTraceResult.getEntity();
        
        if (target == null || !target.isAlive()) {
            return;
        }

        if (target instanceof ILifespanExtendable lifespanExtendable) {
            lifespanExtendable.addLifespan(shooter, spellStats, spellContext, resolver);
        }
    }

    @Override
    public int getDefaultManaCost() {
        return 30;
    }

    @NotNull
    @Override
    public Set<AbstractAugment> getCompatibleAugments() {
        return Set.of(AugmentExtendTime.INSTANCE);
    }

    @Override
    public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
        super.addAugmentDescriptions(map);
        map.put(AugmentExtendTime.INSTANCE, "Adds an additional tick per level");
    }

    @Override
    public String getBookDescription() {
        return "Extends the lifespan of target entities that support lifespan extension by 1 tick per resolution. Works on Voxels, Convergence entities, Block Groups, and Source Jar Chargers. Extend Time adds an additional tick per level.";
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
}

