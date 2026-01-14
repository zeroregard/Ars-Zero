package com.github.ars_zero.common.glyph.geometrize;

import com.github.ars_zero.common.util.GeometryPlayerPreferences;
import com.github.ars_zero.common.shape.BaseShape;
import com.github.ars_zero.common.shape.GeometryDescription;
import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractEffect;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public final class GeometrizeUtils {

    public static final int DEFAULT_LIFESPAN = 20;

    private GeometrizeUtils() {
    }

    public static int countAugmentsAfterEffect(SpellContext context, Class<? extends AbstractSpellPart> effectClass) {
        var recipe = context.getSpell().unsafeList();
        boolean foundEffect = false;
        int count = 0;

        for (AbstractSpellPart part : recipe) {
            if (!foundEffect) {
                if (effectClass.isInstance(part)) {
                    foundEffect = true;
                }
                continue;
            }

            if (part instanceof AbstractEffect) {
                break;
            }

            if (part instanceof AbstractAugment) {
                count++;
            }
        }

        return count;
    }

    public static int getPreferredSize(@Nullable LivingEntity shooter, int augmentCount) {
        int baseSize = 1 + augmentCount;
        if (shooter instanceof Player player) {
            int storedSize = GeometryPlayerPreferences.getPreferredSize(player);
            if (storedSize > 0) {
                baseSize = storedSize;
            }
        }
        return Math.max(1, Math.min(baseSize, EffectGeometrize.INSTANCE.getMaxSize()));
    }

    public static int getPreferredDepth(@Nullable LivingEntity shooter) {
        if (shooter instanceof Player player) {
            return GeometryPlayerPreferences.getPreferredDepth(player);
        }
        return 1;
    }

    public static Vec3 calculateOffsetPosition(Vec3 center, HitResult rayTraceResult, int size,
                                               GeometryDescription geometryDescription) {
        if (!(rayTraceResult instanceof BlockHitResult blockHitResult)) {
            return center;
        }

        Direction hitFace = blockHitResult.getDirection();
        boolean isFlattened = geometryDescription.isFlattened();

        if (isFlattened) {
            return center;
        }

        boolean isSphere = geometryDescription.baseShape() == BaseShape.SPHERE;
        double halfSize = size / 2.0;

        if (isSphere) {
            double offset = halfSize;
            return center.add(
                    hitFace.getStepX() * offset,
                    hitFace.getStepY() * offset,
                    hitFace.getStepZ() * offset);
        }

        double offset = (size % 2 == 0) ? halfSize : Math.floor(halfSize);
        return center.add(
                hitFace.getStepX() * offset,
                hitFace.getStepY() * offset,
                hitFace.getStepZ() * offset);
    }
}

