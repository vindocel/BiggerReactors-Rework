package biggerreactorsrework.gametest;

import biggerreactorsrework.multiblocks.turbine.blocks.*;
import biggerreactorsrework.multiblocks.turbine.tiles.TurbineBaseTile;
import biggerreactorsrework.multiblocks.turbine.TurbineRotorLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import java.util.UUID;

/** Added 2026-10-31: actual block interactions, native placement rollback, protected removal and rotor assembly. */
// Modified 2026-11-01: cover construction-time placement with open walls/roof and the minimum vertical base/rotor.
public final class TurbineRotorPlacementGameTests {
    private record Fixture(GameTestHelper h, Direction.Axis axis, int size, int center) {
        BlockPos pos(int x,int y,int z) { return h.absolutePos(switch(axis){case X->new BlockPos(y,x,z);case Y->new BlockPos(x,y,z);case Z->new BlockPos(x,z,y);}); }
        void set(int x,int y,int z,net.minecraft.world.level.block.Block block){h.getLevel().setBlockAndUpdate(pos(x,y,z),block.defaultBlockState());}
        BlockPos shaft(int y){return pos(center,y,center);}
        void build(boolean coils){
            for(int x=1;x<=size;x++)for(int y=1;y<=7;y++)for(int z=1;z<=size;z++)if(x==1||x==size||y==1||y==7||z==1||z==size)set(x,y,z,TurbineCasing.INSTANCE);
            set(1,3,center,TurbineTerminal.INSTANCE);
            for(int y=1;y<=7;y++)set(center,y,center,y==1||y==7?TurbineRotorBearing.INSTANCE:TurbineRotorShaft.INSTANCE);
            if(coils)for(int x=2;x<size;x++)for(int z=2;z<size;z++)if(x!=center||z!=center)for(int y=5;y<=6;y++)set(x,y,z,Blocks.IRON_BLOCK);
        }
        void ready(){
            var part=(TurbineBaseTile)h.getLevel().getBlockEntity(shaft(2));
            h.assertTrue(part!=null&&part.nullableController()!=null,"shaft not attached");
            var controller=part.controller();
            h.assertTrue(controller.max().get(axis.ordinal())-controller.min().get(axis.ordinal())==6,"structure not merged");
        }
        void openFrame(boolean bareVertical){
            for(int x=1;x<=size;x++)for(int y=1;y<=7;y++)for(int z=1;z<=size;z++) {
                int boundaries=(x==1||x==size?1:0)+(y==1||y==7?1:0)+(z==1||z==size?1:0);
                if(boundaries==0)continue;
                boolean floor=switch(axis){case X->x==1;case Y->y==1;case Z->z==1;};
                boolean bearing=(y==1||y==7)&&x==center&&z==center;
                boolean beams=!bareVertical&&(y==1||y==7)&&(x==center||z==center);
                if(!floor&&!bearing&&!beams&&(bareVertical||boundaries<2))set(x,y,z,Blocks.AIR);
            }
        }
        FakePlayer player(boolean creative){
            var p=new FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"rotor_test"));
            p.setGameMode(creative?GameType.CREATIVE:GameType.SURVIVAL);
            p.setPos(Vec3.atCenterOf(pos(center,2,0)));
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(TurbineRotorBlade.INSTANCE,64));
            return p;
        }
        void click(FakePlayer p,BlockPos target,InteractionHand hand){
            var state=h.getLevel().getBlockState(target);
            var result=p.gameMode.useItemOn(p,h.getLevel(),p.getItemInHand(hand),hand,new BlockHitResult(Vec3.atCenterOf(target),Direction.UP,target,false));
            h.assertTrue(result.consumesAction(),"rotor click failed at "+target+": "+result);
        }
        int count(int y){int n=0;for(int x=2;x<size;x++)for(int z=2;z<size;z++)if(h.getLevel().getBlockState(pos(x,y,z)).is(TurbineRotorBlade.INSTANCE))n++;return n;}
    }
    private static Fixture fixture(GameTestHelper h,Direction.Axis a,int size,int center,boolean coils){var f=new Fixture(h,a,size,center);f.build(coils);return f;}

    @GameTest(template="empty_17x17x17",timeoutTicks=140)
    public static void ringsLevelsSurvivalAndAssembly(GameTestHelper h){
        var f=fixture(h,Direction.Axis.Y,7,4,true);
        h.startSequence().thenWaitUntil(f::ready).thenExecute(()->{
            var p=f.player(false);
            for(int i=0;i<4;i++)f.click(p,f.shaft(4),InteractionHand.MAIN_HAND);
            for(var arm:TurbineRotorLayout.arms(Direction.Axis.Y))h.assertTrue(h.getLevel().getBlockState(f.shaft(2).relative(arm)).is(TurbineRotorBlade.INSTANCE),"first ring was not balanced");
            for(int i=4;i<24;i++)f.click(p,f.shaft(4),InteractionHand.MAIN_HAND);
            h.assertTrue(f.count(2)==8&&f.count(3)==8&&f.count(4)==8&&f.count(5)==0,"levels or coil separation incorrect");
            h.assertTrue(p.getMainHandItem().getCount()==40,"survival did not consume exactly 24");
            var result=TurbineRotorLayout.use(h.getLevel(),f.shaft(2),p,InteractionHand.MAIN_HAND);
            h.assertTrue(result.consumesAction()&&p.getMainHandItem().getCount()==40,"full rotor leaked fallback or consumed an item");
        }).thenWaitUntil(()->{
            var t=(TurbineBaseTile)h.getLevel().getBlockEntity(f.shaft(2));
            h.assertTrue(t.getBlockState().getValue(biggerreactorsrework.core.multiblock.IAssemblyStateBlock.ASSEMBLED),"assisted rotor did not assemble");
            int sum=t.controller().rotorConfiguration.stream().mapToInt(v->v.x+v.y+v.z+v.w).sum();
            h.assertTrue(sum==24,"simulation rotor does not match placed blocks");
        }).thenSucceed();
    }
    @GameTest(template="empty_17x17x17",timeoutTicks=100)
    public static void nineWideCreativeAndOffhand(GameTestHelper h){
        var f=fixture(h,Direction.Axis.Y,9,5,true);
        h.startSequence().thenWaitUntil(f::ready).thenExecute(()->{
            var p=f.player(true);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(TurbineRotorBlade.INSTANCE,7));
            for(int i=0;i<12;i++)f.click(p,f.pos(5,1,5),InteractionHand.OFF_HAND);
            h.assertTrue(f.count(2)==12&&p.getOffhandItem().getCount()==7,"nine-wide arm capacity or creative consumption incorrect");
        }).thenSucceed();
    }
    @GameTest(template="empty_17x17x17",timeoutTicks=100)
    public static void horizontalXIgnoresDefaultShaftState(GameTestHelper h){horizontal(h,Direction.Axis.X);}
    @GameTest(template="empty_17x17x17",timeoutTicks=100)
    public static void horizontalZIgnoresDefaultShaftState(GameTestHelper h){horizontal(h,Direction.Axis.Z);}
    private static void horizontal(GameTestHelper h,Direction.Axis axis){
        var f=fixture(h,axis,7,4,true);
        h.startSequence().thenWaitUntil(f::ready).thenExecute(()->{
            var p=f.player(false);for(int i=0;i<8;i++)f.click(p,f.shaft(3),InteractionHand.MAIN_HAND);
            h.assertTrue(f.count(2)==8,"horizontal rotor used stale Y state");
            for(var arm:TurbineRotorLayout.arms(axis)){
                var state=h.getLevel().getBlockState(f.shaft(2).relative(arm));
                h.assertTrue(state.getValue(biggerreactorsrework.multiblocks.turbine.state.TurbineShaftRotationState.TURBINE_SHAFT_ROTATION_STATE_ENUM_PROPERTY).ordinal()==axis.ordinal(),"unassembled blade has wrong axis");
            }
        }).thenSucceed();
    }
    @GameTest(template="empty_17x17x17",timeoutTicks=100)
    public static void asymmetricArmsAndBladeRedirect(GameTestHelper h){
        var f=fixture(h,Direction.Axis.Y,8,4,true);
        h.startSequence().thenWaitUntil(f::ready).thenExecute(()->{
            var p=f.player(false);f.click(p,f.shaft(3),InteractionHand.MAIN_HAND);
            BlockPos blade=f.shaft(2).relative(Direction.NORTH);
            for(int i=1;i<10;i++)f.click(p,blade,InteractionHand.MAIN_HAND);
            h.assertTrue(f.count(2)==10&&f.count(3)==0,"asymmetric ring capacity incorrect");
            h.assertTrue(h.getLevel().getBlockState(f.shaft(2).relative(Direction.EAST,3)).is(TurbineRotorBlade.INSTANCE),"long side omitted");
        }).thenSucceed();
    }
    @GameTest(template="empty_17x17x17",timeoutTicks=100)
    public static void undefinedBaseAndAdventureDoNotConsume(GameTestHelper h){
        var f=fixture(h,Direction.Axis.Y,7,4,false);
        h.startSequence().thenWaitUntil(f::ready).thenExecute(()->{
            var p=f.player(false);f.set(1,1,3,Blocks.AIR);
            f.click(p,f.shaft(2),InteractionHand.MAIN_HAND);
            f.set(1,1,3,TurbineCasing.INSTANCE);p.getAbilities().mayBuild=false;
            TurbineRotorLayout.use(h.getLevel(),f.shaft(2),p,InteractionHand.MAIN_HAND);
            h.assertTrue(p.getMainHandItem().getCount()==64&&f.count(2)==0,"rejected interaction consumed items");
        }).thenSucceed();
    }
    @GameTest(template="empty_17x17x17",timeoutTicks=100)
    public static void baseAndTwoBearingsWorkBeforeWallsAndRoof(GameTestHelper h){
        var f=fixture(h,Direction.Axis.Y,7,4,false);f.openFrame(true);
        h.startSequence().thenWaitUntil(f::ready).thenExecute(()->{
            var p=f.player(false);
            for(int i=0;i<40;i++)f.click(p,f.pos(4,7,4),InteractionHand.MAIN_HAND);
            for(int y=2;y<=6;y++)h.assertTrue(f.count(y)==8,"open vertical rotor level incomplete: "+y);
            h.assertTrue(p.getMainHandItem().getCount()==24,"open rotor item consumption incorrect");
            f.click(p,f.shaft(4),InteractionHand.MAIN_HAND);
            h.assertTrue(p.getMainHandItem().getCount()==24,"full open rotor consumed an extra blade");
            h.assertTrue(h.getLevel().getBlockState(f.pos(1,3,4)).isAir(),"assistance filled an exterior wall");
        }).thenSucceed();
    }
    @GameTest(template="empty_17x17x17",timeoutTicks=100)
    public static void openHorizontalXFrameAcceptsBlades(GameTestHelper h){openHorizontal(h,Direction.Axis.X);}
    @GameTest(template="empty_17x17x17",timeoutTicks=100)
    public static void openHorizontalZFrameAcceptsBlades(GameTestHelper h){openHorizontal(h,Direction.Axis.Z);}
    private static void openHorizontal(GameTestHelper h,Direction.Axis axis){
        var f=fixture(h,axis,8,4,true);f.openFrame(false);
        h.startSequence().thenWaitUntil(f::ready).thenExecute(()->{
            var p=f.player(false);for(int i=0;i<10;i++)f.click(p,f.pos(4,1,4),InteractionHand.MAIN_HAND);
            h.assertTrue(f.count(2)==10&&f.count(3)==0,"open horizontal asymmetric capacity/order incorrect");
            h.assertTrue(p.getMainHandItem().getCount()==54,"open horizontal stock incorrect");
            for(var arm:TurbineRotorLayout.arms(axis))h.assertTrue(h.getLevel().getBlockState(f.shaft(2).relative(arm)).is(TurbineRotorBlade.INSTANCE),"open horizontal arm missing");
        }).thenSucceed();
    }
    @GameTest(template="empty_17x17x17",timeoutTicks=100)
    public static void missingBearingOrBrokenShaftDoesNotConsume(GameTestHelper h){
        var f=fixture(h,Direction.Axis.Y,7,4,false);f.openFrame(true);
        h.startSequence().thenWaitUntil(f::ready).thenExecute(()->{
            var p=f.player(false);f.set(4,7,4,Blocks.AIR);
            f.click(p,f.shaft(2),InteractionHand.MAIN_HAND);
            h.assertTrue(f.count(2)==0&&p.getMainHandItem().getCount()==64,"missing bearing allowed placement");
            f.set(4,7,4,TurbineRotorBearing.INSTANCE);f.set(4,4,4,Blocks.AIR);
            f.click(p,f.shaft(2),InteractionHand.MAIN_HAND);
            h.assertTrue(f.count(2)==0&&p.getMainHandItem().getCount()==64,"broken shaft allowed placement");
        }).thenSucceed();
    }
    @GameTest(template="empty_17x17x17",timeoutTicks=100)
    public static void cancelledNativePlacementRollsBackBlocksAndItems(GameTestHelper h){
        var f=fixture(h,Direction.Axis.Y,7,4,true);
        h.startSequence().thenWaitUntil(f::ready).thenExecute(()->{
            var p=f.player(false);
            java.util.function.Consumer<net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent> cancel=e->{if(e.getEntity()==p)e.setCanceled(true);};
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(cancel);
            try {
                TurbineRotorLayout.use(h.getLevel(),f.shaft(2),p,InteractionHand.MAIN_HAND);
                h.assertTrue(f.count(2)==0&&p.getMainHandItem().getCount()==64,"cancelled native placement changed stock or world");
            } finally {net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(cancel);}
        }).thenSucceed();
    }
    @GameTest(template="empty_17x17x17",timeoutTicks=100)
    public static void removingAndBreakingOnlyReturnsAttachedSegment(GameTestHelper h){
        var f=fixture(h,Direction.Axis.Y,7,4,true);
        h.startSequence().thenWaitUntil(f::ready).thenExecute(()->{
            var p=f.player(false);for(int i=0;i<16;i++)f.click(p,f.shaft(2),InteractionHand.MAIN_HAND);
            p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setShiftKeyDown(true);
            f.set(1,4,3,Blocks.AIR);
            f.click(p,f.shaft(2),InteractionHand.MAIN_HAND);
            h.assertTrue(f.count(3)==7&&f.count(2)==8,"sneaking did not reverse placement order");
            p.setShiftKeyDown(false);p.getInventory().setSelectedSlot(1);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_PICKAXE));
            TurbineRotorLayout.breakShaft(h.getLevel(),f.shaft(2),p);
            h.assertTrue(f.count(2)==0&&f.count(3)==7,"shaft cleanup crossed to another segment");
            h.assertTrue(p.getInventory().countItem(TurbineRotorBlade.INSTANCE.asItem())==9,"removed blade drops were lost or duplicated");
            h.assertTrue(p.getMainHandItem().getDamageValue()==0,"cleanup damaged the tool per blade");
        }).thenSucceed();
    }
    @GameTest(template="empty_17x17x17",timeoutTicks=100)
    public static void actualShaftBreakDamagesToolOnceAndReturnsAllBlades(GameTestHelper h){
        var f=fixture(h,Direction.Axis.Y,7,4,true);
        h.startSequence().thenWaitUntil(f::ready).thenExecute(()->{
            var p=f.player(true);for(int i=0;i<8;i++)f.click(p,f.shaft(2),InteractionHand.MAIN_HAND);
            p.setGameMode(GameType.SURVIVAL);p.getInventory().clearContent();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_PICKAXE));
            h.assertTrue(p.gameMode.destroyBlock(f.shaft(2)),"native shaft break rejected");
            h.assertTrue(f.count(2)==0&&p.getInventory().countItem(TurbineRotorBlade.INSTANCE.asItem())==8,"native shaft break lost blades");
            h.assertTrue(p.getMainHandItem().getDamageValue()==1,"tool damaged more than once");
        }).thenSucceed();
    }
    @GameTest(template="empty_17x17x17",timeoutTicks=100)
    public static void creativeAndWrongToolCleanupDoNotDropBlades(GameTestHelper h){
        var f=fixture(h,Direction.Axis.Y,7,4,true);
        h.startSequence().thenWaitUntil(f::ready).thenExecute(()->{
            var p=f.player(true);for(int i=0;i<16;i++)f.click(p,f.shaft(2),InteractionHand.MAIN_HAND);
            p.getInventory().clearContent();TurbineRotorLayout.breakShaft(h.getLevel(),f.shaft(2),p);
            p.setGameMode(GameType.SURVIVAL);TurbineRotorLayout.breakShaft(h.getLevel(),f.shaft(3),p);
            h.assertTrue(f.count(2)==0&&f.count(3)==0&&p.getInventory().countItem(TurbineRotorBlade.INSTANCE.asItem())==0,"creative or bare-hand cleanup produced blades");
        }).thenSucceed();
    }
    @GameTest(template="empty_17x17x17",timeoutTicks=100)
    public static void fullInventoryDropsAtShaftAndBreakProtectionIsRespected(GameTestHelper h){
        var f=fixture(h,Direction.Axis.Y,7,4,true);
        h.startSequence().thenWaitUntil(f::ready).thenExecute(()->{
            var p=f.player(true);for(int i=0;i<8;i++)f.click(p,f.shaft(2),InteractionHand.MAIN_HAND);
            p.setGameMode(GameType.SURVIVAL);
            for(int i=0;i<p.getInventory().getContainerSize();i++)p.getInventory().setItem(i,new ItemStack(Items.COBBLESTONE,64));
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_PICKAXE));
            BlockPos protectedBlade=f.shaft(2).north(2);
            java.util.function.Consumer<net.neoforged.neoforge.event.level.block.BreakBlockEvent> cancel=e->{if(e.getPlayer()==p&&e.getPos().equals(protectedBlade))e.setCanceled(true);};
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(cancel);
            try{TurbineRotorLayout.breakShaft(h.getLevel(),f.shaft(2),p);}
            finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(cancel);}
            h.assertTrue(f.count(2)==2,"cancelled arm removal was bypassed");
            int drops=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(f.shaft(2)).inflate(2)).stream()
                    .filter(e->e.getItem().is(TurbineRotorBlade.INSTANCE.asItem())).mapToInt(e->e.getItem().getCount()).sum();
            h.assertTrue(drops==6,"inventory overflow not dropped at shaft: "+drops);
        }).thenSucceed();
    }
    @GameTest(template="empty_17x17x17",timeoutTicks=100)
    public static void entityCollisionDoesNotConsumeOrPlaceGhostBlade(GameTestHelper h){
        var f=fixture(h,Direction.Axis.Y,7,4,true);
        h.startSequence().thenWaitUntil(f::ready).thenExecute(()->{
            var p=f.player(false);
            var target=f.shaft(2).north();
            h.spawn(net.minecraft.world.entity.EntityTypes.COW,h.relativePos(target));
            f.click(p,f.shaft(2),InteractionHand.MAIN_HAND);
            h.assertTrue(f.count(2)==0&&p.getMainHandItem().getCount()==64,"entity collision was ignored");
        }).thenSucceed();
    }
    @GameTest(template="empty_17x17x17",timeoutTicks=100)
    public static void sneakingWithBladesKeepsNativeManualPlacement(GameTestHelper h){
        var f=fixture(h,Direction.Axis.Y,7,4,true);
        h.startSequence().thenWaitUntil(f::ready).thenExecute(()->{
            var p=f.player(false);p.setShiftKeyDown(true);
            p.gameMode.useItemOn(p,h.getLevel(),p.getMainHandItem(),InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(f.shaft(4)),Direction.NORTH,f.shaft(4),false));
            h.assertTrue(f.count(2)==0&&f.count(4)==1&&p.getMainHandItem().getCount()==63,"manual sneaking placement was redirected");
        }).thenSucceed();
    }
    @GameTest(template="empty_17x17x17",timeoutTicks=140)
    public static void changingAssembledRotorPreservesClosedFluidStock(GameTestHelper h){
        var f=fixture(h,Direction.Axis.Y,7,4,true);
        f.set(4,2,3,TurbineRotorBlade.INSTANCE);
        long[] total={0};
        h.startSequence().thenWaitUntil(()->{
            f.ready();var tile=(TurbineBaseTile)h.getLevel().getBlockEntity(f.shaft(2));
            h.assertTrue(tile.getBlockState().getValue(biggerreactorsrework.core.multiblock.IAssemblyStateBlock.ASSEMBLED),"initial rotor not assembled");
        }).thenExecute(()->{
            var p=f.player(false);var tile=(TurbineBaseTile)h.getLevel().getBlockEntity(f.shaft(2));
            var sim=(biggerreactorsrework.multiblocks.turbine.simulation.modern.ModernTurbineSimulation)tile.controller().simulation();
            sim.idealSupply(biggerreactorsrework.registries.FluidTransitionRegistry.liquidTransition(net.minecraft.world.level.material.Fluids.WATER));
            sim.setVentState(biggerreactorsrework.multiblocks.turbine.state.VentState.CLOSED);sim.setActive(true);
            total[0]=sim.fluidTank().vaporAmount()+sim.fluidTank().liquidAmount();
            f.click(p,f.shaft(4),InteractionHand.MAIN_HAND);
            h.assertTrue(f.count(2)==2,"assembled rotor was not modified");
        }).thenIdle(10).thenWaitUntil(()->{
            var tile=(TurbineBaseTile)h.getLevel().getBlockEntity(f.shaft(2));
            h.assertTrue(tile.getBlockState().getValue(biggerreactorsrework.core.multiblock.IAssemblyStateBlock.ASSEMBLED),"rotor did not reassemble");
            var tank=tile.controller().simulation().fluidTank();
            h.assertTrue(tank.vaporAmount()+tank.liquidAmount()==total[0],"closed steam/water stock changed during normal revalidation");
        }).thenSucceed();
    }
    // Modified 2026-11-01: exercise native outline ray casts and unchanged collision.
    private static net.minecraft.world.phys.BlockHitResult aim(GameTestHelper h,Vec3 from,Vec3 to){
        return h.getLevel().clip(new net.minecraft.world.level.ClipContext(h.absoluteVec(from),h.absoluteVec(to),net.minecraft.world.level.ClipContext.Block.OUTLINE,net.minecraft.world.level.ClipContext.Fluid.NONE,net.minecraft.world.phys.shapes.CollisionContext.empty()));
    }
    @GameTest(template="empty_7x7x7")
    public static void slimOutlinesLetPlayersAimPastRotorPartsAndKeepCollision(GameTestHelper h){
        var shaftPos=new BlockPos(3,3,3);var bladePos=new BlockPos(3,3,2);var behind=new BlockPos(3,3,4);
        h.setBlock(shaftPos,TurbineRotorShaft.INSTANCE);h.setBlock(bladePos,TurbineRotorBlade.INSTANCE);h.setBlock(behind,Blocks.STONE);
        var past=aim(h,new Vec3(3.5,3.1,1.5),new Vec3(3.5,3.1,3.9));
        h.assertTrue(past.getBlockPos().equals(h.absolutePos(shaftPos)),"blade corner did not let the aim reach the shaft: "+past.getBlockPos());
        var corner=aim(h,new Vec3(3.1,3.1,2.9),new Vec3(3.1,3.1,4.5));
        h.assertTrue(corner.getBlockPos().equals(h.absolutePos(behind)),"shaft corner did not let the aim reach the block behind: "+corner.getBlockPos());
        var center=aim(h,new Vec3(3.5,3.5,1.5),new Vec3(3.5,3.5,3.9));
        h.assertTrue(center.getBlockPos().equals(h.absolutePos(bladePos)),"blade center was not targetable");
        var axisProperty=biggerreactorsrework.multiblocks.turbine.state.TurbineShaftRotationState.TURBINE_SHAFT_ROTATION_STATE_ENUM_PROPERTY;
        for(var rotation:biggerreactorsrework.multiblocks.turbine.state.TurbineShaftRotationState.values()){
            var axis=Direction.Axis.values()[rotation.ordinal()];
            var shaft=TurbineRotorShaft.INSTANCE.defaultBlockState().setValue(axisProperty,rotation);
            var shaftBox=shaft.getShape(h.getLevel(),BlockPos.ZERO).bounds();
            h.assertTrue(shaft.getCollisionShape(h.getLevel(),BlockPos.ZERO).equals(net.minecraft.world.phys.shapes.Shapes.block()),"shaft collision changed");
            h.assertTrue(shaftBox.getXsize()==1&&shaftBox.getYsize()==1&&shaftBox.getZsize()==1,"shaft outline should still reach its neighbours");
            var arms=TurbineRotorLayout.arms(axis);
            for(int position=0;position<4;position++){
                var blade=TurbineRotorBlade.INSTANCE.defaultBlockState().setValue(axisProperty,rotation).setValue(TurbineRotorBlade.BLADE_POSITION,position);
                var box=blade.getShape(h.getLevel(),BlockPos.ZERO).bounds();var radial=arms.get(position).getAxis();
                for(var a:Direction.Axis.values())h.assertTrue(a==radial?box.max(a)-box.min(a)==1:box.max(a)-box.min(a)<0.5,"blade outline wrong on "+a+" for "+axis+"/"+position);
                h.assertTrue(blade.getCollisionShape(h.getLevel(),BlockPos.ZERO).equals(net.minecraft.world.phys.shapes.Shapes.block()),"blade collision changed");
            }
        }
        h.succeed();
    }
}
