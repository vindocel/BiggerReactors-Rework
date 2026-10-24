package biggerreactorsrework.multiblocks.heatexchanger.tiles;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import biggerreactorsrework.multiblocks.heatexchanger.HeatExchangerMultiblockController;
import biggerreactorsrework.multiblocks.heatexchanger.blocks.HeatExchangerBaseBlock;
import biggerreactorsrework.core.modular.tile.ModularTile;
import biggerreactorsrework.core.multiblock.common.IPersistentMultiblockTile;
import biggerreactorsrework.core.multiblock.rectangular.IRectangularMultiblockTile;
import biggerreactorsrework.core.multiblock.touching.ITouchingMultiblockTile;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class HeatExchangerBaseTile extends ModularTile implements IRectangularMultiblockTile<HeatExchangerBaseTile, HeatExchangerBaseBlock, HeatExchangerMultiblockController>,
        IPersistentMultiblockTile<HeatExchangerBaseTile, HeatExchangerBaseBlock, HeatExchangerMultiblockController>,
        ITouchingMultiblockTile<HeatExchangerBaseTile, HeatExchangerBaseBlock, HeatExchangerMultiblockController> {
    public HeatExchangerBaseTile(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state) {
        super(tileEntityTypeIn, pos, state);
    }
    
    @Nonnull
    @Override
    public HeatExchangerMultiblockController createController() {
        if (level == null) {
            throw new IllegalStateException("Attempt to create controller with null world");
        }
        return new HeatExchangerMultiblockController(level);
    }

    public void runRequest(String requestName, Object requestData) {
        if (nullableController() != null) {
            controller().runRequest(requestName, requestData);
        }
    }
}
