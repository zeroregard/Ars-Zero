package com.github.ars_zero.common.glyph;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.entity.BaseVoxelEntity;
import com.github.ars_zero.common.entity.BlockGroupEntity;
import com.github.ars_zero.common.item.ArsZeroStaff;
import com.github.ars_zero.common.spell.SpellResult;
import com.github.ars_zero.common.spell.StaffCastContext;
import com.github.ars_zero.registry.ModEntities;
import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractEffect;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import com.hollingsworth.arsnouveau.api.spell.SpellSchools;
import com.hollingsworth.arsnouveau.api.spell.SpellSchool;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentDampen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;

/**
 * TranslateEffect - Works with Temporal Context Form to maintain relative position of entities.
 * 
 * When used with Temporal Context Form in the TICK phase, this effect keeps the entity
 * locked to the same position on the player's screen, following their look direction.
 */
public class TranslateEffect extends AbstractEffect {
    
    public static final String ID = "translate_effect";
    public static final TranslateEffect INSTANCE = new TranslateEffect();

    public TranslateEffect() {
        super(ArsZero.prefix(ID), "Translate");
    }

    @Override
    public void onResolveEntity(EntityHitResult rayTraceResult, Level world, @NotNull LivingEntity shooter, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        if (world.isClientSide) return;
        if (!(shooter instanceof Player player)) return;
        
        StaffCastContext staffContext = ArsZeroStaff.getStaffContext(player);
        if (staffContext == null || staffContext.beginResults.isEmpty()) {
            return;
        }
        
        for (SpellResult beginResult : staffContext.beginResults) {
            Entity target = beginResult.targetEntity;
            
            if (target == null || !target.isAlive()) {
                continue;
            }
            
            if (beginResult.relativeOffset == null) {
                continue;
            }
            
            Vec3 newPosition = beginResult.transformLocalToWorld(
                player.getYRot(), 
                player.getXRot(), 
                player.getEyePosition(1.0f),
                staffContext.distanceMultiplier
            );
            
            if (newPosition != null) {
                if (canMoveToPosition(newPosition, world)) {
                    target.setPos(newPosition.x, newPosition.y, newPosition.z);
                    target.setDeltaMovement(Vec3.ZERO);
                    target.setNoGravity(true);
                    
                    if (target instanceof BaseVoxelEntity voxel) {
                        voxel.freezePhysics();
                    }
                }
            }
        }
    }
    
    private static boolean canMoveToPosition(Vec3 targetPos, Level world) {
        BlockPos blockPos = BlockPos.containing(targetPos);
        
        return !world.getBlockState(blockPos).blocksMotion();
    }
    
    public static void restoreEntityPhysics(StaffCastContext context) {
        if (context == null || context.beginResults.isEmpty()) {
            return;
        }
        
        Player player = null;
        if (context.playerId != null && context.beginResults.get(0).casterPosition != null) {
            if (context.beginResults.get(0).targetEntity != null && context.beginResults.get(0).targetEntity.level() instanceof ServerLevel serverLevel) {
                player = serverLevel.getPlayerByUUID(context.playerId);
            }
        }
        
        float playerYaw = 0.0f;
        if (player != null) {
            playerYaw = player.getYRot();
        } else if (!context.beginResults.isEmpty() && context.beginResults.get(0).casterYaw != 0.0f) {
            playerYaw = context.beginResults.get(0).casterYaw;
        }
        
        for (SpellResult beginResult : context.beginResults) {
            Entity target = beginResult.targetEntity;
            
            if (target != null && target.isAlive()) {
                if (target instanceof BlockGroupEntity blockGroup) {
                    float nearestRotation = blockGroup.getNearest90DegreeRotation(playerYaw);
                    blockGroup.placeBlocks(nearestRotation);
                    // Clear blocks after placing to prevent double placement when discard() calls remove()
                    blockGroup.clearBlocks();
                    blockGroup.discard();
                } else {
                    target.noPhysics = false;
                    target.setNoGravity(false);
                }
            }
        }
    }

    @Override
    public void onResolveBlock(BlockHitResult rayTraceResult, Level world, @NotNull LivingEntity shooter, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        if (world.isClientSide) return;
        if (!(shooter instanceof Player player)) return;
        
        StaffCastContext staffContext = ArsZeroStaff.getStaffContext(player);
        if (staffContext == null || staffContext.beginResults.isEmpty()) {
            return;
        }
        
        for (SpellResult beginResult : staffContext.beginResults) {
            if (beginResult.blockGroup != null && beginResult.relativeOffset != null) {
                BlockGroupEntity blockGroup = beginResult.blockGroup;
                
                Vec3 newPosition = beginResult.transformLocalToWorld(
                    player.getYRot(), 
                    player.getXRot(), 
                    player.getEyePosition(1.0f),
                    staffContext.distanceMultiplier
                );
                
                if (newPosition != null && canMoveToPosition(newPosition, world)) {
                    blockGroup.setPos(newPosition.x, newPosition.y, newPosition.z);
                    blockGroup.setDeltaMovement(Vec3.ZERO);
                    blockGroup.setNoGravity(true);
                }
            }
        }
    }

    @Override
    public int getDefaultManaCost() {
        return 0;
    }

    @NotNull
    @Override
    public Set<AbstractAugment> getCompatibleAugments() {
        return Set.of(AugmentAmplify.INSTANCE, AugmentDampen.INSTANCE);
    }

    @Override
    public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
        super.addAugmentDescriptions(map);
        map.put(AugmentAmplify.INSTANCE, "Increases the distance from the player");
        map.put(AugmentDampen.INSTANCE, "Decreases the distance from the player");
    }

    @Override
    public String getBookDescription() {
        return "When used with Temporal Context Form in TICK phase, keeps the target entity or block group locked to the same position on your screen. The target will follow your look direction and movement. Use with Touch + [Target] in BEGIN, then Temporal Context Form + Translate in TICK. For blocks, use Select + [Target] in BEGIN. Amplify increases distance from player.";
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