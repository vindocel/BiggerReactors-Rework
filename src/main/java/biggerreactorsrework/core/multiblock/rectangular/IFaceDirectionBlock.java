package biggerreactorsrework.core.multiblock.rectangular;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import biggerreactorsrework.core.modular.api.BlockModule;
import biggerreactorsrework.core.modular.api.IModularBlock;
import biggerreactorsrework.core.modular.api.ModuleRegistry;
import biggerreactorsrework.core.registry.OnModLoad;
import biggerreactorsrework.core.util.BlockStates;
import biggerreactorsrework.core.util.NonnullDefault;

@NonnullDefault
public interface IFaceDirectionBlock extends IModularBlock {
    class Module extends BlockModule<IFaceDirectionBlock> {
    
        public Module(IFaceDirectionBlock iface) {
            super(iface);
        }
    
        @Override
        public void buildStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(BlockStates.FACING);
        }
    
        @Override
        public BlockState buildDefaultState(BlockState state) {
            return state.setValue(BlockStates.FACING, Direction.UP);
        }
    
        @OnModLoad
        static void onModLoad() {
            ModuleRegistry.registerBlockModule(IFaceDirectionBlock.class, Module::new);
        }
    }
}
