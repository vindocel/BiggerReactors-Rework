package biggerreactorsrework.gametest;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.fluids.LiquidObsidian;
import biggerreactorsrework.multiblocks.reactor.simulation.*;
import biggerreactorsrework.multiblocks.reactor.util.ReactorTransitionTank;
import biggerreactorsrework.registries.*;
import biggerreactorsrework.thermal.*;
import biggerreactorsrework.util.FluidTransitionTank;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.gametest.*;

import java.util.*;

// Added 2026-11-02: exercise production fuel, geometry, tanks, migration and atomic recipes in Minecraft.
@GameTestHolder(BiggerReactors.modid)
@PrefixGameTestTemplate(false)
public final class ThermalModelGameTests {
    static SimulationDescription checker(int x,int y,int z,boolean missing,ThermalModel.PassiveClass passive) {
        var d=new SimulationDescription();d.setSize(x,y,z);
        for(int a=0;a<x;a++)for(int c=0;c<z;c++) {
            if((a+c)%2==0)d.setControlRod(a,c,true);
            else for(int b=0;b<y;b++) {
                if(missing && a==1 && c==2 && b==0)continue;
                if(passive==null)d.setManifold(a,b,c,true);
                else d.setModeratorProperties(a,b,c,new ReactorModeratorRegistry.ModeratorProperties(.55,.85,1.5,3,passive));
            }
        }
        return d;
    }
    static IReactorSimulation simulation(SimulationDescription d,boolean passive,double fill) {
        var s=new SimulationDescription.Builder(false,false,false).build(d,new SimulationConfiguration(293.15,passive));
        s.fuelTank().insertFuel((long)(s.fuelTank().capacity()*fill),false);
        if(!passive)new ReactorTransitionTank(s.coolantTank()).fill(Fluids.WATER,DataComponentPatch.EMPTY,Long.MAX_VALUE,false);
        return s;
    }
    static void close(GameTestHelper h,double expected,double actual,String message) {
        h.assertTrue(Math.abs(expected-actual)<=Math.max(1e-7,Math.abs(expected)*1e-9),message+": expected "+expected+", got "+actual);
    }
    static void balance(GameTestHelper h,IReactorSimulation s) {
        var t=s.thermal();
        close(h,s.fuelTank().burnedLastTick()*12500,t.generated(),"Unpaid heat");
        close(h,t.generated(),t.isolated()+t.structural()+t.processed()+t.missingCoolant()+t.blockedOutput()+t.rounding(),"Thermal losses overlap or omit heat");
        close(h,t.processed(),t.useful()+t.processLoss(),"Process loss missing");
    }

    @GameTest(template="empty_7x7x7")
    public static void drawingsKeepStructuralPercentAcrossFuelAndBars(GameTestHelper h) {
        for(boolean missing:new boolean[]{false,true})for(double fill:new double[]{1,.5,.1})for(double insertion:new double[]{0,25,42,50,75,100}) {
            var s=simulation(checker(3,1,3,missing,null),false,fill);s.setAllControlRodInsertions(insertion);s.tick(true);
            close(h,missing?.75:1,s.thermal().structuralEfficiency(),"Drawing structural efficiency changed with power");
            balance(h,s);
            if(insertion<100)close(h,missing?.75:1,s.thermal().capturable()/s.thermal().generated(),"Bars recovered missing contact");
        }
        var selective=simulation(checker(3,1,3,true,null),false,1);
        selective.controlRodAt(1,1).setInsertion(100);selective.controlRodAt(0,2).setInsertion(100);selective.controlRodAt(2,2).setInsertion(100);
        selective.tick(true);close(h,.75,selective.thermal().capturable()/selective.thermal().generated(),"Selective shutoff evaded global cap");
        h.succeed();
    }

    @GameTest(template="empty_7x7x7")
    public static void checkerboardSizesPayForAllTheirHeat(GameTestHelper h) {
        for(int[] size:new int[][]{{3,3,3},{5,5,5},{9,9,9},{4,3,7}}) {
            var s=simulation(checker(size[0],size[1],size[2],false,null),false,1);
            for(double insertion:new double[]{0,42,75}) {
                s.setAllControlRodInsertions(insertion);s.tick(true);
                close(h,1,s.thermal().structuralEfficiency(),"Wall/corner structural penalty");balance(h,s);
                h.assertTrue(s.thermal().structural()==0 && s.thermal().isolated()==0,"Complete checkerboard lost structural heat");
            }
        }
        h.succeed();
    }

    @GameTest(template="empty_7x7x7")
    public static void starvationAndBlockedOutputWasteFuelWithoutStoringHeat(GameTestHelper h) {
        var s=simulation(checker(3,1,3,false,null),false,1);s.coolantTank().dumpLiquid();
        s.tick(true);h.assertTrue(s.fuelTank().burnedLastTick()>0 && s.thermal().missingCoolant()>0,"Starvation saved fuel");balance(h,s);
        new ReactorTransitionTank(s.coolantTank()).fill(Fluids.WATER,DataComponentPatch.EMPTY,Long.MAX_VALUE,false);
        s.coolantTank().insertVapor(Long.MAX_VALUE);s.tick(true);
        h.assertTrue(s.fuelTank().burnedLastTick()>0 && s.thermal().blockedOutput()>0 && s.coolantTank().transitionedLastTick()==0,"Blocked outlet saved fuel or overfilled");balance(h,s);
        var saved=s.save();saved.put("fuelHeat",9000d);saved.put("reactorHeat",9000d);s.load(saved);
        s.coolantTank().extractVapor(Long.MAX_VALUE);s.tick(false);
        h.assertTrue(s.coolantTank().transitionedLastTick()==0 && s.thermal().generated()==0,"Discarded or legacy heat produced later");
        close(h,293.15,s.fuelHeat(),"Deprecated temperature is not neutral");close(h,1,s.fertility(),"Deprecated fertility is not neutral");
        h.succeed();
    }

    @GameTest(template="empty_7x7x7")
    public static void passiveClassesShareHeatAndDiscardFullBatteryPower(GameTestHelper h) {
        double fuel=-1;
        for(var type:ThermalModel.PassiveClass.values()) {
            var s=simulation(checker(3,1,3,false,type),true,1);s.tick(true);
            if(fuel<0)fuel=s.fuelTank().burnedLastTick();else close(h,fuel,s.fuelTank().burnedLastTick(),"Material multiplied nuclear energy");
            double cooling=ThermalModel.passiveCooling(type,3);
            close(h,cooling,s.thermal().structuralEfficiency(),"Wrong passive weight");balance(h,s);
            close(h,Math.floor(s.thermal().generated()*cooling*ThermalModel.PASSIVE_RF_PER_KJ),s.thermal().potentialRF(),"Passive heat was allocated twice");
            var n=s.save();var battery=n.getCompound("battery");battery.put("storedPower",s.battery().capacity()+500);n.put("battery",battery);s.load(n);s.tick(true);
            h.assertTrue(s.battery().stored()==s.battery().capacity()+500,"Legacy battery overfill was clamped");
            if(type!=ThermalModel.PassiveClass.NONE)h.assertTrue(s.thermal().discardedRF()>0 && s.thermal().acceptedRF()==0,"Full battery banked productive power");
        }
        // Two conductive and two weakly conductive contacts per rod must split one paid budget.
        var d=checker(3,1,3,false,ThermalModel.PassiveClass.SUPERIOR);
        d.setModeratorProperties(1,0,0,new ReactorModeratorRegistry.ModeratorProperties(.1,.25,1.1,.3,ThermalModel.PassiveClass.BASIC));
        var mixed=simulation(d,true,1);mixed.tick(true);balance(h,mixed);
        h.assertTrue(mixed.thermal().potentialRF()<=mixed.thermal().processed()*ThermalModel.PASSIVE_RF_PER_KJ,"Mixture exceeded the passive cap");h.succeed();
    }

    @GameTest(template="empty_7x7x7")
    public static void fractionalFuelAndOverfilledFluidSurviveModelMigration(GameTestHelper h) {
        var old=simulation(checker(3,1,3,false,null),false,1);old.tick(true);var saved=old.save();
        var reduced=simulation(checker(1,1,1,false,null),false,0);reduced.load(saved);
        h.assertTrue(reduced.coolantTank().perSideCapacity()==0 && reduced.coolantTank().liquidAmount()>0,"Reduced capacity deleted stock");
        double fuel=reduced.fuelTank().usableFuel();long cold=reduced.coolantTank().liquidAmount(),hot=reduced.coolantTank().vaporAmount();
        for(int i=0;i<3;i++)reduced.load(reduced.save());
        close(h,fuel,reduced.fuelTank().usableFuel(),"Fraction duplicated on repeated load");
        h.assertTrue(cold==reduced.coolantTank().liquidAmount() && hot==reduced.coolantTank().vaporAmount(),"Repeated migration changed volumes");
        h.assertTrue(reduced.coolantTank().insertLiquid(1)==0,"Zero-capacity input accepted stock");h.succeed();
    }

    @GameTest(template="empty_7x7x7")
    public static void knownLegacyProfilesMigrateAndUnknownStockCanBeRecovered(GameTestHelper h) {
        var profile=FluidTransitionRegistry.liquidTransition(LiquidObsidian.INSTANCE);
        var tank=new FluidTransitionTank(false);tank.perSideCapacity=10000;tank.fill(LiquidObsidian.INSTANCE,DataComponentPatch.EMPTY,1000,false);
        tank.processThermalUnits(300,10);var old=tank.serializeNBT();
        old.putString("profile",CoolantComponents.fingerprint(profile).split(":thermal")[0]);old.putDouble("thermalRemainder",999);
        var restored=new FluidTransitionTank(false);restored.perSideCapacity=0;restored.deserializeNBT(old);
        h.assertTrue(!restored.hasUnresolvedProfile() && restored.fluidAmountInTank(0)==700 && restored.fluidAmountInTank(1)==300,"Known legacy signature failed migration");
        h.assertTrue(restored.maintenanceDrain(0,LiquidObsidian.INSTANCE,DataComponentPatch.EMPTY,700,true)==700 && restored.fluidAmountInTank(0)==700,"Maintenance simulation mutated stock");
        h.assertTrue(restored.maintenanceDrain(0,LiquidObsidian.INSTANCE,DataComponentPatch.EMPTY,700,false)==700,"Zero-capacity cold recovery failed");
        old.putString("profile","unknown-profile");restored.deserializeNBT(old);
        h.assertTrue(restored.hasUnresolvedProfile() && restored.fill(LiquidObsidian.INSTANCE,DataComponentPatch.EMPTY,1,false)==0,"Unknown profile entered production");
        h.assertTrue(restored.maintenanceDrain(0,LiquidObsidian.INSTANCE,DataComponentPatch.EMPTY,Long.MAX_VALUE,false)==700,"Quarantined input could not be recovered");
        h.assertTrue(restored.fluidAmountInTank(1)==300,"Input recovery discarded hot stock");h.succeed();
    }

    @GameTest(template="empty_7x7x7")
    public static void atomicRecipesConserveBothCircuitsAndCryoBatches(GameTestHelper h) {
        for(int steam:new int[]{10,17,20,30}) {
            var p=FluidTransitionRegistry.liquidTransition(LiquidObsidian.INSTANCE);
            var primary=new FluidTransitionTank(true);primary.perSideCapacity=10000;
            primary.fill(p.gases.getFirst(),CoolantComponents.heated(p),1000,false);
            // Detached server-defined profile lets every recipe be exercised even without optional mods.
            var detached=new FluidTransitionRegistry.FluidTransition(p.liquids,p.gases,p.latentHeat,p.boilingPoint,p.liquidRFMKT,p.gasRFMKT,p.turbineMultiplier);
            detached.recovery=p.recovery;detached.steamPerBatch=steam;
            var n=primary.serializeNBT();
            try {
                var field=FluidTransitionTank.class.getDeclaredField("activeTransition");field.setAccessible(true);field.set(primary,detached);
            } catch(ReflectiveOperationException e) { throw new AssertionError(e); }
            var water=new FluidTransitionTank(false);water.perSideCapacity=10000;water.fill(Fluids.WATER,DataComponentPatch.EMPTY,10000,false);
            var exchanger=new ThermalExchanger();
            for(int i=0;i<20;i++)exchanger.tick(primary,water,.5,ThermalModel.Preset.DEFAULT);
            h.assertTrue(primary.fluidAmountInTank(0)==990 && primary.fluidAmountInTank(1)==10,"Sub-batch flow consumed partial primary");
            h.assertTrue(water.fluidAmountInTank(0)==10000-steam && water.fluidAmountInTank(1)==steam,"Wrong fixed steam ratio");
            close(h,10,exchanger.inputKJ,"Wrong kJ input");close(h,steam*.2,exchanger.usefulKJ,"Wrong steam budget");
            water.processThermalUnits(water.fluidAmountInTank(0),.2);
            long before=primary.fluidAmountInTank(0);
            for(int i=0;i<100;i++)exchanger.tick(primary,water,500,ThermalModel.Preset.DEFAULT);
            h.assertTrue(primary.fluidAmountInTank(0)==before && exchanger.credit()==0,"Blocked secondary consumed primary or banked flow");
        }
        h.succeed();
    }
}
