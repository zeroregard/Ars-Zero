package com.github.ars_zero.common.glyph;

import com.github.ars_zero.ArsZero;
import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractEffect;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.SpellSchool;
import com.hollingsworth.arsnouveau.api.spell.SpellSchools;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class DiscardEffect extends AbstractEffect {

    public static final String ID = "discard_effect";
    public static final DiscardEffect INSTANCE = new DiscardEffect();

    public DiscardEffect() {
        super(ArsZero.prefix(ID), "Discard");
    }

    @Override
    public void onResolveEntity(EntityHitResult rayTraceResult, Level world, @NotNull LivingEntity shooter, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        if (world.isClientSide) {
            return;
        }

        Entity target = rayTraceResult.getEntity();
        if (target == null) {
            return;
        }

        if (target instanceof LivingEntity) {
            return;
        }

        target.discard();
    }

    @Override
    public int getDefaultManaCost() {
        return 5;
    }

    @NotNull
    @Override
    public Set<AbstractAugment> getCompatibleAugments() {
        return Set.of();
    }

    @Override
    public String getBookDescription() {
        return "Discards non-living entities. Useful for removing Arcane Voxels to trigger their resolve behavior.";
    }

    @Override
    public SpellTier defaultTier() {
        return SpellTier.ONE;
    }

    @NotNull
    @Override
    public Set<SpellSchool> getSchools() {
        return Set.of(SpellSchools.MANIPULATION);
    }
}

