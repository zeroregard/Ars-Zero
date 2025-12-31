package com.github.ars_zero.client.particle;

import com.github.ars_zero.registry.ModParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

public class ExplosionBurstParticle extends TextureSheetParticle {
    
    private final float initialSize;
    private int trailCounter = 0;
    
    protected ExplosionBurstParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, SpriteSet spriteSet) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.gravity = 0.012f;
        this.hasPhysics = false;
        this.lifetime = 30 + level.random.nextInt(20);
        this.initialSize = (0.3f + level.random.nextFloat() * 0.2f) + 1f;
        this.quadSize = this.initialSize;
        
        this.rCol = 1.0f;
        this.gCol = 0.6f;
        this.bCol = 0.2f;
        
        this.alpha = 1.0f;
        this.pickSprite(spriteSet);
    }
    
    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
    
    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        
        this.move(this.xd, this.yd, this.zd);
        
        if (this.level != null) {
            for (int i = 0; i < 2; i++) {
                this.level.addParticle(
                    net.minecraft.core.particles.ParticleTypes.SMOKE,
                    this.xo, this.yo, this.zo,
                    (this.level.random.nextDouble() - 0.5) * 0.01,
                    (this.level.random.nextDouble() - 0.5) * 0.01,
                    (this.level.random.nextDouble() - 0.5) * 0.01
                );
            }
        }
        
        float t = (float) this.age / (float) this.lifetime;
        this.yd -= this.gravity * (0.4f + t);
        
        this.xd *= 0.96D;
        this.yd *= 0.96D;
        this.zd *= 0.96D;
        
        this.alpha = Math.max(0.0f, 1.0f - t);
        this.quadSize = this.initialSize * (1.0f - t * t);
        
        this.rCol = 1.0f;
        this.gCol = 0.6f - t * 0.4f;
        this.bCol = 0.2f - t * 0.2f;
    }
    
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;
        
        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }
        
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            double vx, vy, vz;
            
            if (xSpeed != 0.0 || ySpeed != 0.0 || zSpeed != 0.0) {
                vx = xSpeed;
                vy = ySpeed;
                vz = zSpeed;
            } else {
                double u = level.random.nextDouble();
                double v = level.random.nextDouble();
                
                double theta = 2.0 * Math.PI * u;
                double phi = Math.acos(2.0 * v - 1.0);
                
                double speed = (0.25 + level.random.nextDouble() * 0.45) * 3.0;
                
                vx = Math.sin(phi) * Math.cos(theta) * speed;
                vy = Math.cos(phi) * speed * 0.8;
                vz = Math.sin(phi) * Math.sin(theta) * speed;
                
                vx += level.random.nextGaussian() * 0.02;
                vy += level.random.nextGaussian() * 0.02;
                vz += level.random.nextGaussian() * 0.02;
            }
            
            return new ExplosionBurstParticle(level, x, y, z, vx, vy, vz, this.spriteSet);
        }
    }
}

