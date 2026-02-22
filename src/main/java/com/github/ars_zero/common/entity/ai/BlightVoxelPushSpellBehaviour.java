package com.github.ars_zero.common.entity.ai;

import com.github.ars_zero.api.spell.MobSpellBehaviour;
import com.github.ars_zero.api.spell.MobSpellResolver;
import com.github.ars_zero.common.casting.CastingStyle;
import com.github.ars_zero.common.entity.ArcaneCircleEntity;
import com.github.ars_zero.common.entity.BlightVoxelEntity;
import com.github.ars_zero.common.entity.MageSkeletonEntity;
import com.github.ars_zero.registry.ModEntities;
import com.github.ars_zero.registry.ModGlyphs;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import com.hollingsworth.arsnouveau.common.spell.method.MethodTouch;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Spell behaviour: casting circle + blight voxel hover for 2s, then Push toward target.
 * Designed for reuse and for future chance-based spell selection.
 */
public class BlightVoxelPushSpellBehaviour implements MobSpellBehaviour {

    /** How long the circle and voxel hover before being pushed (2 seconds). */
    public static final int HOVER_TICKS = 40;
    public static final int CIRCLE_LIFESPAN_TICKS = HOVER_TICKS;
    public static final int BLIGHT_VOXEL_LIFETIME_TICKS = 200;
    /** Touch (5) + Push (25). */
    private static final int MANA_COST = 30;

    private static final Spell PUSH_SPELL = new Spell()
            .add(MethodTouch.INSTANCE)
            .add(ModGlyphs.PUSH_EFFECT);

    @Override
    public int getManaCost() {
        return MANA_COST;
    }

    @Override
    public boolean run(Mob caster, LivingEntity target) {
        if (!(caster.level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        // Point caster at target first so "in front" is toward target
        Vec3 toTarget = target.position().add(0, target.getBbHeight() * 0.5, 0)
                .subtract(caster.getEyePosition(1.0f)).normalize();
        double horizontalLength = Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z);
        float yaw = (float) (Math.atan2(-toTarget.x, toTarget.z) * 180.0 / Math.PI);
        float pitch = (float) (Math.atan2(-toTarget.y, horizontalLength) * 180.0 / Math.PI);
        caster.setYRot(yaw);
        caster.setXRot(pitch);

        // 1) Casting circle "near" (in front of mob), dark green, Anima symbol
        CastingStyle style = new CastingStyle();
        style.setEnabled(true);
        style.setPlacement(CastingStyle.Placement.NEAR);
        style.setColor(0x1B5E20);

        Vec3 spawnPos = caster.getEyePosition(1.0f).add(caster.getLookAngle().scale(1.0));

        ArcaneCircleEntity circleEntity = ModEntities.ARCANE_CIRCLE.get().create(serverLevel);
        if (circleEntity != null) {
            circleEntity.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
            circleEntity.setSyncedRotation(yaw, pitch);
            circleEntity.initialize(caster, style);
            circleEntity.setCurrentSchoolId("school_anima");
            circleEntity.setMaxAliveTicks(CIRCLE_LIFESPAN_TICKS);
            serverLevel.addFreshEntity(circleEntity);
        }

        // 2) Blight voxel at same spot as the circle (in front of mob); no gravity so it hovers during the delay
        BlightVoxelEntity voxel = new BlightVoxelEntity(serverLevel, spawnPos.x, spawnPos.y, spawnPos.z, BLIGHT_VOXEL_LIFETIME_TICKS);
        voxel.setNoGravityCustom(true);
        serverLevel.addFreshEntity(voxel);

        // 3) Schedule Push after HOVER_TICKS (2 seconds); mob will call executePush when chargeTicks hits 0
        if (caster instanceof MageSkeletonEntity mage) {
            mage.setPendingPush(voxel.getId(), HOVER_TICKS);
            return true;
        }
        return false;
    }

    /**
     * Executes the Push on the voxel toward the target. Called by MageSkeletonEntity when hover delay ends.
     * Deducts mana via the resolver.
     */
    public static void executePush(MageSkeletonEntity caster, LivingEntity target, BlightVoxelEntity voxel) {
        if (!(caster.level() instanceof ServerLevel serverLevel) || !voxel.isAlive()) {
            return;
        }
        Vec3 toTarget = target.position().add(0, target.getBbHeight() * 0.5, 0)
                .subtract(caster.getEyePosition(1.0f)).normalize();
        double horizontalLength = Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z);
        float yaw = (float) (Math.atan2(-toTarget.x, toTarget.z) * 180.0 / Math.PI);
        float pitch = (float) (Math.atan2(-toTarget.y, horizontalLength) * 180.0 / Math.PI);
        caster.setYRot(yaw);
        caster.setXRot(pitch);

        SpellContext context = new SpellContext(serverLevel, PUSH_SPELL, caster, LivingCaster.from(caster));
        MobSpellResolver resolver = new MobSpellResolver(context);
        if (resolver.canCast(caster)) {
            resolver.onCastOnEntity(ItemStack.EMPTY, voxel, InteractionHand.MAIN_HAND);
        } else {
            voxel.discard();
        }
    }
}
