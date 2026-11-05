package biggerreactorsrework.fluids;

import biggerreactorsrework.core.registry.ModFluid;
import biggerreactorsrework.core.registry.RegisterFluid;

import javax.annotation.Nonnull;

// Added 2026-11-03: advanced coolant made from Liquid Obsidian and blue ice.
@RegisterFluid(name = "frozen_obsidian", registerBucket = true, light = biggerreactorsrework.multiblocks.MachineLighting.GLOWING_COOLANT, freezes = true, waterLikeMovement = true)
public class FrozenObsidian extends ModFluid {
    
    @RegisterFluid.Instance
    public static FrozenObsidian INSTANCE;
    
    public FrozenObsidian(@Nonnull Properties properties) {
        super(properties);
    }
}
