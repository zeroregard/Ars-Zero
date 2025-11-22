package com.github.ars_zero.common.glyph;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.entity.BaseVoxelEntity;
import com.github.ars_zero.common.entity.BlockGroupEntity;
import com.github.ars_zero.common.entity.GrappleTetherEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.github.ars_zero.common.item.AbstractMultiPhaseCastDevice;
import com.github.ars_zero.common.item.AbstractSpellStaff;
import com.github.ars_zero.common.spell.SpellEffectType;
import com.github.ars_zero.common.spell.SpellResult;
import com.github.ars_zero.common.spell.MultiPhaseCastContext;
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
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

/**
 * AnchorEffect - Works with Temporal Context Form to maintain relative position of entities.
 * 
 * When used with Temporal Context Form in the TICK phase, this effect keeps the entity
 * locked to the same position on the player's screen, following their look direction.
 */
public class AnchorEffect extends AbstractEffect {
    
    private static final Logger LOGGER = LogManager.getLogger();
    
    public static final String ID = "anchor_effect";
    public static final AnchorEffect INSTANCE = new AnchorEffect();

    public AnchorEffect() {
        super(ArsZero.prefix(ID), "Anchor");
    }

    @Override
    public void onResolveEntity(EntityHitResult rayTraceResult, Level world, @NotNull LivingEntity shooter, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        if (world.isClientSide) return;
        if (!(shooter instanceof Player player)) return;
        
        LOGGER.info("[Anchor] onResolveEntity called for player {}", player.getName().getString());
        
        ItemStack casterTool = spellContext.getCasterTool();
        MultiPhaseCastContext castContext = AbstractMultiPhaseCastDevice.findContextByStack(player, casterTool);
        
        if (castContext != null && !castContext.beginResults.isEmpty()) {
            LOGGER.info("[Anchor] Found cast context with {} beginResults", castContext.beginResults.size());
            for (int i = 0; i < castContext.beginResults.size(); i++) {
                SpellResult beginResult = castContext.beginResults.get(i);
                LOGGER.info("[Anchor] beginResults[{}]: targetEntity={}, entityType={}, blockGroup={}, targetPos={}, hitResultType={}", 
                    i, 
                    beginResult.targetEntity != null ? beginResult.targetEntity.getUUID() : "null",
                    beginResult.targetEntity != null ? beginResult.targetEntity.getClass().getSimpleName() : "null",
                    beginResult.blockGroup != null ? beginResult.blockGroup.getUUID() : "null",
                    beginResult.targetPosition != null ? beginResult.targetPosition.toString() : "null",
                    beginResult.hitResult != null ? beginResult.hitResult.getType().toString() : "null");
                
                if (beginResult.targetEntity instanceof GrappleTetherEntity tether) {
                    int tickDelay = calculateTickDelay(spellContext);
                    LOGGER.info("[Anchor] Found GrappleTetherEntity {} in beginResults, extending lifetime by {} ticks", tether.getUUID(), tickDelay);
                    tether.extendLifetime(tickDelay);
                    return;
                }
            }
            LOGGER.warn("[Anchor] No GrappleTetherEntity found in beginResults, continuing with normal anchor logic");
        } else {
            LOGGER.warn("[Anchor] No cast context or empty beginResults. castContext={}, beginResults empty={}", 
                castContext != null, castContext != null && castContext.beginResults.isEmpty());
        }
        
        if (world instanceof ServerLevel serverLevel) {
            GrappleTetherEntity existingTether = findExistingTether(serverLevel, player);
            if (existingTether != null) {
                int tickDelay = calculateTickDelay(spellContext);
                LOGGER.info("[Anchor] Found existing tether {} via entity search, extending lifetime by {} ticks", existingTether.getUUID(), tickDelay);
                existingTether.extendLifetime(tickDelay);
                return;
            }
        }
        
        if (castContext == null || castContext.beginResults.isEmpty()) {
            LOGGER.warn("[Anchor] No cast context or empty beginResults, returning early");
            return;
        }
        
        LOGGER.info("[Anchor] Processing {} beginResults for normal anchor behavior", castContext.beginResults.size());
        for (SpellResult beginResult : castContext.beginResults) {
            Entity target = beginResult.targetEntity;
            
            LOGGER.info("[Anchor] Processing beginResult: target={}, type={}, alive={}, hasOffset={}", 
                target != null ? target.getUUID() : "null",
                target != null ? target.getClass().getSimpleName() : "null",
                target != null && target.isAlive(),
                beginResult.relativeOffset != null);
            
            if (target == null || !target.isAlive()) {
                LOGGER.info("[Anchor] Skipping: target is null or not alive");
                continue;
            }
            
            if (beginResult.relativeOffset == null) {
                LOGGER.info("[Anchor] Skipping: no relative offset");
                continue;
            }
            
            if (target instanceof GrappleTetherEntity) {
                LOGGER.warn("[Anchor] Found GrappleTetherEntity in normal anchor loop - this should have been caught earlier!");
                continue;
            }
            
            Vec3 newPosition = beginResult.transformLocalToWorld(
                player.getYRot(), 
                player.getXRot(), 
                player.getEyePosition(1.0f),
                castContext.distanceMultiplier
            );
            
            if (newPosition != null && canMoveToPosition(newPosition, world)) {
                target.setPos(newPosition.x, newPosition.y, newPosition.z);
                target.setDeltaMovement(Vec3.ZERO);
                target.setNoGravity(true);
                
                if (target instanceof BaseVoxelEntity voxel) {
                    voxel.freezePhysics();
                }
            }
        }
    }
    
    private static boolean canMoveToPosition(Vec3 targetPos, Level world) {
        BlockPos blockPos = BlockPos.containing(targetPos);
        
        return !world.getBlockState(blockPos).blocksMotion();
    }
    
    public static void restoreEntityPhysics(MultiPhaseCastContext context) {
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
        
        List<SpellResult> newResults = new ArrayList<>();
        
        for (SpellResult beginResult : context.beginResults) {
            Entity target = beginResult.targetEntity;
            
            if (target != null && target.isAlive()) {
                if (target instanceof BlockGroupEntity blockGroup) {
                    float nearestRotation = blockGroup.getNearest90DegreeRotation(playerYaw);
                    List<BlockPos> placedPositions = blockGroup.placeBlocks(nearestRotation);
                    blockGroup.clearBlocks();
                    
                    if (player != null && !placedPositions.isEmpty()) {
                        for (BlockPos placedPos : placedPositions) {
                            BlockHitResult blockHit = new BlockHitResult(
                                Vec3.atCenterOf(placedPos),
                                Direction.UP,
                                placedPos,
                                false
                            );
                            SpellResult placedBlockResult = SpellResult.fromHitResultWithCaster(
                                blockHit,
                                SpellEffectType.RESOLVED,
                                player
                            );
                            newResults.add(placedBlockResult);
                        }
                    }
                    
                    blockGroup.discard();
                } else {
                    target.noPhysics = false;
                    target.setNoGravity(false);
                    newResults.add(beginResult);
                }
            } else {
                newResults.add(beginResult);
            }
        }
        
        context.beginResults.clear();
        context.beginResults.addAll(newResults);
    }

    @Override
    public void onResolveBlock(BlockHitResult rayTraceResult, Level world, @NotNull LivingEntity shooter, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        if (world.isClientSide) return;
        if (!(shooter instanceof Player player)) return;
        
        LOGGER.info("[Anchor] onResolveBlock called for player {} at block {}", player.getName().getString(), rayTraceResult.getBlockPos());
        
        ItemStack casterTool = spellContext.getCasterTool();
        MultiPhaseCastContext castContext = AbstractMultiPhaseCastDevice.findContextByStack(player, casterTool);
        
        if (castContext != null && !castContext.beginResults.isEmpty()) {
            LOGGER.info("[Anchor] Found cast context with {} beginResults (block)", castContext.beginResults.size());
            for (int i = 0; i < castContext.beginResults.size(); i++) {
                SpellResult beginResult = castContext.beginResults.get(i);
                LOGGER.info("[Anchor] beginResults[{}] (block): targetEntity={}, entityType={}, blockGroup={}", 
                    i, 
                    beginResult.targetEntity != null ? beginResult.targetEntity.getUUID() : "null",
                    beginResult.targetEntity != null ? beginResult.targetEntity.getClass().getSimpleName() : "null",
                    beginResult.blockGroup != null ? beginResult.blockGroup.getUUID() : "null");
                
                if (beginResult.targetEntity instanceof GrappleTetherEntity tether) {
                    int tickDelay = calculateTickDelay(spellContext);
                    LOGGER.info("[Anchor] Found GrappleTetherEntity {} in beginResults (block), extending lifetime by {} ticks", tether.getUUID(), tickDelay);
                    tether.extendLifetime(tickDelay);
                    return;
                }
            }
            LOGGER.warn("[Anchor] No GrappleTetherEntity found in beginResults (block), continuing with normal anchor logic");
        } else {
            LOGGER.warn("[Anchor] No cast context or empty beginResults (block). castContext={}, beginResults empty={}", 
                castContext != null, castContext != null && castContext.beginResults.isEmpty());
        }
        
        if (world instanceof ServerLevel serverLevel) {
            GrappleTetherEntity existingTether = findExistingTether(serverLevel, player);
            if (existingTether != null) {
                int tickDelay = calculateTickDelay(spellContext);
                existingTether.extendLifetime(tickDelay);
                return;
            }
        }
        
        if (castContext == null || castContext.beginResults.isEmpty()) {
            return;
        }
        
        for (SpellResult beginResult : castContext.beginResults) {
            LOGGER.info("[Anchor] Processing beginResult (block): blockGroup={}, hasOffset={}", 
                beginResult.blockGroup != null ? beginResult.blockGroup.getUUID() : "null",
                beginResult.relativeOffset != null);
            
            if (beginResult.blockGroup != null && beginResult.relativeOffset != null) {
                BlockGroupEntity blockGroup = beginResult.blockGroup;
                LOGGER.warn("[Anchor] Found BlockGroupEntity {} in beginResults - this should not happen with tether! Moving block group.", blockGroup.getUUID());
                
                Vec3 newPosition = beginResult.transformLocalToWorld(
                    player.getYRot(), 
                    player.getXRot(), 
                    player.getEyePosition(1.0f),
                    castContext.distanceMultiplier
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
        return "When used with Temporal Context Form in TICK phase, keeps the target entity or block group locked to the same position on your screen. The target will follow your look direction and movement. Use with Touch + [Target] in BEGIN, then Temporal Context Form + Anchor in TICK. For blocks, use Select + [Target] in BEGIN. Amplify increases distance from player.";
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
    
    @Nullable
    private GrappleTetherEntity findExistingTether(ServerLevel level, Player player) {
        for (net.minecraft.world.entity.Entity entity : level.getAllEntities()) {
            if (entity instanceof GrappleTetherEntity tether) {
                if (tether.getPlayerUUID() != null && tether.getPlayerUUID().equals(player.getUUID())) {
                    return tether;
                }
            }
        }
        return null;
    }
    
    private int calculateTickDelay(SpellContext spellContext) {
        try {
            com.hollingsworth.arsnouveau.api.spell.Spell spell = spellContext.getSpell();
            if (spell == null || spell.isEmpty()) {
                return 1;
            }
            
            int count = 0;
            Iterable<com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart> recipe = spell.recipe();
            for (com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart part : recipe) {
                ResourceLocation partId = part.getRegistryName();
                if (partId != null && partId.equals(ResourceLocation.fromNamespaceAndPath("ars_nouveau", "glyph_delay"))) {
                    count++;
                }
            }
            return Math.max(1, count);
        } catch (Exception e) {
            return 1;
        }
    }
}

