//? if =26.1.2 || =26.3 {
package biggerreactorsrework.multiblocks.heatexchanger.blocks;


import net.minecraft.core.BlockPos;
//?} else if =1.21.1 {
/*package biggerreactorsrework.multiblocks.heatexchanger.blocks;


import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
*/
//?}
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import biggerreactorsrework.multiblocks.heatexchanger.tiles.HeatExchangerComputerPortTile;
import biggerreactorsrework.core.multiblock.IAssemblyStateBlock;
import biggerreactorsrework.core.multiblock.rectangular.IFaceDirectionBlock;
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
public class HeatExchangerComputerPortBlock extends HeatExchangerBaseBlock implements IAssemblyStateBlock, IFaceDirectionBlock {

//? if =26.1.2 || =26.3 {
    @RegisterBlock(name = "heat_exchanger_computer_port", tileEntityClass = HeatExchangerComputerPortTile.class)
    public static final HeatExchangerComputerPortBlock INSTANCE = biggerreactorsrework.core.registry.RegistryIds.named("heat_exchanger_computer_port", () -> new HeatExchangerComputerPortBlock());

    @Nullable
//?} else if =1.21.1 {
/*    @RegisterBlock(name = "heat_exchanger_computer_port", tileEntityClass = HeatExchangerComputerPortTile.class)
    public static final HeatExchangerComputerPortBlock INSTANCE = new HeatExchangerComputerPortBlock();

    @Nullable
*/
//?}
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return HeatExchangerComputerPortTile.SUPPLIER.create(pos, state);
    }
}
