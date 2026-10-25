package biggerreactorsrework.multiblocks.battery.blocks;

import biggerreactorsrework.core.multiblock.IAssemblyStateBlock;
import biggerreactorsrework.core.multiblock.rectangular.IFaceDirectionBlock;
import biggerreactorsrework.core.registry.RegisterBlock;
import biggerreactorsrework.core.util.Util;
import biggerreactorsrework.multiblocks.battery.tiles.BatteryEnergyPortTile;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Locale;

import static biggerreactorsrework.multiblocks.battery.blocks.BatteryEnergyPort.PortDirection.*;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BatteryEnergyPort extends BatteryBaseBlock implements IAssemblyStateBlock, IFaceDirectionBlock {
    
    @RegisterBlock(name = "battery_energy_port", tileEntityClass = BatteryEnergyPortTile.class)
    public static final BatteryEnergyPort INSTANCE = new BatteryEnergyPort();
    
    public BatteryEnergyPort() {
        super();
        registerDefaultState(defaultBlockState().setValue(PORT_DIRECTION_ENUM_PROPERTY, INLET));
    }
    
    public enum PortDirection implements StringRepresentable {
        INLET,
        OUTLET;
        
        @SuppressWarnings("SpellCheckingInspection")
        public static final EnumProperty<PortDirection> PORT_DIRECTION_ENUM_PROPERTY = EnumProperty.create("portdirection", PortDirection.class);
        
        @Override
        public String getSerializedName() {
            return toString().toLowerCase(Locale.US);
        }
        
        public PortDirection opposite() {
            return this == INLET ? OUTLET : INLET;
        }
    }
    
    @Override
    protected void buildStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PORT_DIRECTION_ENUM_PROPERTY);
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return BatteryEnergyPortTile.SUPPLIER.create(pos, state);
    }
    
    @Override
    public InteractionResult onUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand == InteractionHand.MAIN_HAND && Util.isWrench(player.getMainHandItem().getItem())) {
            if (!level.isClientSide() && level.getBlockEntity(pos) instanceof BatteryEnergyPortTile port) {
                PortDirection direction = state.getValue(PORT_DIRECTION_ENUM_PROPERTY).opposite();
                port.setDirection(direction);
                player.displayClientMessage(Component.translatable("message.biggerreactorsrework.battery_energy_port." + direction.getSerializedName()), true);
            }
            return InteractionResult.SUCCESS;
        }
        return super.onUse(state, level, pos, player, hand, hit);
    }
    
    @Override
    public void onNeighborChange(BlockState state, Level level, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
        super.onNeighborChange(state, level, pos, blockIn, fromPos, isMoving);
        if (level.getBlockEntity(pos) instanceof BatteryEnergyPortTile port) {
            port.neighborChanged();
        }
    }
}
