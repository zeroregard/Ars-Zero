package com.github.ars_zero.client.renderer.entity.model;

import com.github.ars_zero.common.entity.AbstractBlightedSkeleton;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Skeleton;

/**
 * Skeleton model that raises both arms when the entity is in "spell cast" pose
 * (see {@link AbstractBlightedSkeleton#getSpellCastArmsUpTicks()}).
 */
public class BlightedSkeletonModel extends SkeletonModel<Skeleton> {

    public BlightedSkeletonModel(ModelPart root) {
        super(root);
    }

    @Override
    public void setupAnim(Skeleton entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        if (entity instanceof AbstractBlightedSkeleton blighted && blighted.getSpellCastArmsUpTicks() > 0) {
            // Both arms raised in front for ~1 second (e.g. spell casting pose)
            float raised = -Mth.HALF_PI;
            this.leftArm.xRot = raised;
            this.rightArm.xRot = raised;
        }
    }
}
