package biggerreactorsrework.gametest;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.Config;
import biggerreactorsrework.core.util.HeatBody;
import biggerreactorsrework.fluids.LiquidObsidian;
import biggerreactorsrework.multiblocks.heatexchanger.HeatExchangerBackpressure;
import biggerreactorsrework.multiblocks.reactor.simulation.*;
import biggerreactorsrework.multiblocks.reactor.simulation.cpu.TimeSlicedReactorSimulation;
import biggerreactorsrework.multiblocks.reactor.util.ReactorTransitionTank;
import biggerreactorsrework.registries.ReactorModeratorRegistry;
import biggerreactorsrework.util.FluidTransitionTank;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.*;
import java.nio.file.*;
import java.util.*;

/** Modified 2026-10-26: measured bottleneck experiments, actual production physics, no changes to player worlds. */
@GameTestHolder(BiggerReactors.modid)
@PrefixGameTestTemplate(false)
public final class OutputBottleneckStudyGameTests {
    private static final int STAGE=160000,SAMPLE=20000;
    private interface Machine {
        Reading tick(int extraction);
        long capacity();
    }
    private record Reading(long produced,long extracted,long stored,double limit,double burn,long primary,double temperature) { }
    private static final class Reactor implements Machine {
        final TimeSlicedReactorSimulation simulation;
        final ReactorTransitionTank tank;
        long supplied,converted,removed;
        Reactor() {
            var description=new SimulationDescription();description.setSize(4,3,4);
            description.setDefaultIModeratorProperties(ReactorModeratorRegistry.blockModeratorProperties(Blocks.AIR));
            for(int x=0;x<4;x++)for(int z=0;z<4;z++) {
                if((x+z)%2==0)description.setControlRod(x,z,true);
                else for(int y=0;y<3;y++)description.setManifold(x,y,z,true);
            }
            simulation=new TimeSlicedReactorSimulation(description,new SimulationConfiguration(293.15,false));
            for(int x=0;x<4;x++)for(int z=0;z<4;z++)if(simulation.controlRodAt(x,z)!=null)
                simulation.controlRodAt(x,z).setInsertion(x==2 && z==0?45:42);
            tank=new ReactorTransitionTank(simulation.coolantTank());
        }
        public long capacity(){return tank.perSideCapacity();}
        public Reading tick(int extraction) {
            simulation.fuelTank().extractWaste(Long.MAX_VALUE,false);simulation.fuelTank().insertFuel(Long.MAX_VALUE,false);
            supplied+=tank.fill(LiquidObsidian.INSTANCE,DataComponentPatch.EMPTY,tank.perSideCapacity(),false);
            simulation.tick(true);
            long produced=simulation.coolantTank().transitionedLastTick();converted+=produced;
            long extracted=tank.drain(extraction,FluidAction.EXECUTE).getAmount();removed+=extracted;
            if(supplied-converted!=tank.liquidAmount() || converted-removed!=tank.vaporAmount())throw new AssertionError("reactor fluid balance failed");
            if(simulation.fuelHeat()>Config.CONFIG.Reactor.MaxFuelTemperature+1e-5)throw new AssertionError("reactor exceeded thermal ceiling");
            return new Reading(produced,extracted,tank.vaporAmount(),simulation.outputLimit(),simulation.fuelTank().burnedLastTick(),0,simulation.fuelHeat());
        }
    }
    private static final class Exchanger implements Machine {
        final FluidTransitionTank primary=new FluidTransitionTank(true),water=new FluidTransitionTank(false);
        final HeatExchangerBackpressure pressure=new HeatExchangerBackpressure(primary,water);
        final HeatBody hot=new HeatBody(),cold=new HeatBody(),air=new HeatBody(),ambient=new HeatBody();
        long hotSupplied,coldRemoved,waterSupplied,steamRemoved;
        Exchanger() {
            var cfg=Config.CONFIG.HeatExchanger;primary.perSideCapacity=water.perSideCapacity=19*cfg.ChannelTankVolumePerBlock;
            hot.setRfPerKelvin(19*cfg.ChannelFEPerKelvinUnitVolume);cold.setRfPerKelvin(19*cfg.ChannelFEPerKelvinUnitVolume);
            air.setRfPerKelvin(16*cfg.AirFEPerKelvinUnitVolume);ambient.setInfinite(true);
            for(var b:List.of(hot,cold,air,ambient))b.setTemperature(293.15);
        }
        public long capacity(){return water.perSideCapacity;}
        public Reading tick(int extraction) {
            var cfg=Config.CONFIG.HeatExchanger;
            hotSupplied+=primary.fill(biggerreactorsrework.fluids.HeatedCoolant.INSTANCE,biggerreactorsrework.registries.CoolantComponents.heated(biggerreactorsrework.registries.FluidTransitionRegistry.liquidTransition(biggerreactorsrework.fluids.LiquidObsidian.INSTANCE)),primary.perSideCapacity,false);
            waterSupplied+=water.fill(Fluids.WATER,DataComponentPatch.EMPTY,water.perSideCapacity,false);
            pressure.tick();double before=hot.rf()+cold.rf()+air.rf();
            double heatIn=-primary.transferWith(hot,19*cfg.ChannelInternalSurfaceArea);
            hot.transferWith(air,60*cfg.AirFEPerKelvinMetreSquared);
            hot.transferWith(cold,18*cfg.ChannelFEPerKelvinMetreSquared);
            cold.transferWith(air,60*cfg.AirFEPerKelvinMetreSquared);
            double heatOut=water.transferWith(cold,19*cfg.ChannelInternalSurfaceArea);
            double ambientLoss=air.transferWith(ambient,238*cfg.AmbientFEPerKelvinMetreSquared);
            double difference=hot.rf()+cold.rf()+air.rf()-before-(heatIn-heatOut-ambientLoss);
            if(Math.abs(difference)>1e-5)throw new AssertionError("exchanger heat balance failed: "+difference);
            long produced=water.transitionedLastTick(),converted=primary.transitionedLastTick();
            coldRemoved+=primary.drain(Integer.MAX_VALUE,FluidAction.EXECUTE).getAmount();
            long extracted=water.drain(extraction,FluidAction.EXECUTE).getAmount();steamRemoved+=extracted;
            if(hotSupplied!=coldRemoved+primary.fluidAmountInTank(0)+primary.fluidAmountInTank(1))throw new AssertionError("primary balance failed");
            if(waterSupplied!=steamRemoved+water.fluidAmountInTank(0)+water.fluidAmountInTank(1))throw new AssertionError("secondary balance failed");
            return new Reading(produced,extracted,water.fluidAmountInTank(1),pressure.limit(),0,converted,hot.temperature());
        }
    }
    private static Map<String,Object> stage(Machine machine,String name,int extraction,StringBuilder timeline) {
        double produced=0,extracted=0,burn=0,limit=0,primary=0,temperature=0;
        Reading last=null;
        for(int tick=0;tick<STAGE;tick++) {
            last=machine.tick(extraction);
            if(tick%200==0)timeline.append(String.format(Locale.ROOT,"%s,%d,%d,%d,%d,%.8f,%.8f,%d,%.5f%n",name,tick,last.produced,last.extracted,last.stored,last.limit,last.burn,last.primary,last.temperature));
            if(tick>=STAGE-SAMPLE){produced+=last.produced;extracted+=last.extracted;burn+=last.burn;limit+=last.limit;primary+=last.primary;temperature+=last.temperature;}
        }
        var row=new LinkedHashMap<String,Object>();row.put("stage",name);row.put("ticks",STAGE);row.put("sample_ticks",SAMPLE);
        row.put("extraction_limit_mB_t",extraction);row.put("production_mB_t",produced/SAMPLE);row.put("extracted_mB_t",extracted/SAMPLE);
        row.put("fuel_mB_t",burn/SAMPLE);row.put("operation_limit",limit/SAMPLE);row.put("primary_conversion_mB_t",primary/SAMPLE);
        row.put("temperature_K",temperature/SAMPLE);row.put("output_stored_mB",last.stored);row.put("output_capacity_mB",machine.capacity());
        return row;
    }
    private static double value(Map<String,Object> row,String key){return ((Number)row.get(key)).doubleValue();}
    private static void run(GameTestHelper h,Machine machine,String name)throws Exception {
        var csv=new StringBuilder("stage,tick,produced_mB,extracted_mB,stored_mB,operation_limit,fuel_mB,primary_converted_mB,temperature_K\n");
        var rows=new ArrayList<Map<String,Object>>();
        var free=stage(machine,"free",Integer.MAX_VALUE,csv);rows.add(free);
        int restricted=Math.max(1,(int)(value(free,"production_mB_t")*.5));
        var limited=stage(machine,"half_extraction",restricted,csv);rows.add(limited);
        var closed=stage(machine,"blocked",0,csv);rows.add(closed);
        var recovered=stage(machine,"recovered",Integer.MAX_VALUE,csv);rows.add(recovered);
        var json=new LinkedHashMap<String,Object>();json.put("machine",name);json.put("stages",rows);
        json.put("conditions","Native reactor/thermal bodies/tanks/controllers; externally supplied inputs; no shared player level; each stage 160000 ticks, final 20000 sampled; restricted extraction fixed at half baseline; primary cold output freely drained");
        Files.writeString(Path.of("output-bottleneck-"+name+".json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(json));
        Files.writeString(Path.of("output-bottleneck-"+name+".csv"),csv);
        BiggerReactors.LOGGER.info("Output bottleneck study {}: {}",name,rows);
        h.assertTrue(value(free,"production_mB_t")>0,"reference produced nothing");
        h.assertTrue(value(limited,"output_stored_mB")>0 && value(limited,"operation_limit")<.999,"restricted extraction did not build backlog or limit operation");
        h.assertTrue(Math.abs(value(limited,"production_mB_t")-restricted)<Math.max(1,restricted*.01),"production did not settle to restricted extraction");
        h.assertTrue(value(closed,"production_mB_t")==0 && value(closed,"output_stored_mB")==machine.capacity(),"fully blocked output did not stop production");
        h.assertTrue(value(closed,"fuel_mB_t")==0 && value(closed,"primary_conversion_mB_t")==0,"blocked output continued burning/converting primary coolant");
        h.assertTrue(Math.abs(value(recovered,"production_mB_t")/value(free,"production_mB_t")-1)<.01,"production did not recover after extraction resumed");
        if(machine instanceof Reactor)h.assertTrue(value(limited,"fuel_mB_t")<value(free,"fuel_mB_t"),"restricted reactor did not reduce fuel burn");
        if(machine instanceof Exchanger)h.assertTrue(value(limited,"primary_conversion_mB_t")<value(free,"primary_conversion_mB_t"),"restricted steam output did not reduce primary cooling");
        h.succeed();
    }
    @GameTest(template="empty_7x7x7",timeoutTicks=400)
    public static void measureRestrictedLavaExtractionFromObsidianReactor(GameTestHelper h)throws Exception {run(h,new Reactor(),"reactor_obsidian");}
    @GameTest(template="empty_7x7x7",timeoutTicks=400)
    public static void measureRestrictedSteamExtractionFromLavaExchanger(GameTestHelper h)throws Exception {run(h,new Exchanger(),"exchanger_lava");}
}
