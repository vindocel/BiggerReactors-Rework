package biggerreactorsrework.multiblocks.reactor.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import biggerreactorsrework.multiblocks.reactor.tiles.ReactorGlassTile;
import biggerreactorsrework.core.modular.block.IConnectedTexture;
import biggerreactorsrework.core.multiblock.IAssemblyStateBlock;
import biggerreactorsrework.core.registry.RegisterBlock;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class ReactorGlass extends ReactorBaseBlock implements IAssemblyStateBlock, IConnectedTexture {
    
    @RegisterBlock(name = "reactor_glass", tileEntityClass = ReactorGlassTile.class)
    public static final ReactorGlass INSTANCE = biggerreactorsrework.core.registry.RegistryIds.named("reactor_glass", () -> new ReactorGlass());
    
    public ReactorGlass() {
        super(false);
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ReactorGlassTile.SUPPLIER.create(pos, state);
    }
    
    @SuppressWarnings("deprecation")
    @Override
    public float getShadeBrightness(BlockState p_60472_, BlockGetter p_60473_, BlockPos p_60474_) {
        return 1.0f;
    }
    
    public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
        return true;
    }
}
