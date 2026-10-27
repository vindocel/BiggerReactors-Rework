package biggerreactorsrework.gametest;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.Config;
import biggerreactorsrework.core.util.HeatBody;
import biggerreactorsrework.multiblocks.heatexchanger.HeatExchangerCapacity;
import biggerreactorsrework.multiblocks.reactor.simulation.*;
import biggerreactorsrework.multiblocks.reactor.simulation.cpu.TimeSlicedReactorSimulation;
import biggerreactorsrework.multiblocks.reactor.util.ReactorTransitionTank;
import biggerreactorsrework.registries.*;
import biggerreactorsrework.util.FluidTransitionTank;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

/** Numerical A/B fixture read from New World's reactor at (-36,72,107), 2026-10-25.
 * No level changes, no balance changes, no infinite primary coolant supply.
 */
@GameTestHolder(BiggerReactors.modid)
@PrefixGameTestTemplate(false)
public final class CoolingCircuitComparisonGameTests {
    private static final int WARM=160000,WINDOW=20000;

    private static TimeSlicedReactorSimulation reactor() {
        return reactor(4);
    }
    private static TimeSlicedReactorSimulation reactor(int height) {
        var d=new SimulationDescription();d.setSize(4,height,4);
        d.setDefaultIModeratorProperties(ReactorModeratorRegistry.blockModeratorProperties(Blocks.AIR));
        for(int x=0;x<4;x++)for(int z=0;z<4;z++) {
            if((x+z)%2==0)d.setControlRod(x,z,true);
            else if(height==3 || x!=2 || z!=1)for(int y=0;y<height;y++)d.setManifold(x,y,z,true);
        }
        var s=new TimeSlicedReactorSimulation(d,new SimulationConfiguration(293.15,false));
        for(int x=0;x<4;x++)for(int z=0;z<4;z++)if(s.controlRodAt(x,z)!=null)s.controlRodAt(x,z).setInsertion(x==2 && z==0?45:42);
        return s;
    }

    private static final class Exchanger {
        final FluidTransitionTank primary=new FluidTransitionTank(true),water=new FluidTransitionTank(false);
        final biggerreactorsrework.multiblocks.heatexchanger.HeatExchangerBackpressure pressure=new biggerreactorsrework.multiblocks.heatexchanger.HeatExchangerBackpressure(primary,water);
        final HeatBody hot=new HeatBody(),cold=new HeatBody(),air=new HeatBody(),ambient=new HeatBody();
        final HeatExchangerCapacity.Geometry g;
        Exchanger(HeatExchangerCapacity.Geometry g) {
            this.g=g;var cfg=Config.CONFIG.HeatExchanger;
            primary.perSideCapacity=g.condenser()*cfg.ChannelTankVolumePerBlock;water.perSideCapacity=g.evaporator()*cfg.ChannelTankVolumePerBlock;
            hot.setRfPerKelvin(g.condenser()*cfg.ChannelFEPerKelvinUnitVolume);cold.setRfPerKelvin(g.evaporator()*cfg.ChannelFEPerKelvinUnitVolume);air.setRfPerKelvin(g.airCapacity());ambient.setInfinite(true);
            for(var body:new HeatBody[]{hot,cold,air,ambient})body.setTemperature(g.ambientTemperature());
        }
        void tick() {
            var cfg=Config.CONFIG.HeatExchanger;
            pressure.tick();
            water.fill(Fluids.WATER,DataComponentPatch.EMPTY,water.perSideCapacity,false);
            primary.transferWith(hot,g.condenser()*cfg.ChannelInternalSurfaceArea);
            hot.transferWith(air,g.condenserAir());hot.transferWith(cold,g.contact());
            cold.transferWith(air,g.evaporatorAir());water.transferWith(cold,g.evaporator()*cfg.ChannelInternalSurfaceArea);
            air.transferWith(ambient,g.ambientArea());
            water.drain(Integer.MAX_VALUE,FluidAction.EXECUTE);
        }
    }

    private static HeatExchangerCapacity.Geometry straight(int channels) {
        var cfg=Config.CONFIG.HeatExchanger;
        return new HeatExchangerCapacity.Geometry(channels,channels,channels*cfg.ChannelFEPerKelvinMetreSquared,
            (3*channels+2)*cfg.AirFEPerKelvinMetreSquared,(3*channels+2)*cfg.AirFEPerKelvinMetreSquared,
            (16*channels+64)*cfg.AmbientFEPerKelvinMetreSquared,(16*(channels+2)-2*channels)*cfg.AirFEPerKelvinUnitVolume,293.15);
    }

    private static Map<String,Object> measure(GameTestHelper h,String name,HeatExchangerCapacity.Geometry geometry) {
        return measure(h,name,geometry,reactor(),FluidTransitionRegistry.liquidTransition(geometry==null?Fluids.WATER:biggerreactorsrework.fluids.LiquidObsidian.INSTANCE));
    }
    private static Map<String,Object> measure(GameTestHelper h,String name,HeatExchangerCapacity.Geometry geometry,TimeSlicedReactorSimulation s,FluidTransitionRegistry.FluidTransition profile) {
        var tank=new ReactorTransitionTank(s.coolantTank());var hx=geometry==null?null:new Exchanger(geometry);
        tank.fill(profile.liquids.getFirst(),DataComponentPatch.EMPTY,tank.perSideCapacity(),false);
        long charge=tank.liquidAmount();
        h.assertTrue((s.fuelTank().capacity()==128000 && s.manifoldContacts()==80 && charge==280000) ||
            (s.fuelTank().capacity()==96000 && s.manifoldContacts()==72 && charge==240000),"fixture no longer matches the user's reactor");
        double burn=0,steam=0,conversion=0,fuelTemperature=0,fraction=0,early=0,late=0;long primaryTransfer=0;
        for(int t=0;t<WARM+2*WINDOW;t++) {
            s.fuelTank().extractWaste(Long.MAX_VALUE,false);s.fuelTank().insertFuel(Long.MAX_VALUE,false);
            if(hx==null)tank.fill(Fluids.WATER,DataComponentPatch.EMPTY,charge,false);
            s.tick(true);
            h.assertTrue(s.fuelHeat()<=Config.CONFIG.Reactor.MaxFuelTemperature+1e-5,"backpressure bypassed the reactor thermal ceiling");
            long currentSteam;
            if(hx==null) {currentSteam=s.coolantTank().transitionedLastTick();tank.drain(Integer.MAX_VALUE,FluidAction.EXECUTE);}
            else {
                int moved=hx.primary.fill(tank.getFluidInTank(1),FluidAction.EXECUTE);tank.drain(moved,FluidAction.EXECUTE);
                hx.tick();currentSteam=hx.water.transitionedLastTick();
                int returned=tank.fill(hx.primary.getFluidInTank(1),FluidAction.EXECUTE);hx.primary.drain(returned,FluidAction.EXECUTE);
                long stock=tank.liquidAmount()+tank.vaporAmount()+hx.primary.fluidAmountInTank(0)+hx.primary.fluidAmountInTank(1);
                h.assertTrue(stock==charge,"closed primary circuit lost or created coolant");
                if(t>=WARM)primaryTransfer+=returned;
            }
            if(t>=WARM) {
                burn+=s.fuelTank().burnedLastTick();steam+=currentSteam;conversion+=s.coolantTank().transitionedLastTick();fuelTemperature+=s.fuelHeat();fraction+=s.burnFraction();
                if(t<WARM+WINDOW)early+=currentSteam;else late+=currentSteam;
            }
        }
        var row=new LinkedHashMap<String,Object>();double ticks=2d*WINDOW;
        row.put("case",name);row.put("steam_mB_t",steam/ticks);row.put("fuel_mB_t",burn/ticks);row.put("steam_per_mB_fuel",steam/burn);
        row.put("reactor_conversion_mB_t",conversion/ticks);row.put("fuel_temperature_K",fuelTemperature/ticks);row.put("automatic_burn_fraction",fraction/ticks);
        row.put("first_window_steam_mB_t",early/WINDOW);row.put("last_window_steam_mB_t",late/WINDOW);
        double drift=Math.abs(late-early)/Math.max(1,late);row.put("relative_window_drift",drift);
        h.assertTrue(drift<.005,"comparison has not reached stable operation: "+name);
        if(hx!=null) {
            row.put("primary_return_mB_t",primaryTransfer/ticks);row.put("primary_total_mB",charge);row.put("reactor_cold_mB",tank.liquidAmount());row.put("reactor_hot_mB",tank.vaporAmount());
            row.put("exchanger_hot_mB",hx.primary.fluidAmountInTank(0));row.put("exchanger_cold_mB",hx.primary.fluidAmountInTank(1));
            row.put("hx_channels_each",geometry.condenser());row.put("hx_direct_contact_faces",geometry.contact()/Config.CONFIG.HeatExchanger.ChannelFEPerKelvinMetreSquared);
            var potential=HeatExchangerCapacity.measure(geometry,profile,FluidTransitionRegistry.liquidTransition(Fluids.WATER));
            row.put("hx_ideal_primary_mB_t",potential.converted());row.put("hx_ideal_steam_mB_t",potential.burn());
        }
        return row;
    }

    /** Added 2026-10-26: identical geometry, charge and controls for every coolant tier. */
    @GameTest(template="empty_7x7x7",timeoutTicks=300)
    public static void coolantProgressionUsesTheSameExchanger(GameTestHelper h)throws Exception {
        var registry=net.minecraft.core.registries.BuiltInRegistries.FLUID;
        var sodiumKey=net.minecraft.resources.ResourceLocation.parse("mekanism:sodium");
        var cryoKey=net.minecraft.resources.ResourceLocation.parse("ad_astra:cryo_fuel");
        if(!registry.containsKey(sodiumKey) || !registry.containsKey(cryoKey)){h.succeed();return;}
        var cfg=Config.CONFIG.HeatExchanger;
        var g=new HeatExchangerCapacity.Geometry(19,19,18*cfg.ChannelFEPerKelvinMetreSquared,
            60*cfg.AirFEPerKelvinMetreSquared,60*cfg.AirFEPerKelvinMetreSquared,
            238*cfg.AmbientFEPerKelvinMetreSquared,16*cfg.AirFEPerKelvinUnitVolume,293.15);
        var rows=new ArrayList<Map<String,Object>>();
        rows.add(measure(h,"water_direct",null,reactor(3),FluidTransitionRegistry.liquidTransition(Fluids.WATER)));
        for(var fluid:List.of(biggerreactorsrework.fluids.LiquidObsidian.INSTANCE,registry.get(cryoKey),registry.get(sodiumKey)))
            rows.add(measure(h,registry.getKey(fluid).toString(),g,reactor(3),FluidTransitionRegistry.liquidTransition(fluid)));
        java.nio.file.Files.writeString(java.nio.file.Path.of("coolant-progression.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(rows));
        BiggerReactors.LOGGER.info("Same-setup coolant progression: {}",rows);
        double water=((Number)rows.getFirst().get("steam_mB_t")).doubleValue();
        var sodium=rows.getLast();
        for(int i=1;i<3;i++) {
            var row=rows.get(i);
            h.assertTrue(((Number)row.get("steam_mB_t")).doubleValue()>water,"closed-loop coolant failed to outperform direct water");
            for(var metric:List.of("steam_mB_t","steam_per_mB_fuel"))
                h.assertTrue(((Number)sodium.get(metric)).doubleValue()>((Number)row.get(metric)).doubleValue(),
                    "sodium failed same-setup progression for "+metric+" against "+row.get("case"));
        }
        double obs=((Number)rows.get(1).get("steam_per_mB_fuel")).doubleValue();
        double cryo=((Number)rows.get(2).get("steam_per_mB_fuel")).doubleValue();
        h.assertTrue(obs>cryo,"obsidian circuit lost its fuel efficiency advantage");
        h.succeed();
    }

    @GameTest(template="empty_7x7x7",timeoutTicks=300)
    public static void compareSavedReactorWaterAndClosedObsidianLoop(GameTestHelper h)throws Exception {
        var cfg=Config.CONFIG.HeatExchanger;var cases=new ArrayList<Map<String,Object>>();
        cases.add(measure(h,"water_direct",null));
        cases.add(measure(h,"obsidian_saved_5x5x5_gap",new HeatExchangerCapacity.Geometry(5,5,0,22*cfg.AirFEPerKelvinMetreSquared,22*cfg.AirFEPerKelvinMetreSquared,150*cfg.AmbientFEPerKelvinMetreSquared,115*cfg.AirFEPerKelvinUnitVolume,293.15)));
        cases.add(measure(h,"obsidian_adjacent_7x4x4",straight(5)));
        cases.add(measure(h,"obsidian_adjacent_41x4x4",straight(39)));
        double baseline=((Number)cases.getFirst().get("steam_mB_t")).doubleValue(),fuelEfficiency=((Number)cases.getFirst().get("steam_per_mB_fuel")).doubleValue();
        for(var row:cases){row.put("steam_vs_water",((Number)row.get("steam_mB_t")).doubleValue()/baseline);row.put("steam_per_fuel_vs_water",((Number)row.get("steam_per_mB_fuel")).doubleValue()/fuelEfficiency);}
        for(int i:new int[]{2,3}) {
            h.assertTrue(i<cases.size()-1 || ((Number)cases.get(i).get("steam_vs_water")).doubleValue()>1,"adjacent obsidian circuit does not outperform direct water with the same rod settings");
            h.assertTrue(((Number)cases.get(i).get("steam_per_fuel_vs_water")).doubleValue()>1,"adjacent obsidian circuit uses more fuel per steam than direct water");
        }
        var report=new LinkedHashMap<String,Object>();report.put("source_world","run/saves/New World");report.put("source_reactor_min",List.of(-36,72,107));
        report.put("external_dimensions",List.of(6,6,6));report.put("fuel_rod_blocks",32);report.put("manifolds",28);report.put("contact_faces",80);
        report.put("rod_insertions","42% on seven rods; 45% at local x=2,z=0");report.put("warmup_ticks",WARM);report.put("sample_ticks",2*WINDOW);
        report.put("conditions","Full fuel; water supplied externally; steam removed freely; closed finite primary charge 280000 mB; no pipe rate limits; no turbine; native production simulations and registry profiles");report.put("cases",cases);
        java.nio.file.Files.writeString(java.nio.file.Path.of("cooling-circuit-comparison.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(report));
        BiggerReactors.LOGGER.info("Cooling circuit A/B comparison: {}",cases);
        h.succeed();
    }
    @GameTest(template="empty_7x7x7",timeoutTicks=300)
    public static void obsidianAdvantageAlsoHoldsWithMoreAdjacentChannels(GameTestHelper h) {
        var key=net.minecraft.resources.ResourceLocation.parse("ad_astra:cryo_fuel");
        if(!net.minecraft.core.registries.BuiltInRegistries.FLUID.containsKey(key)){h.succeed();return;}
        var geometry=straight(39);
        var obs=measure(h,"obsidian_39",geometry,reactor(3),FluidTransitionRegistry.liquidTransition(biggerreactorsrework.fluids.LiquidObsidian.INSTANCE));
        var cryo=measure(h,"cryo_39",geometry,reactor(3),FluidTransitionRegistry.liquidTransition(net.minecraft.core.registries.BuiltInRegistries.FLUID.get(key)));
        double ratio=((Number)obs.get("steam_per_mB_fuel")).doubleValue()/((Number)cryo.get("steam_per_mB_fuel")).doubleValue();
        BiggerReactors.LOGGER.info("Second obsidian/Cryo reference: ratio={} obsidian={} cryo={}",ratio,obs,cryo);
        h.assertTrue(ratio>1,"second exchanger lost the obsidian advantage: "+ratio);h.succeed();
    }

}
