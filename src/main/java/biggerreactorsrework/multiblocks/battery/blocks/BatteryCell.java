package biggerreactorsrework.multiblocks.battery.blocks;

import biggerreactorsrework.core.modular.block.IConnectedTexture;
import biggerreactorsrework.core.multiblock.IAssemblyStateBlock;
import biggerreactorsrework.core.registry.RegisterBlock;
import biggerreactorsrework.multiblocks.battery.tiles.BatteryCellTile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

/**
 * Interior block of the battery: stores energy and raises the transfer limits. The loot table copies a cell's energy
 * into the dropped item, and placing the item restores it.
 */
@ParametersAreNonnullByDefault
public class BatteryCell extends BatteryBaseBlock implements IAssemblyStateBlock, IConnectedTexture, biggerreactorsrework.core.modular.block.BlockTooltip {
    
    public static final BooleanProperty CHARGED = BooleanProperty.create("charged");
    
    @RegisterBlock(name = "battery_cell", tileEntityClass = BatteryCellTile.class)
    public static final BatteryCell INSTANCE = biggerreactorsrework.core.registry.RegistryIds.named("battery_cell", () -> new BatteryCell());
    
    public BatteryCell() {
        super();
        registerDefaultState(defaultBlockState().setValue(CHARGED, false));
    }
    
    @Override
    protected void buildStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CHARGED);
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
    public void onPlaced(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        long energy = storedEnergy(stack);
        if (energy > 0 && !level.isClientSide() && level.getBlockEntity(pos) instanceof BatteryCellTile cell) {
            cell.loadEnergyFromItem(energy);
        }
    }
    
    public static long storedEnergy(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? 0 : Math.max(0, data.copyTag().getLongOr(BatteryCellTile.ENERGY_KEY, 0L));
    }
    
    @Override
    public void appendTooltip(ItemStack stack, java.util.function.Consumer<Component> tooltip) {
        long energy = storedEnergy(stack);
        if (energy > 0) {
            tooltip.accept(Component.translatable("tooltip.biggerreactorsrework.battery_cell.energy", formatEnergy(energy)));
        }
    }
    
    /**
     * RenderHelper.formatValue is client-only.
     */
    private static String formatEnergy(long energy) {
        String[] prefixes = {"", "k", "M", "G", "T", "P", "E"};
        double value = energy;
        int prefix = 0;
        while (value >= 1000 && prefix < prefixes.length - 1) {
            value /= 1000;
            prefix++;
        }
        return (prefix == 0 ? String.valueOf(energy) : String.format("%.1f", value)) + " " + prefixes[prefix] + "FE";
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return BatteryCellTile.SUPPLIER.create(pos, state);
    }
}
