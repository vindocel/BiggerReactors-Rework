package biggerreactorsrework.multiblocks.battery.blocks;

import biggerreactorsrework.core.multiblock.IAssemblyStateBlock;
import biggerreactorsrework.core.registry.RegisterBlock;
import biggerreactorsrework.multiblocks.battery.tiles.BatteryGlassTile;
import biggerreactorsrework.core.modular.block.IConnectedTexture;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class BatteryGlass extends BatteryBaseBlock implements IAssemblyStateBlock, IConnectedTexture {
    
    @RegisterBlock(name = "battery_glass", tileEntityClass = BatteryGlassTile.class)
    public static final BatteryGlass INSTANCE = biggerreactorsrework.core.registry.RegistryIds.named("battery_glass", () -> new BatteryGlass());
    
    public BatteryGlass() {
        super(false);
    }
    
    @SuppressWarnings("deprecation")
    @Override
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0f;
    }
    
    @Override
    public boolean propagatesSkylightDown(BlockState state) {
        return true;
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return BatteryGlassTile.SUPPLIER.create(pos, state);
    }
}
