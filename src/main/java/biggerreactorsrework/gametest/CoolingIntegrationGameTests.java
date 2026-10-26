package biggerreactorsrework.gametest;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.core.util.HeatBody;
import biggerreactorsrework.fluids.HeatedCoolant;
import biggerreactorsrework.registries.*;
import biggerreactorsrework.util.FluidTransitionTank;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.*;

/** Optional mods are not required; with them installed this exercises their actual fluid tank. */
@GameTestHolder(BiggerReactors.modid)
@PrefixGameTestTemplate(false)
public final class CoolingIntegrationGameTests {
    /** Added 2026-10-26: the precise sodium correction preserves existing inventories, not arbitrary profile edits. */
    @GameTest(template="empty_7x7x7")
    public static void sodiumTemperatureCorrectionPreservesSavedCoolant(GameTestHelper h) {
        var registry=BuiltInRegistries.FLUID;var key=ResourceLocation.parse("mekanism:sodium");
        if(!registry.containsKey(key)){h.succeed();return;}
        var profile=FluidTransitionRegistry.liquidTransition(registry.get(key));
        String old="mekanism:sodium@"+Integer.toHexString(java.util.Objects.hash(40d,473.15,4d,4d,.95));
        for(boolean condenser:new boolean[]{false,true}) {
            var tank=new FluidTransitionTank(condenser);tank.perSideCapacity=10000;
            tank.fill(condenser?profile.gases.getFirst():profile.liquids.getFirst(),DataComponentPatch.EMPTY,1000,false);
            tank.absorbRF(condenser?400.5:-400.5);
            var saved=tank.serializeNBT();saved.putString("profile",old);
            var restored=new FluidTransitionTank(condenser);restored.perSideCapacity=10000;restored.deserializeNBT(saved);
            h.assertTrue(!restored.hasUnresolvedProfile() && restored.activeTransition()==profile,"approved sodium correction quarantined stored fluid");
            h.assertTrue(restored.fluidAmountInTank(0)==990 && restored.fluidAmountInTank(1)==10,"sodium correction changed stored volumes");
            saved.putString("profile",old+"invalid");restored.deserializeNBT(saved);
            h.assertTrue(restored.hasUnresolvedProfile(),"unrelated profile edits were accepted");
        }
        h.succeed();
    }
    @GameTest(template="empty_7x7x7")
    public static void emptyGenericCoolantCanBeReplacedWithoutLosingDrainedIdentity(GameTestHelper h) {
        var key=ResourceLocation.parse("ad_astra:cryo_fuel");
        if(!BuiltInRegistries.FLUID.containsKey(key)){h.succeed();return;}
        var cryo=BuiltInRegistries.FLUID.get(key);var tank=new FluidTransitionTank(false);tank.perSideCapacity=10000;
        tank.fill(cryo,DataComponentPatch.EMPTY,2,false);tank.absorbRF(-60.5);
        var drained=tank.drain(2,IFluidHandler.FluidAction.EXECUTE);
        h.assertTrue(drained.getAmount()==2 && drained.getFluid()==HeatedCoolant.INSTANCE && CoolantComponents.resolve(drained.getComponentsPatch())!=null,"last extraction lost the heated coolant identity");
        h.assertTrue(tank.activeTransition()==null && tank.fluidComponentsInTank(1).isEmpty(),"empty generic coolant retained its identity");
        h.assertTrue(tank.fill(net.minecraft.world.level.material.Fluids.WATER,DataComponentPatch.EMPTY,1,false)==1,"generic thermal residue blocked a different coolant");
        h.succeed();
    }
    @GameTest(template="empty_7x7x7")
    public static void heatedCoolantIdentitySurvivesSaveAndExternalTank(GameTestHelper h){
        var cryo=ResourceLocation.parse("ad_astra:cryo_fuel");
        if(!BuiltInRegistries.FLUID.containsKey(cryo)){h.assertTrue(FluidTransitionRegistry.transitions().stream().noneMatch(t->t.genericHot()),"absent optional coolant leaked into registry");h.succeed();return;}
        var fluid=BuiltInRegistries.FLUID.get(cryo);var profile=FluidTransitionRegistry.liquidTransition(fluid);
        h.assertTrue(profile!=null && profile.genericHot(),"Cryo Fuel integration missing");
        var tank=new FluidTransitionTank(false);tank.perSideCapacity=10000;tank.fill(fluid,DataComponentPatch.EMPTY,1000,false);
        var heat=new HeatBody();heat.setRfPerKelvin(10000);heat.setTemperature(2500);tank.transferWith(heat,1000);
        var hot=tank.getFluidInTank(1);
        h.assertTrue(!hot.isEmpty() && hot.getFluid()==HeatedCoolant.INSTANCE,"wrong heated coolant");
        h.assertTrue(CoolantComponents.resolve(hot.getComponentsPatch())==profile,"hot identity cannot be resolved");
        var restored=new FluidTransitionTank(false);restored.perSideCapacity=10000;restored.deserializeNBT(tank.serializeNBT());
        h.assertTrue(restored.getFluidInTank(1).getComponentsPatch().equals(hot.getComponentsPatch()),"save discarded origin");
        var condenser=new FluidTransitionTank(true);condenser.perSideCapacity=10000;
        h.assertTrue(condenser.isFluidValid(0,hot),"capability rejected valid component-bearing coolant");
        h.assertTrue(condenser.fill(HeatedCoolant.INSTANCE,DataComponentPatch.EMPTY,1000,false)==0,"unidentified hot coolant accepted");
        var external=ResourceLocation.parse("mekanism:basic_fluid_tank");
        if(BuiltInRegistries.BLOCK.containsKey(external)){
            var pos=new BlockPos(2,2,2);h.setBlock(pos,BuiltInRegistries.BLOCK.get(external));
            IFluidHandler handler=h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,h.absolutePos(pos),Direction.UP);
            if(handler==null)handler=h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,h.absolutePos(pos),null);
            h.assertTrue(handler!=null,"Mekanism fluid tank capability missing");
            int accepted=handler.fill(hot,IFluidHandler.FluidAction.EXECUTE);
            h.assertTrue(accepted==hot.getAmount(),"Mekanism tank refused identified fluid: accepted="+accepted+", amount="+hot.getAmount()+", valid="+handler.isFluidValid(0,hot)+", capacity="+handler.getTankCapacity(0));
            var drained=handler.drain(accepted,IFluidHandler.FluidAction.EXECUTE);
            h.assertTrue(drained.getAmount()==accepted && drained.getComponentsPatch().equals(hot.getComponentsPatch()),"external transport lost amount or origin");
            hot=drained;
        }
        int accepted=condenser.fill(hot,IFluidHandler.FluidAction.EXECUTE);
        heat.setTemperature(293.15);condenser.transferWith(heat,1000);
        h.assertTrue(accepted>0 && condenser.fluidTypeInTank(1)==fluid && condenser.fluidAmountInTank(1)>0,"hot coolant did not return Cryo Fuel");
        h.succeed();
    }
    @GameTest(template="empty_7x7x7")
    public static void sodiumUsesNativeFluidPairWhenPresent(GameTestHelper h){
        var key=ResourceLocation.parse("mekanism:sodium");
        if(BuiltInRegistries.FLUID.containsKey(key)){
            var t=FluidTransitionRegistry.liquidTransition(BuiltInRegistries.FLUID.get(key));
            h.assertTrue(t!=null,"liquid sodium integration missing");
            h.assertTrue(BuiltInRegistries.FLUID.getKey(t.gases.getFirst()).equals(ResourceLocation.parse("mekanism:superheated_sodium")),"sodium output is not native liquid superheated sodium");
        }
        h.succeed();
    }
}
