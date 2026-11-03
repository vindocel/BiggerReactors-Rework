package biggerreactorsrework.multiblocks.reactor.blocks;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import biggerreactorsrework.Config;
import biggerreactorsrework.multiblocks.reactor.tiles.ReactorManifoldTile;
import biggerreactorsrework.core.modular.block.IConnectedTexture;
import biggerreactorsrework.core.registry.RegisterBlock;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ReactorManifold extends ReactorBaseBlock implements IConnectedTexture {
    
    @RegisterBlock(name = "reactor_manifold", tileEntityClass = ReactorManifoldTile.class)
    public static final ReactorManifold INSTANCE = new ReactorManifold();
    
    public ReactorManifold() {
        super(false);
        registerDefaultState(defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT, false));
    }
    
    // Added 2026-11-03: the server marks manifolds holding a glowing coolant.
    @Override
    protected void buildStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) {
        super.buildStateDefinition(builder);
        builder.add(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT);
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ReactorManifoldTile.SUPPLIER.create(pos, state);
    }
    
    @Override
    public float getShadeBrightness(BlockState p_60472_, BlockGetter p_60473_, BlockPos p_60474_) {
        return 1.0f;
    }
    
    public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
        return true;
    }
    
    @Override
    public boolean isGoodForInterior() {
        return true;
    }
    
    @Override
    public boolean isGoodForExterior() {
        return false;
    }
    
    @Override
    public boolean connectToBlock(Block block) {
        if (block instanceof ReactorBaseBlock reactorBlock) {
            return !(reactorBlock instanceof ReactorGlass) && (reactorBlock).isGoodForExterior() || reactorBlock == this;
        }
        return false;
    }
}
