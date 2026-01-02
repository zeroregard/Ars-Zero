package com.github.ars_zero.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

public class SourceJarChargeParticle extends TextureSheetParticle {
    
    private static final float SOURCE_RED = 1.0f;
    private static final float SOURCE_GREEN = 50.0f / 255.0f;
    private static final float SOURCE_BLUE = 1.0f;
    
    protected SourceJarChargeParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, SpriteSet spriteSet) {
        super(level, x, y, z, 0, 0, 0);
        this.hasPhysics = false;
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.gravity = 0.0f;
        this.lifetime = 30;
        this.quadSize = 0.12f;
        this.rCol = SOURCE_RED;
        this.gCol = SOURCE_GREEN;
        this.bCol = SOURCE_BLUE;
        this.alpha = 0.9f;
        this.pickSprite(spriteSet);
    }
    
    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }
    
    @Override
    public int getLightColor(float pTicks) {
        return 240;
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
            float ageRatio = (float) this.age / (float) this.lifetime;
            this.alpha = Math.max(0.0f, 0.9f * (1.0f - ageRatio * ageRatio));
            this.quadSize = 0.12f * (1.0f + ageRatio * 0.3f);
        }
    }
    
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;
        
        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }
        
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new SourceJarChargeParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet);
        }
    }
}
