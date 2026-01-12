package com.github.ars_zero.common.glyph.convergence;

import com.github.ars_zero.common.shape.GeometryDescription;
import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractEffect;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Shared utility methods for geometry-based convergence effects (Terrain, Break, etc.)
 */
public final class GeometryConvergenceUtils {

    public static final int DEFAULT_LIFESPAN = 20;
    public static final int BASE_SIZE = 5;

    private GeometryConvergenceUtils() {
    }

    /**
     * Calculate the offset position based on hit result, size, and geometry description.
     * Places the structure on top of/adjacent to the hit surface rather than inside it.
     */
    public static Vec3 calculateOffsetPosition(Vec3 hitPos, HitResult rayTraceResult, int size,
            GeometryDescription geometryDescription) {
        if (!(rayTraceResult instanceof BlockHitResult blockHitResult)) {
            return hitPos;
        }

        Direction face = blockHitResult.getDirection();
        Vec3 normal = Vec3.atLowerCornerOf(face.getNormal());

        double offsetAmount;
        if (geometryDescription.isFlattened()) {
            offsetAmount = 0.5;
        } else {
            offsetAmount = (size / 2.0) + 0.5;
        }

        return hitPos.add(normal.scale(offsetAmount));
    }

    /**
     * Count the number of augments following a target effect in the spell.
     * Stops counting when another effect is encountered.
     */
    public static int countAugmentsAfterEffect(SpellContext context, Class<? extends AbstractEffect> effectClass) {
        SpellContext iterator = context.clone();
        boolean foundEffect = false;
        int count = 0;

        while (iterator.hasNextPart()) {
            AbstractSpellPart part = iterator.nextPart();

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

    /**
     * Calculate the size based on augment count.
     */
    public static int calculateSize(int augmentCount) {
        return BASE_SIZE + augmentCount;
    }
}


