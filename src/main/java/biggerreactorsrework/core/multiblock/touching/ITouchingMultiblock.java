package biggerreactorsrework.core.multiblock.touching;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import biggerreactorsrework.core.multiblock.MultiblockController;
import biggerreactorsrework.core.multiblock.MultiblockTileModule;
import biggerreactorsrework.core.multiblock.common.IPersistentMultiblock;
import biggerreactorsrework.core.multiblock.common.IPersistentMultiblockTile;
import biggerreactorsrework.core.multiblock.modular.IModularMultiblockController;
import biggerreactorsrework.core.multiblock.modular.MultiblockControllerModule;
import biggerreactorsrework.core.multiblock.modular.MultiblockControllerModuleRegistry;
import biggerreactorsrework.core.multiblock.rectangular.IRectangularMultiblock;
import biggerreactorsrework.core.multiblock.rectangular.IRectangularMultiblockBlock;
import biggerreactorsrework.core.multiblock.rectangular.IRectangularMultiblockTile;
import biggerreactorsrework.core.multiblock.validated.IValidatedMultiblock;
import biggerreactorsrework.core.multiblock.validated.IValidatedMultiblockControllerModule;
import biggerreactorsrework.core.registry.OnModLoad;
import biggerreactorsrework.core.util.FastArraySet;
import biggerreactorsrework.core.util.NonnullDefault;
import biggerreactorsrework.core.util.VectorUtil;
import org.joml.Vector3i;

import javax.annotation.Nonnull;
import java.util.Objects;

@NonnullDefault
public interface ITouchingMultiblock<
        TileType extends BlockEntity & ITouchingMultiblockTile<TileType, BlockType, ControllerType> & IRectangularMultiblockTile<TileType, BlockType, ControllerType> & IPersistentMultiblockTile<TileType, BlockType, ControllerType>,
        BlockType extends Block & IRectangularMultiblockBlock,
        ControllerType extends MultiblockController<TileType, BlockType, ControllerType> & ITouchingMultiblock<TileType, BlockType, ControllerType> & IRectangularMultiblock<TileType, BlockType, ControllerType> & IPersistentMultiblock<TileType, BlockType, ControllerType>
        > extends IModularMultiblockController<TileType, BlockType, ControllerType> {
    
    final class Module<
            TileType extends BlockEntity & ITouchingMultiblockTile<TileType, BlockType, ControllerType> & IRectangularMultiblockTile<TileType, BlockType, ControllerType> & IPersistentMultiblockTile<TileType, BlockType, ControllerType>,
            BlockType extends Block & IRectangularMultiblockBlock,
            ControllerType extends MultiblockController<TileType, BlockType, ControllerType> & ITouchingMultiblock<TileType, BlockType, ControllerType> & IRectangularMultiblock<TileType, BlockType, ControllerType> & IPersistentMultiblock<TileType, BlockType, ControllerType>
            > extends MultiblockControllerModule<TileType, BlockType, ControllerType> implements IValidatedMultiblockControllerModule {
        
        private boolean assembled = false;
        
        final Vector3i min = new Vector3i();
        final Vector3i max = new Vector3i();
        
        private final FastArraySet<ITouchingMultiblockTile.Module<TileType, BlockType, ControllerType>> touchingModules = new FastArraySet<>();
        private final FastArraySet<MultiblockTileModule<TileType, BlockType, ControllerType>> multiblockModules = new FastArraySet<>();
        
        @OnModLoad
        public static void register() {
            MultiblockControllerModuleRegistry.registerModule(ITouchingMultiblock.class, Module::new);
        }
        
        public Module(IModularMultiblockController<TileType, BlockType, ControllerType> controller) {
            super(controller);
        }
        
        @Override
        public void onStateTransition(IValidatedMultiblock.AssemblyState oldAssemblyState, IValidatedMultiblock.AssemblyState newAssemblyState) {
            switch (newAssemblyState) {
                case ASSEMBLED -> {
                    assembled = true;
                    final var min = controller.min();
                    final var max = controller.max();
                    this.min.set(min);
                    this.max.set(max);
                    for (int i = 0; i < touchingModules.size(); i++) {
                        final var module = touchingModules.get(i);
                        module.assembled = true;
                        module.min.set(min);
                        module.max.set(max);
                    }
                }
                case DISASSEMBLED -> {
                    if (oldAssemblyState == IValidatedMultiblock.AssemblyState.DISASSEMBLED) {
                        break;
                    }
                    final var minE = controller.min();
                    final var maxE = controller.max();
                    for (int i = 0; i < multiblockModules.size(); i++) {
                        final var module = multiblockModules.get(i);
                        final var modulePos = module.iface.getBlockPos();
                        if (modulePos.getX() == minE.x() || modulePos.getX() == maxE.x()
                                || modulePos.getY() == minE.y() || modulePos.getY() == maxE.y()
                                || modulePos.getZ() == minE.z() || modulePos.getZ() == maxE.z()) {
                            module.attachToNeighborsLater();
                        }
                    }
                    assembled = false;
                    for (int i = 0; i < touchingModules.size(); i++) {
                        touchingModules.get(i).assembled = false;
                    }
                    min.set(0);
                    max.set(0);
                }
            }
        }
        
        @Override
        public boolean canAttachPart(TileType tile) {
            if (min.equals(max)) {
                return true;
            }
            final var blockpos = tile.getBlockPos();
            if (VectorUtil.less(blockpos, min) || VectorUtil.greater(blockpos, max)) {
                return false;
            }
            return true;
        }
        
        @Override
        public void onPartAdded(@Nonnull TileType tile) {
            final var module = Objects.requireNonNull(tile.module(ITouchingMultiblockTile.class, ITouchingMultiblockTile.Module.class));
            if (module.assembled && !assembled) {
                assembled = true;
                min.set(module.min);
                max.set(module.max);
            }
            //noinspection unchecked
            touchingModules.add(module);
            multiblockModules.add(tile.multiblockModule());
        }
        
        @Override
        public void onPartRemoved(@Nonnull TileType tile) {
            //noinspection unchecked
            touchingModules.remove(Objects.requireNonNull(tile.module(ITouchingMultiblockTile.class, ITouchingMultiblockTile.Module.class)));
            multiblockModules.remove(tile.multiblockModule());
        }
    }
}
