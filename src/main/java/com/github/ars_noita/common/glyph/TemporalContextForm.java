package com.github.ars_noita.common.glyph;

import com.github.ars_noita.ArsNoita;
import com.github.ars_noita.common.item.ArsNoitaStaff;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.PlayerCaster;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

public class TemporalContextForm extends AbstractSpellPart {
    
    public static final String ID = "temporal_context_form";

    @Override
    public void onResolve(EntityHitResult rayTraceResult, Level world, LivingEntity shooter, SpellStats spellStats, SpellContext spellContext) {
        if (spellContext.getCaster() instanceof PlayerCaster playerCaster) {
            Player player = playerCaster.getPlayer();
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
    public String getRegistryName() {
        return ArsNoita.prefix(ID).toString();
    }

    @Override
    public Component getBookDescription() {
        return Component.translatable("glyph.ars_noita.temporal_context_form.desc");
    }

    @Override
    public int getDefaultManaCost() {
        return 10;
    }

    @Override
    public boolean canBeUsedInSpellbook() {
        return false; // Can only be used in staff Tick or End phases
    }

    @Override
    public boolean canBeUsedInStaff() {
        return true;
    }
}
