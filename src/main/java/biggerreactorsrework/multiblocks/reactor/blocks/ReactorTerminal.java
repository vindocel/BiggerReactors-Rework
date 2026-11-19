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
import biggerreactorsrework.multiblocks.reactor.tiles.ReactorTerminalTile;
import biggerreactorsrework.core.multiblock.IAssemblyStateBlock;
import biggerreactorsrework.core.multiblock.rectangular.IFaceDirectionBlock;
import biggerreactorsrework.core.registry.CreativeTabBlock;
import biggerreactorsrework.core.registry.RegisterBlock;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

//? if =26.1.2 || =26.3 {
@ParametersAreNonnullByDefault
public class ReactorTerminal extends ReactorBaseBlock implements IAssemblyStateBlock, IFaceDirectionBlock {
//?} else if =1.21.1 {
/*@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ReactorTerminal extends ReactorBaseBlock implements IAssemblyStateBlock, IFaceDirectionBlock {
*/
//?}

    @CreativeTabBlock
//? if =26.1.2 || =26.3 {
    @RegisterBlock(name = "reactor_terminal", tileEntityClass = ReactorTerminalTile.class)
    public static final ReactorTerminal INSTANCE = biggerreactorsrework.core.registry.RegistryIds.named("reactor_terminal", () -> new ReactorTerminal());

    @Nullable
//?} else if =1.21.1 {
/*    @RegisterBlock(name = "reactor_terminal", tileEntityClass = ReactorTerminalTile.class)
    public static final ReactorTerminal INSTANCE = new ReactorTerminal();

    @Nullable
*/
//?}
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ReactorTerminalTile.SUPPLIER.create(pos, state);
    }

    @Override
    public boolean usesReactorState() {
        return true;
    }
}
