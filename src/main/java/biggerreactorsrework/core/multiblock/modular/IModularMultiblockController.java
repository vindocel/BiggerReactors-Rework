package biggerreactorsrework.core.multiblock.modular;


import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import biggerreactorsrework.core.multiblock.IMultiblockBlock;
import biggerreactorsrework.core.multiblock.IMultiblockTile;
import biggerreactorsrework.core.multiblock.MultiblockController;
import biggerreactorsrework.core.util.NonnullDefault;

import javax.annotation.Nullable;
import java.util.List;

@NonnullDefault
public interface IModularMultiblockController<
        TileType extends BlockEntity & IMultiblockTile<TileType, BlockType, ControllerType>,
        BlockType extends Block & IMultiblockBlock,
        ControllerType extends MultiblockController<TileType, BlockType, ControllerType>
        > {
    
    default <Type> Type as(Class<Type> clazz) {
        //noinspection unchecked
        return (Type) this;
    }
    
    @Nullable
    MultiblockControllerModule<TileType, BlockType, ControllerType> module(Class<?> interfaceClazz);
    
    @Nullable
    default <T extends MultiblockControllerModule<?, ?, ?>> T module(Class<?> interfaceClazz, Class<T> moduleType) {
        //noinspection unchecked
        return (T) module(interfaceClazz);
    }
    
    List<MultiblockControllerModule<TileType, BlockType, ControllerType>> modules();
}
