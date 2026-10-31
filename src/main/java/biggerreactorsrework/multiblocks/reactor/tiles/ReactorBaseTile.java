package biggerreactorsrework.multiblocks.reactor.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import biggerreactorsrework.multiblocks.reactor.ReactorMultiblockController;
import biggerreactorsrework.multiblocks.reactor.blocks.ReactorBaseBlock;
import biggerreactorsrework.core.modular.tile.ModularTile;
import biggerreactorsrework.core.multiblock.rectangular.IRectangularMultiblockTile;
import biggerreactorsrework.core.multiblock.IMultiblockTile;
import biggerreactorsrework.core.multiblock.common.IPersistentMultiblockTile;
import biggerreactorsrework.core.multiblock.touching.ITouchingMultiblockTile;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class ReactorBaseTile extends ModularTile implements IMultiblockTile<ReactorBaseTile, ReactorBaseBlock, ReactorMultiblockController>,
        IRectangularMultiblockTile<ReactorBaseTile, ReactorBaseBlock, ReactorMultiblockController>,
        IPersistentMultiblockTile<ReactorBaseTile, ReactorBaseBlock, ReactorMultiblockController>,
        ITouchingMultiblockTile<ReactorBaseTile, ReactorBaseBlock, ReactorMultiblockController> {
    
    public int index = -1;
    // Modified 2026-10-30: client appearance identity, separate from menu/request position.
    public BlockPos appearancePos;
    
    public ReactorBaseTile(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state) {
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
    public final ReactorMultiblockController createController() {
        if (level == null) {
            throw new IllegalStateException("Attempt to create controller with null world");
        }
        return new ReactorMultiblockController(level);
    }
    
    public void runRequest(String requestName, Object requestData) {
        if (nullableController() != null) {
            controller().runRequest(requestName, requestData);
        }
    }
    
    public boolean isCurrentController(@Nullable ReactorMultiblockController reactorMultiblockController) {
        return controller() == reactorMultiblockController;
    }
}
