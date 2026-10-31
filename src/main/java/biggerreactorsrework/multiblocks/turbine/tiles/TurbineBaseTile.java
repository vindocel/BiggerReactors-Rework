package biggerreactorsrework.multiblocks.turbine.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import biggerreactorsrework.multiblocks.turbine.TurbineMultiblockController;
import biggerreactorsrework.multiblocks.turbine.blocks.TurbineBaseBlock;
import biggerreactorsrework.core.modular.tile.ModularTile;
import biggerreactorsrework.core.multiblock.IMultiblockTile;
import biggerreactorsrework.core.multiblock.common.IPersistentMultiblockTile;
import biggerreactorsrework.core.multiblock.rectangular.IRectangularMultiblockTile;
import biggerreactorsrework.core.multiblock.touching.ITouchingMultiblockTile;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class TurbineBaseTile extends ModularTile implements IMultiblockTile<TurbineBaseTile, TurbineBaseBlock, TurbineMultiblockController>,
        IRectangularMultiblockTile<TurbineBaseTile, TurbineBaseBlock, TurbineMultiblockController>,
        IPersistentMultiblockTile<TurbineBaseTile, TurbineBaseBlock, TurbineMultiblockController>,
        ITouchingMultiblockTile<TurbineBaseTile, TurbineBaseBlock, TurbineMultiblockController> {
    // Modified 2026-10-30: client appearance identity, separate from menu/request position.
    public BlockPos appearancePos;
    public TurbineBaseTile(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state) {
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
    
    @Override
    public final TurbineMultiblockController createController() {
        return new TurbineMultiblockController(level);
    }
    
    public void runRequest(String requestName, Object requestData) {
        if (nullableController() != null) {
            controller().runRequest(requestName, requestData);
        }
    }
    
    public boolean isCurrentController(TurbineMultiblockController turbineMultiblockController) {
        return controller() == turbineMultiblockController;
    }
}
