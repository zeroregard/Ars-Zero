package com.github.ars_noita.common.glyph;

import com.github.ars_noita.ArsNoita;
import com.github.ars_noita.common.item.ArsNoitaStaff;
import com.github.ars_noita.common.spell.SpellResult;
import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractCastMethod;
import com.hollingsworth.arsnouveau.api.spell.CastResolveType;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;

/**
 * TemporalContextForm - A form that acts as a marker for temporal context usage.
 * 
 * This form doesn't do anything itself - it's just a marker that tells the staff
 * to replace the current hit result with the stored temporal context from previous phases.
 * 
 * Usage Examples:
 * 1. Water -> Freeze -> Break sequence:
 *    - Begin: Touch + Conjure Water (creates water block, stored in temporal context)
 *    - Tick: Temporal Context Form + Freeze (targets water block from Begin phase)
 *    - End: Temporal Context Form + Break (targets ice block from Tick phase)
 * 
 * 2. Entity -> Effect -> Leap sequence:
 *    - Begin: Touch + Conjure Entity (creates entity, stored in temporal context)
 *    - Tick: Temporal Context Form + Effect (applies effect to entity from Begin phase)
 *    - End: Temporal Context Form + Leap (uses entity context for leap direction)
 */
public class TemporalContextForm extends AbstractCastMethod {
    
    public static final String ID = "temporal_context_form";
    public static final TemporalContextForm INSTANCE = new TemporalContextForm();

    public TemporalContextForm() {
        super(ID, "Temporal Context Form");
        ArsNoita.LOGGER.debug("Creating TemporalContextForm form instance");
    }

    @Override
    public CastResolveType onCast(ItemStack stack, LivingEntity caster, Level world, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        // Check if we have stored temporal context
        if (caster instanceof net.minecraft.world.entity.player.Player player) {
            com.github.ars_noita.common.spell.StaffCastContext staffContext = ArsNoitaStaff.getStaffContext(player);
            ArsNoita.LOGGER.debug("TemporalContextForm onCast - staffContext: {}, beginResults: {}", 
                staffContext != null, staffContext != null ? staffContext.beginResults.size() : 0);
            
            if (staffContext == null || staffContext.beginResults.isEmpty()) {
                ArsNoita.LOGGER.debug("TemporalContextForm onCast - no stored context, blocking spell");
                return CastResolveType.FAILURE;
            }
            
            // Use the first result from begin phase
            SpellResult result = staffContext.beginResults.get(0);
            HitResult originalHit = resolver.hitResult;
            resolver.hitResult = result.hitResult;
            ArsNoita.LOGGER.debug("TemporalContextForm onCast - replaced hit result: {} -> {}", originalHit, result.hitResult);
            
            // NOW PROCESS THE EFFECTS WITH THE STORED CONTEXT!
            resolver.onResolveEffect(world, result.hitResult);
            ArsNoita.LOGGER.debug("TemporalContextForm onCast - triggered effect resolution with stored context");
        }
        return CastResolveType.SUCCESS;
    }

    @Override
    public CastResolveType onCastOnBlock(UseOnContext context, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        // Check if we have stored temporal context
        if (context.getPlayer() instanceof net.minecraft.world.entity.player.Player player) {
            com.github.ars_noita.common.spell.StaffCastContext staffContext = ArsNoitaStaff.getStaffContext(player);
            if (staffContext == null || staffContext.beginResults.isEmpty()) {
                ArsNoita.LOGGER.debug("TemporalContextForm onCastOnBlock - no stored context, blocking spell");
                return CastResolveType.FAILURE;
            }
            
            SpellResult result = staffContext.beginResults.get(0);
            resolver.hitResult = result.hitResult;
            ArsNoita.LOGGER.debug("TemporalContextForm onCastOnBlock - using stored context: {}", result.hitResult);
            
            // Process the effects with the stored context
            resolver.onResolveEffect(context.getLevel(), result.hitResult);
            ArsNoita.LOGGER.debug("TemporalContextForm onCastOnBlock - triggered effect resolution");
        }
        return CastResolveType.SUCCESS;
    }

    @Override
    public CastResolveType onCastOnBlock(BlockHitResult blockHitResult, LivingEntity caster, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        // Check if we have stored temporal context
        if (caster instanceof net.minecraft.world.entity.player.Player player) {
            com.github.ars_noita.common.spell.StaffCastContext staffContext = ArsNoitaStaff.getStaffContext(player);
            if (staffContext == null || staffContext.beginResults.isEmpty()) {
                ArsNoita.LOGGER.debug("TemporalContextForm onCastOnBlock (2) - no stored context, blocking spell");
                return CastResolveType.FAILURE;
            }
            
            SpellResult result = staffContext.beginResults.get(0);
            resolver.hitResult = result.hitResult;
            ArsNoita.LOGGER.debug("TemporalContextForm onCastOnBlock (2) - using stored context: {}", result.hitResult);
            
            // Process the effects with the stored context
            resolver.onResolveEffect(caster.getCommandSenderWorld(), result.hitResult);
            ArsNoita.LOGGER.debug("TemporalContextForm onCastOnBlock (2) - triggered effect resolution");
        }
        return CastResolveType.SUCCESS;
    }

    @Override
    public CastResolveType onCastOnEntity(ItemStack stack, LivingEntity caster, Entity target, InteractionHand hand, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        // Check if we have stored temporal context
        if (caster instanceof net.minecraft.world.entity.player.Player player) {
            com.github.ars_noita.common.spell.StaffCastContext staffContext = ArsNoitaStaff.getStaffContext(player);
            if (staffContext == null || staffContext.beginResults.isEmpty()) {
                ArsNoita.LOGGER.debug("TemporalContextForm onCastOnEntity - no stored context, blocking spell");
                return CastResolveType.FAILURE;
            }
            
            SpellResult result = staffContext.beginResults.get(0);
            resolver.hitResult = result.hitResult;
            ArsNoita.LOGGER.debug("TemporalContextForm onCastOnEntity - using stored context: {}", result.hitResult);
            
            // Process the effects with the stored context
            resolver.onResolveEffect(caster.getCommandSenderWorld(), result.hitResult);
            ArsNoita.LOGGER.debug("TemporalContextForm onCastOnEntity - triggered effect resolution");
        }
        return CastResolveType.SUCCESS;
    }

    @Override
    public int getDefaultManaCost() {
        return 5;
    }

    @NotNull
    @Override
    public Set<AbstractAugment> getCompatibleAugments() {
        return Set.of(); // No augments for this form
    }

    @Override
    public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
        super.addAugmentDescriptions(map);
        // No augments to describe
    }

    @Override
    public String getBookDescription() {
        return "A form that acts as a marker for temporal context usage. When used in Tick or End phases, it will target the entity or block that was stored in the temporal context from previous phases.";
    }

    @Override
    public ResourceLocation getRegistryName() {
        return ArsNoita.prefix(ID);
    }
}