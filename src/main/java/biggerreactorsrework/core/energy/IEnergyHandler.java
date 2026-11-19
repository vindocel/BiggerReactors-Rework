package biggerreactorsrework.core.energy;

//? if =26.1.2 || =26.3 {
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.capabilities.BlockCapability;
import biggerreactorsrework.core.CoreEvents;
import biggerreactorsrework.core.transfer.TransferSnapshots;
import biggerreactorsrework.core.util.API;
//?} else if =1.21.1 {
/*import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;
import biggerreactorsrework.core.CoreEvents;
import biggerreactorsrework.core.util.API;
*/
//?}

//? if =26.1.2 || =26.3 {
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
//?} else if =1.21.1 {
/*@API
public interface IEnergyHandler {

    BlockCapability<IEnergyHandler, Direction> CAPABILITY = BlockCapability.createSided(ResourceLocation.fromNamespaceAndPath(biggerreactorsrework.BiggerReactors.modid, "energy"), IEnergyHandler.class);

    long insertEnergy(long maxInsert, boolean simulate);
*/
//?}

    long extractEnergy(long maxExtract, boolean simulate);

    long energyStored();

    long maxEnergyStored();
}
