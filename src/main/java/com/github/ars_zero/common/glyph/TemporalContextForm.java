package com.github.ars_zero.common.glyph;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.item.AbstractMultiPhaseCastDevice;
import com.github.ars_zero.common.item.AbstractSpellStaff;
import com.github.ars_zero.common.spell.SpellResult;
import com.github.ars_zero.common.spell.MultiPhaseCastContext;
import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractCastMethod;
import com.hollingsworth.arsnouveau.api.spell.CastResolveType;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
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
        super(ArsZero.prefix(ID), "Temporal Context Form");
    }

    @Override
    public CastResolveType onCast(ItemStack stack, LivingEntity caster, Level world, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        if (caster instanceof Player player) {
            ItemStack casterTool = spellContext.getCasterTool();
            ArsZero.LOGGER.info("[TemporalContextForm.onCast] Player: {}, casterTool: {}, isEmpty: {}", 
                player.getScoreboardName(), casterTool.getItem(), casterTool.isEmpty());
            
            MultiPhaseCastContext castContext = AbstractMultiPhaseCastDevice.findContextByStack(player, casterTool);
            ArsZero.LOGGER.info("[TemporalContextForm.onCast] Context found: {}, beginResults size: {}", 
                castContext != null, castContext != null ? castContext.beginResults.size() : 0);
            
            if (castContext != null) {
                ArsZero.LOGGER.info("[TemporalContextForm.onCast] Context source: {}, isCasting: {}", 
                    castContext.source, castContext.isCasting);
            }
            
            if (castContext == null || castContext.beginResults.isEmpty()) {
                ArsZero.LOGGER.warn("[TemporalContextForm.onCast] FAILURE: context={}, beginResults empty={}", 
                    castContext != null, castContext != null ? castContext.beginResults.isEmpty() : true);
                return CastResolveType.FAILURE;
            }
            
            ArsZero.LOGGER.info("[TemporalContextForm.onCast] Processing {} beginResults", castContext.beginResults.size());
            for (SpellResult result : castContext.beginResults) {
                resolver.hitResult = result.hitResult;
                resolver.onResolveEffect(world, result.hitResult);
            }
        }
        return CastResolveType.SUCCESS;
    }

    @Override
    public CastResolveType onCastOnBlock(UseOnContext context, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        if (context.getPlayer() instanceof Player player) {
            ItemStack stack = spellContext.getCasterTool();
            ArsZero.LOGGER.info("[TemporalContextForm.onCastOnBlock] Player: {}, casterTool: {}, isEmpty: {}", 
                player.getScoreboardName(), stack.getItem(), stack.isEmpty());
            
            MultiPhaseCastContext castContext = AbstractMultiPhaseCastDevice.findContextByStack(player, stack);
            ArsZero.LOGGER.info("[TemporalContextForm.onCastOnBlock] Context found: {}, beginResults size: {}", 
                castContext != null, castContext != null ? castContext.beginResults.size() : 0);
            
            if (castContext != null) {
                ArsZero.LOGGER.info("[TemporalContextForm.onCastOnBlock] Context source: {}, isCasting: {}", 
                    castContext.source, castContext.isCasting);
            }
            
            if (castContext == null || castContext.beginResults.isEmpty()) {
                ArsZero.LOGGER.warn("[TemporalContextForm.onCastOnBlock] FAILURE: context={}, beginResults empty={}", 
                    castContext != null, castContext != null ? castContext.beginResults.isEmpty() : true);
                return CastResolveType.FAILURE;
            }
            
            ArsZero.LOGGER.info("[TemporalContextForm.onCastOnBlock] Processing {} beginResults", castContext.beginResults.size());
            for (SpellResult result : castContext.beginResults) {
                resolver.hitResult = result.hitResult;
                resolver.onResolveEffect(context.getLevel(), result.hitResult);
            }
        }
        return CastResolveType.SUCCESS;
    }

    @Override
    public CastResolveType onCastOnBlock(BlockHitResult blockHitResult, LivingEntity caster, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        if (caster instanceof Player player) {
            ItemStack stack = spellContext.getCasterTool();
            ArsZero.LOGGER.info("[TemporalContextForm.onCastOnBlock(BlockHit)] Player: {}, casterTool: {}, isEmpty: {}", 
                player.getScoreboardName(), stack.getItem(), stack.isEmpty());
            
            MultiPhaseCastContext castContext = AbstractMultiPhaseCastDevice.findContextByStack(player, stack);
            ArsZero.LOGGER.info("[TemporalContextForm.onCastOnBlock(BlockHit)] Context found: {}, beginResults size: {}", 
                castContext != null, castContext != null ? castContext.beginResults.size() : 0);
            
            if (castContext != null) {
                ArsZero.LOGGER.info("[TemporalContextForm.onCastOnBlock(BlockHit)] Context source: {}, isCasting: {}", 
                    castContext.source, castContext.isCasting);
            }
            
            if (castContext == null || castContext.beginResults.isEmpty()) {
                ArsZero.LOGGER.warn("[TemporalContextForm.onCastOnBlock(BlockHit)] FAILURE: context={}, beginResults empty={}", 
                    castContext != null, castContext != null ? castContext.beginResults.isEmpty() : true);
                return CastResolveType.FAILURE;
            }
            
            ArsZero.LOGGER.info("[TemporalContextForm.onCastOnBlock(BlockHit)] Processing {} beginResults", castContext.beginResults.size());
            for (SpellResult result : castContext.beginResults) {
                resolver.hitResult = result.hitResult;
                resolver.onResolveEffect(caster.getCommandSenderWorld(), result.hitResult);
            }
        }
        return CastResolveType.SUCCESS;
    }

    @Override
    public CastResolveType onCastOnEntity(ItemStack stack, LivingEntity caster, Entity target, InteractionHand hand, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        if (caster instanceof Player player) {
            ItemStack casterTool = spellContext.getCasterTool();
            ArsZero.LOGGER.info("[TemporalContextForm.onCastOnEntity] Player: {}, casterTool: {}, isEmpty: {}, hand: {}", 
                player.getScoreboardName(), casterTool.getItem(), casterTool.isEmpty(), hand);
            
            MultiPhaseCastContext castContext = AbstractMultiPhaseCastDevice.findContextByStack(player, casterTool);
            ArsZero.LOGGER.info("[TemporalContextForm.onCastOnEntity] Context found: {}, beginResults size: {}", 
                castContext != null, castContext != null ? castContext.beginResults.size() : 0);
            
            if (castContext != null) {
                ArsZero.LOGGER.info("[TemporalContextForm.onCastOnEntity] Context source: {}, isCasting: {}", 
                    castContext.source, castContext.isCasting);
            }
            
            if (castContext == null || castContext.beginResults.isEmpty()) {
                ArsZero.LOGGER.warn("[TemporalContextForm.onCastOnEntity] FAILURE: context={}, beginResults empty={}", 
                    castContext != null, castContext != null ? castContext.beginResults.isEmpty() : true);
                return CastResolveType.FAILURE;
            }
            
            ArsZero.LOGGER.info("[TemporalContextForm.onCastOnEntity] Processing {} beginResults", castContext.beginResults.size());
            for (SpellResult result : castContext.beginResults) {
                resolver.hitResult = result.hitResult;
                resolver.onResolveEffect(caster.getCommandSenderWorld(), result.hitResult);
            }
        }
        return CastResolveType.SUCCESS;
    }

    @Override
    public int getDefaultManaCost() {
        return 0;
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
}