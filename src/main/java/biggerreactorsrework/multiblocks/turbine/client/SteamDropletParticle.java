package biggerreactorsrework.multiblocks.turbine.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/** Added 2026-10-31: hangs briefly under its surface, then falls until it lands. */
final class SteamDropletParticle extends TextureSheetParticle {
    private static final int HANG_TICKS = 25;

    private SteamDropletParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        pickSprite(sprites);
        setSize(0.01F, 0.01F);
        setColor(0.35F, 0.5F, 0.95F);
        gravity = 0;
        lifetime = 200;
    }

    @Override
    public void tick() {
        // Particle.tick scales gravity by 0.04: this gives the 0.06 blocks/tick² of a vanilla falling drip.
        if (age == HANG_TICKS) gravity = 1.5F;
        super.tick();
        if (onGround) remove();
    }

    @Override
    public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_OPAQUE; }

    static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        Provider(SpriteSet sprites) { this.sprites = sprites; }
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
            return new SteamDropletParticle(level, x, y, z, sprites);
        }
    }
}
