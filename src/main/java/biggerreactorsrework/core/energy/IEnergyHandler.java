package biggerreactorsrework.core.energy;

import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.capabilities.BlockCapability;
import biggerreactorsrework.core.CoreEvents;
import biggerreactorsrework.core.transfer.TransferSnapshots;
import biggerreactorsrework.core.util.API;

@API
public interface IEnergyHandler extends TransferSnapshots {

    @Override
    default Object transferSnapshot() {
        return null;
    }

    @Override
    default void restoreTransferSnapshot(Object snapshot) {
    }

    
    BlockCapability<IEnergyHandler, Direction> CAPABILITY = BlockCapability.createSided(Identifier.fromNamespaceAndPath(biggerreactorsrework.BiggerReactors.modid, "energy"), IEnergyHandler.class);
    
    long insertEnergy(long maxInsert, boolean simulate);
    
    long extractEnergy(long maxExtract, boolean simulate);
    
    long energyStored();
    
    long maxEnergyStored();
}
