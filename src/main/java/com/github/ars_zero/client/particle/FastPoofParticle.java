package com.github.ars_zero.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

public class FastPoofParticle extends TextureSheetParticle {

  private final float initialSize;
  private final SpriteSet spriteSet;

  protected FastPoofParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed,
      double zSpeed, SpriteSet spriteSet) {
    super(level, x, y, z, xSpeed, ySpeed, zSpeed);
    this.spriteSet = spriteSet;
    this.gravity = 0.0f;
    this.lifetime = 2 + (int) (Math.random() * 2);
    this.initialSize = 0.1f + (float) (Math.random() * 0.1f);
    this.quadSize = this.initialSize;
    // White color like POOF
    this.rCol = 1.0f;
    this.gCol = 1.0f;
    this.bCol = 1.0f;
    this.alpha = 1.0f;
    this.setSpriteFromAge(spriteSet);
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
      this.setSpriteFromAge(this.spriteSet);
      this.move(this.xd, this.yd, this.zd);
      this.xd *= 0.98D;
      this.yd *= 0.98D;
      this.zd *= 0.98D;
      // Fade out 2x faster - ageRatio progresses 2x faster
      float ageRatio = (float) this.age / (float) this.lifetime;
      this.alpha = Math.max(0.0f, 1.0f * (1.0f - ageRatio));
      // Scale up slightly as it fades
      this.quadSize = this.initialSize * (1.0f + ageRatio * 0.5f);
    }
  }

  public static class Provider implements ParticleProvider<SimpleParticleType> {
    private final SpriteSet spriteSet;

    public Provider(SpriteSet spriteSet) {
      this.spriteSet = spriteSet;
    }

    @Override
    public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
        double xSpeed, double ySpeed, double zSpeed) {
      return new FastPoofParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet);
    }
  }
}
