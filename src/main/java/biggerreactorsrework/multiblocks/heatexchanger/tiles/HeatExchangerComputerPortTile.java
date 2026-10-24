package biggerreactorsrework.multiblocks.heatexchanger.tiles;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import biggerreactorsrework.core.registry.RegisterTile;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * The CC:Tweaked peripheral is attached by {@code ComputerCraftCompat} only when that mod is installed;
 * this class never references its API.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class HeatExchangerComputerPortTile extends HeatExchangerBaseTile {

    @RegisterTile("heat_exchanger_computer_port")
    public static final BlockEntityType.BlockEntitySupplier<HeatExchangerComputerPortTile> SUPPLIER = new RegisterTile.Producer<>(HeatExchangerComputerPortTile::new);

    public HeatExchangerComputerPortTile(BlockEntityType<?> TYPE, BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }

    /**
     * Peripheral instance owned by the optional integration, one per port so the computer sees a stable object.
     */
    @Nullable
    public Object peripheral;
}
