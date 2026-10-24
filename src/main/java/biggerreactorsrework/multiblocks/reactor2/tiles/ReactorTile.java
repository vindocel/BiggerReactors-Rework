package biggerreactorsrework.multiblocks.reactor2.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import biggerreactorsrework.multiblocks.reactor2.blocks.ReactorBlock;
import biggerreactorsrework.multiblocks.reactor2.ReactorMultiblockController;
import biggerreactorsrework.core.modular.tile.ModularTile;
import biggerreactorsrework.core.multiblock.IMultiblockTile;
import biggerreactorsrework.core.multiblock.common.IPersistentMultiblockTile;
import biggerreactorsrework.core.multiblock.rectangular.IRectangularMultiblockTile;
import biggerreactorsrework.core.multiblock.touching.ITouchingMultiblockTile;
import biggerreactorsrework.core.util.NonnullDefault;

@NonnullDefault
public class ReactorTile extends ModularTile implements IMultiblockTile<ReactorTile, ReactorBlock, ReactorMultiblockController>, IPersistentMultiblockTile<ReactorTile, ReactorBlock, ReactorMultiblockController>, IRectangularMultiblockTile<ReactorTile, ReactorBlock, ReactorMultiblockController>, ITouchingMultiblockTile<ReactorTile, ReactorBlock, ReactorMultiblockController> {
    
    public long lastCheckedTick = 0;
    
    public ReactorTile(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state) {
        super(tileEntityTypeIn, pos, state);
    }
    
    @Override
    public ReactorMultiblockController createController() {
        assert level != null;
        return new ReactorMultiblockController(level);
    }
}
