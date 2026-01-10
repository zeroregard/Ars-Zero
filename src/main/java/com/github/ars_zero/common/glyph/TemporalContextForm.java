package com.github.ars_zero.common.glyph;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.block.MultiphaseSpellTurretTile;
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
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.TileCaster;
import com.hollingsworth.arsnouveau.common.block.BasicSpellTurret;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
        return resolveFromStoredContext(world, caster, spellContext, resolver);
    }

    @Override
    public CastResolveType onCastOnBlock(UseOnContext context, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        if (context.getPlayer() != null) {
            return resolveFromStoredContext(context.getLevel(), context.getPlayer(), spellContext, resolver);
        }
        return CastResolveType.FAILURE;
    }

    @Override
    public CastResolveType onCastOnBlock(BlockHitResult blockHitResult, LivingEntity caster, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        return resolveFromStoredContext(caster.getCommandSenderWorld(), caster, spellContext, resolver);
    }

    @Override
    public CastResolveType onCastOnEntity(ItemStack stack, LivingEntity caster, Entity target, InteractionHand hand, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        return resolveFromStoredContext(caster.getCommandSenderWorld(), caster, spellContext, resolver);
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

    private CastResolveType resolveFromStoredContext(Level level, LivingEntity caster, SpellContext spellContext, SpellResolver resolver) {
        MultiPhaseCastContext castContext = null;
        Vec3 casterPos = null;
        float casterYaw = 0;
        float casterPitch = 0;
        
        if (caster instanceof Player playerCaster) {
            casterPos = playerCaster.getEyePosition(1.0f);
            casterYaw = playerCaster.getYRot();
            casterPitch = playerCaster.getXRot();
            ItemStack casterTool = spellContext.getCasterTool();
            castContext = AbstractMultiPhaseCastDevice.findContextByStack(playerCaster, casterTool);
        } else if (spellContext.getCaster() instanceof TileCaster tileCaster) {
            BlockEntity tile = tileCaster.getTile();
            ArsZero.LOGGER.debug("[TemporalContextForm] TileCaster detected, tile type: {}", tile != null ? tile.getClass().getSimpleName() : "null");
            if (tile instanceof MultiphaseSpellTurretTile turretTile) {
                castContext = turretTile.getCastContext();
                ArsZero.LOGGER.debug("[TemporalContextForm] Turret tile found, castContext: {}, beginResults size: {}", 
                    castContext != null, castContext != null ? castContext.beginResults.size() : 0);
                BlockPos tilePos = turretTile.getBlockPos();
                casterPos = Vec3.atCenterOf(tilePos);
                Direction facing = turretTile.getBlockState().getValue(BasicSpellTurret.FACING);
                casterYaw = directionToYaw(facing);
                casterPitch = directionToPitch(facing);
            } else {
                ArsZero.LOGGER.warn("[TemporalContextForm] TileCaster tile is not MultiphaseSpellTurretTile: {}", tile != null ? tile.getClass().getName() : "null");
            }
        } else {
            ArsZero.LOGGER.warn("[TemporalContextForm] Caster is neither Player nor TileCaster: {}", caster != null ? caster.getClass().getName() : "null");
        }
        
        if (castContext == null) {
            ArsZero.LOGGER.debug("[TemporalContextForm] No cast context found, returning FAILURE");
            return CastResolveType.FAILURE;
        }
        
        if (castContext.beginResults.isEmpty()) {
            ArsZero.LOGGER.debug("[TemporalContextForm] Cast context found but beginResults is empty (size: {}), tickResults: {}, endResults: {}", 
                castContext.beginResults.size(), castContext.tickResults.size(), castContext.endResults.size());
            return CastResolveType.FAILURE;
        }
        
        SpellContext baseContext = spellContext.clone();
        for (SpellResult result : castContext.beginResults) {
            SpellResolver perTargetResolver = resolver.getNewResolver(baseContext.clone());
            perTargetResolver.hitResult = result.hitResult;
            perTargetResolver.onResolveEffect(level, result.hitResult);
            if (casterPos != null) {
                triggerResolveEffects(spellContext, level, casterPos, casterYaw, casterPitch, result);
            }
        }
        return CastResolveType.SUCCESS;
    }

    private void triggerResolveEffects(SpellContext spellContext, Level level, Vec3 casterPos, float casterYaw, float casterPitch, SpellResult result) {
        if (level == null || result == null) {
            return;
        }
        List<Vec3> targetPositions = computeTargetPositions(casterPos, casterYaw, casterPitch, result);
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

    private List<Vec3> computeTargetPositions(Vec3 casterPos, float casterYaw, float casterPitch, SpellResult result) {
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
        if (positions.isEmpty() && result.relativeOffset != null) {
            Vec3 reconstructed = result.transformLocalToWorld(casterYaw, casterPitch, casterPos);
            if (reconstructed != null) {
                positions.add(reconstructed);
            }
        }
        return positions;
    }

    private static float directionToYaw(Direction facing) {
        return switch (facing) {
            case NORTH -> 180.0f;
            case SOUTH -> 0.0f;
            case WEST -> 90.0f;
            case EAST -> -90.0f;
            default -> 0.0f;
        };
    }

    private static float directionToPitch(Direction facing) {
        return switch (facing) {
            case UP -> -90.0f;
            case DOWN -> 90.0f;
            default -> 0.0f;
        };
    }
}