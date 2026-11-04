package biggerreactorsrework.client.mixin;

import biggerreactorsrework.fluids.FrozenObsidian;
import biggerreactorsrework.fluids.LiquidObsidian;
import biggerreactorsrework.registries.ModParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleEngine.class)
abstract class CoolantParticleMixin {
    @Shadow protected ClientLevel level;

    @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
    private void rework$tintCoolantParticles(ParticleOptions options, double x, double y, double z,
                                           double vx, double vy, double vz, CallbackInfoReturnable<Particle> callback) {
        boolean surface = options == ParticleTypes.SPLASH || options == ParticleTypes.BUBBLE_POP;
        if ((!surface && options != ParticleTypes.BUBBLE) || level == null) return;

        var pos = BlockPos.containing(x, y, z);
        var fluid = level.getFluidState(pos);
        // Vanilla spawns both kinds of entry particles one block above the entity's feet.
        if (fluid.isEmpty()) fluid = level.getFluidState(pos.below());

        var type = fluid.getFluidType();
        boolean frozen = type == FrozenObsidian.INSTANCE.getFluidType();
        if (!frozen && type != LiquidObsidian.INSTANCE.getFluidType()) return;
        var replacement = options == ParticleTypes.BUBBLE ? ModParticles.COOLANT_BUBBLE.get()
                : options == ParticleTypes.BUBBLE_POP ? ModParticles.COOLANT_BUBBLE_POP.get() : ModParticles.COOLANT_SPLASH.get();
        var engine = (ParticleEngine) (Object) this;
        if (engine.createParticle(replacement, x, y, z, vx, vy, vz) instanceof SingleQuadParticle particle) {
            if (frozen) particle.setColor(0.04F, 0.94F, 0.85F);
            else particle.setColor(0.48F, 0.29F, 0.65F);
            callback.setReturnValue(particle);
        }
    }
}
