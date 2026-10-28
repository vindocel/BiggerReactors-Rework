package biggerreactorsrework.gametest;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.multiblocks.reactor.blocks.*;
import biggerreactorsrework.multiblocks.reactor.tiles.*;
import biggerreactorsrework.core.multiblock.common.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;
import com.mojang.datafixers.util.Pair;

/** Modified 2026-10-26: real controllers, persistence takeover and independent-inventory protection. */
@GameTestHolder(BiggerReactors.modid)
@PrefixGameTestTemplate(false)
public final class AuditRegressionGameTests {
    private static void build(GameTestHelper h,int offset) {
        for(int x=offset;x<offset+3;x++)for(int y=1;y<=3;y++)for(int z=1;z<=3;z++)
            if(x==offset || x==offset+2 || y==1 || y==3 || z==1 || z==3)h.setBlock(x,y,z,ReactorCasing.INSTANCE);
        h.setBlock(offset+1,2,2,ReactorFuelRod.INSTANCE);
        h.setBlock(offset+1,3,2,ReactorControlRod.INSTANCE);
        h.setBlock(offset,2,2,ReactorTerminal.INSTANCE);
    }
    private static ReactorControlRodTile rod(GameTestHelper h,int offset) {return (ReactorControlRodTile)h.getBlockEntity(new BlockPos(offset+1,3,2));}
    private static java.lang.reflect.Field field(Object target,String name) {
        try {var f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f;}
        catch(ReflectiveOperationException error){throw new AssertionError(error);}
    }
    @GameTest(template="empty_7x7x7",timeoutTicks=140)
    public static void malformedRodCommandsCannotChangeInsertion(GameTestHelper h) {
        build(h,1);
        h.succeedWhen(()->{
            var tile=rod(h,1);h.assertTrue(tile.nullableController()!=null && tile.controller().simulation()!=null,"reactor not ready");
            tile.setInsertion(25);
            for(Object data:new Object[]{"invalid",Pair.of(Double.NaN,false),Pair.of(Double.POSITIVE_INFINITY,false),Pair.of(1,"invalid")})
                tile.runRequest("changeInsertionLevel",data);
            h.assertTrue(tile.getInsertion()==25,"invalid command changed insertion");
            tile.runRequest("changeInsertionLevel",Pair.of(5d,false));
            h.assertTrue(tile.getInsertion()==30,"valid command was rejected");
        });
    }
    @GameTest(template="empty_7x7x7",timeoutTicks=180)
    public static void removedSaveDelegateIsReplaced(GameTestHelper h) {
        build(h,1);
        final Object[] module={null};final BlockPos broken=new BlockPos(1,1,1);
        h.startSequence().thenWaitUntil(()->h.assertTrue(rod(h,1).nullableController()!=null && rod(h,1).controller().simulation()!=null,"reactor not ready"))
            .thenExecute(()->{
                var controller=rod(h,1).controller();module[0]=controller.persistentModule();
                var tile=(ReactorBaseTile)h.getBlockEntity(broken);
                try {
                    field(module[0],"saveDelegate").set(module[0],tile);
                    field(module[0],"saveDelegateModule").set(module[0],tile.module(IPersistentMultiblockTile.class,IPersistentMultiblockTile.Module.class));
                    field(module[0],"hasSaveDelegate").setBoolean(module[0],true);
                } catch(IllegalAccessException error){throw new AssertionError(error);}
                h.setBlock(broken,Blocks.AIR);
            }).thenExecuteAfter(8,()->{
                var controller=rod(h,1).controller();controller.dirty();
                try {
                    var delegate=(ReactorBaseTile)field(module[0],"saveDelegate").get(module[0]);
                    h.assertTrue(delegate!=null && !delegate.isRemoved(),"save delegate was not replaced");
                    h.assertTrue(!delegate.getBlockPos().equals(h.absolutePos(broken)),"removed delegate reused");
                    var data=delegate.module(IPersistentMultiblockTile.class,IPersistentMultiblockTile.Module.class).writeNBT();
                    h.assertTrue(data.contains("controller_data"),"replacement did not save controller state");
                    h.assertTrue(data.hasUUID("machine_origin"),"machine identity not persisted");
                } catch(IllegalAccessException error){throw new AssertionError(error);}
            }).thenSucceed();
    }
    @GameTest(template="empty_7x7x7",timeoutTicks=180)
    public static void independentStockedReactorsDoNotMerge(GameTestHelper h) {
        build(h,0);build(h,4);
        h.startSequence().thenWaitUntil(()->h.assertTrue(rod(h,0).nullableController()!=null && rod(h,4).nullableController()!=null &&
                rod(h,0).controller().simulation()!=null && rod(h,4).controller().simulation()!=null,"reactors not ready"))
            .thenExecute(()->{
                rod(h,0).controller().simulation().fuelTank().insertFuel(1000,false);
                rod(h,4).controller().simulation().fuelTank().insertFuel(2000,false);
                h.setBlock(0,3,1,Blocks.AIR);h.setBlock(4,3,1,Blocks.AIR);
            }).thenExecuteAfter(8,()->h.setBlock(3,1,2,ReactorCasing.INSTANCE))
            .thenExecuteAfter(8,()->{
                h.assertTrue(rod(h,0).controller()!=rod(h,4).controller(),"independent inventories merged");
                var a=(ReactorFuelRodTile)h.getBlockEntity(new BlockPos(1,2,2));
                var b=(ReactorFuelRodTile)h.getBlockEntity(new BlockPos(5,2,2));
                h.assertTrue(a.fuel+b.fuel==3000,"fuel lost during blocked merge");
            }).thenSucceed();
    }
    @GameTest(template="empty_7x7x7")
    public static void dataMapPreservesRecovery(GameTestHelper h) {
        var json=com.google.gson.JsonParser.parseString("{\"gas\":\"minecraft:lava\",\"latentHeat\":15,\"boilingPoint\":1273.15,\"liquidThermalConductivity\":2,\"gasThermalConductivity\":2,\"turbineMultiplier\":0,\"recovery\":0.59}");
        var codec=biggerreactorsrework.registries.MaterialDataMaps.Transition.CODEC;
        var decoded=codec.parse(com.mojang.serialization.JsonOps.INSTANCE,json).getOrThrow();
        h.assertTrue(decoded.recovery()==.59,"recovery lost in data map");
        json.getAsJsonObject().addProperty("recovery",0);
        h.assertTrue(codec.parse(com.mojang.serialization.JsonOps.INSTANCE,json).error().isPresent(),"invalid recovery accepted");h.succeed();
    }

    @GameTest(template="empty_7x7x7")
    public static void settlingPendingRadiationHonorsFullOutput(GameTestHelper h) {
        var d=OperationGameTests.description(3);
        for(int y=0;y<3;y++)d.setManifold(0,y,1,true);
        var sim=new biggerreactorsrework.multiblocks.reactor.simulation.SimulationDescription.Builder(true,true,true).build(d,
                new biggerreactorsrework.multiblocks.reactor.simulation.SimulationConfiguration(293.15,false));
        sim.fuelTank().insertFuel(10000,false);
        var tank=new biggerreactorsrework.multiblocks.reactor.util.ReactorTransitionTank(sim.coolantTank());
        tank.fill(net.minecraft.world.level.material.Fluids.WATER,net.minecraft.core.component.DataComponentPatch.EMPTY,1000,false);
        sim.coolantTank().insertVapor(sim.coolantTank().perSideCapacity());
        sim.tick(true);long fuel=sim.fuelTank().fuel();double heat=sim.fuelHeat();
        sim.settle();
        h.assertTrue(sim.fuelTank().fuel()==fuel && sim.fuelTank().burnedLastTick()==0,"settle burned fuel into a full output");
        h.assertTrue(sim.fuelHeat()==heat,"settle added heat despite blocked output");h.succeed();
    }
    @GameTest(template="empty_7x7x7")
    public static void pendingStockIsDetectedBeforeControllerLoad(GameTestHelper h) {
        build(h,1);
        h.succeedWhen(()->{
            var tile=rod(h,1);h.assertTrue(tile.nullableController()!=null,"no controller");
            var module=tile.controller().persistentModule();
            var tag=new net.minecraft.nbt.CompoundTag();var tank=new net.minecraft.nbt.CompoundTag();tank.putLong("inAmount",1000);tag.put("condenserTank",tank);
            try {
                field(module,"nbt").set(module,tag);field(module,"shouldReadNBT").setBoolean(module,true);
                h.assertTrue(module.hasPendingContents(),"pending fluid was ignored");
                tank.putLong("inAmount",0);h.assertFalse(module.hasPendingContents(),"empty tank counted as inventory");
            } catch(IllegalAccessException error){throw new AssertionError(error);}
            finally {try {field(module,"nbt").set(module,null);field(module,"shouldReadNBT").setBoolean(module,false);} catch(IllegalAccessException error){throw new AssertionError(error);}}
        });
    }

    @GameTest(template="empty_7x7x7")
    public static void reprocessorSelectsAndPullsAlternativeFluid(GameTestHelper h) {
        var manager=h.getLevel().getRecipeManager();
        var saved=new java.util.ArrayList<>(manager.getRecipes());
        try {
            var ingredient=net.minecraft.world.item.crafting.Ingredient.of(biggerreactorsrework.items.ingots.CyaniteIngot.INSTANCE);
            var output=new net.minecraft.world.item.ItemStack(biggerreactorsrework.items.ingots.BlutoniumIngot.INSTANCE);
            var water=new biggerreactorsrework.machine.recipe.ReprocessingRecipe(ingredient,1,output,net.minecraft.world.level.material.Fluids.WATER,2,0,1);
            var lava=new biggerreactorsrework.machine.recipe.ReprocessingRecipe(ingredient,1,output,net.minecraft.world.level.material.Fluids.LAVA,2,0,1);
            var recipes=new java.util.ArrayList<net.minecraft.world.item.crafting.RecipeHolder<?>>();
            recipes.add(new net.minecraft.world.item.crafting.RecipeHolder<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(BiggerReactors.modid,"audit_a_water"),water));
            recipes.add(new net.minecraft.world.item.crafting.RecipeHolder<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(BiggerReactors.modid,"audit_b_lava"),lava));
            manager.replaceRecipes(recipes);
            h.setBlock(2,2,2,biggerreactorsrework.machine.blocks.CyaniteReprocessor.INSTANCE);
            var tile=(biggerreactorsrework.machine.tiles.CyaniteReprocessorTile)h.getBlockEntity(new BlockPos(2,2,2));tile.tick();
            tile.itemHandlerFor(null).insertItem(0,new net.minecraft.world.item.ItemStack(biggerreactorsrework.items.ingots.CyaniteIngot.INSTANCE),false);
            var source=new net.neoforged.neoforge.fluids.capability.templates.FluidTank(1000);
            source.fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.LAVA,1000),net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            var method=tile.getClass().getDeclaredMethod("pullRecipeFluid",net.neoforged.neoforge.fluids.capability.IFluidHandler.class,int.class);method.setAccessible(true);
            h.assertTrue((Integer)method.invoke(tile,source,1000)==1000,"automatic transfer ignored the alternative recipe fluid");
            tile.tick();tile.tick();
            h.assertTrue(tile.itemHandlerFor(null).getStackInSlot(1).is(output.getItem()),"first water recipe blocked the lava recipe");
            h.assertTrue(tile.fluidFor(null).getFluidInTank(0).getAmount()==998 && source.isEmpty(),"fluid balance failed");
        } catch(ReflectiveOperationException error){throw new AssertionError(error);}
        finally {manager.replaceRecipes(saved);}
        h.succeed();
    }

    @GameTest(template="empty_7x7x7")
    public static void quarantinedFluidStillCountsAsStoredContents(GameTestHelper h) {
        var original=new biggerreactorsrework.util.FluidTransitionTank(false);original.perSideCapacity=1000;
        original.fill(net.minecraft.world.level.material.Fluids.WATER,net.minecraft.core.component.DataComponentPatch.EMPTY,1000,false);
        var saved=original.serializeNBT();saved.putString("profile","incompatible-profile");
        var restored=new biggerreactorsrework.util.FluidTransitionTank(false);restored.perSideCapacity=1000;restored.deserializeNBT(saved);
        h.assertTrue(restored.hasStoredContents(),"quarantined inventory was treated as empty");
        h.assertTrue(restored.serializeNBT().getLong("inAmount")==1000,"quarantined inventory lost");h.succeed();
    }
}
