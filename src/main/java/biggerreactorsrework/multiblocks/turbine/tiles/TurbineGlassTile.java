package biggerreactorsrework.multiblocks.turbine.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import biggerreactorsrework.core.registry.RegisterTile;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class TurbineGlassTile extends TurbineBaseTile {
    
    @RegisterTile("turbine_glass")
    public static final BlockEntityType.BlockEntitySupplier<TurbineGlassTile> SUPPLIER = new RegisterTile.Producer<>(TurbineGlassTile::new);
    
    public TurbineGlassTile(BlockEntityType<?> TYPE, BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }
}
