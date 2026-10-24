package biggerreactorsrework.core.multiblock.common;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import biggerreactorsrework.core.multiblock.IMultiblockTile;
import biggerreactorsrework.core.multiblock.MultiblockController;
import biggerreactorsrework.core.multiblock.modular.IModularMultiblockController;
import biggerreactorsrework.core.multiblock.modular.MultiblockControllerModule;
import biggerreactorsrework.core.multiblock.modular.MultiblockControllerModuleRegistry;
import biggerreactorsrework.core.multiblock.validated.IAssembledTickMultiblockModule;
import biggerreactorsrework.core.multiblock.validated.IValidatedMultiblock;
import biggerreactorsrework.core.multiblock.validated.IValidatedMultiblockBlock;
import biggerreactorsrework.core.multiblock.validated.IValidatedMultiblockTile;
import biggerreactorsrework.core.registry.OnModLoad;
import biggerreactorsrework.core.util.FastArraySet;
import biggerreactorsrework.core.util.NonnullDefault;

import javax.annotation.Nonnull;

@NonnullDefault
public interface ITickablePartsMultiblock<
        TileType extends BlockEntity & IMultiblockTile<TileType, BlockType, ControllerType> & IValidatedMultiblockTile<TileType, BlockType, ControllerType>,
        BlockType extends Block & IValidatedMultiblockBlock,
        ControllerType extends MultiblockController<TileType, BlockType, ControllerType> & ITickablePartsMultiblock<TileType, BlockType, ControllerType> & IValidatedMultiblock<TileType, BlockType, ControllerType>
        > extends IModularMultiblockController<TileType, BlockType, ControllerType> {
    
    
    /**
     * any Tile implementing this will be ticked
     */
    interface Tickable {
        void preTick();
        
        void postTick();
    }
    
    final class Module<
            TileType extends BlockEntity & IMultiblockTile<TileType, BlockType, ControllerType> & IValidatedMultiblockTile<TileType, BlockType, ControllerType>,
            BlockType extends Block & IValidatedMultiblockBlock,
            ControllerType extends MultiblockController<TileType, BlockType, ControllerType> & ITickablePartsMultiblock<TileType, BlockType, ControllerType> & IValidatedMultiblock<TileType, BlockType, ControllerType>
            > extends MultiblockControllerModule<TileType, BlockType, ControllerType> implements IAssembledTickMultiblockModule {
        
        private final FastArraySet<Tickable> tickables = new FastArraySet<>();
        
        @OnModLoad
        public static void register() {
            MultiblockControllerModuleRegistry.registerModule(ITickablePartsMultiblock.class, Module::new);
        }
        
        public Module(IModularMultiblockController<TileType, BlockType, ControllerType> controller) {
            super(controller);
        }
        
        @Override
        public void onPartAdded(@Nonnull TileType tile) {
            if (tile instanceof Tickable tickable) {
                tickables.add(tickable);
            }
        }
        
        @Override
        public void onPartRemoved(@Nonnull TileType tile) {
            if (tile instanceof Tickable tickable) {
                tickables.remove(tickable);
            }
        }
        
        @Override
        public void preTick() {
            tickables.elements().forEach(Tickable::preTick);
        }
        
        @Override
        public void postTick() {
            tickables.elements().forEach(Tickable::postTick);
        }
    }
}
