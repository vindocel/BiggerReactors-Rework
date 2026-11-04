package biggerreactorsrework.gametest;
import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.fluids.*;
import biggerreactorsrework.registries.*;
import biggerreactorsrework.thermal.*;
import biggerreactorsrework.util.FluidTransitionTank;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.gametest.*;
// Modified 2026-11-02: fixed recipes replace the historical 20-percent obsidian comparison.
public final class HotCoolantYieldGameTests {
    static void batch(GameTestHelper h,FluidTransitionRegistry.FluidTransition profile,double rate) {
        var hot=new FluidTransitionTank(true);var water=new FluidTransitionTank(false);hot.perSideCapacity=water.perSideCapacity=100000;
        var components=profile.genericHot()?CoolantComponents.heated(profile):DataComponentPatch.EMPTY;
        hot.fill(profile.gases.getFirst(),components,1000,false);water.fill(Fluids.WATER,DataComponentPatch.EMPTY,100000,false);
        var model=new ThermalExchanger();long steam=0;double heat=0,useful=0,loss=0;
        for(int tick=0;tick<300;tick++) { model.tick(hot,water,rate,ThermalModel.Preset.DEFAULT);steam+=model.steamLastTick;heat+=model.inputKJ;useful+=model.usefulKJ;loss+=model.processLossKJ; }
        h.assertTrue(hot.fluidAmountInTank(0)==0 && hot.fluidAmountInTank(1)==1000,"Primary batch was not conserved");
        h.assertTrue(steam==100L*profile.steamPerBatch,"Geometry changed fixed recipe yield");
        h.assertTrue(Math.abs(heat-1000)<1e-6 && Math.abs(useful-steam*.2)<1e-6 && Math.abs(heat-useful-loss)<1e-6,"Useful/process heat balance failed");
    }
    @GameTest(template="empty_7x7x7") public static void equalHotVolumeUsesFixedRecipes(GameTestHelper h) {
        for(var profile:FluidTransitionRegistry.transitions())if(!profile.liquids.contains(Fluids.WATER))batch(h,profile,500);
        h.succeed();
    }
    @GameTest(template="empty_7x7x7") public static void channelDistanceChangesSpeedWithoutChangingBatchYield(GameTestHelper h) {
        var profile=FluidTransitionRegistry.liquidTransition(LiquidObsidian.INSTANCE);
        for(int gap=0;gap<=3;gap++)batch(h,profile,500*ThermalModel.gapFactor(gap));h.succeed();
    }
}
