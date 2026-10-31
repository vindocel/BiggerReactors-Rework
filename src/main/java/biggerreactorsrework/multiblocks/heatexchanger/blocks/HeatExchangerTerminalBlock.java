package biggerreactorsrework.multiblocks.heatexchanger.blocks;


import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import biggerreactorsrework.multiblocks.heatexchanger.tiles.HeatExchangerTerminalTile;
import biggerreactorsrework.core.multiblock.IAssemblyStateBlock;
import biggerreactorsrework.core.multiblock.rectangular.IFaceDirectionBlock;
import biggerreactorsrework.core.registry.RegisterBlock;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class HeatExchangerTerminalBlock extends HeatExchangerBaseBlock implements IAssemblyStateBlock, IFaceDirectionBlock {
    
    @RegisterBlock(name = "heat_exchanger_terminal", tileEntityClass = HeatExchangerTerminalTile.class)
    public static final HeatExchangerTerminalBlock INSTANCE = biggerreactorsrework.core.registry.RegistryIds.named("heat_exchanger_terminal", () -> new HeatExchangerTerminalBlock());
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return HeatExchangerTerminalTile.SUPPLIER.create(pos, state);
    }
}
