package com.github.ars_zero.common.glyph.convergence;

import com.github.ars_zero.common.entity.PlayerChargerEntity;
import com.github.ars_zero.common.entity.SourceJarChargerEntity;
import com.github.ars_zero.registry.ModEntities;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.common.block.tile.SourceJarTile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class ChargerHelper {

    private static final int DEFAULT_LIFESPAN = 20;

    private ChargerHelper() {
    }

    public static void handlePlayerCharger(ServerLevel serverLevel, Vec3 pos, EntityHitResult entityHitResult,
            @Nullable LivingEntity shooter, SpellContext spellContext, EffectConvergence convergence) {
        if (entityHitResult.getEntity() instanceof Player targetPlayer && shooter != null) {
            PlayerChargerEntity chargerEntity = new PlayerChargerEntity(ModEntities.PLAYER_CHARGER.get(), serverLevel);
            chargerEntity.setPos(pos.x, pos.y, pos.z);
            chargerEntity.setTargetPlayerUUID(targetPlayer.getUUID());
            chargerEntity.setCasterUUID(shooter.getUUID());
            chargerEntity.setLifespan(DEFAULT_LIFESPAN);
            SoundEvent resolveSound = convergence.getResolveSoundFromStyle(spellContext);
            chargerEntity.setResolveSound(resolveSound);
            serverLevel.addFreshEntity(chargerEntity);
            convergence.updateTemporalContext(shooter, chargerEntity, spellContext);
            convergence.triggerResolveEffects(spellContext, serverLevel, pos);
        }
    }

    public static void handleBlockCharger(ServerLevel serverLevel, Vec3 pos, BlockHitResult blockHitResult,
            @Nullable LivingEntity shooter, SpellContext spellContext, EffectConvergence convergence) {
        BlockPos blockPos = blockHitResult.getBlockPos();
        if (serverLevel.getBlockEntity(blockPos) instanceof SourceJarTile && shooter != null) {
            SourceJarChargerEntity chargerEntity = new SourceJarChargerEntity(ModEntities.SOURCE_JAR_CHARGER.get(),
                    serverLevel);
            chargerEntity.setPos(pos.x, pos.y, pos.z);
            chargerEntity.setJarPos(blockPos);
            chargerEntity.setCasterUUID(shooter.getUUID());
            chargerEntity.setLifespan(DEFAULT_LIFESPAN);
            SoundEvent resolveSound = convergence.getResolveSoundFromStyle(spellContext);
            chargerEntity.setResolveSound(resolveSound);
            serverLevel.addFreshEntity(chargerEntity);
            convergence.updateTemporalContext(shooter, chargerEntity, spellContext);
            convergence.triggerResolveEffects(spellContext, serverLevel, pos);
        }
    }
}

