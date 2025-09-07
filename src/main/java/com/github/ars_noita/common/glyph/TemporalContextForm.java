package com.github.ars_noita.common.glyph;

import com.github.ars_noita.ArsNoita;
import com.github.ars_noita.common.item.ArsNoitaStaff;
import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractEffect;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.PlayerCaster;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

import java.util.Set;

public class TemporalContextForm extends AbstractEffect {
    
    public static final String ID = "temporal_context_form";

    public TemporalContextForm() {
        super(ID, "Temporal Context Form");
    }

    @Override
    public void onResolveEntity(EntityHitResult rayTraceResult, Level world, LivingEntity shooter, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        if (spellContext.getCaster() instanceof PlayerCaster playerCaster) {
            Player player = playerCaster.player;
            ItemStack mainHand = player.getMainHandItem();
            
            if (mainHand.getItem() instanceof ArsNoitaStaff staff) {
                ArsNoitaStaff.StaffPhase currentPhase = staff.getCurrentPhase();
                
                // Resolve based on current staff phase context
                switch (currentPhase) {
                    case BEGIN:
                        resolveBeginPhase(rayTraceResult, world, shooter, spellStats, spellContext);
                        break;
                    case TICK:
                        resolveTickPhase(rayTraceResult, world, shooter, spellStats, spellContext);
                        break;
                    case END:
                        resolveEndPhase(rayTraceResult, world, shooter, spellStats, spellContext);
                        break;
                }
            }
        }
    }

    private void resolveBeginPhase(EntityHitResult rayTraceResult, Level world, LivingEntity shooter, SpellStats spellStats, SpellContext spellContext) {
        // Begin phase logic - executes once on press
        ArsNoita.LOGGER.info("TemporalContextForm resolving in BEGIN phase");
    }

    private void resolveTickPhase(EntityHitResult rayTraceResult, Level world, LivingEntity shooter, SpellStats spellStats, SpellContext spellContext) {
        // Tick phase logic - executes every tick while held
        ArsNoita.LOGGER.info("TemporalContextForm resolving in TICK phase");
    }

    private void resolveEndPhase(EntityHitResult rayTraceResult, Level world, LivingEntity shooter, SpellStats spellStats, SpellContext spellContext) {
        // End phase logic - executes once on release
        ArsNoita.LOGGER.info("TemporalContextForm resolving in END phase");
    }

    @Override
    public ResourceLocation getRegistryName() {
        return ArsNoita.prefix(ID);
    }

    @Override
    public String getBookDescription() {
        return "glyph.ars_noita.temporal_context_form.desc";
    }

    @Override
    public int getDefaultManaCost() {
        return 10;
    }

    public boolean canBeUsedInSpellbook() {
        return false; // Can only be used in staff Tick or End phases
    }

    public boolean canBeUsedInStaff() {
        return true;
    }

    @Override
    public Integer getTypeIndex() {
        return 8; // Form type index
    }

    @Override
    public Set<AbstractAugment> getCompatibleAugments() {
        return Set.of(); // No augments for this form
    }
}
