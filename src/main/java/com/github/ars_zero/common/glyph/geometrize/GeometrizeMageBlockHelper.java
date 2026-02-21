package com.github.ars_zero.common.glyph.geometrize;

import com.github.ars_zero.common.entity.GeometryEntity;
import com.github.ars_zero.common.spell.TemporalContextRecorder;
import com.github.ars_zero.common.shape.GeometryDescription;
import com.github.ars_zero.registry.ModEntities;
import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractEffect;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAOE;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentPierce;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public final class GeometrizeMageBlockHelper {

  private GeometrizeMageBlockHelper() {
  }

  public static void handleMageBlock(ServerLevel serverLevel, Vec3 pos, @Nullable LivingEntity shooter,
      SpellContext spellContext, EffectGeometrize geometrize, HitResult rayTraceResult,
      SpellResolver resolver) {
    BlockPos centerBlock = BlockPos.containing(pos);
    Vec3 center = Vec3.atCenterOf(centerBlock);

    GeometryDescription geometryDescription = GeometrizeCompatibilityHelper.resolveGeometryDescription(
        spellContext, shooter);

    List<AbstractAugment> augments = collectAugmentsAfterGeometrize(spellContext);
    int augmentCount = augments.size();
    int size = GeometrizeUtils.getPreferredSize(shooter, augmentCount);
    int depth = GeometrizeUtils.getPreferredDepth(shooter);

    Vec3 offsetCenter = GeometrizeUtils.calculateOffsetPosition(center, rayTraceResult, size,
        geometryDescription);

    GeometryEntity entity = new GeometryEntity(
        ModEntities.GEOMETRY_CONTROLLER.get(), serverLevel);
    entity.setPos(offsetCenter.x, offsetCenter.y, offsetCenter.z);
    if (shooter != null) {
      entity.setCaster(shooter);
      if (shooter instanceof Player player) {
        entity.setMarkerPos(player.blockPosition());
      }
    }
    entity.setLifespan(GeometrizeUtils.DEFAULT_LIFESPAN);
    entity.setGeometryDescription(geometryDescription);
    entity.setSize(size);
    entity.setDepth(depth);
    entity.setBasePosition(offsetCenter);
    entity.setSpellContext(spellContext, resolver);

    serverLevel.addFreshEntity(entity);
    TemporalContextRecorder.record(spellContext, entity);
    spellContext.setCanceled(true);
    geometrize.triggerResolveEffects(spellContext, serverLevel, center);
  }

  private static List<AbstractAugment> collectAugmentsAfterGeometrize(SpellContext context) {
    List<AbstractAugment> augments = new ArrayList<>();
    var recipe = context.getSpell().unsafeList();
    boolean foundGeometrize = false;

    for (AbstractSpellPart part : recipe) {
      if (!foundGeometrize) {
        if (part == EffectGeometrize.INSTANCE) {
          foundGeometrize = true;
        }
        continue;
      }

      if (part instanceof AbstractEffect) {
        break;
      }

      if (part instanceof AbstractAugment augment) {
        if (augment != AugmentAOE.INSTANCE && augment != AugmentPierce.INSTANCE) {
          augments.add(augment);
        }
      }
    }

    return augments;
  }
}
