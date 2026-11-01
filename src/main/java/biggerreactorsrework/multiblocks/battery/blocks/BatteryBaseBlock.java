package biggerreactorsrework.multiblocks.battery.blocks;

import net.minecraft.world.level.material.MapColor;

import biggerreactorsrework.core.modular.block.ModularBlock;
import biggerreactorsrework.core.multiblock.IMultiblockBlock;
import biggerreactorsrework.core.multiblock.rectangular.IRectangularMultiblockBlock;
import biggerreactorsrework.multiblocks.battery.state.BatteryActivity;
import biggerreactorsrework.multiblocks.battery.tiles.BatteryBaseTile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.ParametersAreNonnullByDefault;

import static biggerreactorsrework.core.multiblock.IAssemblyStateBlock.ASSEMBLED;

@ParametersAreNonnullByDefault
public abstract class BatteryBaseBlock extends ModularBlock implements IMultiblockBlock, EntityBlock, IRectangularMultiblockBlock {
    // Modified 2026-10-27: active terminal light.
    // Modified 2026-11-01: declare the machine palette color for map renderers.
    public static final Block.Properties PROPERTIES_SOLID = Block.Properties.of().mapColor(MapColor.WOOL).lightLevel(biggerreactorsrework.multiblocks.MachineLighting::emission).sound(SoundType.METAL).destroyTime(2).explosionResistance(10).isValidSpawn((a, b, c, d) -> false).requiresCorrectToolForDrops();
    public static final Block.Properties PROPERTIES_GLASS = Block.Properties.of().mapColor(MapColor.WOOL).lightLevel(biggerreactorsrework.multiblocks.MachineLighting::emission).sound(SoundType.GLASS).noOcclusion().destroyTime(2).explosionResistance(2).isValidSpawn((a, b, c, d) -> false).requiresCorrectToolForDrops();
    
    public BatteryBaseBlock() {
        this(true);
    }
    
    public BatteryBaseBlock(boolean solid) {
        super(solid ? PROPERTIES_SOLID : PROPERTIES_GLASS);
        if (usesBatteryState()) {
            registerDefaultState(defaultBlockState().setValue(BatteryActivity.BATTERY_STATE_ENUM_PROPERTY, BatteryActivity.IDLE));
        }
    }
    
    public boolean usesBatteryState() {
        return false;
    }
    
    @Override
    protected void buildStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        if (usesBatteryState()) {
            builder.add(BatteryActivity.BATTERY_STATE_ENUM_PROPERTY);
        }
    }
    
    @Override
    public boolean isGoodForInterior() {
        return false;
    }
    
    @Override
    public boolean isGoodForExterior() {
        return true;
    }
    
    @Override
    public boolean isGoodForFrame() {
        return false;
    }
    
    @Override
    public InteractionResult onUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (hand == InteractionHand.MAIN_HAND && state.hasProperty(ASSEMBLED) && state.getValue(ASSEMBLED)) {
            // Ports open their own screen; every other block opens the terminal.
            if (level.getBlockEntity(pos) instanceof MenuProvider menuProvider) {
                if (!level.isClientSide()) {
                    player.openMenu(menuProvider, pos);
                }
                return InteractionResult.SUCCESS;
            }
            if (level.getBlockEntity(pos) instanceof BatteryBaseTile tile) {
                if (!level.isClientSide() && tile.isBatteryAssembled()) {
                    tile.openTerminal(player);
                }
                return InteractionResult.SUCCESS;
            }
        }
        return super.onUse(state, level, pos, player, hand, hitResult);
    }
}
