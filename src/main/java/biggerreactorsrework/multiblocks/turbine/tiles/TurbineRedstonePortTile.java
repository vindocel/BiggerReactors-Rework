package biggerreactorsrework.multiblocks.turbine.tiles;

import biggerreactorsrework.core.client.gui.GuiSync;
import biggerreactorsrework.core.multiblock.common.IEventMultiblock;
import biggerreactorsrework.core.multiblock.common.ITickablePartsMultiblock;
import biggerreactorsrework.core.multiblock.validated.IValidatedMultiblock.AssemblyState;
import biggerreactorsrework.core.registry.RegisterTile;
import biggerreactorsrework.core.util.BlockStates;
import biggerreactorsrework.multiblocks.turbine.containers.TurbineRedstonePortContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Map;

/** Native turbine commands and inclusive threshold sensors. */
public final class TurbineRedstonePortTile extends TurbineBaseTile implements MenuProvider,
        ITickablePartsMultiblock.Tickable, IEventMultiblock.AssemblyStateTransition, GuiSync.IGUIPacket {
    @RegisterTile("turbine_redstone_port")
    public static final RegisterTile.Producer<TurbineRedstonePortTile> SUPPLIER = new RegisterTile.Producer<>(TurbineRedstonePortTile::new);
    public enum Mode { DISABLED, ACTIVITY, COILS, FLOW, RPM, ENERGY, VAPOR, LIQUID }
    public int mode;
    /** 0 level, 1 pulse, 2 above, 3 below, 4 between. */
    public int behavior;
    public double low, high;
    private boolean powered, initialized;
    private boolean emitting;
    private Direction facing;
    public TurbineRedstonePortTile(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type,pos,state); }
    public boolean emitting(Direction side) { return facing!=null && side.getOpposite()==facing && emitting; }
    public static boolean inRange(double value, int behavior, double low, double high) {
        if(!Double.isFinite(value))return false;
        return switch(behavior){case 2->value>=low;case 3->value<=low;case 4->value>=low&&value<=high;default->false;};
    }
    public boolean configure(Object data) {
        if(!(data instanceof Map<?,?> map) || !(map.get("mode") instanceof Integer m) || !(map.get("behavior") instanceof Integer b)
                || !(map.get("low") instanceof Number a) || !(map.get("high") instanceof Number z)) return false;
        double l=a.doubleValue(), h=z.doubleValue();
        if(m<0||m>=Mode.values().length||b<0||b>4||!Double.isFinite(l)||!Double.isFinite(h)||l<0||h<0)return false;
        if(m>0&&m<=3&&b>1 || m>=4&&b<2 || b==4&&h<l || m==3&&(l>100||h>100))return false;
        mode=m;behavior=b;low=l;high=h;initialized=false;setChanged();return true;
    }
    @Override public void runRequest(String request,Object data){if(request.equals("configure"))configure(data);}
    @Override public void preTick(){
        if(level==null||facing==null||nullableController()==null)return;
        boolean next=level.hasSignal(worldPosition.relative(facing),facing);
        boolean changed=!initialized||next!=powered, rising=initialized&&next&&!powered;
        var sim=controller().simulation();boolean output=false;
        if(mode>0&&mode<=3){
            boolean execute=behavior==0?changed:rising;
            if(execute){
                switch(Mode.values()[mode]){
                    case ACTIVITY->controller().setActive(behavior==0?next:!sim.active());
                    case COILS->controller().setCoilEngaged(behavior==0?next:!sim.coilEngaged());
                    case FLOW->sim.setNominalFlowRate((long)(sim.flowRateLimit()*(behavior==1||next?low:high)/100.0));
                    default->{}
                }
                controller().dirty();
            }
        } else if(mode>=4){
            double value=switch(Mode.values()[mode]){
                case RPM->sim.RPM();
                case ENERGY->sim.battery().capacity()>0?100.0*sim.battery().stored()/sim.battery().capacity():0;
                case VAPOR->sim.fluidTank().vaporAmount();
                case LIQUID->sim.fluidTank().liquidAmount();
                default->0;
            };
            output=inRange(value,behavior,low,high);
        }
        initialized=true;powered=next;setOutput(output);
        setLit(output || (mode>0 && mode<=3 && next));
    }
    private void setLit(boolean lit) {
        var property=biggerreactorsrework.multiblocks.turbine.blocks.TurbineRedstonePort.IS_LIT;
        if(level!=null && getBlockState().getValue(property)!=lit)
            level.setBlockAndUpdate(worldPosition,getBlockState().setValue(property,lit));
    }
    private void setOutput(boolean value){
        if(emitting==value)return;emitting=value;
        if(level!=null){level.updateNeighborsAt(worldPosition,getBlockState().getBlock());if(facing!=null)level.updateNeighborsAt(worldPosition.relative(facing),getBlockState().getBlock());}
    }
    @Override public void postTick(){}
    @Override public void onAssemblyStateTransition(AssemblyState oldState,AssemblyState next){
        if(next!=AssemblyState.ASSEMBLED){setOutput(false);setLit(false);}
        facing=next==AssemblyState.ASSEMBLED?getBlockState().getValue(BlockStates.FACING):null;
        initialized=false;
    }
    @Override public Component getDisplayName(){return Component.translatable("block.biggerreactorsrework.turbine_redstone_port");}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inv,Player player){return new TurbineRedstonePortContainer(id,worldPosition,player);}
    @Override public Map<?,?> write(){return Map.of("mode",mode,"behavior",behavior,"low",low,"high",high,"appearancePos",writeAppearance());}
    @Override public void read(Map<?,?> data){
        readAppearance(data);
        if(data.get("mode") instanceof Integer m && m>=0&&m<Mode.values().length)mode=m;
        if(data.get("behavior") instanceof Integer b && b>=0&&b<=4)behavior=b;
        if(data.get("low") instanceof Number n)low=n.doubleValue();
        if(data.get("high") instanceof Number n)high=n.doubleValue();
    }
    @Override protected CompoundTag writeNBT(){var n=super.writeNBT();n.putInt("sensor",mode);n.putInt("behavior",behavior);n.putDouble("low",low);n.putDouble("high",high);return n;}
    @Override protected void readNBT(CompoundTag n){super.readNBT(n);configure(Map.of("mode",n.getIntOr("sensor", 0),"behavior",n.getIntOr("behavior", 0),"low",n.getDoubleOr("low", 0D),"high",n.getDoubleOr("high", 0D)));}
}
