package biggerreactorsrework.multiblocks.turbine.blocks;

import biggerreactorsrework.core.multiblock.IAssemblyStateBlock;
import biggerreactorsrework.core.multiblock.rectangular.IFaceDirectionBlock;
import biggerreactorsrework.core.registry.RegisterBlock;
import biggerreactorsrework.multiblocks.turbine.tiles.TurbineRedstonePortTile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Uses the turbine port's own lit/unlit textures. */
public final class TurbineRedstonePort extends TurbineBaseBlock implements IAssemblyStateBlock, IFaceDirectionBlock {
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty IS_LIT =
            net.minecraft.world.level.block.state.properties.BooleanProperty.create("is_lit");
    @RegisterBlock(name="turbine_redstone_port",tileEntityClass=TurbineRedstonePortTile.class)
    public static final TurbineRedstonePort INSTANCE=new TurbineRedstonePort();
    public TurbineRedstonePort() {
        registerDefaultState(defaultBlockState().setValue(IS_LIT, false));
    }
    @Override protected void buildStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.buildStateDefinition(builder);
        builder.add(IS_LIT);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return TurbineRedstonePortTile.SUPPLIER.create(pos,state);}
    @Override public boolean isSignalSource(BlockState state){return true;}
    @Override public int getSignal(BlockState state,BlockGetter level,BlockPos pos,Direction side){return level.getBlockEntity(pos) instanceof TurbineRedstonePortTile tile&&tile.emitting(side)?15:0;}
    @Override public int getDirectSignal(BlockState state,BlockGetter level,BlockPos pos,Direction side){return getSignal(state,level,pos,side);}
}
