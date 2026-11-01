package biggerreactorsrework.gametest;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.core.multiblock.common.IPersistentMultiblockTile;
import biggerreactorsrework.multiblocks.turbine.blocks.*;
import biggerreactorsrework.multiblocks.turbine.tiles.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;

/** Modified 2026-10-26: assemble real turbines from loaded parts with pending inventories. */
@GameTestHolder(BiggerReactors.modid)
@PrefixGameTestTemplate(false)
public final class TurbineReloadGameTests {
    private static final BlockPos TERMINAL=new BlockPos(1,3,3);
    private static CompoundTag contents() {
        var data=new CompoundTag();var fluid=new CompoundTag();
        fluid.putString("inFluid","biggerreactorsrework:steam");fluid.putString("outFluid","minecraft:water");
        fluid.putLong("inAmount",1000);fluid.putLong("outAmount",2000);data.put("fluidTank",fluid);
        var battery=new CompoundTag();battery.putLong("storedPower",3000);data.put("battery",battery);
        data.putBoolean("coilEngaged",true);data.putLong("maxFlowRate",1000);return data;
    }
    private static void build(GameTestHelper h,boolean loaded,boolean replicas) {
        for(int x=1;x<=5;x++)for(int y=1;y<=5;y++)for(int z=1;z<=5;z++)
            if(x==1 || x==5 || y==1 || y==5 || z==1 || z==5)h.setBlock(x,y,z,TurbineCasing.INSTANCE);
        h.setBlock(TERMINAL,TurbineTerminal.INSTANCE);
        h.setBlock(3,1,3,TurbineRotorBearing.INSTANCE);h.setBlock(3,5,3,TurbineRotorBearing.INSTANCE);
        for(int y=2;y<=4;y++)h.setBlock(3,y,3,TurbineRotorShaft.INSTANCE);
        for(int[] offset:new int[][]{{1,0},{-1,0},{0,1},{0,-1}})h.setBlock(3+offset[0],2,3+offset[1],TurbineRotorBlade.INSTANCE);
        for(int x=2;x<=4;x++)for(int z=2;z<=4;z++)if(x!=3 || z!=3)h.setBlock(x,4,z,Blocks.IRON_BLOCK);
        if(loaded) {
            var origin=java.util.UUID.randomUUID();
            for(int x=1;x<=5;x++)for(int y=1;y<=5;y++)for(int z=1;z<=5;z++){
                var tile=h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(x,y,z)));
                if(!(tile instanceof TurbineBaseTile part))continue;
                part.multiblockModule().readNBT(new CompoundTag());
                var persistent=new CompoundTag();persistent.putString("last_assembly_state","DISASSEMBLED");
                if(replicas)persistent.putUUID("machine_origin",origin);
                if(replicas || x==1 && y==3 && z==3)persistent.put("controller_data",contents());
                part.module(IPersistentMultiblockTile.class,IPersistentMultiblockTile.Module.class).readNBT(persistent);
            }
        }
    }
    private static void assertAssembled(GameTestHelper h) {
        var terminal=(TurbineTerminalTile)h.getBlockEntity(TERMINAL);
        h.assertTrue(terminal.nullableController()!=null,"no controller");
        var bottom=(TurbineRotorBearingTile)h.getBlockEntity(new BlockPos(3,1,3));
        var top=(TurbineRotorBearingTile)h.getBlockEntity(new BlockPos(3,5,3));
        h.assertTrue(bottom.nullableController()==terminal.nullableController() && top.nullableController()==terminal.nullableController(),
                "bearings and terminal were split into different controllers");
        h.assertTrue(terminal.controller().assemblyState()==biggerreactorsrework.core.multiblock.validated.IValidatedMultiblock.AssemblyState.ASSEMBLED,
                "turbine did not assemble: "+terminal.controller().getDebugInfo());
    }
    @GameTest(template="empty_7x7x7",timeoutTicks=120)
    public static void freshTurbineAssembles(GameTestHelper h){build(h,false,false);h.succeedWhen(()->assertAssembled(h));}
    @GameTest(template="empty_7x7x7",timeoutTicks=120)
    public static void loadedStockedTurbineAssembles(GameTestHelper h){build(h,true,false);h.succeedWhen(()->assertAssembled(h));}
    @GameTest(template="empty_7x7x7",timeoutTicks=120)
    public static void loadedSameOriginReplicasAssemble(GameTestHelper h){build(h,true,true);h.succeedWhen(()->assertAssembled(h));}

    private static void largePhase(GameTestHelper h,int from,int to,java.util.UUID origin) {
        for(int x=from;x<=to;x++)for(int y=1;y<=12;y++)for(int z=1;z<=9;z++) {
            var pos=new BlockPos(x,y,z);net.minecraft.world.level.block.Block block=Blocks.AIR;
            if(x==1 || x==9 || y==1 || y==12 || z==1 || z==9)block=TurbineCasing.INSTANCE;
            if(x==1 && y==3 && z==5)block=TurbineTerminal.INSTANCE;
            if(x==5 && z==5)block=y==1 || y==12?TurbineRotorBearing.INSTANCE:TurbineRotorShaft.INSTANCE;
            if(y>=2 && y<=7 && ((x==5 && z>=2 && z<=8) || (z==5 && x>=2 && x<=8)) && !(x==5 && z==5))block=TurbineRotorBlade.INSTANCE;
            if(y>=10 && y<=11 && x>=2 && x<=8 && z>=2 && z<=8 && !(x==5 && z==5))block=Blocks.IRON_BLOCK;
            h.setBlock(pos,block);
            var tile=h.getLevel().getBlockEntity(h.absolutePos(pos));
            if(tile instanceof TurbineBaseTile part) {
                part.multiblockModule().readNBT(new CompoundTag());
                var saved=new CompoundTag();saved.putString("last_assembly_state","DISASSEMBLED");
                if(origin!=null)saved.putUUID("machine_origin",origin);
                if((x+y+z)%7==0 || x==1 && y==3 && z==5)saved.put("controller_data",contents());
                part.module(IPersistentMultiblockTile.class,IPersistentMultiblockTile.Module.class).readNBT(saved);
            }
        }
    }
    private static void largeReload(GameTestHelper h,boolean identified) {
        var origin=identified?java.util.UUID.randomUUID():null;
        largePhase(h,1,4,origin);
        h.startSequence().thenExecuteAfter(15,()->largePhase(h,5,9,origin))
            .thenWaitUntil(()->{
                var bottom=(TurbineRotorBearingTile)h.getBlockEntity(new BlockPos(5,1,5));
                var top=(TurbineRotorBearingTile)h.getBlockEntity(new BlockPos(5,12,5));
                var terminal=(TurbineTerminalTile)h.getBlockEntity(new BlockPos(1,3,5));
                h.assertTrue(bottom!=null && top!=null && terminal!=null,"missing physical parts");
                h.assertTrue(bottom.nullableController()==top.nullableController() && bottom.nullableController()==terminal.nullableController(),"large turbine left split controllers after reload");
                h.assertTrue(terminal.nullableController()!=null && terminal.controller().assemblyState()==biggerreactorsrework.core.multiblock.validated.IValidatedMultiblock.AssemblyState.ASSEMBLED,"large turbine not assembled");
            }).thenSucceed();
    }
    @GameTest(template="empty_17x17x17",batch="large_reload",timeoutTicks=160)
    public static void largeStockedReloadWithoutOrigin(GameTestHelper h){largeReload(h,false);}
    @GameTest(template="empty_17x17x17",batch="large_reload",timeoutTicks=160)
    public static void largeStockedReloadWithOrigin(GameTestHelper h){largeReload(h,true);}

    @SuppressWarnings("unchecked")
    @GameTest(template="empty_7x7x7")
    public static void queuedMergeFollowsAlreadyMergedController(GameTestHelper h) {
        try {
            var controllers=new biggerreactorsrework.multiblocks.turbine.TurbineMultiblockController[3];
            for(int i=0;i<3;i++) {
                var pos=new BlockPos(1+i*2,2,2);h.setBlock(pos,TurbineRotorBearing.INSTANCE);
                var part=(TurbineBaseTile)h.getBlockEntity(pos);
                controllers[i]=part.createController();controllers[i].attemptAttach(part.multiblockModule(),true);
            }
            var cls=biggerreactorsrework.core.multiblock.MultiblockController.class;
            var queue=cls.getDeclaredField("controllersToMerge");queue.setAccessible(true);
            var merge=cls.getDeclaredMethod("processMerges");merge.setAccessible(true);
            ((java.util.Set<Object>)queue.get(controllers[2])).add(controllers[1]);merge.invoke(controllers[2]);
            ((java.util.Set<Object>)queue.get(controllers[0])).add(controllers[1]);merge.invoke(controllers[0]);
            h.assertTrue(controllers[0].blocks.size()==3,"queued alias lost the live controller and its rotor bearings");
        }catch(ReflectiveOperationException error){throw new AssertionError(error);}
        h.succeed();
    }

    private static void loadSavedRange(GameTestHelper h,int low,int high) { loadSavedRange(h,low,high,0); }
    private static void loadSavedRange(GameTestHelper h,int low,int high,int seed) {
        try(var input=TurbineReloadGameTests.class.getResourceAsStream("/data/biggerreactorsrework/structure/saved_turbine_reload.nbt")) {
            var saved=net.minecraft.nbt.NbtIo.readCompressed(java.util.Objects.requireNonNull(input),net.minecraft.nbt.NbtAccounter.unlimitedHeap());
            var palette=saved.getList("palette",net.minecraft.nbt.Tag.TAG_COMPOUND);
            var blocks=saved.getList("blocks",net.minecraft.nbt.Tag.TAG_COMPOUND);
            var base=h.absolutePos(new BlockPos(1,1,1));
            var order=new java.util.ArrayList<Integer>();for(int i=0;i<blocks.size();i++)order.add(i);
            if(seed!=0)java.util.Collections.shuffle(order,new java.util.Random(seed));
            for(int i:order) {
                var entry=blocks.getCompound(i);var pos=entry.getList("pos",net.minecraft.nbt.Tag.TAG_LONG);
                int localX=((net.minecraft.nbt.NumericTag)pos.get(0)).getAsInt();
                if(localX<low || localX>high)continue;
                var relative=new BlockPos(localX+1,((net.minecraft.nbt.NumericTag)pos.get(1)).getAsInt()+1,((net.minecraft.nbt.NumericTag)pos.get(2)).getAsInt()+1);
                var state=net.minecraft.nbt.NbtUtils.readBlockState(net.minecraft.core.registries.BuiltInRegistries.BLOCK.asLookup(),palette.getCompound(entry.getInt("state")));
                h.setBlock(relative,state);
                if(!entry.contains("nbt"))continue;
                var tile=(TurbineBaseTile)h.getBlockEntity(relative);var data=entry.getCompound("nbt").copy();
                var touching=data.getCompound("sub").getCompound("touching_multiblock");
                touching.putInt("minx",touching.getInt("minx")-12+base.getX());touching.putInt("maxx",touching.getInt("maxx")-12+base.getX());
                touching.putInt("miny",touching.getInt("miny")+base.getY());touching.putInt("maxy",touching.getInt("maxy")+base.getY());
                touching.putInt("minz",touching.getInt("minz")-18+base.getZ());touching.putInt("maxz",touching.getInt("maxz")-18+base.getZ());
                tile.loadWithComponents(data,h.getLevel().registryAccess());
            }
        }catch(java.io.IOException error){throw new AssertionError(error);}
    }
    private static void assertSavedAssembled(GameTestHelper h) {
            var bottom=(TurbineRotorBearingTile)h.getBlockEntity(new BlockPos(5,1,5));
            var top=(TurbineRotorBearingTile)h.getBlockEntity(new BlockPos(5,12,5));
            h.assertTrue(bottom.nullableController()!=null && bottom.nullableController()==top.nullableController(),"saved turbine bearings belong to different controllers");
            h.assertTrue(bottom.controller().assemblyState()==biggerreactorsrework.core.multiblock.validated.IValidatedMultiblock.AssemblyState.ASSEMBLED,"saved turbine did not reassemble");
            h.assertTrue(bottom.controller().simulation().fluidTank().liquidAmount()==4200000,"saved condensed water was lost or duplicated");
            h.assertTrue(bottom.controller().simulation().battery().stored()>=18300000,"saved energy was lost on reconstruction");
    }
    @GameTest(template="empty_17x17x17",batch="saved_reload",timeoutTicks=180)
    public static void savedPlayerTurbineReassembles(GameTestHelper h) {
        loadSavedRange(h,0,8);h.succeedWhen(()->assertSavedAssembled(h));
    }
    @GameTest(template="empty_17x17x17",batch="saved_reload_staged",timeoutTicks=180)
    public static void savedPlayerTurbineAcrossChunksReassembles(GameTestHelper h) {
        loadSavedRange(h,0,3);
        h.startSequence().thenExecuteAfter(20,()->loadSavedRange(h,4,8)).thenWaitUntil(()->assertSavedAssembled(h)).thenSucceed();
    }
    @GameTest(template="empty_17x17x17",batch="saved_random_1",timeoutTicks=180)
    public static void savedTurbineLoadOrder1(GameTestHelper h) {
        loadSavedRange(h,0,8,1);h.succeedWhen(()->assertSavedAssembled(h));
    }
    @GameTest(template="empty_17x17x17",batch="saved_random_7",timeoutTicks=180)
    public static void savedTurbineLoadOrder7(GameTestHelper h) {
        loadSavedRange(h,0,8,7);h.succeedWhen(()->assertSavedAssembled(h));
    }
    @GameTest(template="empty_17x17x17",batch="saved_random_42",timeoutTicks=180)
    public static void savedTurbineLoadOrder42(GameTestHelper h) {
        loadSavedRange(h,0,8,42);h.succeedWhen(()->assertSavedAssembled(h));
    }
    @GameTest(template="empty_17x17x17",batch="saved_random_91",timeoutTicks=180)
    public static void savedTurbineLoadOrder91(GameTestHelper h) {
        loadSavedRange(h,0,8,91);h.succeedWhen(()->assertSavedAssembled(h));
    }
    @GameTest(template="empty_17x17x17",batch="saved_random_150",timeoutTicks=180)
    public static void savedTurbineLoadOrder150(GameTestHelper h) {
        loadSavedRange(h,0,8,150);h.succeedWhen(()->assertSavedAssembled(h));
    }

    @GameTest(template="empty_7x7x7",timeoutTicks=120)
    public static void fullyConsumedSteamFlowStillSynchronizesHaze(GameTestHelper h) {
        build(h,false,false);
        h.startSequence().thenWaitUntil(()->assertAssembled(h)).thenExecute(()->{
            var bearing=(TurbineRotorBearingTile)h.getBlockEntity(new BlockPos(3,1,3));
            var sim=bearing.controller().simulation();
            var transition=biggerreactorsrework.registries.FluidTransitionRegistry.liquidTransition(net.minecraft.world.level.material.Fluids.WATER);
            long flow=Math.min(10000,sim.flowRateLimit());
            h.assertTrue(flow>0,"test turbine has no flow capacity");
            sim.setNominalFlowRate(flow);sim.setActive(true);
            for(int i=0;i<25;i++) {
                sim.fluidTank().dumpLiquid();
                h.assertTrue(sim.fluidTank().fill(transition.gases.getFirst(),net.minecraft.core.component.DataComponentPatch.EMPTY,flow,false)==flow,"exact steam feed was not accepted");
                bearing.controller().tick();
                h.assertTrue(sim.flowLastTick()==flow && sim.fluidTank().vaporAmount()==0,"test did not consume all incoming steam");
                h.assertTrue(bearing.getUpdateNBT().getFloat("steamDensity")==1,"empty-buffer flowing turbine lost haze");
                h.assertTrue(bearing.getDataNBT().getFloat("steamDensity")==1 || !bearing.isRenderBearing,"initial window snapshot omitted through-flow haze");
            }
            bearing.controller().tick();
            h.assertTrue(bearing.currentSteamDensity()==0,"starved empty turbine retained flow haze");
            sim.fluidTank().fill(transition.gases.getFirst(),net.minecraft.core.component.DataComponentPatch.EMPTY,flow,false);bearing.controller().tick();
            sim.setActive(false);
            h.assertTrue(bearing.currentSteamDensity()==0,"stale last-tick conversion kept a stopped empty turbine hazy");
            sim.fluidTank().fill(transition.gases.getFirst(),net.minecraft.core.component.DataComponentPatch.EMPTY,flow,false);
            h.assertTrue(bearing.currentSteamDensity()>0,"stopped turbine lost its stored-steam haze");
        }).thenSucceed();
    }

    @GameTest(template="empty_7x7x7",timeoutTicks=120)
    public static void storedSteamHazeAndDelayedCondensationAreIndependent(GameTestHelper h) {
        build(h,false,false);
        h.startSequence().thenWaitUntil(() -> assertAssembled(h)).thenExecute(() -> {
            var bearing = (TurbineRotorBearingTile) h.getBlockEntity(new BlockPos(3,1,3));
            var sim = bearing.controller().simulation();
            sim.setActive(false);
            var transition=biggerreactorsrework.registries.FluidTransitionRegistry.liquidTransition(net.minecraft.world.level.material.Fluids.WATER);
            var modern=(biggerreactorsrework.multiblocks.turbine.simulation.modern.ModernTurbineSimulation)sim;
            modern.idealSupply(transition);
            h.assertTrue(bearing.getUpdateNBT().getFloat("steamDensity")>0,"idle stored steam has no haze");
            h.assertTrue(!bearing.getUpdateNBT().getBoolean("condensationReady"),"idle steam started dripping");
            sim.setActive(true);sim.setCoilEngaged(false);
            for(int tick=0;tick<120;tick++) {
                modern.idealSupply(transition);
                bearing.controller().tick();
                if(tick<59)h.assertTrue(!bearing.getUpdateNBT().getBoolean("condensationReady"),"condensation started without warmup");
            }
            h.assertTrue(bearing.getUpdateNBT().getBoolean("condensationReady"),"continuous rotation did not warm condensation");
            sim.setActive(false);bearing.controller().tick();
            h.assertTrue(bearing.getUpdateNBT().getFloat("steamDensity")>0,"switching off erased stored steam haze");
            h.assertTrue(!bearing.getUpdateNBT().getBoolean("condensationReady"),"coasting turbine continued dripping");
            h.assertTrue(sim.RPM()>0,"test did not cover a coasting rotor");
            sim.fluidTank().dumpVapor();
            h.assertTrue(bearing.getUpdateNBT().getFloat("steamDensity")==0,"empty tank retained haze");
        }).thenSucceed();
    }

    @GameTest(template="empty_7x7x7",timeoutTicks=20)
    public static void rotorSpeedIsLimitedUnderExcessSteam(GameTestHelper h) {
        var sim = new biggerreactorsrework.multiblocks.turbine.simulation.modern.ModernTurbineSimulation();
        sim.resize(3, 3, 3);
        sim.setRotorConfiguration(new java.util.ArrayList<>(java.util.List.of(new org.joml.Vector4i(1, 1, 1, 1))));
        // Low induction drag ensures the steam supply can actually drive the rotor into the governor.
        sim.setCoilData(1, 0, new biggerreactorsrework.registries.TurbineCoilRegistry.CoilData(1, 1, 0.1));
        sim.updateInternalValues();
        sim.setNominalFlowRate(sim.flowRateLimit());
        sim.setActive(true);
        var steam = biggerreactorsrework.registries.FluidTransitionRegistry.liquidTransition(net.minecraft.world.level.material.Fluids.WATER);
        h.assertTrue(steam != null, "missing steam transition");
        // A legacy save already above the limit must recover before computing generation.
        var saved = sim.serializeNBT();
        saved.putDouble("rotorEnergy", sim.rotorMass() * 10000);
        sim.deserializeNBT(saved);
        for (boolean coils : new boolean[]{false, true}) {
            sim.setCoilEngaged(coils);
            for (int tick = 0; tick < 1000; tick++) {
                sim.idealSupply(steam);
                sim.tick();
                h.assertTrue(sim.RPM() <= 2200 && sim.RPM() >= 0, "rotor exceeded speed limit");
                h.assertTrue(sim.serializeNBT().getDouble("rotorEnergy") <= sim.rotorMass() * 2200,
                        "excess rotor energy accumulated behind the RPM display");
                if (coils) h.assertTrue(sim.FEGeneratedLastTick() > 0, "generation stopped at the speed limit");
                sim.battery().extract(Long.MAX_VALUE);
            }
            h.assertTrue(sim.RPM() == 2200, "excess steam did not reach the speed limit: " + sim.RPM());
        }
        sim.setActive(false);
        sim.tick();
        h.assertTrue(sim.RPM() < 2200, "rotor did not slow down without steam");
        h.succeed();
    }

    @GameTest(template="empty_7x7x7",timeoutTicks=120)
    public static void steamWindowsSynchronizeOnlyGlassAndClearOnDisassembly(GameTestHelper h) {
        build(h,false,false);
        h.setBlock(1,2,2,biggerreactorsrework.multiblocks.turbine.blocks.TurbineGlass.INSTANCE);
        h.setBlock(1,2,3,biggerreactorsrework.multiblocks.turbine.blocks.TurbineGlass.INSTANCE);
        h.startSequence().thenWaitUntil(()->assertAssembled(h)).thenExecute(()->{
            var a=(TurbineRotorBearingTile)h.getBlockEntity(new BlockPos(3,1,3));
            var b=(TurbineRotorBearingTile)h.getBlockEntity(new BlockPos(3,5,3));
            var bearing=a.isRenderBearing?a:b;
            var data=bearing.getDataNBT();
            h.assertTrue(data.getIntArray("steamGlass").length==6,"window snapshot includes non-glass or loses glass");
            var snapshot=TurbineRotorBearingTile.SUPPLIER.create(bearing.getBlockPos(),bearing.getBlockState());
            snapshot.handleDataNBT(data);
            h.assertTrue(snapshot.steamWindows.stream().mapToDouble(biggerreactorsrework.core.util.FluidSurfaceMesh.Face::area).sum()==2,"client film does not exactly cover the two windows");
            var removed=new CompoundTag();removed.putBoolean("disassembled",true);
            snapshot.handleUpdateNBT(removed);
            h.assertTrue(snapshot.steamWindows.isEmpty(),"disassembly left stale window film");
            data.remove("steamGlass");snapshot.handleDataNBT(data);
            h.assertTrue(snapshot.steamWindows.isEmpty(),"absent geometry invented window films");
        }).thenSucceed();
    }
}
