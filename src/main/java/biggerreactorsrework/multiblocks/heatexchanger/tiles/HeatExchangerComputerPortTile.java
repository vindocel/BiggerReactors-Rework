package biggerreactorsrework.multiblocks.heatexchanger.tiles;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import biggerreactorsrework.core.registry.RegisterTile;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class HeatExchangerComputerPortTile extends HeatExchangerBaseTile implements biggerreactorsrework.compat.ComputerPortState.Port {

    @RegisterTile("heat_exchanger_computer_port")
    public static final BlockEntityType.BlockEntitySupplier<HeatExchangerComputerPortTile> SUPPLIER = new RegisterTile.Producer<>(HeatExchangerComputerPortTile::new);

    public HeatExchangerComputerPortTile(BlockEntityType<?> TYPE, BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }

    // Modified 2026-10-30: keep block identity while isolating the optional component API.
    private final biggerreactorsrework.compat.ComputerPortState computerState = new biggerreactorsrework.compat.ComputerPortState(this);
    @Override public biggerreactorsrework.compat.ComputerPortState computerPortState() { return computerState; }
    @Override public void onAdded() { super.onAdded(); computerState.onLoad(); }
    @Override public void onRemoved(boolean chunkUnload) { computerState.remove(); super.onRemoved(chunkUnload); }
    @Override protected void readNBT(net.minecraft.nbt.CompoundTag tag) {
        super.readNBT(tag); computerState.load(tag, registries());
    }
    @Override protected net.minecraft.nbt.CompoundTag writeNBT() {
        var tag = super.writeNBT(); computerState.save(tag, registries()); return tag;
    }
    @Override protected net.minecraft.nbt.CompoundTag getDataNBT() { return new net.minecraft.nbt.CompoundTag(); }
}
