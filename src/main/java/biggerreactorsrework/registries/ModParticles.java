package biggerreactorsrework.registries;

import biggerreactorsrework.BiggerReactors;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Added 2026-10-31: condensation droplet, with a color shader packs do not treat as faint water. */
public final class ModParticles {
    private static final DeferredRegister<ParticleType<?>> TYPES = DeferredRegister.create(Registries.PARTICLE_TYPE, BiggerReactors.modid);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> STEAM_DROPLET =
            TYPES.register("steam_droplet", () -> new SimpleParticleType(false));

    public static void register(IEventBus bus) { TYPES.register(bus); }
    private ModParticles() {}
}
