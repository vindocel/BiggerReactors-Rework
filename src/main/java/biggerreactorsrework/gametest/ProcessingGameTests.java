package biggerreactorsrework.gametest;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.machine.blocks.CyaniteReprocessor;
import biggerreactorsrework.machine.tiles.CyaniteReprocessorTile;
import biggerreactorsrework.machine.recipe.ReprocessingRecipes;
import biggerreactorsrework.items.ingots.CyaniteIngot;
import biggerreactorsrework.items.ingots.BlutoniumIngot;
import biggerreactorsrework.util.FluidTransitionTank;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import biggerreactorsrework.core.transfer.FluidTanks.FluidAction;
import net.neoforged.neoforge.gametest.*;

/** Actual machine processing and tank contracts. */
public final class ProcessingGameTests {
    @GameTest(template="empty_7x7x7")
    public static void reprocessingSurvivesSave(GameTestHelper h) {
        var pos=new BlockPos(2,2,2);h.setBlock(pos,CyaniteReprocessor.INSTANCE);
        var tile=(CyaniteReprocessorTile)GameTestSupport.be(h,pos);tile.tick();
        var recipe=ReprocessingRecipes.all(h.getLevel()).getFirst().value();
        var items=tile.itemHandlerFor(null);items.insertItem(0,new ItemStack(CyaniteIngot.INSTANCE,recipe.count()+1),false);
        int energy=recipe.energy()*recipe.duration(),water=recipe.water()*recipe.duration();
        for(int i=0;i<energy;i++)tile.energyFor(null).receiveEnergy(1,false);
        tile.fluidFor(null).fill(new FluidStack(recipe.fluid(),water),FluidAction.EXECUTE);
        for(int i=0;i<recipe.duration()/2;i++)tile.tick();
        var saved=tile.saveWithoutMetadata(h.getLevel().registryAccess());
        int progress=saved.getCompoundOrEmpty("cyaniteReprocessorState").getIntOr("workTime",0);
        h.assertTrue(progress>0,"machine made no progress");
        GameTestSupport.load(tile,saved,h);
        for(int i=progress;i<recipe.duration();i++)tile.tick();
        h.assertTrue(items.getStackInSlot(0).getCount()==1,"wrong input count or work lost on load");
        h.assertTrue(items.getStackInSlot(1).is(BlutoniumIngot.INSTANCE)&&items.getStackInSlot(1).getCount()==1,"wrong output");
        h.assertTrue(tile.energyFor(null).getEnergyStored()==0,"energy not conserved");
        h.assertTrue(tile.fluidFor(null).getFluidInTank(0).isEmpty(),"fluid not conserved");h.succeed();
    }
    @GameTest(template="empty_7x7x7")
    public static void transitionTankRejectsNegativeAmounts(GameTestHelper h) {
        var tank=new FluidTransitionTank(false);tank.perSideCapacity=5_000_000_000L;
        h.assertTrue(tank.tankCapacity(0)==5_000_000_000L,"long capacity truncated internally");
        h.assertTrue(tank.fill(Fluids.WATER,DataComponentPatch.EMPTY,1000,true)==1000,"simulation rejected valid water");
        h.assertTrue(tank.fluidAmountInTank(0)==0,"simulation mutated tank");
        h.assertTrue(tank.fill(Fluids.WATER,DataComponentPatch.EMPTY,-10,false)==0,"negative fill accepted");
        tank.fill(Fluids.WATER,DataComponentPatch.EMPTY,1000,false);
        h.assertTrue(tank.drain(-10,FluidAction.EXECUTE).isEmpty(),"negative drain accepted");
        h.assertTrue(tank.fluidAmountInTank(0)==1000,"negative operation changed fluid");
        var condenser=new FluidTransitionTank(true);h.assertTrue(!condenser.fluidValidForTank(0,Fluids.WATER),"condenser advertises water input");
        h.assertTrue(condenser.fluidValidForTank(1,Fluids.WATER),"condenser does not advertise water output");h.succeed();
    }
}
