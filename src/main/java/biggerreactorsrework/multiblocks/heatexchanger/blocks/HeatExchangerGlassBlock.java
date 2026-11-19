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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import biggerreactorsrework.multiblocks.heatexchanger.tiles.HeatExchangerGlassTile;
import biggerreactorsrework.core.modular.block.IConnectedTexture;
import biggerreactorsrework.core.multiblock.IAssemblyStateBlock;
import biggerreactorsrework.core.registry.RegisterBlock;

import javax.annotation.Nullable;
//? if =26.1.2 || =26.3 {
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
//?} else if =1.21.1 {
/*import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
*/
//?}
public class HeatExchangerGlassBlock extends HeatExchangerBaseBlock implements IAssemblyStateBlock, IConnectedTexture {

//? if =26.1.2 || =26.3 {
    @RegisterBlock(name = "heat_exchanger_glass", tileEntityClass = HeatExchangerGlassTile.class)
    public static final HeatExchangerGlassBlock INSTANCE = biggerreactorsrework.core.registry.RegistryIds.named("heat_exchanger_glass", () -> new HeatExchangerGlassBlock());

    public HeatExchangerGlassBlock() {
//?} else if =1.21.1 {
/*    @RegisterBlock(name = "heat_exchanger_glass", tileEntityClass = HeatExchangerGlassTile.class)
    public static final HeatExchangerGlassBlock INSTANCE = new HeatExchangerGlassBlock();

    public HeatExchangerGlassBlock() {
*/
//?}
        super(PROPERTIES_GLASS);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return HeatExchangerGlassTile.SUPPLIER.create(pos, state);
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
}
