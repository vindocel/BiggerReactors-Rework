package biggerreactorsrework.multiblocks.reactor.blocks;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import biggerreactorsrework.multiblocks.reactor.tiles.ReactorTerminalTile;
import biggerreactorsrework.core.multiblock.IAssemblyStateBlock;
import biggerreactorsrework.core.multiblock.rectangular.IFaceDirectionBlock;
import biggerreactorsrework.core.registry.CreativeTabBlock;
import biggerreactorsrework.core.registry.RegisterBlock;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ReactorTerminal extends ReactorBaseBlock implements IAssemblyStateBlock, IFaceDirectionBlock {
    
    @CreativeTabBlock
    @RegisterBlock(name = "reactor_terminal", tileEntityClass = ReactorTerminalTile.class)
    public static final ReactorTerminal INSTANCE = new ReactorTerminal();
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ReactorTerminalTile.SUPPLIER.create(pos, state);
    }
    
    @Override
    public boolean usesReactorState() {
        return true;
    }
}
