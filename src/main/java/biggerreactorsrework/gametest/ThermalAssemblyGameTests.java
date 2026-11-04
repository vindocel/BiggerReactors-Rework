package biggerreactorsrework.gametest;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.calculator.design.*;
import biggerreactorsrework.multiblocks.reactor.tiles.ReactorBaseTile;
import biggerreactorsrework.multiblocks.reactor.state.ReactorActivity;
import biggerreactorsrework.multiblocks.heatexchanger.blocks.*;
import biggerreactorsrework.multiblocks.heatexchanger.tiles.*;
import biggerreactorsrework.util.GuideStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.gametest.*;
import java.util.*;
import static biggerreactorsrework.core.multiblock.IAssemblyStateBlock.ASSEMBLED;

// Added 2026-11-02: real block-built machines, independent of description-only numerical fixtures.
public final class ThermalAssemblyGameTests {
    @GameTest(template="empty_64x48x40",timeoutTicks=300)
    public static void assembledReactorSizeAndMissingManifoldMatrix(GameTestHelper h) {
        int[][] sizes={{3,3,3},{5,5,5},{9,9,9},{4,3,7},{3,1,3}};
        var layouts=new ArrayList<MachineLayout>();var origins=new ArrayList<BlockPos>();
        for(int i=0;i<sizes.length;i++) {
            int[] size=sizes[i];var design=new ReactorDesign(size[0],size[1],size[2],ReactorDesign.Pattern.CHECKER,null,42);
            var layout=design.layout(List.of(new MachineLayout.Port(new BlockPos(design.width()-1,1,1),MachineLayout.Role.REACTOR_HOT_OUT),
                new MachineLayout.Port(new BlockPos(design.width()-1,1,size[2]),MachineLayout.Role.REACTOR_COLD_IN)));
            var origin=new BlockPos(1+(i%4)*14,1,1+(i/4)*16);layouts.add(layout);origins.add(origin);
            layout.blocks().forEach((pos,state)->h.setBlock(pos.offset(origin),state));
            if(i==4)h.setBlock(origin.offset(2,1,3),Blocks.AIR);
        }
        h.startSequence().thenWaitUntil(()->{
            for(int i=0;i<layouts.size();i++)for(var e:layouts.get(i).blocks().entrySet())if(e.getValue().hasProperty(ASSEMBLED) && !(i==4 && e.getKey().equals(new BlockPos(2,1,3))))
                h.assertTrue(h.getBlockState(e.getKey().offset(origins.get(i))).getValue(ASSEMBLED),"Thermal matrix reactor did not assemble");
        }).thenExecute(()->{
            for(int i=0;i<layouts.size();i++) {
                var port=layouts.get(i).ports().getFirst();
                var c=((ReactorBaseTile)GameTestSupport.be(h,port.pos().offset(origins.get(i)))).controller();
                c.setAllControlRodLevels(42);c.setActive(ReactorActivity.ACTIVE);
                double burned=0;long steam=0;
                for(int tick=0;tick<100;tick++) {
                    c.simulation().fuelTank().extractWaste(Long.MAX_VALUE,false);c.simulation().fuelTank().insertFuel(Long.MAX_VALUE,false);
                    c.coolantTank().fill(Fluids.WATER,DataComponentPatch.EMPTY,Long.MAX_VALUE,false);c.tick();
                    burned+=c.simulation().fuelTank().burnedLastTick();steam+=c.simulation().coolantTank().transitionedLastTick();
                    ThermalModelGameTests.balance(h,c.simulation());
                    ThermalModelGameTests.close(h,i==4?.75:1,c.simulation().thermal().structuralEfficiency(),"Block-built geometry disagrees");
                    c.coolantTank().drain(c.coolantTank().vaporType(),c.coolantTank().fluidComponentsInTank(1),Long.MAX_VALUE,false);
                }
                double expected=burned*12500*(i==4?.75:1);
                h.assertTrue(Math.abs(expected-steam)<=100.001,"Built reactor conversion differs beyond one-mB resolution per tick");
                BiggerReactors.LOGGER.info("Thermal v1 built reactor {}x{}x{}: {} mB/t fuel, {} mB/t steam, {} structure",sizes[i][0],sizes[i][1],sizes[i][2],burned/100,steam/100d,c.simulation().thermal().structuralEfficiency());
                var before=c.simulation().fuelTank().usableFuel();
                c.onDisassembled();c.onStateTransition(biggerreactorsrework.core.multiblock.validated.IValidatedMultiblock.AssemblyState.DISASSEMBLED,biggerreactorsrework.core.multiblock.validated.IValidatedMultiblock.AssemblyState.ASSEMBLED);
                ThermalModelGameTests.close(h,before,c.simulation().fuelTank().usableFuel(),"Reassembly duplicated a fuel fraction");
            }
        }).thenSucceed();
    }

    @GameTest(template="empty_64x48x40",timeoutTicks=300)
    public static void multipleIndependentExchangerPathsShareBuffersAndRenderEveryChannel(GameTestHelper h) {
        var base=new ExchangerDesign(4,2).layout();int width=base.width(),height=base.height(),depth=7;
        var parts=new LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        for(int x=0;x<width;x++)for(int y=0;y<height;y++)for(int z=0;z<depth;z++)
            if(x==0 || x==width-1 || y==0 || y==height-1 || z==0 || z==depth-1)parts.put(new BlockPos(x,y,z),HeatExchangerCasingBlock.INSTANCE.defaultBlockState());
        for(int offset:new int[]{0,3})for(var e:base.blocks().entrySet())
            if(e.getValue().getBlock() instanceof HeatExchangerCondenserChannelBlock || e.getValue().getBlock() instanceof HeatExchangerEvaporatorChannelBlock || e.getValue().getBlock() instanceof HeatExchangerFluidPortBlock)
                parts.put(e.getKey().offset(0,0,offset),e.getValue());
        BlockPos terminal=new BlockPos(1,1,0);parts.put(terminal,HeatExchangerTerminalBlock.INSTANCE.defaultBlockState());GuideStructures.connect(parts);
        parts.forEach(h::setBlock);
        h.startSequence().thenWaitUntil(()->h.assertTrue(h.getBlockState(terminal).getValue(ASSEMBLED),"Multiple paths did not assemble"))
            .thenExecute(()->{
                var c=((HeatExchangerBaseTile)GameTestSupport.be(h,terminal)).controller();
                h.assertTrue(c.fluidPorts().size()==8,"Extra ports were discarded");
                for(var channels:List.of(c.condenserChannels,c.evaporatorChannels)) {
                    var indices=new HashSet<Integer>();for(var channel:channels) {
                        h.assertTrue(channel.coolantPathLength==channels.size() && channel.coolantAnchor!=null,"Additional path missing render data");indices.add(channel.coolantPathIndex);
                    }
                    h.assertTrue(indices.size()==channels.size(),"Multiple paths duplicate render indices");
                }
                var player=h.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE);
                var absolute=h.absolutePos(terminal);player.setPos(absolute.getX()+.5,absolute.getY()+1,absolute.getZ()+.5);
                var menu=new biggerreactorsrework.multiblocks.heatexchanger.containers.HeatExchangerTerminalContainer(17,absolute,player);player.containerMenu=menu;
                var request=new biggerreactorsrework.core.serialization.DataCompound();request.put("menu",17);request.put("position",absolute.asLong());request.put("request","balancedOutputs");request.put("data",true);
                biggerreactorsrework.core.client.gui.GuiSync.handleServerRequest(request,player);
                h.assertTrue(c.balancedOutputs,"Validated terminal request did not reach controller");
                request.put("data","invalid");biggerreactorsrework.core.client.gui.GuiSync.handleServerRequest(request,player);
                h.assertTrue(c.balancedOutputs,"Malformed balanced command changed state");
                var n=c.write();c.read(n);
                h.assertTrue(c.balancedOutputs,"Balanced mode did not persist");
                h.assertTrue(c.thermalLinks>0,"Assembled links not cached");
            }).thenSucceed();
    }
}
