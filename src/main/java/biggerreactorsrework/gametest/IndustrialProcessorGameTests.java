package biggerreactorsrework.gametest;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.machine.blocks.CyaniteReprocessor;
import biggerreactorsrework.machine.tiles.CyaniteReprocessorTile;
import biggerreactorsrework.items.ingots.*;
import biggerreactorsrework.fluids.*;
import biggerreactorsrework.registries.*;
import biggerreactorsrework.util.FluidTransitionTank;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.*;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.*;

/** Modified 2026-10-27: real Industrial Processor, exact input accounting and legacy coolant rejection. */
@GameTestHolder(BiggerReactors.modid)
@PrefixGameTestTemplate(false)
public final class IndustrialProcessorGameTests {
    private static CyaniteReprocessorTile machine(GameTestHelper h) {
        h.setBlock(2,2,2,CyaniteReprocessor.INSTANCE);
        var t=(CyaniteReprocessorTile)h.getBlockEntity(new BlockPos(2,2,2));t.tick();
        for(int i=0;i<10;i++)t.energyFor(null).receiveEnergy(500,false);
        t.fluidFor(null).fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE);return t;
    }
    @GameTest(template="empty_7x7x7")
    public static void obsidianUsesExactlyOneBlockOneHundredWaterAndTwoHundredFE(GameTestHelper h) {
        var t=machine(h);t.getItemHandler().setStackInSlot(1,new ItemStack(BlutoniumIngot.INSTANCE,64));
        t.itemHandlerFor(null).insertItem(0,new ItemStack(Items.OBSIDIAN),false);
        for(int i=0;i<200;i++)t.tick();
        h.assertTrue(t.fluidFor(null).getFluidInTank(0).getAmount()==900,"obsidian water cost not 100 mB");
        h.assertTrue(t.fluidFor(null).getFluidInTank(1).getAmount()==100,"fluid output blocked by unrelated full item slot");
        h.assertTrue(t.energyFor(null).getEnergyStored()==4800,"energy cost not 200 FE");
        h.assertTrue(t.itemHandlerFor(null).getStackInSlot(0).isEmpty(),"obsidian item not consumed");
        h.assertTrue(t.fluidFor(null).fill(new FluidStack(LiquidObsidian.INSTANCE,100),FluidAction.EXECUTE)==0,"product can be injected from outside");
        var preview=t.fluidFor(null).drain(100,FluidAction.SIMULATE);
        h.assertTrue(preview.getAmount()==100 && t.fluidFor(null).getFluidInTank(1).getAmount()==100,"drain simulation mutated output");
        h.succeed();
    }
    @GameTest(template="empty_7x7x7")
    // Modified 2026-10-30: the consolidated recipe identity is the forward persistence contract.
    public static void currentCyaniteSavePreservesPaidProgress(GameTestHelper h) {
        var t=machine(h);t.itemHandlerFor(null).insertItem(0,new ItemStack(CyaniteIngot.INSTANCE,2),false);
        for(int i=0;i<40;i++)t.tick();
        var tag=t.saveWithFullMetadata(h.getLevel().registryAccess());
        t.loadWithComponents(tag,h.getLevel().registryAccess());
        for(int i=0;i<160;i++)t.tick();
        h.assertTrue(t.itemHandlerFor(null).getStackInSlot(1).is(BlutoniumIngot.INSTANCE),"current cyanite progress or item output lost");
        h.assertTrue(t.fluidFor(null).getFluidInTank(0).getAmount()==800,"cyanite costs changed");
        h.assertTrue(t.energyFor(null).getEnergyStored()==4800,"legacy FE changed");
        h.assertTrue(t.fluidFor(null).getFluidInTank(1).isEmpty(),"old save fabricated fluid output");h.succeed();
    }
    @GameTest(template="empty_7x7x7")
    public static void lavaAndLegacyProfilesCannotBecomeHeatedObsidian(GameTestHelper h) {
        var tank=new FluidTransitionTank(true);tank.perSideCapacity=1000;
        h.assertTrue(tank.fill(Fluids.LAVA,DataComponentPatch.EMPTY,1000,false)==0,"lava still accepted");
        h.assertTrue(tank.fill(HeatedCoolant.INSTANCE,DataComponentPatch.EMPTY,1000,false)==0,"unidentified hot coolant accepted");
        for(boolean cond:new boolean[]{false,true}) {
            var saved=new net.minecraft.nbt.CompoundTag();saved.putString("inFluid",cond?"minecraft:lava":"biggerreactorsrework:liquid_obsidian");
            saved.putString("outFluid",cond?"biggerreactorsrework:liquid_obsidian":"minecraft:lava");
            saved.putLong("inAmount",500);saved.putLong("outAmount",200);
            var restored=new FluidTransitionTank(cond);restored.perSideCapacity=1000;restored.deserializeNBT(saved);
            h.assertTrue(restored.hasUnresolvedProfile() && restored.activeTransition()==null,"legacy pair reinterpreted");
            h.assertTrue(restored.serializeNBT().getLong("outAmount")==200,"legacy volume discarded");
            h.assertTrue(restored.fill(Fluids.WATER,DataComponentPatch.EMPTY,1,false)==0,"quarantine bypassed");
            restored.dumpTank(0);restored.dumpTank(1);
            h.assertTrue(!restored.hasUnresolvedProfile() && !restored.hasStoredContents(),"manual clearing kept invalid stock");
        }
        h.succeed();
    }
    @GameTest(template="empty_7x7x7")
    public static void fullFluidOutputDoesNotBlockCyaniteAndBlocksOnlyObsidian(GameTestHelper h) {
        var t=machine(h);
        try {
            var field=t.getClass().getDeclaredField("outputTank");field.setAccessible(true);
            var out=(net.neoforged.neoforge.fluids.capability.templates.FluidTank)field.get(t);
            out.fill(new FluidStack(LiquidObsidian.INSTANCE,out.getCapacity()),FluidAction.EXECUTE);
            t.itemHandlerFor(null).insertItem(0,new ItemStack(CyaniteIngot.INSTANCE,2),false);
            for(int i=0;i<200;i++)t.tick();
            h.assertTrue(t.getItemHandler().getStackInSlot(1).is(BlutoniumIngot.INSTANCE),"unrelated full fluid output blocked cyanite");
            t.itemHandlerFor(null).insertItem(0,new ItemStack(Items.OBSIDIAN),false);
            int energy=t.energyFor(null).getEnergyStored(),water=t.fluidFor(null).getFluidInTank(0).getAmount();
            for(int i=0;i<200;i++)t.tick();
            h.assertTrue(t.energyFor(null).getEnergyStored()==energy && t.fluidFor(null).getFluidInTank(0).getAmount()==water,"blocked obsidian consumed inputs");
            h.assertTrue(t.getItemHandler().getStackInSlot(0).getCount()==1,"blocked obsidian item lost");
        } catch(ReflectiveOperationException e){throw new AssertionError(e);}h.succeed();
    }
    @GameTest(template="empty_7x7x7")
    public static void switchingInputCannotReuseProgressOrProduceWithoutEnergy(GameTestHelper h) {
        var t=machine(h);t.itemHandlerFor(null).insertItem(0,new ItemStack(CyaniteIngot.INSTANCE,2),false);
        for(int i=0;i<199;i++)t.tick();
        t.getItemHandler().setStackInSlot(0,new ItemStack(Items.OBSIDIAN));t.tick();
        h.assertTrue(t.fluidFor(null).getFluidInTank(1).isEmpty(),"cyanite progress reused for obsidian");
        var tag=t.saveWithFullMetadata(h.getLevel().registryAccess());tag.getCompound("cyaniteReprocessorState").putInt("energyStored",0);
        t.loadWithComponents(tag,h.getLevel().registryAccess());for(int i=0;i<300;i++)t.tick();
        h.assertTrue(t.fluidFor(null).getFluidInTank(1).isEmpty(),"no-energy processor produced fluid");h.succeed();
    }

}
