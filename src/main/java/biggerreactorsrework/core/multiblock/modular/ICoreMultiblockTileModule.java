package biggerreactorsrework.core.multiblock.modular;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import biggerreactorsrework.core.multiblock.IMultiblockBlock;
import biggerreactorsrework.core.multiblock.IMultiblockTile;
import biggerreactorsrework.core.multiblock.MultiblockController;
import biggerreactorsrework.core.util.NonnullDefault;

@NonnullDefault
public interface ICoreMultiblockTileModule<
        TileType extends BlockEntity & IMultiblockTile<TileType, BlockType, ControllerType>,
        BlockType extends Block & IMultiblockBlock,
        ControllerType extends MultiblockController<TileType, BlockType, ControllerType>
        > {
    
    default boolean shouldConnectTo(TileType tile, Direction direction) {
        return true;
    }
    
    default void aboutToAttemptAttach() {
    }
    
    default void aboutToUnloadDetach() {
    }
    
    default void aboutToRemovedDetach() {
    }
    
    default void onControllerChange() {
    }
}
