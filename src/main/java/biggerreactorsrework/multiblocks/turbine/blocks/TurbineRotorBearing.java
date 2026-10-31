package biggerreactorsrework.multiblocks.turbine.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import biggerreactorsrework.multiblocks.turbine.tiles.TurbineRotorBearingTile;
import biggerreactorsrework.core.multiblock.IAssemblyStateBlock;
import biggerreactorsrework.core.multiblock.rectangular.IFaceDirectionBlock;
import biggerreactorsrework.core.registry.RegisterBlock;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class TurbineRotorBearing extends TurbineBaseBlock implements IAssemblyStateBlock, IFaceDirectionBlock {
    
    @RegisterBlock(name = "turbine_rotor_bearing", tileEntityClass = TurbineRotorBearingTile.class)
    public static final TurbineRotorBearing INSTANCE = biggerreactorsrework.core.registry.RegistryIds.named("turbine_rotor_bearing", () -> new TurbineRotorBearing());
    
    public TurbineRotorBearing() {
        super();
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return TurbineRotorBearingTile.SUPPLIER.create(pos, state);
    }
}
