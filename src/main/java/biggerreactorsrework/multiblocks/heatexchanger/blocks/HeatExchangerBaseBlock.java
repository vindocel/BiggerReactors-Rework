package biggerreactorsrework.multiblocks.heatexchanger.blocks;

import net.minecraft.world.level.material.MapColor;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import biggerreactorsrework.core.modular.block.ModularBlock;
import biggerreactorsrework.core.multiblock.rectangular.IRectangularMultiblockBlock;

import javax.annotation.ParametersAreNonnullByDefault;

import static biggerreactorsrework.core.multiblock.IAssemblyStateBlock.ASSEMBLED;

@ParametersAreNonnullByDefault
public abstract class HeatExchangerBaseBlock extends ModularBlock implements IRectangularMultiblockBlock, EntityBlock, biggerreactorsrework.core.modular.block.BlockTooltip {
    // Modified 2026-10-27: assembled terminal and hot-channel light.
    
    // Modified 2026-11-01: declare the machine palette color for map renderers.
    public static final Properties PROPERTIES_SOLID = Properties.of().mapColor(MapColor.TERRACOTTA_BROWN).lightLevel(biggerreactorsrework.multiblocks.MachineLighting::emission).sound(SoundType.METAL).destroyTime(2).explosionResistance(10).isValidSpawn((a, b, c, d) -> false);
    public static final Properties PROPERTIES_GLASS = Properties.of().mapColor(MapColor.TERRACOTTA_BROWN).lightLevel(biggerreactorsrework.multiblocks.MachineLighting::emission).sound(SoundType.GLASS).noOcclusion().destroyTime(2).explosionResistance(2).isValidSpawn((a, b, c, d) -> false);
    
    public HeatExchangerBaseBlock() {
        super(PROPERTIES_SOLID);
    }
    
    public HeatExchangerBaseBlock(Properties properties) {
        super(properties.requiresCorrectToolForDrops());
    }

    // Modified 2026-10-25: explain useful placement before the player builds the circuits.
    @Override
    public void appendTooltip(net.minecraft.world.item.ItemStack stack,java.util.function.Consumer<net.minecraft.network.chat.Component> tooltip) {
        if(this instanceof HeatExchangerCondenserChannelBlock || this instanceof HeatExchangerEvaporatorChannelBlock)
            for(int i=1;i<=3;i++)tooltip.accept(net.minecraft.network.chat.Component.translatable("tooltip.biggerreactorsrework.channel_contact."+i).withStyle(net.minecraft.ChatFormatting.GRAY));
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
            if (level.getBlockEntity(pos) instanceof MenuProvider menuProvider) {
                if (!level.isClientSide()) {
                    player.openMenu(menuProvider, pos);
                }
                return InteractionResult.SUCCESS;
            }
        }
        return super.onUse(state, level, pos, player, hand, hitResult);
    }
}
