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
    public static void reactorBacklogDiscardsPaidHeatAndSurvivesSave(GameTestHelper h) {
        var s=reactor();var tank=new ReactorTransitionTank(s.coolantTank());
        tank.fill(Fluids.WATER,DataComponentPatch.EMPTY,tank.perSideCapacity(),false);
        s.coolantTank().insertVapor(tank.perSideCapacity());
        long before=s.fuelTank().fuel();for(int i=0;i<30;i++)s.tick(true);
        h.assertTrue(s.fuelTank().fuel()<before && s.coolantTank().transitionedLastTick()==0 && s.thermal().blockedOutput()>0,"Backlog failed to record paid loss");
        var saved=s.save();s.load(saved);double fuel=s.fuelTank().usableFuel();s.tick(false);
        h.assertTrue(s.fuelTank().usableFuel()==fuel && s.coolantTank().transitionedLastTick()==0,"Reload or inactive tick duplicated fuel/heat");
        tank.drain(Integer.MAX_VALUE,FluidAction.EXECUTE);s.tick(true);
        h.assertTrue(s.coolantTank().transitionedLastTick()>0,"Output did not resume immediately");h.succeed();
    }

    @GameTest(template="empty_7x7x7")
    public static void exchangerPartialStockDoesNotChangeFixedRecipe(GameTestHelper h) {
        var x=new Exchanger();var model=new biggerreactorsrework.thermal.ThermalExchanger();
        model.tick(x.hot,x.water,500,biggerreactorsrework.thermal.ThermalModel.Preset.DEFAULT);
        h.assertTrue(model.hotLastTick==500 && model.steamLastTick==1000,"Wrong initial fixed recipe");
        x.water.processThermalUnits(49000,2);x.water.fill(Fluids.WATER,DataComponentPatch.EMPTY,Long.MAX_VALUE,false);
        model.tick(x.hot,x.water,500,biggerreactorsrework.thermal.ThermalModel.Preset.DEFAULT);
        h.assertTrue(model.hotLastTick==500 && model.steamLastTick==1000,"Half-full output incorrectly imposed gradual throttling");h.succeed();
    }

    @GameTest(template="empty_7x7x7")
    public static void exchangerSteamBacklogAtomicallyBlocksBothConversions(GameTestHelper h) {
        var x=new Exchanger();var model=new biggerreactorsrework.thermal.ThermalExchanger();
        x.water.processThermalUnits(100000,2);long before=x.hot.fluidAmountInTank(0);
        model.tick(x.hot,x.water,500,biggerreactorsrework.thermal.ThermalModel.Preset.DEFAULT);
        h.assertTrue(x.hot.fluidAmountInTank(0)==before && model.hotLastTick==0 && model.steamLastTick==0,"Blocked secondary consumed primary");
        h.assertTrue(model.credit()==0,"Blocked exchanger stored productive credit");h.succeed();
    }
}
