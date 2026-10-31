package biggerreactorsrework.multiblocks.reactor.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import biggerreactorsrework.multiblocks.reactor.tiles.ReactorControlRodTile;
import biggerreactorsrework.core.multiblock.IAssemblyStateBlock;
import biggerreactorsrework.core.multiblock.rectangular.IFaceDirectionBlock;
import biggerreactorsrework.core.registry.RegisterBlock;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class ReactorControlRod extends ReactorBaseBlock implements IAssemblyStateBlock, IFaceDirectionBlock {
    
    @RegisterBlock(name = "reactor_control_rod", tileEntityClass = ReactorControlRodTile.class)
    public static final ReactorControlRod INSTANCE = biggerreactorsrework.core.registry.RegistryIds.named("reactor_control_rod", () -> new ReactorControlRod());
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ReactorControlRodTile.SUPPLIER.create(pos, state);
    }
}
