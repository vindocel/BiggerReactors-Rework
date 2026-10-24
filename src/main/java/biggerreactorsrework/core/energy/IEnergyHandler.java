package biggerreactorsrework.core.energy;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;
import biggerreactorsrework.core.CoreEvents;
import biggerreactorsrework.core.util.API;

@API
public interface IEnergyHandler {
    
    BlockCapability<IEnergyHandler, Direction> CAPABILITY = BlockCapability.createSided(ResourceLocation.fromNamespaceAndPath(biggerreactorsrework.BiggerReactors.modid, "energy"), IEnergyHandler.class);
    
    long insertEnergy(long maxInsert, boolean simulate);
    
    long extractEnergy(long maxExtract, boolean simulate);
    
    long energyStored();
    
    long maxEnergyStored();
}
