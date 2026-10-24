package biggerreactorsrework.multiblocks.reactor.tiles;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import biggerreactorsrework.core.multiblock.common.IEventMultiblock;
import biggerreactorsrework.core.registry.RegisterTile;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * The CC:Tweaked peripheral is attached by {@code ComputerCraftCompat} only when that mod is installed;
 * this class never references its API.
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ReactorComputerPortTile extends ReactorBaseTile implements IEventMultiblock.AssemblyStateTransition.OnAssembly {

    @RegisterTile("reactor_computer_port")
    public static final BlockEntityType.BlockEntitySupplier<ReactorComputerPortTile> SUPPLIER = new RegisterTile.Producer<>(ReactorComputerPortTile::new);

    public ReactorComputerPortTile(BlockEntityType<?> TYPE, BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }

    /**
     * Peripheral instance owned by the optional integration, one per port so the computer sees a stable object.
     */
    @Nullable
    public Object peripheral;
    /**
     * Set by the integration together with {@link #peripheral}: refreshes its control rod list after assembly.
     */
    @Nullable
    public Runnable onAssemblyHook;

    @Override
    public void onAssembly() {
        if (onAssemblyHook != null) {
            onAssemblyHook.run();
        }
    }
}
