package com.github.ars_zero.common.glyph;

import com.github.ars_zero.ArsZero;
import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractCastMethod;
import com.hollingsworth.arsnouveau.api.spell.CastResolveType;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentDampen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;

public class NearForm extends AbstractCastMethod {
    
    public static final String ID = "near_form";
    public static final NearForm INSTANCE = new NearForm();

    public NearForm() {
        super(ID, "Near");
    }

    @Override
    public CastResolveType onCast(ItemStack stack, LivingEntity caster, Level world, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        return performNearCast(caster, world, spellStats, resolver);
    }

    @Override
    public CastResolveType onCastOnEntity(ItemStack stack, LivingEntity caster, Entity target, InteractionHand hand, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        return performNearCast(caster, caster.level(), spellStats, resolver);
    }

    @Override
    public CastResolveType onCastOnBlock(UseOnContext context, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        LivingEntity caster = (LivingEntity) context.getPlayer();
        if (caster == null) return CastResolveType.FAILURE;
        return performNearCast(caster, context.getLevel(), spellStats, resolver);
    }

    @Override
    public CastResolveType onCastOnBlock(BlockHitResult blockHitResult, LivingEntity caster, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        return performNearCast(caster, caster.level(), spellStats, resolver);
    }
    
    private CastResolveType performNearCast(LivingEntity caster, Level world, SpellStats spellStats, SpellResolver resolver) {
        double baseDistance = 1.0;
        double distance = baseDistance + spellStats.getAmpMultiplier() * 0.5;
        
        Vec3 eyePos = caster.getEyePosition(1.0f);
        Vec3 lookVec = caster.getLookAngle();
        Vec3 targetPos = eyePos.add(lookVec.scale(distance));
        
        BlockHitResult blockRayTrace = world.clip(new ClipContext(
            eyePos,
            targetPos,
            ClipContext.Block.OUTLINE,
            ClipContext.Fluid.NONE,
            caster
        ));
        
        AABB searchBox = new AABB(eyePos, targetPos).inflate(0.5);
        EntityHitResult entityHitResult = ProjectileUtil.getEntityHitResult(
            world,
            caster,
            eyePos,
            targetPos,
            searchBox,
            (entity) -> !entity.isSpectator() && entity.isPickable() && entity != caster
        );
        
        if (entityHitResult != null) {
            resolver.onResolveEffect(world, entityHitResult);
            ArsZero.LOGGER.debug("Near form resolved entity at {} (distance: {})", entityHitResult.getLocation(), distance);
        } else {
            Vec3 resolvePos = blockRayTrace.getType() == HitResult.Type.MISS ? targetPos : blockRayTrace.getLocation();
            Direction direction = Direction.getNearest(lookVec.x, lookVec.y, lookVec.z);
            BlockPos blockPos = BlockPos.containing(resolvePos);
            BlockHitResult result = new BlockHitResult(resolvePos, direction, blockPos, false);
            resolver.onResolveEffect(world, result);
            ArsZero.LOGGER.debug("Near form resolved block at {} with direction {} (distance: {})", resolvePos, direction, distance);
        }
        
        return CastResolveType.SUCCESS;
    }

    @Override
    public int getDefaultManaCost() {
        return 5;
    }

    @NotNull
    @Override
    public Set<AbstractAugment> getCompatibleAugments() {
        return Set.of(AugmentAmplify.INSTANCE, AugmentDampen.INSTANCE);
    }

    @Override
    public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
        super.addAugmentDescriptions(map);
        map.put(AugmentAmplify.INSTANCE, "Increases the cast distance");
        map.put(AugmentDampen.INSTANCE, "Decreases the cast distance");
    }

    @Override
    public String getBookDescription() {
        return "Casts the spell at a short distance in front of you (1 block by default). Perfect for conjuring entities or blocks in front of you without needing to aim at something. Distance can be adjusted with Amplify/Dampen.";
    }

    @Override
    public ResourceLocation getRegistryName() {
        return ArsZero.prefix(ID);
    }
}

