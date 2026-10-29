package biggerreactorsrework.gametest;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.core.util.HeatBody;
import biggerreactorsrework.multiblocks.heatexchanger.HeatExchangerCapacity;
import biggerreactorsrework.multiblocks.heatexchanger.tiles.HeatExchangerBaseTile;
import biggerreactorsrework.registries.FluidTransitionRegistry;
import biggerreactorsrework.util.FluidTransitionTank;
import biggerreactorsrework.util.GuideStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.gametest.*;
import static biggerreactorsrework.core.multiblock.IAssemblyStateBlock.ASSEMBLED;

@GameTestHolder(BiggerReactors.modid)
@PrefixGameTestTemplate(false)
public final class HeatExchangerCoolingGameTests {
    /** Added 2026-10-26: sparse stocks and abrupt drains must still cover the full pipe path. */
    @GameTest(template="empty_7x7x7")
    public static void sparseFluidStocksFillTheWholeVisualCircuit(GameTestHelper h) {
        var tank=new FluidTransitionTank(false);tank.perSideCapacity=1000000;
        tank.fill(Fluids.WATER,DataComponentPatch.EMPTY,2,false);tank.absorbRF(-secondaryHeat(1));
        var half=biggerreactorsrework.core.fluids.FluidRenderSnapshot.capture(tank);
        h.assertTrue(half.inputFill()==.5 && half.outputFill()==.5,"small stocks disappeared during render quantization");
        tank.drain(1,net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        var cold=biggerreactorsrework.core.fluids.FluidRenderSnapshot.capture(tank);
        h.assertTrue(cold.inputFill()==1 && cold.outputFill()==0,"remaining input did not occupy space freed by output");
        tank.absorbRF(-secondaryHeat(1));
        var hot=biggerreactorsrework.core.fluids.FluidRenderSnapshot.capture(tank);
        h.assertTrue(hot.inputFill()==0 && hot.outputFill()==1,"remaining output did not fill the circuit");
        tank.drain(1,net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        var empty=biggerreactorsrework.core.fluids.FluidRenderSnapshot.capture(tank);
        h.assertTrue(empty.inputFill()==0 && empty.outputFill()==0,"empty circuit retained phantom fluid");
        for(double[] stocks:new double[][]{{.8,.2},{.08,.2},{0,.2},{.08,0},{.000001,.000002},{1,1}})
            for(int i=0;i<7;i++) {
                var a=biggerreactorsrework.core.util.FluidPath.span(stocks[0],stocks[1],i,7,false);
                var b=biggerreactorsrework.core.util.FluidPath.span(stocks[0],stocks[1],i,7,true);
                h.assertTrue(a.start()==0 && b.end()==1 && Math.abs(a.end()-b.start())<1e-6,"abrupt stock drop left a gap in the path");
                for(var entry:net.minecraft.core.Direction.values())for(var exit:net.minecraft.core.Direction.values())if(entry!=exit) {
                    var input=biggerreactorsrework.core.util.FluidPath.boxes(entry,exit,a);
                    var output=biggerreactorsrework.core.util.FluidPath.boxes(entry,exit,b);
                    double filled=input.stream().mapToDouble(HeatExchangerCoolingGameTests::volume).sum()+output.stream().mapToDouble(HeatExchangerCoolingGameTests::volume).sum();
                    h.assertTrue(Math.abs(filled-channelArea())<1e-6,"pipe orientation or elbow left an unfilled volume");
                    for(var x:input)for(var y:output)h.assertTrue(overlap(x,y)<1e-8,"phases overlap after a stock change");
                }
            }
        h.succeed();
    }
    @GameTest(template="empty_7x7x7")
    public static void pipeFillFollowsLengthInEveryDirection(GameTestHelper h) {
        for(var entry:net.minecraft.core.Direction.values())for(var exit:net.minecraft.core.Direction.values())if(entry!=exit){
            var a=biggerreactorsrework.core.util.FluidPath.boxes(entry,exit,new biggerreactorsrework.core.util.FluidPath.Span(0,.6f));
            var b=biggerreactorsrework.core.util.FluidPath.boxes(entry,exit,new biggerreactorsrework.core.util.FluidPath.Span(.6f,1));
            // Modified 2026-10-29: the model opening spans pixels 2..14, with 0.02 px clearance on each side.
            for(var box:java.util.stream.Stream.concat(a.stream(),b.stream()).toList()) {
                float[] lower={box.x0(),box.y0(),box.z0()},upper={box.x1(),box.y1(),box.z1()};
                for(var axis:net.minecraft.core.Direction.Axis.values()) if(axis!=entry.getAxis() && axis!=exit.getAxis()) {
                    int i=switch(axis){case X->0;case Y->1;case Z->2;};
                    h.assertTrue(Math.abs(lower[i]-2.02/16)<1e-6 && Math.abs(upper[i]-13.98/16)<1e-6,"fluid does not meet the channel's inner frame");
                }
            }
            h.assertTrue(Math.abs(a.stream().mapToDouble(HeatExchangerCoolingGameTests::volume).sum()-.6*channelArea())<1e-6,"input volume depends on pipe orientation");
            h.assertTrue(Math.abs(b.stream().mapToDouble(HeatExchangerCoolingGameTests::volume).sum()-.4*channelArea())<1e-6,"output volume depends on pipe orientation");
            for(var x:a)for(var y:b)h.assertTrue(overlap(x,y)<1e-8,"two fluids overlap at an elbow");
        }
        for(int i=0;i<9;i++) {
            var steam=biggerreactorsrework.core.util.FluidPath.span(.95,.05,i,10,true);
            h.assertTrue(steam.start()==steam.end(),"small steam stock was repeated in every block");
        }
        var tip=biggerreactorsrework.core.util.FluidPath.span(.95,.05,9,10,true);
        h.assertTrue(tip.start()==.5f && tip.end()==1,"steam does not occupy the outlet tip");
        h.succeed();
    }
    private static double volume(biggerreactorsrework.core.util.FluidPath.Box b){return (b.x1()-b.x0())*(b.y1()-b.y0())*(b.z1()-b.z0());}
    private static double channelArea(){return Math.pow((12-.04)/16,2);}
    private static double overlap(biggerreactorsrework.core.util.FluidPath.Box a,biggerreactorsrework.core.util.FluidPath.Box b){return Math.max(0,Math.min(a.x1(),b.x1())-Math.max(a.x0(),b.x0()))*Math.max(0,Math.min(a.y1(),b.y1())-Math.max(a.y0(),b.y0()))*Math.max(0,Math.min(a.z1(),b.z1())-Math.max(a.z0(),b.z0()));}
    @GameTest(template="empty_7x7x7",timeoutTicks=150)
    public static void savedHeatSurvivesReassembly(GameTestHelper h) {
        GuideStructures.exchanger().parts().forEach(h::setBlock);
        var pos=new BlockPos(1,1,0);
        h.startSequence().thenWaitUntil(()->h.assertTrue(h.getBlockState(pos).getValue(ASSEMBLED),"exchanger not assembled"))
            .thenExecute(()->{
                var c=((HeatExchangerBaseTile)h.getBlockEntity(pos)).controller();
                c.condenserHeatBody.setTemperature(900);c.evaporatorHeatBody.setTemperature(450);c.airHeatBody.setTemperature(500);
                var saved=c.write();c.read(saved);c.onReassembled();
                h.assertTrue(c.condenserHeatBody.temperature()==900,"reassembly discarded primary heat");
                h.assertTrue(c.evaporatorHeatBody.temperature()==450,"reassembly discarded secondary heat");
                h.assertTrue(c.airHeatBody.temperature()==500,"reassembly discarded internal heat");
                var channel=c.evaporatorChannels.iterator().next();
                h.assertTrue(channel.coolantAnchor!=null,"channel has no aggregate render anchor");
                h.assertTrue(channel.coolantPathLength==c.evaporatorChannels.size(),"render bar does not cover the whole circuit");
                var indices=new java.util.HashSet<Integer>();for(var part:c.evaporatorChannels)indices.add(part.coolantPathIndex);
                h.assertTrue(indices.size()==c.evaporatorChannels.size(),"two channels share the same position in the render bar");
                var inlet=(biggerreactorsrework.multiblocks.heatexchanger.tiles.HeatExchangerFluidPortTile)h.getBlockEntity(new BlockPos(0,1,2));
                var old=new java.util.HashMap<BlockPos,Integer>();for(var part:c.evaporatorChannels)old.put(part.getBlockPos(),part.coolantPathIndex);
                c.setInletPort(inlet,!inlet.isInlet());
                for(var part:c.evaporatorChannels)h.assertTrue(part.coolantPathIndex==c.evaporatorChannels.size()-1-old.get(part.getBlockPos()),"changing inlet did not reverse the measuring bar");
                h.assertFalse(channel.getBlockState().hasProperty(ASSEMBLED),"regression fixture must be a channel without ASSEMBLED");
                for(var part:c.condenserChannels)h.assertTrue(part.renderTerminal()!=null,"condenser rendering cannot resolve assembled terminal");
                for(var part:c.evaporatorChannels)h.assertTrue(part.renderTerminal()!=null,"evaporator rendering cannot resolve assembled terminal");
                var previousAnchor=channel.coolantAnchor;channel.coolantAnchor=null;
                h.assertTrue(channel.renderTerminal()==null,"channel with no anchor must not render");channel.coolantAnchor=previousAnchor;
                long capacity=c.evaporatorTank.perSideCapacity;
                c.evaporatorTank.fill(Fluids.WATER,DataComponentPatch.EMPTY,capacity,false);
                c.evaporatorTank.absorbRF(-secondaryHeat(capacity/2));
                var view=biggerreactorsrework.core.fluids.FluidRenderSnapshot.capture(c.evaporatorTank);
                h.assertTrue(view.inputFill()==.5 && view.outputFill()==.5,"50/50 channel view does not share the space");
                h.assertTrue(biggerreactorsrework.core.fluids.FluidRenderSnapshot.load(view.save()).equals(view),"channel view changed on save/sync");
                h.assertTrue(c.evaporatorTank.fluidAmountInTank(0)+c.evaporatorTank.fluidAmountInTank(1)==capacity,"render snapshot mutated fluid stock");
            }).thenSucceed();
    }
    private static double secondaryHeat(long amount){return amount*FluidTransitionRegistry.liquidTransition(Fluids.WATER).latentHeat;}

    @GameTest(template="empty_7x7x7")
    public static void indirectExchangeStabilizesAndConservesPrimaryVolume(GameTestHelper h) {
        var cfg=biggerreactorsrework.Config.CONFIG.HeatExchanger;
        var geometry=new HeatExchangerCapacity.Geometry(39,39,0,79,79,79.6,4170,293.15);
        var primary=FluidTransitionRegistry.liquidTransition(biggerreactorsrework.fluids.LiquidObsidian.INSTANCE);
        var secondary=FluidTransitionRegistry.liquidTransition(Fluids.WATER);
        var potential=HeatExchangerCapacity.measure(geometry,primary,secondary);
        var hot=new FluidTransitionTank(true);var water=new FluidTransitionTank(false);
        hot.perSideCapacity=water.perSideCapacity=39*cfg.ChannelTankVolumePerBlock;
        HeatBody c=new HeatBody(),e=new HeatBody(),air=new HeatBody(),ambient=new HeatBody();
        c.setRfPerKelvin(39*cfg.ChannelFEPerKelvinUnitVolume);e.setRfPerKelvin(39*cfg.ChannelFEPerKelvinUnitVolume);air.setRfPerKelvin(4170);ambient.setInfinite(true);
        for(var b:new HeatBody[]{c,e,air,ambient})b.setTemperature(293.15);
        int flow=(int)Math.min(3076,potential.converted()*.5);
        long supplied=0,returned=0,early=0,late=0;
        for(int tick=0;tick<60000;tick++) {
            supplied+=hot.fill(biggerreactorsrework.fluids.HeatedCoolant.INSTANCE,biggerreactorsrework.registries.CoolantComponents.heated(FluidTransitionRegistry.liquidTransition(biggerreactorsrework.fluids.LiquidObsidian.INSTANCE)),flow,false);
            water.fill(Fluids.WATER,DataComponentPatch.EMPTY,water.perSideCapacity,false);
            hot.transferWith(c,39*cfg.ChannelInternalSurfaceArea);c.transferWith(air,geometry.condenserAir());c.transferWith(e,0);
            e.transferWith(air,geometry.evaporatorAir());water.transferWith(e,39*cfg.ChannelInternalSurfaceArea);air.transferWith(ambient,geometry.ambientArea());
            long amount=hot.drain(hot.fluidTypeInTank(1),hot.fluidComponentsInTank(1),Long.MAX_VALUE,false);returned+=amount;
            water.drain(water.fluidTypeInTank(1),water.fluidComponentsInTank(1),Long.MAX_VALUE,false);
            if(tick<2000)early+=amount;if(tick>=58000)late+=amount;
        }
        h.assertTrue(supplied==returned+hot.fluidAmountInTank(0)+hot.fluidAmountInTank(1),"primary coolant was lost or duplicated");
        h.assertTrue(c.temperature()>293.15 && e.temperature()>293.15,"channel temperatures did not warm up");
        h.assertTrue(Math.abs(late/2000d-flow)<Math.max(2,flow*.01),"oversized exchanger did not stabilize at incoming flow");
        h.assertTrue(potential.converted()>late/2000d,"full tank estimate must exceed partial supply throughput");
        BiggerReactors.LOGGER.info("Indirect HX warmup: first 2000 ticks={} mB/t; final 2000={} mB/t; ideal={} mB/t; residual={} mB",early/2000d,late/2000d,potential.converted(),hot.fluidAmountInTank(0));
        h.succeed();
    }
}
