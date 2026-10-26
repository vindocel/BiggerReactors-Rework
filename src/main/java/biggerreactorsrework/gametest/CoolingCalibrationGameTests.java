package biggerreactorsrework.gametest;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.multiblocks.reactor.simulation.*;
import biggerreactorsrework.multiblocks.turbine.simulation.modern.ModernTurbineSimulation;
import biggerreactorsrework.registries.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;
import org.joml.Vector4i;
import java.util.*;

/** Reproducible numerical baseline, real registry and real turbine. Runs without an external mod. */
@GameTestHolder(BiggerReactors.modid)
@PrefixGameTestTemplate(false)
public final class CoolingCalibrationGameTests {
    private static SimulationDescription layout(boolean passive,ReactorModeratorRegistry.ModeratorProperties solid){
        var d=new SimulationDescription();d.setSize(3,3,3);d.setDefaultIModeratorProperties(new ReactorModeratorRegistry.ModeratorProperties(.1,.25,1.1,.05));
        for(int x=0;x<3;x++)for(int z=0;z<3;z++){
            if((x+z)%2==0)d.setControlRod(x,z,true);
            else for(int y=0;y<3;y++){if(passive)d.setModeratorProperties(x,y,z,solid);else d.setManifold(x,y,z,true);}
        }
        return d;
    }
    @GameTest(template="empty_7x7x7",timeoutTicks=200)
    public static void reportAndVerifyCoolantProgression(GameTestHelper h)throws Exception{
        var results=new ArrayList<Map<String,Object>>();double best=0;String bestId="";
        var distinct=new HashSet<List<Double>>();
        for(var entry:ReactorModeratorRegistry.snapshot().entrySet()){
            var p=entry.getValue();if(!distinct.add(List.of(p.absorption,p.heatEfficiency,p.moderation,p.heatConductivity)))continue;
            var result=ReactorCapacity.measure(layout(true,p),new SimulationConfiguration(293.15,true),null);
            double ratio=result.power()/Math.max(1e-9,result.burn());
            if(ratio>best){best=ratio;bestId=BuiltInRegistries.BLOCK.getKey(entry.getKey()).toString();}
        }
        results.add(Map.of("system","best_solid","moderator",bestId,"FE_per_mB_fuel",best));
        var water=FluidTransitionRegistry.liquidTransition(net.minecraft.world.level.material.Fluids.WATER);
        for(var profile:FluidTransitionRegistry.transitions()){
            var reactor=ReactorCapacity.measure(layout(false,null),new SimulationConfiguration(293.15,false),profile);
            var turbine=new ModernTurbineSimulation();turbine.resize(5,13,5);
            var rotor=new ArrayList<Vector4i>();for(int i=0;i<10;i++)rotor.add(new Vector4i(2,2,2,2));for(int i=0;i<2;i++)rotor.add(new Vector4i());
            turbine.setRotorConfiguration(rotor);
            var coil=TurbineCoilRegistry.getCoilData(Blocks.IRON_BLOCK);
            for(int y=0;y<2;y++)for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)if(x!=0 || z!=0)turbine.setCoilData(x,z,coil);
            turbine.updateInternalValues();turbine.setNominalFlowRate(turbine.flowRateLimit());turbine.setActive(true);turbine.setVentState(biggerreactorsrework.multiblocks.turbine.state.VentState.ALL);
            var actual=new biggerreactorsrework.multiblocks.reactor.simulation.cpu.TimeSlicedReactorSimulation(layout(false,null),new SimulationConfiguration(293.15,false));
            var primary=new biggerreactorsrework.multiblocks.reactor.util.ReactorTransitionTank(actual.coolantTank());
            primary.fill(profile.liquids.getFirst(),net.minecraft.core.component.DataComponentPatch.EMPTY,primary.perSideCapacity(),false);
            int channels=profile==water?4:(int)Math.ceil(3600/Math.max(50,profile.boilingPoint-water.boilingPoint));
            var hotTank=new biggerreactorsrework.util.FluidTransitionTank(true);hotTank.perSideCapacity=channels*10000;
            var waterTank=new biggerreactorsrework.util.FluidTransitionTank(false);waterTank.perSideCapacity=channels*10000;
            var c=new biggerreactorsrework.core.util.HeatBody();var e=new biggerreactorsrework.core.util.HeatBody();var air=new biggerreactorsrework.core.util.HeatBody();var ambient=new biggerreactorsrework.core.util.HeatBody();
            c.setRfPerKelvin(channels*1000);e.setRfPerKelvin(channels*1000);air.setRfPerKelvin((16*(channels+2)-2*channels)*10);ambient.setInfinite(true);
            for(var body:new biggerreactorsrework.core.util.HeatBody[]{c,e,air,ambient})body.setTemperature(293.15);
            double generated=0,burned=0,steamTotal=0,hotTotal=0;
            for(int tick=0;tick<180000;tick++){
                actual.fuelTank().extractWaste(Long.MAX_VALUE,false);actual.fuelTank().insertFuel(Long.MAX_VALUE,false);actual.tick(true);
                var target=profile==water?(biggerreactorsrework.core.fluids.IFluidTankHandler)turbine.fluidTank():hotTank;
                int moved=target.fill(primary.getFluidInTank(1),net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                primary.drain(moved,net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                long currentSteam=actual.coolantTank().transitionedLastTick();
                if(profile!=water){
                    waterTank.fill(net.minecraft.world.level.material.Fluids.WATER,net.minecraft.core.component.DataComponentPatch.EMPTY,channels*10000,false);
                    hotTank.transferWith(c,channels*80);c.transferWith(air,(3*channels+2)*.5);c.transferWith(e,channels*50);e.transferWith(air,(3*channels+2)*.5);waterTank.transferWith(e,channels*80);air.transferWith(ambient,(16*channels+64)*.2);
                    int back=primary.fill(hotTank.getFluidInTank(1),net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);hotTank.drain(back,net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                    int steamMoved=turbine.fluidTank().fill(waterTank.getFluidInTank(1),net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);waterTank.drain(steamMoved,net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);currentSteam=waterTank.transitionedLastTick();
                }else primary.fill(net.minecraft.world.level.material.Fluids.WATER,net.minecraft.core.component.DataComponentPatch.EMPTY,primary.perSideCapacity(),false);
                turbine.tick();turbine.battery().extract(Long.MAX_VALUE);
                if(tick>=160000){generated+=turbine.FEGeneratedLastTick();burned+=actual.fuelTank().burnedLastTick();steamTotal+=currentSteam;hotTotal+=actual.coolantTank().transitionedLastTick();}
            }
            double steam=steamTotal/20000;
            double bestGeneration=0;int bestCoils=0;
            for(int count=1;count<=48;count++){
                var candidate=new ModernTurbineSimulation();candidate.resize(5,13,5);candidate.setRotorConfiguration(rotor);
                var bestCoil=TurbineCoilRegistry.getCoilData(biggerreactorsrework.blocks.materials.MaterialBlock.LUDICRITE);
                int added=0;
                for(int y=0;y<2;y++)for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)if((x!=0 || z!=0) && added++<count)candidate.setCoilData(x,z,bestCoil);
                candidate.updateInternalValues();candidate.setNominalFlowRate((long)steam);
                var study=candidate.capacityStudy().get();
                if(study.power()>bestGeneration){bestGeneration=study.power();bestCoils=count;}
            }
            var output=new biggerreactorsrework.core.util.Statistics.Potential(0,0,bestGeneration);
            reactor=new biggerreactorsrework.core.util.Statistics.Potential(burned/20000,hotTotal/20000,0);
            double ratio=output.power()/Math.max(1e-9,reactor.burn());
            results.add(Map.of("system",BuiltInRegistries.FLUID.getKey(profile.liquids.getFirst()).toString(),"burn_mB_t",reactor.burn(),"hot_mB_t",reactor.converted(),"steam_mB_t",steam,"FE_t",output.power(),"FE_per_mB_fuel",ratio,"hx_channels_per_type",channels,"turbine_coils",bestCoils));
        }
        java.nio.file.Path out=java.nio.file.Path.of("cooling-calibration.json");java.nio.file.Files.writeString(out,new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(results));
        BiggerReactors.LOGGER.info("Cooling calibration: {}",results);
        h.assertTrue(best>0 && results.size()>=3,"calibration produced no usable outputs");
        var ratios=new HashMap<String,Double>();
        for(var row:results)ratios.put(String.valueOf(row.get("system")),((Number)row.get("FE_per_mB_fuel")).doubleValue());
        double w=ratios.get("minecraft:water"),o=ratios.get("biggerreactorsrework:liquid_obsidian");
        h.assertTrue(w>best,"water circuit must outperform the best tested solid moderator per fuel");
        h.assertTrue(o>w,"obsidian circuit must outperform water per fuel");
        if(ratios.containsKey("ad_astra:cryo_fuel"))h.assertTrue(ratios.get("ad_astra:cryo_fuel")>=o && ratios.get("ad_astra:cryo_fuel")<=o*1.10,"Cryo Fuel must remain near the obsidian tier");
        if(ratios.containsKey("mekanism:sodium"))h.assertTrue(ratios.get("mekanism:sodium")>Math.max(o,ratios.getOrDefault("ad_astra:cryo_fuel",o)),"sodium must be the highest tested tier");
        h.succeed();
    }
}
