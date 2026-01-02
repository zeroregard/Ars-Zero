package com.github.ars_zero.client.particle;

import com.github.ars_zero.common.particle.ExplosiveChargeParticleOptions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;

public class ExplosiveChargeParticle extends TextureSheetParticle {
    
    protected ExplosiveChargeParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, float r, float g, float b, SpriteSet spriteSet) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.gravity = 0.0f;
        this.lifetime = 200;
        this.quadSize = 0.2f;
        this.rCol = r;
        this.gCol = g;
        this.bCol = b;
        this.alpha = 0.8f;
        this.pickSprite(spriteSet);
    }
    
    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }
    
    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
        } else {
            this.move(this.xd, this.yd, this.zd);
            this.xd *= 0.98D;
            this.yd *= 0.98D;
            this.zd *= 0.98D;
            float ageRatio = (float) this.age / (float) this.lifetime;
            this.alpha = Math.max(0.0f, 0.8f * (1.0f - ageRatio));
        }
    }
    
    public static class Provider implements ParticleProvider<ExplosiveChargeParticleOptions> {
        private final SpriteSet spriteSet;
        
        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }
        
        @Override
        public Particle createParticle(ExplosiveChargeParticleOptions type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new ExplosiveChargeParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, type.r, type.g, type.b, this.spriteSet);
        }
    }
}

