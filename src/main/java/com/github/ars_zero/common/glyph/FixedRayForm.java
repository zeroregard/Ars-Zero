package com.github.ars_zero.common.glyph;

import com.github.ars_zero.ArsZero;
import com.hollingsworth.arsnouveau.api.particle.ParticleEmitter;
import com.hollingsworth.arsnouveau.api.particle.configurations.properties.SoundProperty;
import com.hollingsworth.arsnouveau.api.particle.timelines.TimelineEntryData;
import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractCastMethod;
import com.hollingsworth.arsnouveau.api.spell.CastResolveType;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.common.items.Glyph;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAOE;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentSensitive;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;

public class FixedRayForm extends AbstractCastMethod {
    public static final String ID = "fixed_ray_form";
    public static final FixedRayForm INSTANCE = new FixedRayForm();

    public FixedRayForm() {
        super(ID, "Fixed Ray");
    }

    double getRange(SpellStats stats) {
        return BASE_RANGE.get() + BONUS_RANGE_PER_AUGMENT.get() * stats.getAoeMultiplier();
    }

    public ModConfigSpec.DoubleValue BASE_RANGE;
    public ModConfigSpec.DoubleValue BONUS_RANGE_PER_AUGMENT;

    @Override
    public void buildConfig(ModConfigSpec.Builder builder) {
        super.buildConfig(builder);
        BASE_RANGE = builder.comment("Base range in blocks").defineInRange("base_range", 16d, 0d, Double.MAX_VALUE);
        BONUS_RANGE_PER_AUGMENT = builder.comment("Bonus range per augment").defineInRange("bonus_range_per_augment", 16d, 0d, Double.MAX_VALUE);
    }

    public CastResolveType fireRay(Level world, LivingEntity shooter, SpellStats stats, SpellContext spellContext, SpellResolver resolver) {
        Vec3 fromPoint = shooter.getEyePosition(1.0f);
        Vec3 viewVector = shooter.getViewVector(1.0f);
        return fireRay(world, shooter, stats, spellContext, resolver, fromPoint, viewVector);
    }

    public CastResolveType fireRay(Level world, LivingEntity shooter, SpellStats stats, SpellContext spellContext, SpellResolver resolver, Vec3 fromPoint, Vec3 viewVector) {
        int sensitivity = stats.getBuffCount(AugmentSensitive.INSTANCE);
        double range = getRange(stats);

        Vec3 toPoint = fromPoint.add(viewVector.scale(range));
        ClipContext rayTraceContext = new ClipContext(fromPoint, toPoint, sensitivity >= 1 ? ClipContext.Block.OUTLINE : ClipContext.Block.COLLIDER, sensitivity >= 2 ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE, shooter);
        BlockHitResult blockTarget = world.clip(rayTraceContext);

        if (blockTarget.getType() != HitResult.Type.MISS) {
            double distance = fromPoint.distanceTo(blockTarget.getLocation());
            toPoint = fromPoint.add(viewVector.scale(Math.min(range, distance)));
        }
        EntityHitResult entityTarget = ProjectileUtil.getEntityHitResult(world, shooter, fromPoint, toPoint, new AABB(fromPoint, toPoint).inflate(1.5d), e -> e != shooter && e.isAlive() && e instanceof Entity, 0.0F);

        if (entityTarget != null) {
            resolver.onResolveEffect(world, entityTarget);
            Vec3 hitPoint = findNearestPointOnLine(fromPoint, toPoint, entityTarget.getLocation());
            send(world, spellContext, fromPoint, hitPoint);
            return CastResolveType.SUCCESS;
        }

        if (blockTarget.getType() == HitResult.Type.BLOCK) {
            resolver.onResolveEffect(world, blockTarget);
            send(world, spellContext, fromPoint, blockTarget.getLocation());
            return CastResolveType.SUCCESS;
        }

        if (blockTarget.getType() == HitResult.Type.MISS && sensitivity >= 2) {
            Vec3 approximateNormal = fromPoint.subtract(toPoint).normalize();
            blockTarget = new BlockHitResult(toPoint, Direction.getNearest(approximateNormal.x, approximateNormal.y, approximateNormal.z), BlockPos.containing(toPoint), true);
            resolver.onResolveEffect(world, blockTarget);
            send(world, spellContext, fromPoint, blockTarget.getLocation());
        } else {
            // BUG FIX: Always resolve the spell even when ray misses
            // This ensures the spell works even when hitting dead entity collision boxes
            Vec3 approximateNormal = fromPoint.subtract(toPoint).normalize();
            BlockHitResult missTarget = new BlockHitResult(toPoint, Direction.getNearest(approximateNormal.x, approximateNormal.y, approximateNormal.z), BlockPos.containing(toPoint), true);
            resolver.onResolveEffect(world, missTarget);
            send(world, spellContext, fromPoint, toPoint);
        }
        return CastResolveType.SUCCESS;
    }

    public void playResolveSound(SpellContext spellContext, Level level, Vec3 position) {
        // Use a simple sound effect since we don't have access to the original timeline
        // This could be enhanced with proper sound configuration if needed
    }

    public ParticleEmitter resolveEmitter(SpellContext spellContext, Vec3 position) {
        // Create a simple particle emitter for the ray effect
        // This could be enhanced with proper particle configuration if needed
        return null;
    }

    private void send(Level world, SpellContext spellContext, Vec3 from, Vec3 to) {
        double distance = from.distanceTo(to);
        var player = spellContext.getUnwrappedCaster();
        double start = 0.0, increment = 0.5;
        if (player.position().distanceToSqr(from) < 4.0 && to.subtract(from).normalize().dot(player.getViewVector(1f)) > Mth.SQRT_OF_TWO / 2) {
            start = Math.min(2.0, distance / 2.0);
            increment = 0.25;
        }
        for (double d = start; d < distance; d += increment) {
            double fractionalDistance = d / distance;
            Vec3 position = new Vec3(Mth.lerp(fractionalDistance, from.x, to.x),
                    Mth.lerp(fractionalDistance, from.y, to.y),
                    Mth.lerp(fractionalDistance, from.z, to.z));
            ParticleEmitter particleEmitter = resolveEmitter(spellContext, position);
            if (particleEmitter != null) {
                particleEmitter.tick(world);
            }
        }
        playResolveSound(spellContext, world, from);
    }

    @Nonnull
    private static Vec3 findNearestPointOnLine(@Nonnull Vec3 fromPoint, @Nonnull Vec3 toPoint, @Nonnull Vec3 hitPoint) {
        Vec3 u = toPoint.subtract(fromPoint);
        Vec3 pq = hitPoint.subtract(fromPoint);
        Vec3 w2 = pq.subtract(u.scale(pq.dot(u) / u.lengthSqr()));
        return hitPoint.subtract(w2);
    }

    @Override
    public CastResolveType onCast(@Nullable ItemStack itemStack, LivingEntity shooter, Level world, SpellStats stats, SpellContext spellContext, SpellResolver spellResolver) {
        return fireRay(world, shooter, stats, spellContext, spellResolver);
    }

    @Override
    public CastResolveType onCastOnBlock(UseOnContext itemUseContext, SpellStats stats, SpellContext spellContext, SpellResolver spellResolver) {
        return fireRay(itemUseContext.getLevel(), itemUseContext.getPlayer(), stats, spellContext, spellResolver);
    }

    @Override
    public CastResolveType onCastOnBlock(BlockHitResult blockRayTraceResult, LivingEntity shooter, SpellStats stats, SpellContext spellContext, SpellResolver spellResolver) {
        return fireRay(shooter.level(), shooter, stats, spellContext, spellResolver);
    }

    @Override
    public CastResolveType onCastOnEntity(@Nullable ItemStack itemStack, LivingEntity shooter, Entity target, InteractionHand hand, SpellStats stats, SpellContext spellContext, SpellResolver spellResolver) {
        return fireRay(shooter.level(), shooter, stats, spellContext, spellResolver);
    }

    @Override
    public int getDefaultManaCost() {
        return 15;
    }

    @Nonnull
    @Override
    public Set<AbstractAugment> getCompatibleAugments() {
        return Set.of(AugmentAOE.INSTANCE, AugmentSensitive.INSTANCE);
    }

    @Override
    protected void buildAugmentLimitsConfig(ModConfigSpec.Builder builder, Map<ResourceLocation, Integer> defaults) {
        defaults.put(AugmentSensitive.INSTANCE.getRegistryName(), 2);
        super.buildAugmentLimitsConfig(builder, defaults);
    }

    @Override
    public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
        super.addAugmentDescriptions(map);
        map.put(AugmentSensitive.INSTANCE, "Sensitive 1 lets the ray strike objects that do not block motion, such as plants or floating Magelight globes. Sensitive 2 allows the ray to strike fluids.");
        map.put(AugmentAOE.INSTANCE, "Increases reach.");
    }

    @Override
    public String getBookDescription() {
        return "Instantaneously strikes the pointed-at target, at limited yet greater range than Touch. Mana is expended whether or not the ray hits anything. AOE increases range. Sensitive 1 lets the ray strike objects that do not block motion, such as plants or floating Magelight globes. Sensitive 2 allows the ray to strike fluids. This fixed version ensures spells always resolve even when the ray misses due to dead entity collision boxes.";
    }

    @Override
    public Glyph getGlyph() {
        if (glyphItem == null) {
            glyphItem = new Glyph(this) {
                @Override
                public @NotNull String getCreatorModId(@NotNull ItemStack itemStack) {
                    return ArsZero.MOD_ID;
                }
            };
        }
        return this.glyphItem;
    }

    @Override
    public ResourceLocation getRegistryName() {
        return ArsZero.prefix(ID);
    }
}