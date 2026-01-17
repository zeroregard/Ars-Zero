package com.github.ars_zero.common.glyph.geometrize;

import com.github.ars_zero.common.glyph.augment.AugmentFlatten;
import com.github.ars_zero.common.glyph.augment.AugmentHollow;
import com.github.ars_zero.common.glyph.augment.AugmentSphere;
import com.github.ars_zero.common.glyph.augment.IGeometryAugment;
import com.github.ars_zero.common.shape.BaseShape;
import com.github.ars_zero.common.shape.FillMode;
import com.github.ars_zero.common.shape.GeometryDescription;
import com.github.ars_zero.common.shape.ProjectionMode;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public final class GeometrizeCompatibilityHelper {

    private GeometrizeCompatibilityHelper() {
    }

    public static GeometryDescription resolveGeometryDescription(SpellContext context, @Nullable LivingEntity caster) {
        boolean isSphere = false;
        boolean isHollow = false;
        boolean isFlattened = false;

        var recipe = context.getSpell().unsafeList();
        for (AbstractSpellPart part : recipe) {
            if (part instanceof IGeometryAugment) {
                if (part == AugmentSphere.INSTANCE) {
                    isSphere = true;
                } else if (part == AugmentHollow.INSTANCE) {
                    isHollow = true;
                } else if (part == AugmentFlatten.INSTANCE) {
                    isFlattened = true;
                }
            }
        }

        BaseShape baseShape = isSphere ? BaseShape.SPHERE : BaseShape.CUBE;
        FillMode fillMode = isHollow ? FillMode.HOLLOW : FillMode.SOLID;
        ProjectionMode projectionMode = isFlattened ? ProjectionMode.FLATTENED : ProjectionMode.FULL_3D;
        Vec3 orientation = caster != null ? caster.getLookAngle() : new Vec3(0, -1, 0);

        return new GeometryDescription(baseShape, fillMode, projectionMode, orientation);
    }
}

