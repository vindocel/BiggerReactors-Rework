package biggerreactorsrework.gametest;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.Config;
import biggerreactorsrework.core.util.HeatBody;
import biggerreactorsrework.fluids.*;
import biggerreactorsrework.registries.*;
import biggerreactorsrework.util.FluidTransitionTank;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

/** Modified 2026-10-27: equal hot volumes, not nuclear burn, define the approved steam yield. */
@GameTestHolder(BiggerReactors.modid)
@PrefixGameTestTemplate(false)
public final class HotCoolantYieldGameTests {
    private static FluidTransitionTank primary(FluidTransitionRegistry.FluidTransition profile) {
        var tank=new FluidTransitionTank(true);tank.perSideCapacity=10000;
        tank.fill(HeatedCoolant.INSTANCE,CoolantComponents.heated(profile),1000,false);return tank;
    }
    @GameTest(template="empty_7x7x7")
    public static void equalHotVolumeProducesTwentyPercentMoreSteam(GameTestHelper h) {
        var obs=FluidTransitionRegistry.liquidTransition(LiquidObsidian.INSTANCE);
        var cryoId=ResourceLocation.parse("ad_astra:cryo_fuel");
        // Core run verifies the native obsidian budget; optional run compares the real Cryo profile.
        var hot=primary(obs);var water=new FluidTransitionTank(false);water.perSideCapacity=20000;
        water.fill(Fluids.WATER,DataComponentPatch.EMPTY,20000,false);
        double heat=hot.absorbRF(1e9);water.absorbRF(-heat);
        h.assertTrue(hot.fluidAmountInTank(1)==1000 && hot.fluidAmountInTank(0)==0,"hot batch volume not conserved");
        h.assertTrue(heat==36000 && water.fluidAmountInTank(1)==9000,"1000 mB hot obsidian must yield 9000 mB ideal steam");
        if(BuiltInRegistries.FLUID.containsKey(cryoId)) {
            var cryo=FluidTransitionRegistry.liquidTransition(BuiltInRegistries.FLUID.get(cryoId));
            var coldWater=new FluidTransitionTank(false);coldWater.perSideCapacity=20000;coldWater.fill(Fluids.WATER,DataComponentPatch.EMPTY,20000,false);
            coldWater.absorbRF(-primary(cryo).absorbRF(1e9));
            h.assertTrue(coldWater.fluidAmountInTank(1)==7500,"Cryo budget changed");
            h.assertTrue(water.fluidAmountInTank(1)*5==coldWater.fluidAmountInTank(1)*6,"equal hot volumes do not yield exactly +20%");
        }
        h.succeed();
    }
    private static Map<String,Object> channelBatch(FluidTransitionRegistry.FluidTransition profile) {
        var cfg=Config.CONFIG.HeatExchanger;var hot=primary(profile);
        var water=new FluidTransitionTank(false);water.perSideCapacity=190000;
        var c=new HeatBody();var e=new HeatBody();
        c.setRfPerKelvin(19*cfg.ChannelFEPerKelvinUnitVolume);e.setRfPerKelvin(19*cfg.ChannelFEPerKelvinUnitVolume);
        c.setTemperature(373.15);e.setTemperature(373.15);double initial=c.rf()+e.rf();long steam=0;
        for(int tick=0;tick<200000;tick++) {
            water.fill(Fluids.WATER,DataComponentPatch.EMPTY,water.perSideCapacity,false);
            hot.transferWith(c,19*cfg.ChannelInternalSurfaceArea);
            c.transferWith(e,18*cfg.ChannelFEPerKelvinMetreSquared);
            water.transferWith(e,19*cfg.ChannelInternalSurfaceArea);
            steam+=water.drain(Integer.MAX_VALUE,FluidAction.EXECUTE).getAmount();
        }
        double retained=c.rf()+e.rf()-initial;
        return Map.of("source",BuiltInRegistries.FLUID.getKey(profile.liquids.getFirst()).toString(),"hot_consumed_mB",1000-hot.fluidAmountInTank(0),
            "cold_returned_mB",hot.fluidAmountInTank(1),"steam_mB",steam,"retained_channel_heat",retained,
            "declared_batch_heat",1000*profile.latentHeat,"channels_each",19,"contact_faces",18,"ambient_loss",0);
    }
    @GameTest(template="empty_7x7x7",timeoutTicks=200)
    public static void equalBatchNativeChannelsConserveHeatAndShowTheGain(GameTestHelper h)throws Exception {
        var rows=new ArrayList<Map<String,Object>>();
        var obs=FluidTransitionRegistry.liquidTransition(LiquidObsidian.INSTANCE);rows.add(channelBatch(obs));
        var id=ResourceLocation.parse("ad_astra:cryo_fuel");
        if(BuiltInRegistries.FLUID.containsKey(id))rows.add(channelBatch(FluidTransitionRegistry.liquidTransition(BuiltInRegistries.FLUID.get(id))));
        for(var row:rows) {
            long returned=((Number)row.get("cold_returned_mB")).longValue();double steam=((Number)row.get("steam_mB")).doubleValue();
            double heat=((Number)row.get("declared_batch_heat")).doubleValue();double retained=((Number)row.get("retained_channel_heat")).doubleValue();
            h.assertTrue(returned==1000,"native channels failed to return the complete batch");
            h.assertTrue(Math.abs(heat-(steam*4+retained))<4.01,"native channels created/lost unexplained heat");
        }
        if(rows.size()==2) {
            double a=((Number)rows.get(0).get("steam_mB")).doubleValue(),b=((Number)rows.get(1).get("steam_mB")).doubleValue();
            h.assertTrue(Math.abs(a-1.2*b)<=1.2,"equal batches lost the 20% steam advantage");
        }
        java.nio.file.Files.writeString(java.nio.file.Path.of("hot-coolant-yield.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(rows));
        BiggerReactors.LOGGER.info("Equal hot-batch native exchanger yield: {}",rows);h.succeed();
    }
}
