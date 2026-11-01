package biggerreactorsrework.gametest;

import biggerreactorsrework.core.multiblock.IAssemblyStateBlock;
import biggerreactorsrework.fluids.LiquidObsidian;
import biggerreactorsrework.fluids.Steam;
import biggerreactorsrework.multiblocks.reactor.blocks.ReactorCoolantPort;
import biggerreactorsrework.multiblocks.reactor.tiles.ReactorCoolantPortTile;
import biggerreactorsrework.multiblocks.turbine.blocks.TurbineRotorBearing;
import biggerreactorsrework.multiblocks.turbine.blocks.TurbineRotorShaft;
import biggerreactorsrework.multiblocks.turbine.state.TurbineShaftRotationState;
import biggerreactorsrework.util.GuideStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import java.util.UUID;

/** Added 2026-11-01: native placement and whole-bucket interactions, including rejection and creative conservation. */
public final class PortInteractionGameTests {
    private static FakePlayer player(GameTestHelper h, boolean creative) {
        var p = new FakePlayer(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "port_test"));
        p.setGameMode(creative ? GameType.CREATIVE : GameType.SURVIVAL);
        p.setPos(Vec3.atCenterOf(h.absolutePos(BlockPos.ZERO)).add(-2, 2, -2));
        return p;
    }
    private static void place(FakePlayer p, BlockPos anchor, Direction face) {
        p.getMainHandItem().useOn(new UseOnContext(p, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(anchor), face, anchor, false)));
    }
    private static void click(GameTestHelper h, FakePlayer p, BlockPos pos, Direction face, InteractionHand hand) {
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), face, pos, false);
        p.gameMode.useItemOn(p, h.getLevel(), p.getItemInHand(hand), hand, hit);
    }
    @GameTest(template="empty_7x7x7")
    public static void firstShaftOrientsImmediatelyOnEveryBearingFace(GameTestHelper h) {
        var p = player(h, false);
        var bearing = h.absolutePos(new BlockPos(3,3,3));
        for (var face : Direction.values()) {
            h.getLevel().setBlockAndUpdate(bearing, TurbineRotorBearing.INSTANCE.defaultBlockState());
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(TurbineRotorShaft.INSTANCE, 2));
            place(p, bearing, face);
            var first = bearing.relative(face);
            var state = h.getLevel().getBlockState(first);
            h.assertTrue(state.is(TurbineRotorShaft.INSTANCE), "shaft not placed on " + face);
            h.assertTrue(state.getValue(TurbineShaftRotationState.TURBINE_SHAFT_ROTATION_STATE_ENUM_PROPERTY).ordinal() == face.getAxis().ordinal(),
                    "first shaft has wrong axis on " + face + ": " + state);
            place(p, first, face);
            h.assertTrue(h.getLevel().getBlockState(first.relative(face)).getValue(TurbineShaftRotationState.TURBINE_SHAFT_ROTATION_STATE_ENUM_PROPERTY)
                    == state.getValue(TurbineShaftRotationState.TURBINE_SHAFT_ROTATION_STATE_ENUM_PROPERTY), "second shaft changed axis on " + face);
            h.getLevel().removeBlock(first, false); h.getLevel().removeBlock(first.relative(face), false);
        }
        h.succeed();
    }
    @GameTest(template="empty_7x7x7")
    public static void sourceAndFlowingObsidianCanBeReplacedByPlacedBlocks(GameTestHelper h) {
        var p = player(h, false); var pos = h.absolutePos(new BlockPos(3,3,3));
        for (int fluidLevel : new int[]{0, 4, 12}) {
            h.getLevel().setBlockAndUpdate(pos, LiquidObsidian.INSTANCE.defaultFluidState().createLegacyBlock().setValue(LiquidBlock.LEVEL, fluidLevel));
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE, 2));
            place(p, pos, Direction.UP);
            h.assertTrue(h.getLevel().getBlockState(pos).is(Blocks.STONE), "obsidian blocked replacement at fluid level " + fluidLevel);
            h.assertTrue(p.getMainHandItem().getCount() == 1, "replacement did not consume exactly one block");
            h.assertTrue(h.getLevel().getFluidState(pos).isEmpty(), "replaced fluid survived inside stone");
        }
        h.succeed();
    }
    private static void reactor(GameTestHelper h, boolean creative) {
        GuideStructures.reactor(5,true).parts().forEach(h::setBlock);
        var pos = h.absolutePos(new BlockPos(4,1,2));
        h.startSequence().thenWaitUntil(() -> h.assertTrue(h.getLevel().getBlockState(pos).getValue(IAssemblyStateBlock.ASSEMBLED), "reactor not assembled"))
                .thenExecute(() -> {
                    var tile = (ReactorCoolantPortTile)h.getLevel().getBlockEntity(pos);
                    var p = player(h,creative);
                    p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.WATER_BUCKET));
                    click(h,p,pos,Direction.EAST,InteractionHand.MAIN_HAND);
                    h.assertTrue(tile.fluidAmountInTank(0)==1000,"water bucket did not fill reactor exactly once");
                    h.assertTrue(p.getMainHandItem().is(creative?Items.WATER_BUCKET:Items.BUCKET),"wrong bucket remainder");
                    p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.LAVA_BUCKET));
                    click(h,p,pos,Direction.EAST,InteractionHand.MAIN_HAND);
                    h.assertTrue(tile.fluidAmountInTank(0)==1000 && p.getMainHandItem().is(Items.LAVA_BUCKET),"rejected bucket changed stock/item");
                    h.assertTrue(h.getLevel().getFluidState(pos.east()).isEmpty(),"rejected bucket spilled into world");
                    p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.WATER_BUCKET));
                    p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
                    click(h,p,pos,Direction.WEST,InteractionHand.OFF_HAND);
                    h.assertTrue(tile.fluidAmountInTank(0)==2000,"offhand bucket did not use the same inlet");
                }).thenSucceed();
    }
    @GameTest(template="empty_7x7x7",timeoutTicks=120)
    public static void survivalBucketsFillReactorAndRejectWrongFluidsWithoutSpilling(GameTestHelper h) { reactor(h,false); }
    @GameTest(template="empty_7x7x7",timeoutTicks=120)
    public static void creativeBucketsFillReactorWithoutConsumingTheHeldBucket(GameTestHelper h) { reactor(h,true); }

    @GameTest(template="empty_7x7x7",timeoutTicks=120)
    public static void bucketRequiresWholeVolumeAndPermissionAndCannotFillAnOutlet(GameTestHelper h) {
        GuideStructures.reactor(5,true).parts().forEach(h::setBlock);
        var pos=h.absolutePos(new BlockPos(4,1,2));
        h.startSequence().thenWaitUntil(()->h.assertTrue(h.getLevel().getBlockState(pos).getValue(IAssemblyStateBlock.ASSEMBLED),"reactor not assembled"))
                .thenExecute(()->{
                    var tile=(ReactorCoolantPortTile)h.getLevel().getBlockEntity(pos);var p=player(h,false);
                    var empty=tile.transferSnapshot();
                    long amount=tile.tankCapacity(0)-500;
                    tile.fill(net.minecraft.world.level.material.Fluids.WATER,net.minecraft.core.component.DataComponentPatch.EMPTY,amount,false);
                    p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.WATER_BUCKET));
                    click(h,p,pos,Direction.EAST,InteractionHand.MAIN_HAND);
                    h.assertTrue(tile.fluidAmountInTank(0)==amount&&p.getMainHandItem().is(Items.WATER_BUCKET),"partial bucket insertion changed stock or item");
                    tile.restoreTransferSnapshot(empty);
                    p.getAbilities().mayBuild=false;
                    click(h,p,pos,Direction.EAST,InteractionHand.MAIN_HAND);
                    h.assertTrue(tile.fluidAmountInTank(0)==0,"denied player changed stock");
                    p.getAbilities().mayBuild=true;
                    tile.setDirection(biggerreactorsrework.multiblocks.reactor.blocks.ReactorAccessPort.PortDirection.OUTLET);
                    click(h,p,pos,Direction.EAST,InteractionHand.MAIN_HAND);
                    h.assertTrue(tile.fluidAmountInTank(0)==0&&p.getMainHandItem().is(Items.WATER_BUCKET),"outlet accepted cold input");
                }).thenSucceed();
    }

    @GameTest(template="empty_7x7x7",timeoutTicks=120)
    public static void turbineBucketsUseSteamInputAndReturnCondensedWater(GameTestHelper h) {
        GuideStructures.turbine().parts().forEach(h::setBlock);var pos=h.absolutePos(new BlockPos(2,1,0));
        h.startSequence().thenWaitUntil(()->h.assertTrue(h.getLevel().getBlockState(pos).getValue(IAssemblyStateBlock.ASSEMBLED),"turbine not assembled"))
                .thenExecute(()->{
                    var tile=(biggerreactorsrework.multiblocks.turbine.tiles.TurbineFluidPortTile)h.getLevel().getBlockEntity(pos);var p=player(h,false);
                    p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Steam.INSTANCE.getBucket()));
                    click(h,p,pos,Direction.NORTH,InteractionHand.MAIN_HAND);
                    h.assertTrue(tile.fluidAmountInTank(0)==1000&&p.getMainHandItem().is(Items.BUCKET),"steam bucket did not fill turbine");
                    var tank=(biggerreactorsrework.util.FluidTransitionTank)tile.controller().simulation().fluidTank();
                    tank.absorbRF(1e9);
                    h.assertTrue(tile.fluidAmountInTank(1)==1000,"steam was not condensed for extraction");
                    tile.setDirection(biggerreactorsrework.multiblocks.turbine.blocks.TurbineFluidPort.PortDirection.OUTLET);
                    p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BUCKET,2));
                    click(h,p,pos,Direction.NORTH,InteractionHand.MAIN_HAND);
                    h.assertTrue(tile.fluidAmountInTank(1)==0&&p.getMainHandItem().is(Items.BUCKET)&&p.getMainHandItem().getCount()==1,"stacked bucket extraction changed the wrong item/volume");
                    h.assertTrue(p.getInventory().contains(new ItemStack(Items.WATER_BUCKET)),"filled bucket was not returned to inventory");
                }).thenSucceed();
    }

    @GameTest(template="empty_7x7x7",timeoutTicks=120)
    public static void exchangerBucketsRespectBothCircuits(GameTestHelper h) {
        GuideStructures.exchanger().parts().forEach(h::setBlock);var first=h.absolutePos(new BlockPos(0,1,1));
        h.startSequence().thenWaitUntil(()->h.assertTrue(h.getLevel().getBlockState(first).getValue(IAssemblyStateBlock.ASSEMBLED),"exchanger not assembled"))
                .thenExecute(()->{
                    var p=player(h,false);
                    for(var relative:java.util.List.of(new BlockPos(0,1,1),new BlockPos(0,1,2))){
                        var pos=h.absolutePos(relative);var tile=(biggerreactorsrework.multiblocks.heatexchanger.tiles.HeatExchangerFluidPortTile)h.getLevel().getBlockEntity(pos);
                        tile.setInletOtherOutlet(true);
                        if(tile.isCondenser()){
                            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Steam.INSTANCE.getBucket()));
                            click(h,p,pos,Direction.WEST,InteractionHand.MAIN_HAND);
                            h.assertTrue(tile.fluidAmountInTank(0)==1000&&tile.fluidTypeInTank(0)==Steam.INSTANCE,"steam bucket did not fill primary circuit");
                        }else{
                            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.WATER_BUCKET));
                            click(h,p,pos,Direction.WEST,InteractionHand.MAIN_HAND);
                            h.assertTrue(tile.fluidAmountInTank(0)==1000,"water bucket did not fill secondary circuit");
                        }
                        h.assertTrue(p.getMainHandItem().is(Items.BUCKET),"exchanger did not return empty bucket");
                    }
                }).thenSucceed();
    }

    @GameTest(template="empty_7x7x7",timeoutTicks=120)
    public static void liquidObsidianBucketFillsReactorWithoutPlacingWorldFluid(GameTestHelper h) {
        GuideStructures.reactor(5,true).parts().forEach(h::setBlock);var pos=h.absolutePos(new BlockPos(4,1,2));
        h.startSequence().thenWaitUntil(()->h.assertTrue(h.getLevel().getBlockState(pos).getValue(IAssemblyStateBlock.ASSEMBLED),"reactor not assembled"))
                .thenExecute(()->{
                    var tile=(ReactorCoolantPortTile)h.getLevel().getBlockEntity(pos);var p=player(h,false);
                    p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LiquidObsidian.INSTANCE.getBucket()));
                    click(h,p,pos,Direction.EAST,InteractionHand.MAIN_HAND);
                    h.assertTrue(tile.fluidAmountInTank(0)==1000&&tile.fluidTypeInTank(0)==LiquidObsidian.INSTANCE,"obsidian bucket did not fill reactor");
                    h.assertTrue(p.getMainHandItem().is(Items.BUCKET)&&h.getLevel().getFluidState(pos.east()).isEmpty(),"obsidian bucket spilled or did not return its container");
                }).thenSucceed();
    }

    @GameTest(template="empty_7x7x7")
    public static void unassembledFluidPortsRefuseBucketsWithoutSpilling(GameTestHelper h) {
        var p=player(h,false);int x=1;
        for(var block:java.util.List.of(ReactorCoolantPort.INSTANCE,biggerreactorsrework.multiblocks.turbine.blocks.TurbineFluidPort.INSTANCE,
                biggerreactorsrework.multiblocks.heatexchanger.blocks.HeatExchangerFluidPortBlock.INSTANCE)){
            var relative=new BlockPos(x++,2,2);h.setBlock(relative,block);var pos=h.absolutePos(relative);
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.WATER_BUCKET));
            click(h,p,pos,Direction.UP,InteractionHand.MAIN_HAND);
            h.assertTrue(p.getMainHandItem().is(Items.WATER_BUCKET)&&h.getLevel().getFluidState(pos.above()).isEmpty(),"unassembled port consumed or spilled bucket");
        }
        h.succeed();
    }

    @GameTest(template="empty_7x7x7",timeoutTicks=120)
    public static void plainBucketsCannotDiscardHeatedCoolantIdentity(GameTestHelper h) {
        GuideStructures.exchanger().parts().forEach(h::setBlock);var first=h.absolutePos(new BlockPos(0,1,1));
        h.startSequence().thenWaitUntil(()->h.assertTrue(h.getLevel().getBlockState(first).getValue(IAssemblyStateBlock.ASSEMBLED),"exchanger not assembled"))
                .thenExecute(()->{
                    // Use the actual evaporator output: generic heated fluid needs its profile components.
                    var hot=java.util.stream.Stream.of(new BlockPos(0,1,1),new BlockPos(0,1,2))
                            .map(pos->(biggerreactorsrework.multiblocks.heatexchanger.tiles.HeatExchangerFluidPortTile)h.getLevel().getBlockEntity(h.absolutePos(pos)))
                            .filter(tile->!tile.isCondenser()).findFirst().orElseThrow();
                        var hotTank=hot.controller().evaporatorTank;
                        hotTank.fill(LiquidObsidian.INSTANCE,net.minecraft.core.component.DataComponentPatch.EMPTY,1000,false);
                        hotTank.absorbRF(-1e9);hot.setInletOtherOutlet(false);
                        var p=player(h,false);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BUCKET));
                        long before=hotTank.fluidAmountInTank(1);
                        h.assertTrue(before==1000&&!hotTank.fluidComponentsInTank(1).isEmpty(),"test did not prepare identified generic hot output");
                        click(h,p,hot.getBlockPos(),Direction.WEST,InteractionHand.MAIN_HAND);
                        h.assertTrue(hotTank.fluidAmountInTank(1)==before&&p.getMainHandItem().is(Items.BUCKET),"plain bucket lost heated identity");
                }).thenSucceed();
    }
}
