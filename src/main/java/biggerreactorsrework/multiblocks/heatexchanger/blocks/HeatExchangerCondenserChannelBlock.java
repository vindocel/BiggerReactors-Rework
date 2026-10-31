package biggerreactorsrework.multiblocks.heatexchanger.blocks;


import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import biggerreactorsrework.multiblocks.heatexchanger.tiles.HeatExchangerChannelTile;
import biggerreactorsrework.core.modular.block.IConnectedTexture;
import biggerreactorsrework.core.registry.RegisterBlock;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class HeatExchangerCondenserChannelBlock extends HeatExchangerBaseBlock implements IConnectedTexture {
    
    @RegisterBlock(name = "heat_exchanger_condenser_channel", tileEntityClass = HeatExchangerChannelTile.class)
    public static final HeatExchangerCondenserChannelBlock INSTANCE = biggerreactorsrework.core.registry.RegistryIds.named("heat_exchanger_condenser_channel", () -> new HeatExchangerCondenserChannelBlock());
    
    public HeatExchangerCondenserChannelBlock() {
        super(PROPERTIES_GLASS);
        registerDefaultState(defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT, false));
    }

    // Modified 2026-10-27: the server marks only channel segments containing a visible hot phase.
    @Override
    protected void buildStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) {
        super.buildStateDefinition(builder);
        builder.add(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return HeatExchangerChannelTile.SUPPLIER.create(pos, state);
    }
    
    @SuppressWarnings("deprecation")
    public float getShadeBrightness(BlockState state, BlockGetter worldIn, BlockPos pos) {
        return 1.0F;
    }
    
    @Override
    public boolean propagatesSkylightDown(BlockState state) {
        return true;
    }
    
    @Override
    public boolean connectToBlock(Block block) {
        return IConnectedTexture.super.connectToBlock(block) || block == HeatExchangerFluidPortBlock.INSTANCE;
    }
    
    @Override
    public boolean isGoodForExterior() {
        return false;
    }
    
    @Override
    public boolean isGoodForInterior() {
        return true;
    }
}
