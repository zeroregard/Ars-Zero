package com.github.ars_zero.common.glyph.convergence;

import alexthw.ars_elemental.common.glyphs.EffectConjureTerrain;
import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.glyph.augment.IGeometryAugment;
import com.github.ars_zero.common.glyph.augment.IFillAugment;
import com.github.ars_zero.common.glyph.augment.IProjectionAugment;
import com.github.ars_zero.common.glyph.augment.IShapeAugment;
import com.github.ars_zero.common.shape.BaseShape;
import com.github.ars_zero.common.shape.FillMode;
import com.github.ars_zero.common.shape.ProjectionMode;
import com.github.ars_zero.common.shape.GeometryDescription;
import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractEffect;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectConjureWater;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectExplosion;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;

public final class ConvergenceCompatibilityHelper {
    private static final Logger LOGGER = LogManager.getLogger(ArsZero.MOD_ID);

    private ConvergenceCompatibilityHelper() {
    }

    public static boolean isConvergenceAugment(AbstractAugment augment) {
        return augment instanceof IGeometryAugment;
    }

    public static boolean isShapeAugment(AbstractAugment augment) {
        return augment instanceof IShapeAugment;
    }

    public static boolean isFillAugment(AbstractAugment augment) {
        return augment instanceof IFillAugment;
    }

    public static boolean isProjectionAugment(AbstractAugment augment) {
        return augment instanceof IProjectionAugment;
    }

    public static boolean isTerrainOnlyAugment(AbstractAugment augment) {
        return augment instanceof IGeometryAugment;
    }

    public static boolean hasTerrainOnlyAugments(SpellContext context) {
        SpellContext iterator = context.clone();
        ResourceLocation convergenceId = EffectConvergence.INSTANCE.getRegistryName();
        boolean foundConvergence = false;

        while (iterator.hasNextPart()) {
            AbstractSpellPart part = iterator.nextPart();

            if (!foundConvergence) {
                if (part instanceof AbstractEffect effect && convergenceId.equals(effect.getRegistryName())) {
                    foundConvergence = true;
                }
                continue;
            }

            if (part instanceof AbstractEffect) {
                break;
            }

            if (part instanceof AbstractAugment augment && isTerrainOnlyAugment(augment)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isCompatibleWithTerrainAugments(AbstractEffect effect) {
        if (effect instanceof EffectConjureTerrain) {
            return true;
        }
        return false;
    }

    public static boolean isIncompatibleCombination(SpellContext context) {
        if (!hasTerrainOnlyAugments(context)) {
            return false;
        }

        AbstractEffect firstEffect = findFirstConvergenceEffect(context);
        if (firstEffect == null) {
            return false;
        }

        return !isCompatibleWithTerrainAugments(firstEffect);
    }

    @Nullable
    private static AbstractEffect findFirstConvergenceEffect(SpellContext context) {
        SpellContext iterator = context.clone();
        ResourceLocation convergenceId = EffectConvergence.INSTANCE.getRegistryName();
        boolean foundConvergence = false;

        while (iterator.hasNextPart()) {
            AbstractSpellPart next = iterator.nextPart();

            if (!foundConvergence) {
                if (next instanceof AbstractEffect effect && convergenceId.equals(effect.getRegistryName())) {
                    foundConvergence = true;
                }
                continue;
            }

            if (next instanceof AbstractEffect effect) {
                if (effect instanceof EffectExplosion || effect instanceof EffectConjureWater
                        || effect instanceof EffectConjureTerrain) {
                    return effect;
                }
            }
        }

        return null;
    }

    public static GeometryDescription resolveGeometryDescription(SpellContext context, @Nullable LivingEntity caster) {
        BaseShape baseShape = BaseShape.CUBE;
        FillMode fillMode = FillMode.SOLID;
        ProjectionMode projectionMode = ProjectionMode.FULL_3D;
        Vec3 orientation = null;

        SpellContext iterator = context.clone();

        LOGGER.info("[GeometryDebug] Starting resolveGeometryDescription (context already past Convergence)");

        while (iterator.hasNextPart()) {
            AbstractSpellPart part = iterator.nextPart();
            LOGGER.info("[GeometryDebug] Processing part: {} (class: {})",
                    part.getRegistryName(), part.getClass().getSimpleName());

            if (part instanceof AbstractEffect) {
                LOGGER.info("[GeometryDebug] Skipping effect: {}", part.getRegistryName());
                continue;
            }

            LOGGER.info(
                    "[GeometryDebug] Checking augment: {}, isIShapeAugment={}, isIFillAugment={}, isIProjectionAugment={}",
                    part.getRegistryName(),
                    part instanceof IShapeAugment,
                    part instanceof IFillAugment,
                    part instanceof IProjectionAugment);

            if (part instanceof IShapeAugment shapeAugment) {
                baseShape = shapeAugment.getShape();
                LOGGER.info("[GeometryDebug] Set baseShape to: {}", baseShape);
            } else if (part instanceof IFillAugment fillAugment) {
                fillMode = fillAugment.getFillMode();
                LOGGER.info("[GeometryDebug] Set fillMode to: {}", fillMode);
            } else if (part instanceof IProjectionAugment projectionAugment) {
                projectionMode = projectionAugment.getProjectionMode();
                LOGGER.info("[GeometryDebug] Set projectionMode to: {}", projectionMode);
            }
        }

        if (projectionMode == ProjectionMode.FLATTENED && caster != null) {
            orientation = caster.getLookAngle();
        }

        GeometryDescription result = new GeometryDescription(baseShape, fillMode, projectionMode, orientation);
        LOGGER.info("[GeometryDebug] Final GeometryDescription: {}", result);
        return result;
    }

    public static int countShapeAugments(SpellContext context) {
        return countAugmentsMatching(context, IShapeAugment.class);
    }

    public static int countFillAugments(SpellContext context) {
        return countAugmentsMatching(context, IFillAugment.class);
    }

    public static int countProjectionAugments(SpellContext context) {
        return countAugmentsMatching(context, IProjectionAugment.class);
    }

    private static int countAugmentsMatching(SpellContext context, Class<?> markerInterface) {
        SpellContext iterator = context.clone();
        ResourceLocation convergenceId = EffectConvergence.INSTANCE.getRegistryName();
        boolean foundConvergence = false;
        int count = 0;

        while (iterator.hasNextPart()) {
            AbstractSpellPart part = iterator.nextPart();

            if (!foundConvergence) {
                if (part instanceof AbstractEffect effect && convergenceId.equals(effect.getRegistryName())) {
                    foundConvergence = true;
                }
                continue;
            }

            if (part instanceof AbstractEffect) {
                break;
            }

            if (markerInterface.isInstance(part)) {
                count++;
            }
        }

        return count;
    }
}
