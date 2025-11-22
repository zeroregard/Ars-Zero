package com.github.ars_zero.common.glyph;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.entity.GrappleTetherEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.github.ars_zero.common.item.AbstractMultiPhaseCastDevice;
import com.github.ars_zero.common.spell.MultiPhaseCastContext;
import com.github.ars_zero.common.spell.SpellEffectType;
import com.github.ars_zero.common.spell.SpellResult;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

public class TetherGlyphEffect extends AbstractEffect {
    
    private static final Logger LOGGER = LogManager.getLogger();
    
    public static final String ID = "tether_effect";
    public static final TetherGlyphEffect INSTANCE = new TetherGlyphEffect();
    private static final int BASE_LIFETIME = 20;
    private static final float DEFAULT_MAX_LENGTH = 10.0f;
    private static final float MIN_LENGTH = 2.0f;
    private static final float MAX_LENGTH = 50.0f;
    
    public TetherGlyphEffect() {
        super(ArsZero.prefix(ID), "Tether");
    }
    
    @Override
    public void onResolveBlock(BlockHitResult rayTraceResult, Level world, @NotNull LivingEntity shooter, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        if (world.isClientSide) return;
        if (!(shooter instanceof Player player)) return;
        if (!(world instanceof ServerLevel serverLevel)) return;
        
        BlockPos targetPos = rayTraceResult.getBlockPos();
        if (targetPos == null) return;
        
        if (targetPos.equals(BlockPos.ZERO)) return;
        
        float maxLength = getMaxLength(spellStats);
        int lifetime = getLifetime(spellStats, spellContext);
        
        GrappleTetherEntity existingTether = findExistingTether(serverLevel, player, targetPos, null);
        if (existingTether != null) {
            existingTether.discard();
        }
        
        GrappleTetherEntity tether = new GrappleTetherEntity(serverLevel, targetPos, player, maxLength, lifetime);
        serverLevel.addFreshEntity(tether);
        LOGGER.info("[Tether] Created tether entity {} for player {} targeting block {}", tether.getUUID(), player.getName().getString(), targetPos);
        updateTemporalContext(shooter, tether, spellContext);
    }
    
    @Override
    public void onResolveEntity(EntityHitResult rayTraceResult, Level world, @NotNull LivingEntity shooter, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        if (world.isClientSide) return;
        if (!(shooter instanceof Player player)) return;
        if (!(world instanceof ServerLevel serverLevel)) return;
        
        net.minecraft.world.entity.Entity target = rayTraceResult.getEntity();
        if (target == null) {
            LOGGER.warn("[Tether] onResolveEntity: target entity is null");
            return;
        }
        
        LOGGER.info("[Tether] onResolveEntity called for player {} targeting entity {} (type: {})", 
            player.getName().getString(), target.getUUID(), target.getClass().getSimpleName());
        
        float maxLength = getMaxLength(spellStats);
        int lifetime = getLifetime(spellStats, spellContext);
        
        LOGGER.info("[Tether] Creating tether: maxLength={}, lifetime={}", maxLength, lifetime);
        
        GrappleTetherEntity existingTether = findExistingTether(serverLevel, player, null, target);
        if (existingTether != null) {
            existingTether.discard();
        }
        
        GrappleTetherEntity tether = new GrappleTetherEntity(serverLevel, target, player, maxLength, lifetime);
        LOGGER.info("[Tether] Creating tether entity for player {} targeting entity {} at position {}", 
            player.getName().getString(), target.getUUID(), target.position());
        serverLevel.addFreshEntity(tether);
        LOGGER.info("[Tether] Created and added tether entity {} to world. Entity alive: {}, position: {}", 
            tether.getUUID(), tether.isAlive(), tether.position());
        updateTemporalContext(shooter, tether, spellContext);
    }
    
    private float getMaxLength(SpellStats spellStats) {
        int amplifyLevel = spellStats.getBuffCount(AugmentAmplify.INSTANCE);
        int dampenLevel = spellStats.getBuffCount(AugmentDampen.INSTANCE);
        
        float length = DEFAULT_MAX_LENGTH;
        length += amplifyLevel * 5.0f;
        length -= dampenLevel * 2.0f;
        
        return Math.max(MIN_LENGTH, Math.min(MAX_LENGTH, length));
    }
    
    private int getLifetime(SpellStats spellStats, SpellContext spellContext) {
        int lifetime = BASE_LIFETIME;
        
        int delayCount = countDelayAugments(spellContext);
        if (delayCount > 0) {
            lifetime += delayCount;
        }
        
        return lifetime;
    }
    
    private int countDelayAugments(SpellContext spellContext) {
        try {
            com.hollingsworth.arsnouveau.api.spell.Spell spell = spellContext.getSpell();
            if (spell == null || spell.isEmpty()) {
                return 0;
            }
            
            int count = 0;
            Iterable<com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart> recipe = spell.recipe();
            for (com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart part : recipe) {
                ResourceLocation partId = part.getRegistryName();
                if (partId != null && partId.equals(ResourceLocation.fromNamespaceAndPath("ars_nouveau", "glyph_delay"))) {
                    count++;
                }
            }
            return count;
        } catch (Exception e) {
            return 0;
        }
    }
    
    private GrappleTetherEntity findExistingTether(ServerLevel level, Player player, @Nullable BlockPos targetBlock, @Nullable net.minecraft.world.entity.Entity targetEntity) {
        for (net.minecraft.world.entity.Entity entity : level.getAllEntities()) {
            if (entity instanceof GrappleTetherEntity tether) {
                if (tether.getPlayerUUID() != null && tether.getPlayerUUID().equals(player.getUUID())) {
                    if (targetBlock != null && tether.isTarget(targetBlock)) {
                        return tether;
                    }
                    if (targetEntity != null && tether.isTarget(targetEntity)) {
                        return tether;
                    }
                    if (targetBlock == null && targetEntity == null) {
                        return tether;
                    }
                }
            }
        }
        return null;
    }
    
    private void updateTemporalContext(LivingEntity shooter, GrappleTetherEntity tether, SpellContext spellContext) {
        if (!(shooter instanceof Player player)) {
            LOGGER.warn("[Tether] updateTemporalContext called with non-player shooter: {}", shooter);
            return;
        }
        
        ItemStack casterTool = spellContext.getCasterTool();
        MultiPhaseCastContext context = AbstractMultiPhaseCastDevice.findContextByStack(player, casterTool);
        if (context == null) {
            LOGGER.warn("[Tether] updateTemporalContext: No cast context found for player {}", player.getName().getString());
            return;
        }
        
        EntityHitResult entityHit = new EntityHitResult(tether, tether.position());
        SpellResult tetherResult = SpellResult.fromHitResultWithCaster(entityHit, SpellEffectType.RESOLVED, player);
        
        if (!context.beginResults.isEmpty()) {
            SpellResult oldResult = context.beginResults.get(0);
            LOGGER.info("[Tether] Replacing old result in beginResults. Old entity type: {}", oldResult.targetEntity != null ? oldResult.targetEntity.getClass().getSimpleName() : "null");
            if (oldResult.targetEntity instanceof GrappleTetherEntity) {
                LOGGER.info("[Tether] Discarding old tether entity {}", oldResult.targetEntity.getUUID());
                ((GrappleTetherEntity) oldResult.targetEntity).discard();
            }
        }
        
        context.beginResults.clear();
        context.beginResults.add(tetherResult);
        LOGGER.info("[Tether] Stored tether entity {} in beginResults. beginResults size: {}", tether.getUUID(), context.beginResults.size());
    }
    
    @Override
    public int getDefaultManaCost() {
        return 50;
    }
    
    @NotNull
    @Override
    public Set<AbstractAugment> getCompatibleAugments() {
        return Set.of(AugmentAmplify.INSTANCE, AugmentDampen.INSTANCE);
    }
    
    @Override
    public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
        super.addAugmentDescriptions(map);
        map.put(AugmentAmplify.INSTANCE, "Increases the maximum tether length");
        map.put(AugmentDampen.INSTANCE, "Decreases the maximum tether length");
    }
    
    @Override
    public String getBookDescription() {
        return "Creates a physics tether to the target block or entity. The tether pulls you toward the anchor point if extended beyond its maximum length. Base lifespan is 20 ticks. Each Delay augment adds 1 tick to the lifespan. When Anchor is used on an existing tether, it extends the lifespan by the tick delay value.";
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
