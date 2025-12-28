package com.github.ars_zero.common.glyph;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.entity.ExplosionControllerEntity;
import com.github.ars_zero.common.item.AbstractMultiPhaseCastDevice;
import com.github.ars_zero.common.spell.ISubsequentEffectProvider;
import com.github.ars_zero.common.spell.MultiPhaseCastContext;
import com.github.ars_zero.common.spell.SpellAugmentExtractor;
import com.github.ars_zero.common.spell.SpellEffectType;
import com.github.ars_zero.common.spell.SpellResult;
import com.github.ars_zero.registry.ModEntities;
import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractEffect;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import com.hollingsworth.arsnouveau.api.spell.SpellSchools;
import com.hollingsworth.arsnouveau.api.spell.SpellSchool;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentDampen;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectExplosion;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;

public class EffectConvergence extends AbstractEffect implements ISubsequentEffectProvider {
    
    public static final String ID = "effect_convergence";
    public static final EffectConvergence INSTANCE = new EffectConvergence();
    
    private static final ResourceLocation[] SUBSEQUENT_GLYPHS = new ResourceLocation[]{
        EffectExplosion.INSTANCE.getRegistryName()
    };
    
    private static final int DEFAULT_LIFESPAN = 40;

    public EffectConvergence() {
        super(ArsZero.prefix(ID), "Convergence");
    }

    @Override
    public ResourceLocation[] getSubsequentEffectGlyphs() {
        return SUBSEQUENT_GLYPHS;
    }

    @Override
    public void onResolve(HitResult rayTraceResult, Level world, @Nullable LivingEntity shooter, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        if (world.isClientSide || !(world instanceof ServerLevel serverLevel)) {
            return;
        }

        Vec3 pos = safelyGetHitPos(rayTraceResult);
        
        if (hasExplosionEffect(spellContext)) {
            ExplosionControllerEntity entity = new ExplosionControllerEntity(ModEntities.EXPLOSION_CONTROLLER.get(), serverLevel);
            entity.setPos(pos.x, pos.y, pos.z);
            
            SpellContext iterator = spellContext.clone();
            while (iterator.hasNextPart()) {
                AbstractSpellPart next = iterator.nextPart();
                if (next instanceof EffectExplosion explosionEffect) {
                    SpellAugmentExtractor.AugmentData augmentData = SpellAugmentExtractor.extractApplicableAugments(spellContext, explosionEffect);
                    
                    double intensity = calculateExplosionIntensity(spellStats);
                    float baseDamage = EffectExplosion.INSTANCE.DAMAGE.get().floatValue();
                    float powerMultiplier = EffectExplosion.INSTANCE.AMP_DAMAGE.get().floatValue();
                    
                    entity.setExplosionParams(intensity, baseDamage, powerMultiplier, augmentData.aoeLevel, augmentData.amplifyLevel, augmentData.dampenLevel);
                    entity.setLifespan(DEFAULT_LIFESPAN);
                    
                    serverLevel.addFreshEntity(entity);
                    
                    updateTemporalContext(shooter, entity, spellContext);
                    
                    consumeEffect(spellContext, explosionEffect);
                    break;
                }
            }
        }
    }
    
    // TODO: Not a huge fan of this, we should be doing things differently
    private void updateTemporalContext(LivingEntity shooter, ExplosionControllerEntity entity, SpellContext spellContext) {
        if (!(shooter instanceof Player player)) {
            return;
        }
        
        ItemStack casterTool = spellContext.getCasterTool();
        MultiPhaseCastContext context = AbstractMultiPhaseCastDevice.findContextByStack(player, casterTool);
        if (context == null) {
            return;
        }
        
        SpellResult entityResult = SpellResult.fromHitResultWithCaster(
            new EntityHitResult(entity), 
            SpellEffectType.RESOLVED, 
            player
        );
        
        context.beginResults.clear();
        context.beginResults.add(entityResult);
    }

    private boolean hasExplosionEffect(SpellContext context) {
        SpellContext iterator = context.clone();
        while (iterator.hasNextPart()) {
            AbstractSpellPart next = iterator.nextPart();
            if (next instanceof EffectExplosion) {
                return true;
            }
        }
        return false;
    }

    private double calculateExplosionIntensity(SpellStats spellStats) {
        double base = EffectExplosion.INSTANCE.BASE.get();
        double ampValue = EffectExplosion.INSTANCE.AMP_VALUE.get();
        double aoeBonus = EffectExplosion.INSTANCE.AOE_BONUS.get();
        
        double intensity = base + ampValue * spellStats.getAmpMultiplier() + aoeBonus * spellStats.getAoeMultiplier();
        int dampen = spellStats.getBuffCount(AugmentDampen.INSTANCE);
        intensity -= 0.5 * dampen;
        
        return Math.max(0.0, intensity);
    }

    private void consumeEffect(SpellContext context, AbstractEffect targetEffect) {
        ResourceLocation targetId = targetEffect.getRegistryName();
        while (context.hasNextPart()) {
            AbstractSpellPart consumed = context.nextPart();
            if (consumed instanceof AbstractEffect consumedEffect && effectsMatch(consumedEffect, targetEffect, targetId)) {
                break;
            }
        }
    }

    private boolean effectsMatch(AbstractEffect candidate, AbstractEffect reference, ResourceLocation id) {
        if (candidate == reference) {
            return true;
        }
        ResourceLocation candidateId = candidate.getRegistryName();
        return id != null && id.equals(candidateId);
    }

    @Override
    public int getDefaultManaCost() {
        return 200;
    }

    @Override
    public SpellTier defaultTier() {
        return SpellTier.THREE;
    }

    @NotNull
    @Override
    public Set<AbstractAugment> getCompatibleAugments() {
        return Set.of();
    }

    @Override
    public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
        super.addAugmentDescriptions(map);
    }

    @Override
    public String getBookDescription() {
        return "Creates a convergence point that can be augmented with other effects.";
    }

    @NotNull
    @Override
    public Set<SpellSchool> getSchools() {
        return Set.of(SpellSchools.MANIPULATION);
    }
}