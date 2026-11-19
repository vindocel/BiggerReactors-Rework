//? if =26.1.2 || =26.3 {
package biggerreactorsrework.multiblocks.heatexchanger.blocks;


import net.minecraft.core.BlockPos;
//?} else if =1.21.1 {
/*package biggerreactorsrework.multiblocks.heatexchanger.blocks;


import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
*/
//?}
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import biggerreactorsrework.multiblocks.heatexchanger.tiles.HeatExchangerChannelTile;
import biggerreactorsrework.core.modular.block.IConnectedTexture;
import biggerreactorsrework.core.registry.RegisterBlock;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

//? if =26.1.2 || =26.3 {
@ParametersAreNonnullByDefault
public class HeatExchangerEvaporatorChannelBlock extends HeatExchangerBaseBlock implements IConnectedTexture {
//?} else if =1.21.1 {
/*@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class HeatExchangerEvaporatorChannelBlock extends HeatExchangerBaseBlock implements IConnectedTexture {
*/
//?}

//? if =26.1.2 || =26.3 {
    @RegisterBlock(name = "heat_exchanger_evaporator_channel", tileEntityClass = HeatExchangerChannelTile.class)
    public static final HeatExchangerEvaporatorChannelBlock INSTANCE = biggerreactorsrework.core.registry.RegistryIds.named("heat_exchanger_evaporator_channel", () -> new HeatExchangerEvaporatorChannelBlock());

    public HeatExchangerEvaporatorChannelBlock() {
//?} else if =1.21.1 {
/*    @RegisterBlock(name = "heat_exchanger_evaporator_channel", tileEntityClass = HeatExchangerChannelTile.class)
    public static final HeatExchangerEvaporatorChannelBlock INSTANCE = new HeatExchangerEvaporatorChannelBlock();

    public HeatExchangerEvaporatorChannelBlock() {
*/
//?}
        super(PROPERTIES_GLASS);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return HeatExchangerChannelTile.SUPPLIER.create(pos, state);
    }

    @SuppressWarnings("deprecation")
    @Override
    public float getShadeBrightness(BlockState state, BlockGetter worldIn, BlockPos pos) {
        return 1.0F;
    }

//? if =26.1.2 || =26.3 {
    @Override
    public boolean propagatesSkylightDown(BlockState state) {
        return true;
//?} else if =1.21.1 {
/*    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
        return true;
*/
//?}
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
