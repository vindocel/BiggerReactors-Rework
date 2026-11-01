package biggerreactorsrework.multiblocks.turbine.blocks;

import net.minecraft.MethodsReturnNonnullByDefault;
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
@MethodsReturnNonnullByDefault
public class TurbineRotorBearing extends TurbineBaseBlock implements IAssemblyStateBlock, IFaceDirectionBlock {
    // Modified 2026-11-01: allow outside access to the same real-block rotor helper.
    @Override public net.minecraft.world.InteractionResult onUse(BlockState state, net.minecraft.world.level.Level level, BlockPos pos,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        var result = biggerreactorsrework.multiblocks.turbine.TurbineRotorLayout.use(level,pos,player,hand);
        return result == net.minecraft.world.InteractionResult.PASS ? super.onUse(state,level,pos,player,hand,hit) : result;
    }
    
    @RegisterBlock(name = "turbine_rotor_bearing", tileEntityClass = TurbineRotorBearingTile.class)
    public static final TurbineRotorBearing INSTANCE = new TurbineRotorBearing();
    
    public TurbineRotorBearing() {
        super();
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return TurbineRotorBearingTile.SUPPLIER.create(pos, state);
    }
}
