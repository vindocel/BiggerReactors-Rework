package biggerreactorsrework.core.multiblock;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import biggerreactorsrework.core.modular.api.BlockModule;
import biggerreactorsrework.core.modular.api.IModularBlock;
import biggerreactorsrework.core.modular.api.ModuleRegistry;
import biggerreactorsrework.core.registry.OnModLoad;
import biggerreactorsrework.core.util.NonnullDefault;

@NonnullDefault
public interface IAssemblyStateBlock extends IModularBlock {
    
    BooleanProperty ASSEMBLED = BooleanProperty.create("assembled");
    
    final class Module extends BlockModule<IAssemblyStateBlock> {
        private Module(IAssemblyStateBlock iface) {
            super(iface);
        }
        
        @Override
        public BlockState buildDefaultState(BlockState state) {
            state = state.setValue(ASSEMBLED, false);
            return state;
        }
        
        @Override
        public void buildStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(ASSEMBLED);
        }
        
        @OnModLoad
        static void onModLoad() {
            ModuleRegistry.registerBlockModule(IAssemblyStateBlock.class, Module::new);
        }
    }
}
