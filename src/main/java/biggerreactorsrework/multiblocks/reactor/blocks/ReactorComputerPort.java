package biggerreactorsrework.multiblocks.reactor.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import biggerreactorsrework.multiblocks.reactor.tiles.ReactorComputerPortTile;
import biggerreactorsrework.core.multiblock.IAssemblyStateBlock;
import biggerreactorsrework.core.multiblock.rectangular.IFaceDirectionBlock;
import biggerreactorsrework.core.registry.RegisterBlock;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class ReactorComputerPort extends ReactorBaseBlock implements IAssemblyStateBlock, IFaceDirectionBlock {
    
    @RegisterBlock(name = "reactor_computer_port", tileEntityClass = ReactorComputerPortTile.class)
    public static final ReactorComputerPort INSTANCE = biggerreactorsrework.core.registry.RegistryIds.named("reactor_computer_port", () -> new ReactorComputerPort());
    
    public ReactorComputerPort() {
        super();
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ReactorComputerPortTile.SUPPLIER.create(pos, state);
    }
}
