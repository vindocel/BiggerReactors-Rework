package biggerreactorsrework.multiblocks.battery.tiles;

import biggerreactorsrework.core.registry.RegisterTile;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Holds this cell's share of the battery's energy.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BatteryCellTile extends BatteryBaseTile {
    
    /**
     * Also the key the loot table copies into the item's custom data.
     */
    public static final String ENERGY_KEY = "energy";
    
    @RegisterTile("battery_cell")
    public static final BlockEntityType.BlockEntitySupplier<BatteryCellTile> SUPPLIER = new RegisterTile.Producer<>(BatteryCellTile::new);
    
    private long energy = 0;
    
    public BatteryCellTile(BlockEntityType<?> TYPE, BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }
    
    public long energy() {
        return energy;
    }
    
    public void setEnergy(long energy) {
        if (this.energy != energy) {
            this.energy = energy;
            setChanged();
        }
    }
    
    public void loadEnergyFromItem(long energy) {
        energy = Math.max(0, energy);
        if (nullableController() != null) {
            controller().addCellEnergy(this, energy);
        } else {
            setEnergy(energy);
        }
    }
    
    @Override
    protected void readNBT(CompoundTag compound) {
        super.readNBT(compound);
        energy = Math.max(0, compound.getLong(ENERGY_KEY));
    }
    
    @Override
    protected CompoundTag writeNBT() {
        // Shares are only split on demand; saves and drops are when they are needed.
        if (nullableController() != null) {
            controller().syncCells();
        }
        CompoundTag nbt = super.writeNBT();
        if (energy > 0) {
            nbt.putLong(ENERGY_KEY, energy);
        }
        return nbt;
    }
}
