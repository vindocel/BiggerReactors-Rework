package biggerreactorsrework.gametest;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.core.util.HeatBody;
import biggerreactorsrework.multiblocks.heatexchanger.HeatExchangerBackpressure;
import biggerreactorsrework.multiblocks.reactor.simulation.SimulationConfiguration;
import biggerreactorsrework.multiblocks.reactor.simulation.cpu.TimeSlicedReactorSimulation;
import biggerreactorsrework.multiblocks.reactor.util.ReactorTransitionTank;
import biggerreactorsrework.registries.FluidTransitionRegistry;
import biggerreactorsrework.util.FluidTransitionTank;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.*;

/** Modified 2026-10-26: real tanks and simulation, including transfer order and retained heat. */
@GameTestHolder(BiggerReactors.modid)
@PrefixGameTestTemplate(false)
public final class OutputBackpressureGameTests {
    private static TimeSlicedReactorSimulation reactor() {
        var d=OperationGameTests.description(3);
        for(int y=0;y<3;y++){d.setManifold(0,y,1,true);d.setManifold(2,y,1,true);d.setManifold(1,y,0,true);d.setManifold(1,y,2,true);}
        var sim=new TimeSlicedReactorSimulation(d,new SimulationConfiguration(293.15,false));
        sim.fuelTank().insertFuel(12000,false);return sim;
    }
    private static final class Exchanger {
        final FluidTransitionTank hot=new FluidTransitionTank(true),water=new FluidTransitionTank(false);
        final HeatExchangerBackpressure pressure=new HeatExchangerBackpressure(hot,water);
        Exchanger() {
            hot.perSideCapacity=water.perSideCapacity=100000;
            hot.fill(biggerreactorsrework.fluids.HeatedCoolant.INSTANCE,biggerreactorsrework.registries.CoolantComponents.heated(FluidTransitionRegistry.liquidTransition(biggerreactorsrework.fluids.LiquidObsidian.INSTANCE)),100000,false);
            water.fill(Fluids.WATER,DataComponentPatch.EMPTY,100000,false);
        }
    }
    @GameTest(template="empty_7x7x7",timeoutTicks=200)
    public static void reactorFullExtractionBatchesNeverReduceOperation(GameTestHelper h) {
        for(var profile:FluidTransitionRegistry.transitions())for(int period:new int[]{1,10,20})for(boolean after:new boolean[]{false,true}) {
            var s=reactor();var tank=new ReactorTransitionTank(s.coolantTank());
            long extracted=0,converted=0;
            for(int t=0;t<500;t++) {
                if(!after && t%period==0)extracted+=tank.drain(Integer.MAX_VALUE,FluidAction.EXECUTE).getAmount();
                tank.fill(profile.liquids.getFirst(),DataComponentPatch.EMPTY,tank.perSideCapacity(),false);
                s.tick(true);converted+=s.coolantTank().transitionedLastTick();
                if(after && t%period==period-1)extracted+=tank.drain(Integer.MAX_VALUE,FluidAction.EXECUTE).getAmount();
                h.assertTrue(s.outputLimit()==1,"healthy batch transfer reduced reactor output");
                h.assertTrue(converted==extracted+tank.vaporAmount(),"batch transfer created or lost reactor product");
            }
        }
        h.succeed();
    }
    @GameTest(template="empty_7x7x7")
    public static void reactorBacklogLimitsBurnAndConversionAndSurvivesSave(GameTestHelper h) {
        var s=reactor();var tank=new ReactorTransitionTank(s.coolantTank());
        tank.fill(Fluids.WATER,DataComponentPatch.EMPTY,tank.perSideCapacity(),false);
        s.coolantTank().insertVapor(tank.perSideCapacity()/2);
        for(int t=0;t<20;t++)s.tick(false);
        h.assertTrue(Math.abs(s.outputLimit()-.5)<1e-9,"half retained stock did not halve the output limit");
        var saved=s.save();s.load(saved);s.tick(true);
        h.assertTrue(s.outputLimit()<=.5 && s.burnFraction()<=.5,"reload lost backlog or burn ignored limit");
        s.coolantTank().insertVapor(tank.perSideCapacity()-tank.vaporAmount());
        saved=s.save();saved.put("fuelHeat",3000d);s.load(saved);
        long fuel=s.fuelTank().fuel();double temperature=s.fuelHeat();
        for(int t=0;t<30;t++)s.tick(true);
        h.assertTrue(s.outputLimit()==0 && s.coolantTank().transitionedLastTick()==0,"stored heat bypassed full output");
        h.assertTrue(s.fuelTank().fuel()==fuel,"full hot output consumed fuel");
        h.assertTrue(s.fuelHeat()>293.15 && s.fuelHeat()<=temperature,"blocked operation erased or created stored heat");
        tank.drain(Integer.MAX_VALUE,FluidAction.EXECUTE);
        for(int t=0;t<20;t++){s.tick(true);tank.drain(Integer.MAX_VALUE,FluidAction.EXECUTE);}
        h.assertTrue(s.outputLimit()>.999,"reactor did not recover after extraction resumed");
        h.succeed();
    }
    @GameTest(template="empty_7x7x7")
    public static void exchangerOnlySteamStockTriggersGradualReduction(GameTestHelper h) {
        var x=new Exchanger();
        x.hot.absorbRF(75000*FluidTransitionRegistry.liquidTransition(biggerreactorsrework.fluids.LiquidObsidian.INSTANCE).latentHeat);
        for(int t=0;t<30;t++)x.pressure.tick();
        h.assertTrue(x.pressure.limit()==1,"cold output, hot input or full water input imposed a penalty");
        x.water.absorbRF(-50000*FluidTransitionRegistry.liquidTransition(Fluids.WATER).latentHeat);
        for(int t=0;t<20;t++)x.pressure.tick();
        h.assertTrue(Math.abs(x.pressure.limit()-.5)<1e-9,"retained steam did not reduce exchanger limit");
        var saved=x.pressure.save();var restored=new Exchanger();restored.pressure.load(saved);
        restored.water.absorbRF(-50000*FluidTransitionRegistry.liquidTransition(Fluids.WATER).latentHeat);
        restored.pressure.tick();h.assertTrue(Math.abs(restored.pressure.limit()-.5)<1e-9,"exchanger reload forgot its limit");
        h.succeed();
    }
    @GameTest(template="empty_7x7x7")
    public static void exchangerSteamBacklogLimitsBothConversionsWithoutLosingHeat(GameTestHelper h) {
        long[] converted=new long[2],evaporated=new long[2];
        for(int mode=0;mode<2;mode++) {
            var x=new Exchanger();
            if(mode==1)x.water.absorbRF(-50000*FluidTransitionRegistry.liquidTransition(Fluids.WATER).latentHeat);
            // Hold the same input stock in both cases to isolate the output limiter.
            x.water.fill(Fluids.WATER,DataComponentPatch.EMPTY,100000,false);
            for(int t=0;t<20;t++)x.pressure.tick();
            var body=new HeatBody();body.setRfPerKelvin(10000);body.setTemperature(300);
            double before=body.rf();x.hot.transferWith(body,80);
            converted[mode]=x.hot.transitionedLastTick();
            h.assertTrue(Math.abs(body.rf()-before-x.hot.rfTransferredLastTick())<1.01,"primary throttling lost heat");
            body.setTemperature(900);before=body.rf();x.water.transferWith(body,80);
            h.assertTrue(Math.abs(before-body.rf()-x.water.rfTransferredLastTick())<1.01,"steam throttling lost heat");
            long waterConverted=x.water.transitionedLastTick();
            evaporated[mode]=waterConverted;
            if(mode==0)h.assertTrue(waterConverted>0,"reference exchanger produced no steam");
        }
        h.assertTrue(Math.abs(converted[1]-.5*converted[0])<=1,"steam backlog did not limit primary cooling linearly");
        h.assertTrue(Math.abs(evaporated[1]-.5*evaporated[0])<=1,"steam backlog did not limit secondary evaporation linearly");
        var full=new Exchanger();full.water.absorbRF(-400000);full.pressure.tick();
        var body=new HeatBody();body.setRfPerKelvin(10000);body.setTemperature(300);
        full.hot.transferWith(body,80);
        h.assertTrue(full.hot.transitionedLastTick()==0 && body.temperature()==300,"full steam output still cooled primary");
        body.setTemperature(900);full.water.transferWith(body,80);
        h.assertTrue(full.water.transitionedLastTick()==0 && body.temperature()==900,"full steam output still produced steam");
        h.succeed();
    }
}
