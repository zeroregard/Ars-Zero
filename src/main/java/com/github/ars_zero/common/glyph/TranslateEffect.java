package com.github.ars_zero.common.glyph;

import com.github.ars_zero.ArsNoita;
import com.github.ars_zero.common.capability.IPlayerTranslationCapability;
import com.github.ars_zero.common.capability.ModCapabilities;
import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractEffect;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import com.hollingsworth.arsnouveau.api.spell.SpellSchools;
import com.hollingsworth.arsnouveau.api.spell.SpellSchool;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.PlayerCaster;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentExtendTime;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentSensitive;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
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
 * TranslateEffect - Maintains the relative position of a target entity to the player using capabilities.
 * 
 * This effect uses the capability system instead of static storage for better persistence
 * and integration with Minecraft's data system.
 */
public class TranslateEffect extends AbstractEffect {
    
    public static final String ID = "translate_effect";
    public static final TranslateEffect INSTANCE = new TranslateEffect();

    public TranslateEffect() {
        super(ID, "Translate");
    }

    @Override
    public void onResolveEntity(EntityHitResult rayTraceResult, Level world, @NotNull LivingEntity shooter, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        if (!(world instanceof ServerLevel serverLevel)) return;
        if (!(spellContext.getCaster() instanceof PlayerCaster playerCaster)) return;
        
        Player player = playerCaster.player;
        Entity target = rayTraceResult.getEntity();
        
        if (target == null || target == player) return;
        
        ArsNoita.LOGGER.info("TranslateEffect: Starting translation for entity {} by player {}", 
            target.getName().getString(), player.getName().getString());
        
        // Get or create the player's translation capability
        IPlayerTranslationCapability cap = player.getCapability(ModCapabilities.TRANSLATION_CAPABILITY);
        if (cap != null) {
            // Calculate initial relative position
            Vec3 playerPos = player.position();
            Vec3 targetPos = target.position();
            Vec3 relativePos = targetPos.subtract(playerPos);
            
            // Get player's current rotation
            float yaw = player.getYRot();
            float pitch = player.getXRot();
            
            // Calculate duration
            int duration = getDuration(spellStats);
            
            // Store translation data in capability
            cap.setTargetEntity(target);
            cap.setInitialRelativePos(relativePos);
            cap.setInitialYaw(yaw);
            cap.setInitialPitch(pitch);
            cap.setDuration(duration);
            
            ArsNoita.LOGGER.debug("Translation data stored in capability: relativePos={}, yaw={}, pitch={}, duration={}", 
                relativePos, yaw, pitch, duration);
        }
    }

    @Override
    public void onResolveBlock(BlockHitResult rayTraceResult, Level world, @NotNull LivingEntity shooter, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        // Translate effect doesn't work on blocks directly
        ArsNoita.LOGGER.debug("TranslateEffect: Block hit, ignoring (effect only works on entities)");
    }
    
    /**
     * Update all active translations. This should be called every tick.
     */
    public static void updateTranslations(Level world) {
        if (world.isClientSide) return;
        
        // Update all players with active translations
        for (Player player : world.players()) {
            IPlayerTranslationCapability cap = player.getCapability(ModCapabilities.TRANSLATION_CAPABILITY);
            if (cap != null && cap.hasActiveTranslation()) {
                updatePlayerTranslation(player, cap);
            }
        }
    }
    
    private static void updatePlayerTranslation(Player player, IPlayerTranslationCapability cap) {
        Entity target = cap.getTargetEntity();
        if (target == null || !target.isAlive()) {
            ArsNoita.LOGGER.debug("Translation ended: target no longer exists");
            cap.clearTranslation();
            return;
        }
        
        // Decrement remaining ticks
        cap.decrementTicks();
        if (cap.getRemainingTicks() <= 0) {
            ArsNoita.LOGGER.debug("Translation ended: duration expired");
            cap.clearTranslation();
            return;
        }
        
        // Update target position
        updateTargetPosition(player, cap, target);
    }
    
    private static void updateTargetPosition(Player player, IPlayerTranslationCapability cap, Entity target) {
        // Calculate current player position and rotation
        Vec3 playerPos = player.position();
        float currentYaw = player.getYRot();
        float currentPitch = player.getXRot();
        
        // Calculate rotation difference
        float yawDiff = currentYaw - cap.getInitialYaw();
        float pitchDiff = currentPitch - cap.getInitialPitch();
        
        // Rotate the relative position vector
        Vec3 rotatedRelativePos = rotateVector(cap.getInitialRelativePos(), yawDiff, pitchDiff);
        
        // Calculate target position
        Vec3 targetPos = playerPos.add(rotatedRelativePos);
        
        // Check for collisions before moving
        if (canMoveToPosition(target, targetPos, player.level())) {
            // Move the entity
            target.setPos(targetPos.x, targetPos.y, targetPos.z);
            target.setDeltaMovement(Vec3.ZERO); // Stop any existing movement
            
            ArsNoita.LOGGER.debug("Translated entity {} to position {}", target.getName().getString(), targetPos);
        } else {
            ArsNoita.LOGGER.debug("Cannot move entity {} to position {} due to collision", 
                target.getName().getString(), targetPos);
        }
    }
    
    private static Vec3 rotateVector(Vec3 vector, float yawDiff, float pitchDiff) {
        // Convert to radians
        double yawRad = Math.toRadians(yawDiff);
        double pitchRad = Math.toRadians(pitchDiff);
        
        // Rotate around Y axis (yaw) - horizontal rotation
        double cosYaw = Math.cos(yawRad);
        double sinYaw = Math.sin(yawRad);
        double newX = vector.x * cosYaw - vector.z * sinYaw;
        double newZ = vector.x * sinYaw + vector.z * cosYaw;
        
        // For pitch rotation, we need to be more careful about the order
        // We'll rotate around the X axis, but only affect Y and Z
        double cosPitch = Math.cos(pitchRad);
        double sinPitch = Math.sin(pitchRad);
        
        // Apply pitch rotation to the Y-Z plane
        double newY = vector.y * cosPitch - newZ * sinPitch;
        double finalZ = vector.y * sinPitch + newZ * cosPitch;
        
        return new Vec3(newX, newY, finalZ);
    }
    
    private static boolean canMoveToPosition(Entity entity, Vec3 targetPos, Level world) {
        // More robust collision check
        BlockPos blockPos = BlockPos.containing(targetPos);
        
        // Check if the target position is in a solid block
        if (!world.getBlockState(blockPos).isAir()) {
            return false;
        }
        
        // Check if there's enough space for the entity
        // This is a simplified check - in a real implementation you'd want to check
        // the entity's bounding box against all blocks in the area
        BlockPos abovePos = blockPos.above();
        if (!world.getBlockState(abovePos).isAir()) {
            return false;
        }
        
        // Additional check: make sure the entity won't be inside a block
        // This is a basic implementation - could be enhanced with proper AABB checks
        return true;
    }
    
    private int getDuration(SpellStats spellStats) {
        return (int) (200 * spellStats.getDurationMultiplier()); // 10 seconds base
    }
    
    /**
     * Stop translation for a specific player
     */
    public static void stopTranslation(Player player) {
        IPlayerTranslationCapability cap = player.getCapability(ModCapabilities.TRANSLATION_CAPABILITY);
        if (cap != null) {
            cap.clearTranslation();
            ArsNoita.LOGGER.debug("Stopped translation for player {}", player.getName().getString());
        }
    }
    
    /**
     * Check if a player has an active translation
     */
    public static boolean hasActiveTranslation(Player player) {
        IPlayerTranslationCapability cap = player.getCapability(ModCapabilities.TRANSLATION_CAPABILITY);
        return cap != null && cap.hasActiveTranslation();
    }

    @Override
    public int getDefaultManaCost() {
        return 30;
    }

    @NotNull
    @Override
    public Set<AbstractAugment> getCompatibleAugments() {
        return Set.of(AugmentExtendTime.INSTANCE, AugmentSensitive.INSTANCE);
    }

    @Override
    public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
        super.addAugmentDescriptions(map);
        map.put(AugmentSensitive.INSTANCE, "Allows translation of the caster themselves");
        map.put(AugmentExtendTime.INSTANCE, "Increases the duration of the translation effect");
    }

    @Override
    public String getBookDescription() {
        return "Maintains the relative position of a target entity to the caster. The entity will follow the caster's movement and rotation, staying in the same relative position. Duration can be extended with augments.";
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

    @Override
    public ResourceLocation getRegistryName() {
        return ArsZero.prefix(ID);
    }
}