package com.github.ars_zero.common.entity.explosion;

import com.github.ars_zero.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class ExplosionParticleHelper {
    
    public static void spawnSpiralParticles(ServerLevel level, Vec3 center, float charge, int tickCount) {
        double spiralRadius = 3.0 + charge * 4.0;
        double time = tickCount * 0.1;
        
        int particleCount = 2;
        for (int i = 0; i < particleCount; i++) {
            double angle = (time * 2.0 * Math.PI) + (i * 2.0 * Math.PI / particleCount);
            double heightOffset = Math.sin(time * 0.5 + i * 0.3) * 0.5;
            
            double x = center.x + Math.cos(angle) * spiralRadius;
            double y = center.y + heightOffset;
            double z = center.z + Math.sin(angle) * spiralRadius;
            
            double speedX = -Math.sin(angle) * 0.08;
            double speedY = Math.cos(time * 0.5 + i * 0.3) * 0.01;
            double speedZ = Math.cos(angle) * 0.08;

            level.sendParticles(ModParticles.EXPLOSIVE_CHARGE.get(), x, y, z, 1, speedX, speedY, speedZ, 0.0);
        }
    }
    
    public static void spawnSmokeResidue(ServerLevel level, BlockPos pos) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        
        double speedX = (level.getRandom().nextDouble() - 0.5) * 0.02;
        double speedY = level.getRandom().nextDouble() * 0.05;
        double speedZ = (level.getRandom().nextDouble() - 0.5) * 0.02;
        
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 1, speedX, speedY, speedZ, 0.0);
    }
    
    public static void spawnExplosionBurst(ServerLevel level, Vec3 center, double radius) {
    }
    
    @OnlyIn(Dist.CLIENT)
    public static void spawnExplosionBurstClient(ClientLevel level, Vec3 center, double radius) {
        double spawnRadius = radius * 0.5f;
        
        for (int i = 0; i < radius * 4; i++) {
            double u = level.random.nextDouble();
            double v = level.random.nextDouble();
            
            double theta = 2.0 * Math.PI * u;
            double phi = Math.acos(2.0 * v - 1.0);
            
            double offsetX = Math.sin(phi) * Math.cos(theta) * spawnRadius;
            double offsetY = Math.cos(phi) * spawnRadius;
            double offsetZ = Math.sin(phi) * Math.sin(theta) * spawnRadius;
            
            double distance = Math.sqrt(offsetX * offsetX + offsetY * offsetY + offsetZ * offsetZ);
            double dirX = offsetX / distance;
            double dirY = offsetY / distance;
            double dirZ = offsetZ / distance;
            
            double speed = (0.25 + level.random.nextDouble() * 0.45) * 0.25f * radius;
            
            double vx = dirX * speed;
            double vy = dirY * speed;
            double vz = dirZ * speed;
            
            vx += level.random.nextGaussian() * 0.02;
            vy += level.random.nextGaussian() * 0.02;
            vz += level.random.nextGaussian() * 0.02;
            
            level.addParticle(
                ModParticles.EXPLOSION_BURST.get(),
                center.x + offsetX, center.y + offsetY, center.z + offsetZ,
                vx, vy, vz
            );
        }
    }
}

