package com.github.ars_zero.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

public class BlightSplashParticle extends TextureSheetParticle {
    
    protected BlightSplashParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, SpriteSet spriteSet) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.pickSprite(spriteSet);
        this.gravity = 0.04f;
        this.lifetime = (int)(5.0D / (Math.random() * 0.8D + 0.2D));
        this.quadSize = 0.2f + (float)(Math.random() * 0.1);
        this.rCol = 0.1f;
        this.gCol = 1.0f;
        this.bCol = 0.1f;
        this.alpha = 1.0f;
    }
    
    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_LIT;
    }
    
    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
        } else {
            this.yd -= this.gravity;
            this.move(this.xd, this.yd, this.zd);
            this.xd *= 0.98D;
            this.yd *= 0.98D;
            this.zd *= 0.98D;
            this.rCol = 0.1f;
            this.gCol = 0.75f;
            this.bCol = 0.1f;
        }
    }
    
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;
        
        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }
        
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new BlightSplashParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet);
        }
    }
}
