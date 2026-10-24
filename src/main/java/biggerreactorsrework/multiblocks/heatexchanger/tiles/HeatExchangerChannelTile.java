package biggerreactorsrework.multiblocks.heatexchanger.tiles;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import biggerreactorsrework.multiblocks.heatexchanger.blocks.HeatExchangerCondenserChannelBlock;
import biggerreactorsrework.core.registry.RegisterTile;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * The channel model is chosen from the block state and baked into the chunk mesh,
 * see {@code HeatExchangerChannelModels}.
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class HeatExchangerChannelTile extends HeatExchangerBaseTile {

    public final boolean CONDENSER;

    public long lastCheckedTick;

    @RegisterTile("heat_exchanger_channel")
    public static final BlockEntityType.BlockEntitySupplier<HeatExchangerChannelTile> SUPPLIER = new RegisterTile.Producer<>(HeatExchangerChannelTile::new);

    public HeatExchangerChannelTile(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state) {
        super(tileEntityTypeIn, pos, state);
        CONDENSER = state.getBlock() == HeatExchangerCondenserChannelBlock.INSTANCE;
    }
}
