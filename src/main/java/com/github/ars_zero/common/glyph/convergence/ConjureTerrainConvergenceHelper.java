package com.github.ars_zero.common.glyph.convergence;

import alexthw.ars_elemental.common.glyphs.EffectConjureTerrain;
import com.github.ars_zero.common.entity.terrain.ConjureTerrainConvergenceEntity;
import com.github.ars_zero.registry.ModEntities;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public final class ConjureTerrainConvergenceHelper {
    private static final int DEFAULT_LIFESPAN = 20;

    private ConjureTerrainConvergenceHelper() {
    }

    public static void handleConjureTerrain(ServerLevel serverLevel, Vec3 pos, @Nullable LivingEntity shooter,
            SpellContext spellContext, EffectConvergence convergence) {
        BlockPos centerBlock = BlockPos.containing(pos);
        Vec3 center = Vec3.atCenterOf(centerBlock);

        ConjureTerrainConvergenceEntity entity = new ConjureTerrainConvergenceEntity(
                ModEntities.CONJURE_TERRAIN_CONVERGENCE_CONTROLLER.get(), serverLevel);
        entity.setPos(center.x, center.y, center.z);
        if (shooter instanceof Player player) {
            entity.setCasterUUID(player.getUUID());
            entity.setMarkerPos(player.blockPosition());
        }
        entity.setLifespan(DEFAULT_LIFESPAN);

        SpellContext iterator = spellContext.clone();
        while (iterator.hasNextPart()) {
            AbstractSpellPart next = iterator.nextPart();
            if (next instanceof EffectConjureTerrain conjureTerrainEffect) {
                serverLevel.addFreshEntity(entity);
                convergence.updateTemporalContext(shooter, entity, spellContext);
                convergence.consumeEffect(spellContext, conjureTerrainEffect);
                convergence.triggerResolveEffects(spellContext, serverLevel, center);
                break;
            }
        }
    }
}

