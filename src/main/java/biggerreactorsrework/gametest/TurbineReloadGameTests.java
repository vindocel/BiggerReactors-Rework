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
}
