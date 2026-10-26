package biggerreactorsrework.multiblocks.heatexchanger.tiles;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import biggerreactorsrework.multiblocks.heatexchanger.blocks.HeatExchangerCondenserChannelBlock;
import biggerreactorsrework.core.registry.RegisterTile;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * The channel model is chosen from the block state and baked into the chunk mesh,
 * see {@code HeatExchangerChannelModels}.
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class HeatExchangerChannelTile extends HeatExchangerBaseTile {

    public final boolean CONDENSER;

    public long lastCheckedTick;
    public BlockPos coolantAnchor;
    /** Channels have no ASSEMBLED property; their terminal supplies the assembly state. */
    @javax.annotation.Nullable
    public HeatExchangerTerminalTile renderTerminal() {
        if(coolantAnchor==null || getLevel()==null || !(getLevel().getBlockEntity(coolantAnchor) instanceof HeatExchangerTerminalTile terminal))return null;
        var state=terminal.getBlockState();var assembled=biggerreactorsrework.core.multiblock.IAssemblyStateBlock.ASSEMBLED;
        return state.hasProperty(assembled) && state.getValue(assembled)?terminal:null;
    }
    public int coolantPathIndex,coolantPathLength;
    public net.minecraft.core.Direction coolantEntry=net.minecraft.core.Direction.DOWN,coolantExit=net.minecraft.core.Direction.UP;
    @Override protected net.minecraft.nbt.CompoundTag writeNBT(){var tag=super.writeNBT();if(coolantAnchor!=null)tag.putLong("coolantAnchor",coolantAnchor.asLong());tag.putInt("pathIndex",coolantPathIndex);tag.putInt("pathLength",coolantPathLength);tag.putInt("pathEntry",coolantEntry.ordinal());tag.putInt("pathExit",coolantExit.ordinal());return tag;}
    @Override protected void readNBT(net.minecraft.nbt.CompoundTag tag){super.readNBT(tag);coolantAnchor=tag.contains("coolantAnchor")?BlockPos.of(tag.getLong("coolantAnchor")):null;coolantPathIndex=Math.max(0,tag.getInt("pathIndex"));coolantPathLength=Math.max(0,tag.getInt("pathLength"));coolantEntry=net.minecraft.core.Direction.from3DDataValue(tag.getInt("pathEntry"));coolantExit=net.minecraft.core.Direction.from3DDataValue(tag.getInt("pathExit"));}
    @Override protected net.minecraft.nbt.CompoundTag getUpdateNBT(){return writeNBT();}
    @Override protected void handleUpdateNBT(net.minecraft.nbt.CompoundTag tag){readNBT(tag);}

    @RegisterTile("heat_exchanger_channel")
    public static final RegisterTile.Producer<HeatExchangerChannelTile> SUPPLIER = new RegisterTile.Producer<>(HeatExchangerChannelTile::new);

    public HeatExchangerChannelTile(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state) {
        super(tileEntityTypeIn, pos, state);
        CONDENSER = state.getBlock() == HeatExchangerCondenserChannelBlock.INSTANCE;
    }
}
