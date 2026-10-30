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
    // Modified 2026-10-30: terminal and fluid-port screens use one synchronized appearance identity.
    public BlockPos appearancePos;
    public HeatExchangerBaseTile(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state) {
        super(tileEntityTypeIn, pos, state);
        appearancePos = pos.immutable();
    }

    public long writeAppearance() {
        appearancePos = nullableController() == null ? getBlockPos() : controller().appearancePos(getBlockPos());
        return appearancePos.asLong();
    }
    public void readAppearance(java.util.Map<?, ?> data) {
        if (data.get("appearancePos") instanceof Long position) appearancePos = BlockPos.of(position);
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
