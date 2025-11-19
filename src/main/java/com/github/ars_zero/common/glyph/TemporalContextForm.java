package com.github.ars_zero.common.glyph;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.item.AbstractMultiPhaseCastDevice;
import com.github.ars_zero.common.item.AbstractSpellStaff;
import com.github.ars_zero.common.spell.MultiPhaseCastContext;
import com.github.ars_zero.common.spell.SpellResult;
import com.github.ars_zero.registry.ModParticleTimelines;
import com.hollingsworth.arsnouveau.api.particle.ParticleEmitter;
import com.hollingsworth.arsnouveau.api.particle.configurations.properties.SoundProperty;
import com.hollingsworth.arsnouveau.api.particle.timelines.TimelineEntryData;
import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractCastMethod;
import com.hollingsworth.arsnouveau.api.spell.CastResolveType;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
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
            return resolveFromStoredContext(world, player, spellContext, resolver);
        }
        return CastResolveType.FAILURE;
    }

    @Override
    public CastResolveType onCastOnBlock(UseOnContext context, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        if (context.getPlayer() instanceof Player player) {
            return resolveFromStoredContext(context.getLevel(), player, spellContext, resolver);
        }
        return CastResolveType.FAILURE;
    }

    @Override
    public CastResolveType onCastOnBlock(BlockHitResult blockHitResult, LivingEntity caster, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        if (caster instanceof Player player) {
            return resolveFromStoredContext(caster.getCommandSenderWorld(), player, spellContext, resolver);
        }
        return CastResolveType.FAILURE;
    }

    @Override
    public CastResolveType onCastOnEntity(ItemStack stack, LivingEntity caster, Entity target, InteractionHand hand, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        if (caster instanceof Player player) {
            return resolveFromStoredContext(caster.getCommandSenderWorld(), player, spellContext, resolver);
        }
        return CastResolveType.FAILURE;
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

    private CastResolveType resolveFromStoredContext(Level level, Player player, SpellContext spellContext, SpellResolver resolver) {
        ItemStack casterTool = spellContext.getCasterTool();
        MultiPhaseCastContext castContext = AbstractMultiPhaseCastDevice.findContextByStack(player, casterTool);
        
        if (castContext == null || castContext.beginResults.isEmpty()) {
            return CastResolveType.FAILURE;
        }
        
        for (SpellResult result : castContext.beginResults) {
            resolver.hitResult = result.hitResult;
            resolver.onResolveEffect(level, result.hitResult);
            triggerResolveEffects(spellContext, level, player, result);
        }
        return CastResolveType.SUCCESS;
    }

    private void triggerResolveEffects(SpellContext spellContext, Level level, Player player, SpellResult result) {
        if (level == null || result == null) {
            return;
        }
        List<Vec3> targetPositions = computeTargetPositions(player, result);
        if (targetPositions.isEmpty() && result.hitResult != null) {
            targetPositions.add(result.hitResult.getLocation());
        }
        if (targetPositions.isEmpty()) {
            return;
        }
        var timeline = spellContext.getParticleTimeline(ModParticleTimelines.TEMPORAL_CONTEXT_TIMELINE.get());
        TimelineEntryData entryData = timeline.onResolvingEffect();
        SoundProperty resolveSound = timeline.resolveSound();
        for (Vec3 position : targetPositions) {
            ParticleEmitter particleEmitter = createStaticEmitter(entryData, position);
            particleEmitter.tick(level);
            resolveSound.sound.playSound(level, position.x, position.y, position.z);
        }
    }

    private List<Vec3> computeTargetPositions(Player player, SpellResult result) {
        List<Vec3> positions = new ArrayList<>();
        if (result.targetEntity != null && result.targetEntity.isAlive()) {
            positions.add(result.targetEntity.position());
        }
        if (result.blockGroup != null) {
            positions.add(result.blockGroup.position());
            if (result.blockPositions != null) {
                for (BlockPos pos : result.blockPositions) {
                    positions.add(Vec3.atCenterOf(pos));
                }
            }
        } else if (result.blockPositions != null && !result.blockPositions.isEmpty()) {
            for (BlockPos pos : result.blockPositions) {
                positions.add(Vec3.atCenterOf(pos));
            }
        } else if (result.targetPosition != null) {
            positions.add(Vec3.atCenterOf(result.targetPosition));
        }
        if (positions.isEmpty()) {
            Vec3 reconstructed = result.transformLocalToWorld(player.getYRot(), player.getXRot(), player.getEyePosition(1.0f));
            if (reconstructed != null) {
                positions.add(reconstructed);
            }
        }
        return positions;
    }
}