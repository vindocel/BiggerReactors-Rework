package biggerreactorsrework.gametest;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.fluids.LiquidObsidian;
import biggerreactorsrework.multiblocks.reactor.simulation.*;
import biggerreactorsrework.multiblocks.reactor.simulation.cpu.TimeSlicedReactorSimulation;
import biggerreactorsrework.multiblocks.reactor.util.ReactorTransitionTank;
import biggerreactorsrework.registries.FluidTransitionRegistry;
import biggerreactorsrework.util.FluidTransitionTank;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.material.Fluids;
import biggerreactorsrework.core.transfer.FluidTanks.FluidAction;
import net.neoforged.neoforge.gametest.*;

/** Empty means unselected even if the last conversion left fractional thermal energy. */
public final class EmptyCoolantTankGameTests {
    @GameTest(template="empty_7x7x7")
    public static void lastDrainClearsBothExchangerProfiles(GameTestHelper h) {
        var steam=FluidTransitionRegistry.liquidTransition(Fluids.WATER).gases.getFirst();
        for(boolean condenser:new boolean[]{true,false}) {
            var tank=new FluidTransitionTank(condenser);tank.perSideCapacity=10000;
            tank.fill(condenser?biggerreactorsrework.fluids.HeatedCoolant.INSTANCE:LiquidObsidian.INSTANCE,condenser?biggerreactorsrework.registries.CoolantComponents.heated(FluidTransitionRegistry.liquidTransition(LiquidObsidian.INSTANCE)):DataComponentPatch.EMPTY,2,false);
            tank.absorbRF((condenser?1:-1)*(2*FluidTransitionRegistry.liquidTransition(LiquidObsidian.INSTANCE).latentHeat+.5));
            var saved=tank.serializeNBT();
            h.assertTrue(tank.drain(2,FluidAction.SIMULATE).getAmount()==2 && tank.serializeNBT().equals(saved),"drain preview reset the profile");
            tank.drain(1,FluidAction.EXECUTE);
            var next=condenser?steam:Fluids.WATER;
            h.assertTrue(tank.fill(next,DataComponentPatch.EMPTY,1,false)==0,"one remaining output unit allowed coolant mixing");
            var drained=tank.drain(1,FluidAction.EXECUTE);
            h.assertTrue(drained.getAmount()==1,"last output was lost while resetting");
            h.assertTrue(tank.fill(next,DataComponentPatch.EMPTY,1,true)==1,"fractional thermal residue locked an empty exchanger");
            h.assertTrue(tank.activeTransition()==null && tank.fluidTypeInTank(0)==Fluids.EMPTY && tank.fluidTypeInTank(1)==Fluids.EMPTY,"empty tank retained its old fluid identity");
            h.assertTrue(tank.serializeNBT().isEmpty(),"empty tank still saved the old profile");
            h.assertTrue(tank.fill(next,DataComponentPatch.EMPTY,1,false)==1,"new fluid rejected after empty reset");
        }
        h.succeed();
    }

    @GameTest(template="empty_7x7x7")
    public static void oldEmptySaveAndManualDumpClearSelection(GameTestHelper h) {
        var tank=new FluidTransitionTank(true);tank.perSideCapacity=10000;tank.fill(biggerreactorsrework.fluids.HeatedCoolant.INSTANCE,biggerreactorsrework.registries.CoolantComponents.heated(FluidTransitionRegistry.liquidTransition(biggerreactorsrework.fluids.LiquidObsidian.INSTANCE)),100,false);
        var oldSave=tank.serializeNBT();oldSave.putLong("inAmount",0);oldSave.putLong("outAmount",0);oldSave.putDouble("thermalRemainder",.5);
        var restored=new FluidTransitionTank(true);restored.perSideCapacity=10000;restored.deserializeNBT(oldSave);
        var steam=FluidTransitionRegistry.liquidTransition(Fluids.WATER).gases.getFirst();
        h.assertTrue(restored.activeTransition()==null && restored.fill(steam,DataComponentPatch.EMPTY,1,false)==1,"old empty save kept a coolant lock");
        tank.absorbRF(FluidTransitionRegistry.liquidTransition(LiquidObsidian.INSTANCE).latentHeat+.5);tank.dumpTank(0);
        h.assertTrue(tank.activeTransition()!=null,"dumping only input cleared nonempty output");
        tank.dumpTank(1);
        h.assertTrue(tank.activeTransition()==null && tank.serializeNBT().isEmpty(),"manual empty retained old selection");
        h.assertTrue(tank.fill(steam,DataComponentPatch.EMPTY,1,false)==1,"manual empty rejected new coolant");
        h.succeed();
    }

    @GameTest(template="empty_7x7x7")
    public static void reactorResetDoesNotCarryHeatToAnotherCoolant(GameTestHelper h) {
        var d=OperationGameTests.description(3);for(int y=0;y<3;y++)d.setManifold(0,y,1,true);
        var simulation=new TimeSlicedReactorSimulation(d,new SimulationConfiguration(293.15,false));var tank=new ReactorTransitionTank(simulation.coolantTank());
        tank.fill(LiquidObsidian.INSTANCE,DataComponentPatch.EMPTY,1,false);
        ((biggerreactorsrework.multiblocks.reactor.simulation.base.CoolantTank)simulation.coolantTank()).absorbRF(-100);
        tank.drain(1,FluidAction.EXECUTE);
        h.assertTrue(tank.activeTransition()==null && simulation.coolantTank().liquidAmount()==0 && simulation.coolantTank().vaporAmount()==0,"reactor did not clear an empty coolant profile");
        h.assertTrue(tank.fill(Fluids.WATER,DataComponentPatch.EMPTY,1,false)==1,"reactor refused replacement coolant");
        ((biggerreactorsrework.multiblocks.reactor.simulation.base.CoolantTank)simulation.coolantTank()).absorbRF(-.01);
        h.assertTrue(simulation.coolantTank().vaporAmount()==0,"previous coolant's fractional heat generated free steam");
        h.succeed();
    }
}
