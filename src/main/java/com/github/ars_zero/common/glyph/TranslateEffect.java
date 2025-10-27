package com.github.ars_zero.common.glyph;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.item.ArsZeroStaff;
import com.github.ars_zero.common.spell.SpellResult;
import com.github.ars_zero.common.spell.StaffCastContext;
import com.github.ars_zero.registry.ModSounds;
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
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
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
        
        
        SpellResult beginResult = staffContext.beginResults.get(0);
        Entity target = beginResult.targetEntity;
        
        com.github.ars_zero.ArsZero.LOGGER.info("TranslateEffect targeting: {}, type: {}", 
            target != null ? target.getName().getString() : "null", 
            target != null ? target.getClass().getSimpleName() : "null");
        
        if (target == null || !target.isAlive()) {
            return;
        }
        
        
        if (beginResult.relativeOffset == null) {
            return;
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
                
                if (target instanceof com.github.ars_zero.common.entity.BaseVoxelEntity voxel) {
                    voxel.freezePhysics();
                }
                
                if (target.tickCount % 5 == 0) {
                    world.playSound(null, player.getX(), player.getY(), player.getZ(), 
                        ModSounds.EFFECT_ANCHOR.get(), SoundSource.NEUTRAL, 1.0f, 1.0f);
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
        
        SpellResult beginResult = context.beginResults.get(0);
        Entity target = beginResult.targetEntity;
        
        if (target != null && target.isAlive()) {
            target.noPhysics = false;
            target.setNoGravity(false);
        }
    }

    @Override
    public void onResolveBlock(BlockHitResult rayTraceResult, Level world, @NotNull LivingEntity shooter, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        ArsZero.LOGGER.debug("TranslateEffect: Block hit, effect only works on entities");
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
        return "When used with Temporal Context Form in TICK phase, keeps the target entity locked to the same position on your screen. The entity will follow your look direction and movement. Use with Touch + [Target] in BEGIN, then Temporal Context Form + Translate in TICK. Amplify increases distance from player.";
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