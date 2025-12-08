package com.github.ars_zero.common.glyph;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.entity.ArcaneShieldEntity;
import com.github.ars_zero.common.util.MathHelper;
import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractEffect;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import com.hollingsworth.arsnouveau.api.spell.SpellSchools;
import com.hollingsworth.arsnouveau.api.spell.SpellSchool;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAOE;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentExtendTime;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentSplit;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;

public class ConjureArcaneShieldEffect extends AbstractEffect {
    
    public static final String ID = "conjure_arcane_shield_effect";
    public static final ConjureArcaneShieldEffect INSTANCE = new ConjureArcaneShieldEffect();
    private static final float DEFAULT_HEALTH_MULTIPLIER = 1.0f;
    
    public ConjureArcaneShieldEffect() {
        super(ArsZero.prefix(ID), "Conjure Arcane Shield");
    }
    
    @Override
    public void onResolveEntity(EntityHitResult rayTraceResult, Level world, @NotNull LivingEntity shooter, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        if (world instanceof ServerLevel serverLevel) {
            Vec3 pos = rayTraceResult.getEntity().position();
            createShields(serverLevel, pos.x, pos.y, pos.z, shooter, spellStats, spellContext);
        }
    }
    
    @Override
    public void onResolveBlock(BlockHitResult rayTraceResult, Level world, @NotNull LivingEntity shooter, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        if (world instanceof ServerLevel serverLevel) {
            Vec3 hitLocation = rayTraceResult.getLocation();
            createShields(serverLevel, hitLocation.x, hitLocation.y, hitLocation.z, shooter, spellStats, spellContext);
        }
    }
    
    private void createShields(ServerLevel level, double x, double y, double z, LivingEntity shooter, SpellStats spellStats, SpellContext spellContext) {
        int splitLevel = spellStats.getBuffCount(AugmentSplit.INSTANCE);
        int aoeLevel = Math.min(spellStats.getBuffCount(AugmentAOE.INSTANCE), 1);
        boolean reflectiveMode = aoeLevel > 0;
        
        int duration = getDuration(spellStats);
        
        float healthMultiplier = DEFAULT_HEALTH_MULTIPLIER;
        int amplifyLevel = spellStats.getBuffCount(AugmentAmplify.INSTANCE);
        if (amplifyLevel > 0) {
            healthMultiplier += amplifyLevel * 0.5f;
        }
        
        int entityCount;
        double circleRadius;
        
        switch (Math.min(splitLevel, 3)) {
            case 1:
                entityCount = 3;
                circleRadius = 0.5;
                break;
            case 2:
                entityCount = 5;
                circleRadius = 0.75;
                break;
            case 3:
                entityCount = 7;
                circleRadius = 1.0;
                break;
            default:
                entityCount = 1;
                circleRadius = 0.0;
                break;
        }
        
        if (entityCount == 1) {
            ArcaneShieldEntity shield = new ArcaneShieldEntity(level, x, y, z, healthMultiplier, reflectiveMode, duration);
            if (shooter != null) {
                shield.setOwner(shooter);
            }
            level.addFreshEntity(shield);
        } else {
            Vec3 center = new Vec3(x, y, z);
            Vec3 lookDirection = shooter.getLookAngle();
            java.util.List<Vec3> positions = MathHelper.getCirclePositions(center, lookDirection, circleRadius, entityCount);
            
            for (Vec3 pos : positions) {
                ArcaneShieldEntity shield = new ArcaneShieldEntity(level, pos.x, pos.y, pos.z, healthMultiplier, reflectiveMode, duration);
                if (shooter != null) {
                    shield.setOwner(shooter);
                }
                level.addFreshEntity(shield);
            }
        }
    }
    
    private int getDuration(SpellStats spellStats) {
        double durationMultiplier = spellStats.getDurationMultiplier();
        if (durationMultiplier <= 0) {
            durationMultiplier = 1.0;
        }
        return (int) (1200 * durationMultiplier);
    }
    
    @Override
    public int getDefaultManaCost() {
        return 10;
    }
    
    @NotNull
    @Override
    public Set<AbstractAugment> getCompatibleAugments() {
        return Set.of(AugmentAmplify.INSTANCE, AugmentExtendTime.INSTANCE, AugmentSplit.INSTANCE, AugmentAOE.INSTANCE);
    }
    
    @Override
    protected void addDefaultAugmentLimits(Map<ResourceLocation, Integer> defaults) {
        defaults.put(AugmentSplit.INSTANCE.getRegistryName(), 3);
        defaults.put(AugmentAOE.INSTANCE.getRegistryName(), 1);
    }
    
    @Override
    public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
        super.addAugmentDescriptions(map);
        map.put(AugmentAmplify.INSTANCE, "Increases the health multiplier of the shield");
        map.put(AugmentExtendTime.INSTANCE, "Increases the duration the shield remains");
        map.put(AugmentSplit.INSTANCE, "Splits the shield into multiples");
        map.put(AugmentAOE.INSTANCE, "Switches the shield to reflective mode");
    }
    
    @Override
    public String getBookDescription() {
        return "Conjures an arcane shield that absorbs damage. In reflective mode (with AOE augment), it reflects projectiles and reverses knockback from the front.";
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
