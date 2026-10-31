package biggerreactorsrework.multiblocks.turbine.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import biggerreactorsrework.multiblocks.turbine.tiles.TurbineTerminalTile;
import biggerreactorsrework.core.multiblock.IAssemblyStateBlock;
import biggerreactorsrework.core.multiblock.rectangular.IFaceDirectionBlock;
import biggerreactorsrework.core.registry.RegisterBlock;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class TurbineTerminal extends TurbineBaseBlock implements IAssemblyStateBlock, IFaceDirectionBlock {
    
    @RegisterBlock(name = "turbine_terminal", tileEntityClass = TurbineTerminalTile.class)
    public static final TurbineTerminal INSTANCE = biggerreactorsrework.core.registry.RegistryIds.named("turbine_terminal", () -> new TurbineTerminal());
    
    public TurbineTerminal() {
        super();
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return TurbineTerminalTile.SUPPLIER.create(pos, state);
    }
    
    @Override
    public boolean usesTurbineState() {
        return true;
    }
}
