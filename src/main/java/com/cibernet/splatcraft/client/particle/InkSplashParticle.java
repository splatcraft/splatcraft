package com.cibernet.splatcraft.client.particle;

import com.cibernet.splatcraft.particle.InkSplashParticleEffect;
import com.cibernet.splatcraft.particle.InkedParticleEffect;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.RainSplashParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public class InkSplashParticle extends RainSplashParticle {
    private final SpriteProvider spriteProvider;
    protected final float baseScale;

    private InkSplashParticle(ClientWorld world, double x, double y, double z, double velocityX, double velocityY, double velocityZ, InkedParticleEffect particle, SpriteProvider spriteProvider) {
        super(world, x, y, z);
        this.gravityStrength = 0.04f;

        if (velocityY == 0.0d && (velocityX != 0.0d || velocityZ != 0.0d)) {
            this.velocityX = velocityX;
            this.velocityY = 0.4d;
            this.velocityZ = velocityZ;
        }

        float rand = (float)Math.random() * 0.4f + 0.6f;
        this.colorRed = ((float)(Math.random() * 0.20000000298023224d) + 0.8f) * particle.getRed() * rand;
        this.colorGreen = ((float)(Math.random() * 0.20000000298023224d) + 0.8f) * particle.getGreen() * rand;
        this.colorBlue = ((float)(Math.random() * 0.20000000298023224d) + 0.8f) * particle.getBlue() * rand;
        this.baseScale = particle.getScale() * (0.33f * (this.random.nextFloat() * 0.5f + 0.5f) * 2.0f);

        this.maxAge = 20 + this.random.nextInt(4);
        this.spriteProvider = spriteProvider;
        this.setSpriteForAge(spriteProvider);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.age++ >= this.maxAge) {
            this.markDead();
        } else {
            this.setSpriteForAge(this.spriteProvider);
        }
    }

    @Override
    public float getSize(float tickDelta) {
        return this.isInvisible() ? 0.0f : this.baseScale;
    }
    public boolean isInvisible() {
        return MinecraftClient.getInstance().gameRenderer.getCamera().getPos().squaredDistanceTo(new Vec3d(this.x, this.y, this.z)) < 0.7d;
    }

    @Environment(EnvType.CLIENT)
    public static class Factory implements ParticleFactory<InkSplashParticleEffect> {
        private final SpriteProvider spriteProvider;

        public Factory(SpriteProvider spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        @Override
        public Particle createParticle(InkSplashParticleEffect particleEffect, ClientWorld clientWorld, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
            return new InkSplashParticle(clientWorld, x, y, z, velocityX, velocityY, velocityZ, particleEffect, this.spriteProvider);
        }
    }
}
