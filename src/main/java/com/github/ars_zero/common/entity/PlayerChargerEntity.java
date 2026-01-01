package com.github.ars_zero.common.entity;

import com.hollingsworth.arsnouveau.client.particle.GlowParticleData;
import com.hollingsworth.arsnouveau.client.particle.ParticleColor;
import com.hollingsworth.arsnouveau.api.mana.IManaCap;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class PlayerChargerEntity extends AbstractChargerEntity {
    private static final String TAG_TARGET_PLAYER_UUID = "target_player_uuid";
    private static final String TAG_INITIAL_MANA_ADDED = "initial_mana_added";
    
    private static final int INITIAL_MANA_AMOUNT = 200;
    
    private UUID targetPlayerUUID;
    private boolean initialManaAdded = false;

    public PlayerChargerEntity(net.minecraft.world.entity.EntityType<? extends PlayerChargerEntity> entityType, Level level) {
        super(entityType, level);
    }

    public void setTargetPlayerUUID(UUID uuid) {
        this.targetPlayerUUID = uuid;
    }

    @Override
    public void tick() {
        if (!this.level().isClientSide && targetPlayerUUID != null && casterUUID != null && targetPlayerUUID.equals(casterUUID)) {
            ServerLevel serverLevel = (ServerLevel) this.level();
            
            if (!initialManaAdded) {
                if (serverLevel.getEntity(targetPlayerUUID) instanceof Player targetPlayer) {
                    IManaCap targetManaCap = CapabilityRegistry.getMana(targetPlayer);
                    if (targetManaCap != null) {
                        double currentMana = targetManaCap.getCurrentMana();
                        double maxMana = targetManaCap.getMaxMana();
                        double manaToAdd = Math.min(INITIAL_MANA_AMOUNT, maxMana - currentMana);
                        if (manaToAdd > 0) {
                            targetManaCap.addMana(manaToAdd);
                            initialManaAdded = true;
                        }
                    }
                }
            }
        }
        
        super.tick();
    }

    @Override
    protected boolean canTransferMana() {
        if (targetPlayerUUID == null || this.level().isClientSide) {
            return false;
        }
        if (this.level() instanceof ServerLevel serverLevel) {
            if (serverLevel.getEntity(targetPlayerUUID) instanceof Player targetPlayer) {
                IManaCap targetManaCap = CapabilityRegistry.getMana(targetPlayer);
                return targetManaCap != null && targetManaCap.getCurrentMana() < targetManaCap.getMaxMana();
            }
        }
        return false;
    }

    @Override
    protected boolean transferMana(ServerLevel level, int manaAmount) {
        if (targetPlayerUUID == null) {
            return false;
        }
        if (level.getEntity(targetPlayerUUID) instanceof Player targetPlayer) {
            IManaCap targetManaCap = CapabilityRegistry.getMana(targetPlayer);
            if (targetManaCap != null) {
                double currentMana = targetManaCap.getCurrentMana();
                double maxMana = targetManaCap.getMaxMana();
                double manaToAdd = Math.min(manaAmount, maxMana - currentMana);
                if (manaToAdd > 0) {
                    targetManaCap.addMana(manaToAdd);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected Vec3 getParticlePosition() {
        if (targetPlayerUUID != null && this.level() instanceof ServerLevel serverLevel) {
            if (serverLevel.getEntity(targetPlayerUUID) instanceof Player targetPlayer) {
                return targetPlayer.position().add(0, targetPlayer.getEyeHeight(), 0);
            }
        }
        return this.position();
    }

    @Override
    protected void spawnChargeParticles(ServerLevel level, Vec3 position, int tickCount, double manaRegen) {
        double centerX = position.x;
        double centerY = position.y;
        double centerZ = position.z;
        
        double orbitRadius = 0.6;
        double baseRotationSpeed = 0.15;
        double regenMultiplier = Math.max(0.5, Math.min(2.0, manaRegen / 10.0));
        double rotationSpeed = baseRotationSpeed * regenMultiplier;
        
        ParticleColor purpleColor = ParticleColor.PURPLE;
        
        int particleCount = 4;
        for (int i = 0; i < particleCount; i++) {
            double particleOffset = i * 2.0 * Math.PI / particleCount;
            double baseAngle = (tickCount * rotationSpeed) + particleOffset;
            
            double heightOffset = Math.sin(tickCount * 0.1 + i * 0.7) * 0.15;
            
            double x = centerX + Math.cos(baseAngle) * orbitRadius;
            double y = centerY + heightOffset;
            double z = centerZ + Math.sin(baseAngle) * orbitRadius;
            
            double baseOrbitSpeed = 0.1;
            double orbitSpeed = baseOrbitSpeed * regenMultiplier;
            double speedX = -Math.sin(baseAngle) * orbitSpeed;
            double speedY = Math.cos(tickCount * 0.1 + i * 0.7) * 0.02;
            double speedZ = Math.cos(baseAngle) * orbitSpeed;
            
            double toCenterX = centerX - x;
            double toCenterY = centerY - y;
            double toCenterZ = centerZ - z;
            double distToCenter = Math.sqrt(toCenterX * toCenterX + toCenterZ * toCenterZ);
            
            if (distToCenter > 0.05) {
                double inwardPull = 0.03;
                speedX += (toCenterX / distToCenter) * inwardPull;
                speedZ += (toCenterZ / distToCenter) * inwardPull;
            }
            
            level.sendParticles(GlowParticleData.createData(purpleColor), x, y, z, 1, speedX, speedY, speedZ, 0.0);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains(TAG_TARGET_PLAYER_UUID)) {
            this.targetPlayerUUID = compound.getUUID(TAG_TARGET_PLAYER_UUID);
        }
        if (compound.contains(TAG_INITIAL_MANA_ADDED)) {
            this.initialManaAdded = compound.getBoolean(TAG_INITIAL_MANA_ADDED);
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        if (targetPlayerUUID != null) {
            compound.putUUID(TAG_TARGET_PLAYER_UUID, targetPlayerUUID);
        }
        compound.putBoolean(TAG_INITIAL_MANA_ADDED, initialManaAdded);
    }
}

