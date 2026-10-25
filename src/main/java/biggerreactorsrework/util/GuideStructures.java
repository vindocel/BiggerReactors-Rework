package biggerreactorsrework.util;

import biggerreactorsrework.blocks.materials.MaterialBlock;
import biggerreactorsrework.multiblocks.reactor.blocks.*;
import biggerreactorsrework.multiblocks.turbine.blocks.*;
import biggerreactorsrework.multiblocks.heatexchanger.blocks.*;
import biggerreactorsrework.multiblocks.battery.blocks.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/** Example definitions shared by the book and formation tests. */
public final class GuideStructures {
    public record Example(String id, int width, int height, int depth, Map<BlockPos, BlockState> parts) { }
    private static void put(Map<BlockPos,BlockState> parts, int x,int y,int z,Block block) { parts.put(new BlockPos(x,y,z),block.defaultBlockState()); }
    private static Map<BlockPos,BlockState> shell(int w,int h,int d,Block casing) {
        var parts=new LinkedHashMap<BlockPos,BlockState>();
        for(int x=0;x<w;x++) for(int y=0;y<h;y++) for(int z=0;z<d;z++)
            if(x==0||x==w-1||y==0||y==h-1||z==0||z==d-1) put(parts,x,y,z,casing);
        return parts;
    }
    public static Example reactor(int size, boolean active) {
        var p=shell(size,size,size,ReactorCasing.INSTANCE);int c=size/2;
        for(int x=1;x<size-1;x++) for(int z=1;z<size-1;z++) for(int y=1;y<size-1;y++)
            put(p,x,y,z,x==c&&z==c ? ReactorFuelRod.INSTANCE : MaterialBlock.GRAPHITE);
        put(p,c,size-1,c,ReactorControlRod.INSTANCE);
        put(p,0,1,c,ReactorTerminal.INSTANCE);put(p,c,1,0,ReactorAccessPort.INSTANCE);
        put(p,size-1,1,c,active?ReactorCoolantPort.INSTANCE:ReactorPowerTap.INSTANCE);
        if(active) put(p,c,1,size-1,ReactorCoolantPort.INSTANCE);
        return new Example(active?"active":size==3?"minimal":"moderated",size,size,size,Collections.unmodifiableMap(p));
    }
    public static Example turbine() {
        var p=shell(5,7,5,TurbineCasing.INSTANCE);
        put(p,2,0,2,TurbineRotorBearing.INSTANCE);put(p,2,6,2,TurbineRotorBearing.INSTANCE);
        for(int y=1;y<=5;y++) {
            put(p,2,y,2,TurbineRotorShaft.INSTANCE);
            if(y<=3) {
                for(int i=1;i<=3;i++) if(i!=2) { put(p,i,y,2,TurbineRotorBlade.INSTANCE);put(p,2,y,i,TurbineRotorBlade.INSTANCE); }
            } else for(int x=1;x<=3;x++) for(int z=1;z<=3;z++) if(x!=2||z!=2) put(p,x,y,z,Blocks.IRON_BLOCK);
        }
        put(p,0,1,2,TurbineTerminal.INSTANCE);put(p,4,1,2,TurbinePowerTap.INSTANCE);
        put(p,2,1,0,TurbineFluidPort.INSTANCE);put(p,2,1,4,TurbineFluidPort.INSTANCE);
        return new Example("turbine",5,7,5,Collections.unmodifiableMap(p));
    }
    public static Example exchanger() {
        var p=shell(4,3,4,HeatExchangerCasingBlock.INSTANCE);
        for(int x=1;x<=2;x++) { put(p,x,1,1,HeatExchangerCondenserChannelBlock.INSTANCE);put(p,x,1,2,HeatExchangerEvaporatorChannelBlock.INSTANCE); }
        for(int x:new int[]{0,3}) for(int z:new int[]{1,2}) put(p,x,1,z,HeatExchangerFluidPortBlock.INSTANCE);
        put(p,1,1,0,HeatExchangerTerminalBlock.INSTANCE);
        connect(p);
        return new Example("exchanger",4,3,4,Collections.unmodifiableMap(p));
    }
    public static Example battery() {
        var p=shell(5,5,5,BatteryCasing.INSTANCE);
        for(int x=1;x<=3;x++) for(int y=1;y<=3;y++) for(int z=1;z<=3;z++) put(p,x,y,z,BatteryCell.INSTANCE);
        put(p,0,2,2,BatteryTerminal.INSTANCE);put(p,2,2,0,BatteryEnergyPort.INSTANCE);put(p,2,2,4,BatteryEnergyPort.INSTANCE);
        connect(p);
        return new Example("battery",5,5,5,Collections.unmodifiableMap(p));
    }
    /** Structure placement does not call player placement hooks. Encode the same texture connections. */
    private static void connect(Map<BlockPos,BlockState> p) {
        p.replaceAll((pos,state)-> {
            if (!(state.getBlock() instanceof biggerreactorsrework.core.modular.block.IConnectedTexture connected)) return state;
            var properties=java.util.Map.of(
                net.minecraft.core.Direction.UP,biggerreactorsrework.core.modular.block.IConnectedTexture.Module.TOP_CONNECTED_PROPERTY,
                net.minecraft.core.Direction.DOWN,biggerreactorsrework.core.modular.block.IConnectedTexture.Module.BOTTOM_CONNECTED_PROPERTY,
                net.minecraft.core.Direction.NORTH,biggerreactorsrework.core.modular.block.IConnectedTexture.Module.NORTH_CONNECTED_PROPERTY,
                net.minecraft.core.Direction.SOUTH,biggerreactorsrework.core.modular.block.IConnectedTexture.Module.SOUTH_CONNECTED_PROPERTY,
                net.minecraft.core.Direction.WEST,biggerreactorsrework.core.modular.block.IConnectedTexture.Module.WEST_CONNECTED_PROPERTY,
                net.minecraft.core.Direction.EAST,biggerreactorsrework.core.modular.block.IConnectedTexture.Module.EAST_CONNECTED_PROPERTY);
            for(var entry:properties.entrySet()) state=state.setValue(entry.getValue(),connected.connectToBlock(p.getOrDefault(pos.relative(entry.getKey()),Blocks.AIR.defaultBlockState()).getBlock()));
            return state;
        });
    }
    public static List<Example> all() { return List.of(reactor(3,false),reactor(5,false),reactor(5,true),turbine(),exchanger(),battery()); }
    private GuideStructures() { }
}
