package biggerreactorsrework.gametest;

// Added 2026-10-30: actual optional network discovery, callbacks and port lifetime.
import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.compat.ComputerPortState;
import biggerreactorsrework.multiblocks.reactor.blocks.*;
import biggerreactorsrework.multiblocks.reactor.tiles.ReactorComputerPortTile;
import biggerreactorsrework.multiblocks.turbine.blocks.*;
import biggerreactorsrework.multiblocks.heatexchanger.blocks.HeatExchangerComputerPortBlock;
import biggerreactorsrework.util.GuideStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.*;
import java.util.Map;

@GameTestHolder(BiggerReactors.modid)
@PrefixGameTestTemplate(false)
public final class OpenComputersGameTests {
    private static boolean installed() { return ModList.get().isLoaded("opencomputers"); }

    @GameTest(template = "empty_7x7x7", timeoutTicks = 140)
    public static void reactorComponentControlsCurrentRodsAndRejectsInvalidCalls(GameTestHelper h) {
        for (int x=1;x<=3;x++) for (int y=1;y<=3;y++) for (int z=1;z<=3;z++)
            if (x==1 || x==3 || y==1 || y==3 || z==1 || z==3) h.setBlock(x,y,z,ReactorCasing.INSTANCE);
        h.setBlock(2,2,2,ReactorFuelRod.INSTANCE); h.setBlock(2,3,2,ReactorControlRod.INSTANCE);
        h.setBlock(1,2,2,ReactorTerminal.INSTANCE); h.setBlock(3,2,2,ReactorComputerPort.INSTANCE);
        h.startSequence().thenWaitUntil(() -> {
            var tile=(ReactorComputerPortTile)h.getBlockEntity(new BlockPos(3,2,2));
            h.assertTrue(tile.nullableController()!=null && tile.controller().simulation()!=null,"reactor not assembled");
        }).thenExecute(() -> {
            var tile=(ReactorComputerPortTile)h.getBlockEntity(new BlockPos(3,2,2));
            if (installed()) Probe.reactor(h,tile);
            else h.assertTrue(tile.computerPortState().component()==null,"optional component created without its mod");
        }).thenExecute(() -> h.setBlock(3,3,3,Blocks.AIR))
          .thenWaitUntil(() -> {
              if (installed()) h.assertTrue(Boolean.FALSE.equals(Probe.call(h,new BlockPos(3,2,2),"connected")),"component retained assembled controller");
          }).thenSucceed();
    }

    @GameTest(template = "empty_7x7x7", timeoutTicks = 140)
    public static void turbineComponentReadsAndChangesLiveControls(GameTestHelper h) {
        if (!installed()) { h.succeed(); return; }
        for (int x=1;x<=5;x++) for (int y=1;y<=5;y++) for (int z=1;z<=5;z++)
            if (x==1 || x==5 || y==1 || y==5 || z==1 || z==5) h.setBlock(x,y,z,TurbineCasing.INSTANCE);
        h.setBlock(1,3,3,TurbineTerminal.INSTANCE); h.setBlock(5,3,3,TurbineComputerPort.INSTANCE);
        h.setBlock(3,1,3,TurbineRotorBearing.INSTANCE); h.setBlock(3,5,3,TurbineRotorBearing.INSTANCE);
        for (int y=2;y<=4;y++) h.setBlock(3,y,3,TurbineRotorShaft.INSTANCE);
        for (int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) h.setBlock(3+d[0],2,3+d[1],TurbineRotorBlade.INSTANCE);
        for (int x=2;x<=4;x++) for (int z=2;z<=4;z++) if (x!=3 || z!=3) h.setBlock(x,4,z,Blocks.IRON_BLOCK);
        var pos=new BlockPos(5,3,3);
        h.startSequence().thenWaitUntil(() -> h.assertTrue(Boolean.TRUE.equals(Probe.call(h,pos,"connected")),"turbine not assembled"))
            .thenExecute(() -> {
                Probe.call(h,pos,"setActive",true); Probe.call(h,pos,"setCoilEngaged",false);
                Probe.call(h,pos,"setNominalFlowRate",1d); Probe.call(h,pos,"setVentState","overflow");
                h.assertTrue(Boolean.TRUE.equals(Probe.call(h,pos,"active")),"activity command failed");
                h.assertTrue(Boolean.FALSE.equals(Probe.call(h,pos,"coilEngaged")),"coil command failed");
                h.assertTrue("overflow".equals(Probe.call(h,pos,"ventState")),"vent command failed");
                var tank=(Map<?,?>)Probe.call(h,pos,"fluidTank");
                h.assertTrue(((Number)tank.get("nominalFlowRate")).longValue()==1,"flow command failed");
                h.assertTrue(Probe.call(h,pos,"battery") instanceof Map && Probe.call(h,pos,"rotor") instanceof Map,"missing telemetry");
                Probe.reject(h,pos,"setNominalFlowRate",Double.NaN); Probe.reject(h,pos,"setNominalFlowRate",-1d);
                Probe.reject(h,pos,"setNominalFlowRate",.5d); Probe.reject(h,pos,"setVentState","invalid");
            }).thenSucceed();
    }

    @GameTest(template = "empty_7x7x7", timeoutTicks = 140)
    public static void exchangerComponentReturnsDetachedChannelTables(GameTestHelper h) {
        if (!installed()) { h.succeed(); return; }
        GuideStructures.exchanger().parts().forEach(h::setBlock);
        var pos=new BlockPos(2,1,0);
        h.setBlock(pos,HeatExchangerComputerPortBlock.INSTANCE);
        h.startSequence().thenWaitUntil(() -> h.assertTrue(Boolean.TRUE.equals(Probe.call(h,pos,"connected")),"exchanger not assembled"))
            .thenExecute(() -> {
                for (var name:new String[]{"condenser","evaporator"}) {
                    var table=(Map<?,?>)Probe.call(h,pos,name);
                    h.assertTrue(table.get("input") instanceof Map && table.get("output") instanceof Map,"channel fluids missing");
                    h.assertTrue(Double.isFinite(((Number)table.get("temperature")).doubleValue()),"invalid channel temperature");
                }
                h.assertTrue(Probe.call(h,pos,"internalEnvironment") instanceof Map,"missing internal heat telemetry");
                Probe.call(h,pos,"dumpCondenser"); Probe.call(h,pos,"dumpEvaporator");
            }).thenSucceed();
    }

    @GameTest(template = "empty_7x7x7", timeoutTicks = 100)
    public static void nativeCableFindsAllPortsAndAddressesSurviveReload(GameTestHelper h) {
        var blocks=new net.minecraft.world.level.block.Block[]{ReactorComputerPort.INSTANCE,TurbineComputerPort.INSTANCE,HeatExchangerComputerPortBlock.INSTANCE};
        if (!installed()) {
            for(int i=0;i<blocks.length;i++) {
                var pos=new BlockPos(1,1,1+i*2); h.setBlock(pos,blocks[i]);
                var tile=h.getBlockEntity(pos); var registries=h.getLevel().registryAccess();
                var saved=tile.saveWithFullMetadata(registries);
                var opaque=new net.minecraft.nbt.CompoundTag(); opaque.putString("opaque_test_address","retained-without-optional-mod");
                var local=saved.getCompound("local"); local.put("computerComponent",opaque); saved.put("local",local);
                tile.loadWithComponents(saved,registries);
                h.assertTrue(tile.saveWithFullMetadata(registries).getCompound("local").getCompound("computerComponent").equals(opaque),
                        "port discarded optional component data while OpenComputers was absent");
                h.assertTrue(((ComputerPortState.Port)tile).computerPortState().component()==null,"optional API loaded in baseline");
            }
            h.succeed(); return;
        }
        for(int i=0;i<blocks.length;i++) {
            h.setBlock(1,1,1+i*2,blocks[i]);
            h.setBlock(2,1,1+i*2,BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("opencomputers","cable")));
        }
        h.startSequence().thenIdle(5).thenExecute(() -> Probe.lifetime(h)).thenSucceed();
    }

    /** Keep optional API linkage out of test entry points so the baseline runs without OC. */
    private static final class Probe {
        static li.cil.oc.api.network.Component component(GameTestHelper h,BlockPos pos) {
            var environment=h.getLevel().getCapability(biggerreactorsrework.compat.opencomputers.OpenComputersCompat.ENVIRONMENT,h.absolutePos(pos),Direction.EAST);
            h.assertTrue(environment!=null,"native environment capability missing");
            return (li.cil.oc.api.network.Component)environment.node();
        }
        static Object call(GameTestHelper h,BlockPos pos,String method,Object... args) {
            try { return decode(component(h,pos).invoke(method,null,args)[0]); }
            catch(Exception error) { throw new AssertionError("OC callback failed: "+method,error); }
        }
        // OC's real registry converts Java maps to its relocated Scala maps and strings to bytes.
        // Decode the public Scala interfaces reflectively without adding a runtime dependency to tests.
        static Object decode(Object value) throws ReflectiveOperationException {
            if (value instanceof byte[] bytes) return new String(bytes,java.nio.charset.StandardCharsets.UTF_8);
            var mapType=Class.forName("li.cil.oc.internal.scalalib.collection.Map");
            if (!mapType.isInstance(value)) return value;
            var iteratorType=Class.forName("li.cil.oc.internal.scalalib.collection.Iterator");
            var productType=Class.forName("li.cil.oc.internal.scalalib.Product");
            var iterator=mapType.getMethod("iterator").invoke(value);
            var result=new java.util.LinkedHashMap<Object,Object>();
            while ((Boolean)iteratorType.getMethod("hasNext").invoke(iterator)) {
                var pair=iteratorType.getMethod("next").invoke(iterator);
                var key=productType.getMethod("productElement",int.class).invoke(pair,0);
                var entry=productType.getMethod("productElement",int.class).invoke(pair,1);
                result.put(decode(key),decode(entry));
            }
            return result;
        }
        static void reject(GameTestHelper h,BlockPos pos,String method,Object... args) {
            try { component(h,pos).invoke(method,null,args); }
            catch(Exception expected) { return; }
            throw new AssertionError("OC accepted invalid arguments for "+method);
        }
        static void reactor(GameTestHelper h,ReactorComputerPortTile tile) {
            var pos=new BlockPos(3,2,2);
            h.assertTrue(((Number)call(h,pos,"controlRodCount")).intValue()==1,"late attachment missed rods");
            call(h,pos,"setControlRodLevel",0,42d); call(h,pos,"setControlRodName",0,"Server rod");
            h.assertTrue(tile.controller().controlRodLevel(0)==42 && tile.controller().controlRodName(0).equals("Server rod"),"rod commands failed");
            call(h,pos,"setAllControlRodLevels",25d);
            h.assertTrue(((Number)((Map<?,?>)call(h,pos,"getControlRod",0)).get("level")).doubleValue()==25,"rod snapshot stale");
            h.assertTrue(call(h,pos,"fuelTank") instanceof Map && call(h,pos,"battery") instanceof Map,"reactor telemetry missing");
            reject(h,pos,"getControlRod",-1); reject(h,pos,"getControlRod",.5d); reject(h,pos,"setControlRodLevel",0,Double.NaN);
            reject(h,pos,"setControlRodLevel",0,101d); reject(h,pos,"setControlRodName",0,"x".repeat(129));
            var node=component(h,pos);
            for(var name:node.methods()) h.assertFalse(node.annotation(name).direct(),"live callback allowed computer executor access: "+name);
            var failure=java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                try { node.invoke("setControlRodLevel",null,0,99d); return false; }
                catch(Exception expected) { return true; }
            }).join();
            h.assertTrue(failure && tile.controller().controlRodLevel(0)==25,"off-thread callback changed live state");
        }
        static void lifetime(GameTestHelper h) {
            var names=new String[]{"reactor","turbine","heat_exchanger"};
            for(int i=0;i<names.length;i++) {
                var pos=new BlockPos(1,1,1+i*2); var tile=h.getBlockEntity(pos); var node=component(h,pos);
                h.assertTrue(node.name().equals("biggerreactorsrework_"+names[i]),"wrong component type");
                h.assertTrue(node.methods().contains("connected") && node.methods().contains("apiVersion"),"callbacks not discovered");
                var cable=h.getLevel().getCapability(biggerreactorsrework.compat.opencomputers.OpenComputersCompat.ENVIRONMENT,h.absolutePos(pos.east()),Direction.WEST);
                h.assertTrue(cable!=null && node.isNeighborOf(cable.node()),"native cable did not connect to port");
                var address=node.address(); h.assertTrue(address!=null,"no assigned address");
                var saved=tile.saveWithFullMetadata(h.getLevel().registryAccess());
                tile.onChunkUnloaded(); h.assertTrue(node.network()==null,"unloaded port leaked node");
                h.assertTrue(Boolean.FALSE.equals(call(h,pos,"connected")),"unloaded port still reports connected");
                reject(h,pos,i==2 ? "condenser" : "active");
                // A fresh entity must restore the exact address before rejoining the network.
                h.getLevel().removeBlockEntity(tile.getBlockPos());
                var restored=net.minecraft.world.level.block.entity.BlockEntity.loadStatic(tile.getBlockPos(),tile.getBlockState(),saved,h.getLevel().registryAccess());
                h.getLevel().setBlockEntity(restored); restored.onLoad();
                var next=component(h,pos); h.assertTrue(address.equals(next.address()),"component address changed on reload");
                h.assertTrue(next.isNeighborOf(cable.node()),"restored port did not rejoin cable");
                h.setBlock(pos,Blocks.AIR); h.assertTrue(next.network()==null,"removed port leaked node");
            }
        }
    }
}
