//? if =26.1.2 || =26.3 {
package biggerreactorsrework.multiblocks.reactor.blocks;

import net.minecraft.core.BlockPos;
//?} else if =1.21.1 {
/*package biggerreactorsrework.multiblocks.reactor.blocks;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
*/
//?}
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import biggerreactorsrework.multiblocks.reactor.tiles.ReactorComputerPortTile;
import biggerreactorsrework.core.multiblock.IAssemblyStateBlock;
import biggerreactorsrework.core.multiblock.rectangular.IFaceDirectionBlock;
import biggerreactorsrework.core.registry.RegisterBlock;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

//? if =26.1.2 || =26.3 {
@ParametersAreNonnullByDefault
public class ReactorComputerPort extends ReactorBaseBlock implements IAssemblyStateBlock, IFaceDirectionBlock {
//?} else if =1.21.1 {
/*@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ReactorComputerPort extends ReactorBaseBlock implements IAssemblyStateBlock, IFaceDirectionBlock {
*/
//?}

//? if =26.1.2 || =26.3 {
    @RegisterBlock(name = "reactor_computer_port", tileEntityClass = ReactorComputerPortTile.class)
    public static final ReactorComputerPort INSTANCE = biggerreactorsrework.core.registry.RegistryIds.named("reactor_computer_port", () -> new ReactorComputerPort());

    public ReactorComputerPort() {
//?} else if =1.21.1 {
/*    @RegisterBlock(name = "reactor_computer_port", tileEntityClass = ReactorComputerPortTile.class)
    public static final ReactorComputerPort INSTANCE = new ReactorComputerPort();

    public ReactorComputerPort() {
*/
//?}
        super();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ReactorComputerPortTile.SUPPLIER.create(pos, state);
    }
}
