package biggerreactorsrework.multiblocks.turbine.tiles;

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
public class TurbineComputerPortTile extends TurbineBaseTile {

    @RegisterTile("turbine_computer_port")
    public static final BlockEntityType.BlockEntitySupplier<TurbineComputerPortTile> SUPPLIER = new RegisterTile.Producer<>(TurbineComputerPortTile::new);

    public TurbineComputerPortTile(BlockEntityType<?> TYPE, BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }

    /**
     * Peripheral instance owned by the optional integration, one per port so the computer sees a stable object.
     */
    @Nullable
    public Object peripheral;
}
