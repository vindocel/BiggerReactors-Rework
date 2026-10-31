package biggerreactorsrework.multiblocks.heatexchanger.blocks;


import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import biggerreactorsrework.multiblocks.heatexchanger.tiles.HeatExchangerCasingTile;
import biggerreactorsrework.core.multiblock.IAssemblyStateBlock;
import biggerreactorsrework.core.multiblock.rectangular.IAxisPositionBlock;
import biggerreactorsrework.core.registry.RegisterBlock;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class HeatExchangerCasingBlock extends HeatExchangerBaseBlock implements IAssemblyStateBlock, IAxisPositionBlock {
    
    @RegisterBlock(name = "heat_exchanger_casing", tileEntityClass = HeatExchangerCasingTile.class)
    public static final HeatExchangerCasingBlock INSTANCE = biggerreactorsrework.core.registry.RegistryIds.named("heat_exchanger_casing", () -> new HeatExchangerCasingBlock());
    
    @Override
    public boolean isGoodForFrame() {
        return true;
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return HeatExchangerCasingTile.SUPPLIER.create(pos, state);
    }
}
