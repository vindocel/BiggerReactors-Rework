package biggerreactorsrework.multiblocks.battery.blocks;

import biggerreactorsrework.core.multiblock.IAssemblyStateBlock;
import biggerreactorsrework.core.registry.RegisterBlock;
import biggerreactorsrework.multiblocks.battery.tiles.BatteryCasingTile;
import biggerreactorsrework.core.multiblock.rectangular.IAxisPositionBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class BatteryCasing extends BatteryBaseBlock implements IAssemblyStateBlock, IAxisPositionBlock {
    
    @RegisterBlock(name = "battery_casing", tileEntityClass = BatteryCasingTile.class)
    public static final BatteryCasing INSTANCE = biggerreactorsrework.core.registry.RegistryIds.named("battery_casing", () -> new BatteryCasing());
    
    @Override
    public boolean isGoodForFrame() {
        return true;
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return BatteryCasingTile.SUPPLIER.create(pos, state);
    }
}
