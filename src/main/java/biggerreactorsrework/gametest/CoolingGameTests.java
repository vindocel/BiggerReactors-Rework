package biggerreactorsrework.gametest;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.core.util.HeatBody;
import biggerreactorsrework.fluids.LiquidObsidian;
import biggerreactorsrework.multiblocks.reactor.simulation.*;
import biggerreactorsrework.multiblocks.reactor.simulation.cpu.TimeSlicedReactorSimulation;
import biggerreactorsrework.multiblocks.reactor.util.ReactorTransitionTank;
import biggerreactorsrework.registries.*;
import biggerreactorsrework.util.FluidTransitionTank;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.gametest.*;

/** Real registered fluids and production simulation; no client or optional mod required. */
@GameTestHolder(BiggerReactors.modid)
@PrefixGameTestTemplate(false)
public final class CoolingGameTests {
    private static TimeSlicedReactorSimulation reactor(int sides,boolean strongSolid) {
        var d=OperationGameTests.description(3);
        for(int y=0;y<3;y++) {
            d.setManifold(0,y,1,true);
            if(sides==4){d.setManifold(2,y,1,true);d.setManifold(1,y,0,true);d.setManifold(1,y,2,true);}
            if(strongSolid)d.setModeratorProperties(0,y,0,new ReactorModeratorRegistry.ModeratorProperties(.99,.99,9,999));
        }
        var s=new TimeSlicedReactorSimulation(d,new SimulationConfiguration(293.15,false));
        s.fuelTank().insertFuel(12000,false);
        return s;
    }
    @GameTest(template="empty_7x7x7")
    public static void liquidsAreNotWorldModerators(GameTestHelper h) {
        h.assertFalse(ReactorModeratorRegistry.isBlockAllowed(Blocks.WATER),"water remains a world moderator");
        h.assertFalse(ReactorModeratorRegistry.isBlockAllowed(Blocks.LAVA),"lava remains a world moderator");
        h.assertTrue(FluidTransitionRegistry.liquidTransition(Fluids.WATER)!=null,"water coolant missing");
        h.assertTrue(FluidTransitionRegistry.liquidTransition(LiquidObsidian.INSTANCE)!=null,"obsidian coolant missing");
        h.succeed();
    }
    @GameTest(template="empty_7x7x7")
    public static void fillSimulationAndReactorWrapperConserveVolume(GameTestHelper h) {
        var s=reactor(4,false);var tank=new ReactorTransitionTank(s.coolantTank());
        long capacity=s.coolantTank().perSideCapacity();
        h.assertTrue(capacity==120000,"buffer must count the 12 manifolds only");
        h.assertTrue(tank.fill(LiquidObsidian.INSTANCE,DataComponentPatch.EMPTY,1000,true)==1000,"fill preview rejected");
        h.assertTrue(s.coolantTank().liquidAmount()==0,"preview mutated reactor");
        tank.fill(LiquidObsidian.INSTANCE,DataComponentPatch.EMPTY,1000,false);
        h.assertTrue(s.coolantTank().liquidAmount()==1000,"wrapper discarded incoming fluid");
        h.assertTrue(tank.fill(Fluids.WATER,DataComponentPatch.EMPTY,1000,false)==0,"mixed coolants");
        h.succeed();
    }
    @GameTest(template="empty_7x7x7")
    public static void missingColdAndBlockedHotStopBurning(GameTestHelper h) {
        var s=reactor(4,false);
        for(int i=0;i<100;i++)s.tick(true);
        h.assertTrue(s.fuelTank().fuel()==12000,"reactor burned without cold coolant");
        var tank=new ReactorTransitionTank(s.coolantTank());
        tank.fill(LiquidObsidian.INSTANCE,DataComponentPatch.EMPTY,120000,false);
        for(int i=0;i<200;i++)s.tick(true);
        s.coolantTank().insertVapor(120000-s.coolantTank().vaporAmount());
        long fuel=s.fuelTank().fuel();
        for(int i=0;i<100;i++)s.tick(true);
        h.assertTrue(s.fuelTank().fuel()==fuel,"blocked outlet still consumes fuel");
        h.assertTrue(s.burnFraction()==0,"blocked outlet did not reduce reaction");
        h.succeed();
    }
    @GameTest(template="empty_7x7x7")
    public static void activeSolidsAreExactlyNeutral(GameTestHelper h) {
        var a=reactor(4,false);var b=reactor(4,true);
        for(var s:new TimeSlicedReactorSimulation[]{a,b})new ReactorTransitionTank(s.coolantTank()).fill(LiquidObsidian.INSTANCE,DataComponentPatch.EMPTY,120000,false);
        for(int i=0;i<5000;i++){a.tick(true);b.tick(true);}
        h.assertTrue(Math.abs(a.fuelHeat()-b.fuelHeat())<1e-8,"solid changed active temperature");
        h.assertTrue(a.fuelTank().fuel()==b.fuelTank().fuel(),"solid changed active consumption");
        h.assertTrue(a.coolantTank().vaporAmount()==b.coolantTank().vaporAmount(),"solid changed active output");
        h.succeed();
    }
    @GameTest(template="empty_7x7x7")
    public static void nativeCycleConservesFluidAndSavedIdentity(GameTestHelper h) {
        var evaporator=new FluidTransitionTank(false);evaporator.perSideCapacity=10000;
        evaporator.fill(LiquidObsidian.INSTANCE,DataComponentPatch.EMPTY,1000,false);
        var body=new HeatBody();body.setRfPerKelvin(10000);body.setTemperature(2000);
        evaporator.transferWith(body,1000);
        long hot=evaporator.fluidAmountInTank(1),cold=evaporator.fluidAmountInTank(0);
        h.assertTrue(hot>0 && hot+cold==1000,"evaporation created or lost fluid");
        var restored=new FluidTransitionTank(false);restored.perSideCapacity=10000;restored.deserializeNBT(evaporator.serializeNBT());
        h.assertTrue(restored.fluidAmountInTank(1)==hot && restored.fluidTypeInTank(1)==biggerreactorsrework.fluids.HeatedCoolant.INSTANCE,"save changed hot output");
        var condenser=new FluidTransitionTank(true);condenser.perSideCapacity=10000;
        condenser.fill(biggerreactorsrework.fluids.HeatedCoolant.INSTANCE,biggerreactorsrework.registries.CoolantComponents.heated(FluidTransitionRegistry.liquidTransition(biggerreactorsrework.fluids.LiquidObsidian.INSTANCE)),hot,false);
        body.setTemperature(300);condenser.transferWith(body,1000);
        h.assertTrue(condenser.fluidAmountInTank(0)+condenser.fluidAmountInTank(1)==hot,"condensation created or lost fluid");
        h.assertTrue(condenser.fluidTypeInTank(1)==LiquidObsidian.INSTANCE,"wrong cold output");
        h.succeed();
    }
    @GameTest(template="empty_7x7x7")
    public static void zeroPowerCoolantsCannotEnterTurbine(GameTestHelper h) {
        var tank=new biggerreactorsrework.multiblocks.turbine.simulation.modern.FluidTank();tank.perSideCapacity=10000;
        h.assertTrue(tank.fill(biggerreactorsrework.fluids.HeatedCoolant.INSTANCE,biggerreactorsrework.registries.CoolantComponents.heated(FluidTransitionRegistry.liquidTransition(biggerreactorsrework.fluids.LiquidObsidian.INSTANCE)),1000,false)==0,"lava bypassed turbine whitelist");
        h.succeed();
    }

    @GameTest(template="empty_7x7x7")
    public static void reactorHeatIsCarriedOrExplicitlyLost(GameTestHelper h) {
        var s=reactor(4,false);
        var wrapper=new ReactorTransitionTank(s.coolantTank());
        wrapper.fill(LiquidObsidian.INSTANCE,DataComponentPatch.EMPTY,1000,false);
        var tank=(biggerreactorsrework.multiblocks.reactor.simulation.base.CoolantTank)s.coolantTank();
        double removed=-tank.absorbRF(-100);
        double stored=tank.vaporAmount()*tank.transition().latentHeat()+tank.save().getDouble("thermalRemainder");
        h.assertTrue(Math.abs(removed-stored-tank.environmentalLossLastTick)<1e-8,"heat balance includes unaccounted energy");
        h.assertTrue(tank.liquidAmount()+tank.vaporAmount()==1000,"heat recovery created fluid");
        h.succeed();
    }

    @GameTest(template="empty_7x7x7")
    public static void changedSavedProfileIsQuarantined(GameTestHelper h) {
        var tank=new FluidTransitionTank(false);tank.perSideCapacity=10000;
        tank.fill(LiquidObsidian.INSTANCE,DataComponentPatch.EMPTY,1000,false);
        var saved=tank.serializeNBT();saved.putString("profile","unrecognized-profile");
        var restored=new FluidTransitionTank(false);restored.perSideCapacity=10000;restored.deserializeNBT(saved);
        h.assertTrue(restored.activeTransition()==null,"changed profile was reused");
        h.assertTrue(restored.fill(Fluids.WATER,DataComponentPatch.EMPTY,1000,false)==0,"quarantined contents were overwritten");
        h.assertTrue(restored.serializeNBT().equals(saved),"quarantine discarded stored contents");
        restored.dumpTank(0);restored.dumpTank(1);
        h.assertTrue(restored.fill(Fluids.WATER,DataComponentPatch.EMPTY,1000,false)==1000,"explicit empty did not unlock quarantine");
        h.succeed();
    }
}
