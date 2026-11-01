package biggerreactorsrework.gametest;

import biggerreactorsrework.Config;
import biggerreactorsrework.core.config.PackConfiguration;
import biggerreactorsrework.multiblocks.reactor.blocks.*;
import biggerreactorsrework.multiblocks.reactor.tiles.ReactorAccessPortTile;
import biggerreactorsrework.multiblocks.battery.blocks.*;
import biggerreactorsrework.multiblocks.battery.tiles.BatteryTerminalTile;
import biggerreactorsrework.multiblocks.battery.tiles.BatteryCellTile;
import biggerreactorsrework.registries.ReactorFuelRegistry;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;

/** Added 2026-11-01: real ore-filter decoding, fuel transfer, saved battery energy and optional script dispatch. */
public final class PackConfigurationGameTests {
    private static PackConfiguration.Option option(String key) { return PackConfiguration.serverOptions().get(key); }
    private static void rejects(GameTestHelper h, Runnable action) {
        boolean rejected = false; try { action.run(); } catch (IllegalArgumentException | IllegalStateException expected) { rejected = true; }
        h.assertTrue(rejected, "unsafe edit was accepted");
    }
    @GameTest(template="empty_7x7x7")
    public static void serverRulesCannotBeEditedByClientOrReloadedWhileRunning(GameTestHelper h) {
        var capacity = option("Battery.CellCapacity"); long before = Config.CONFIG.Battery.CellCapacity;
        h.assertTrue(capacity != null && option("Battery.CellTransferRate") != null && option("Worldgen.GenerateUranium") != null, "missing pack options");
        h.assertTrue(!PackConfiguration.editable(capacity), "live server rules exposed for local editing");
        rejects(h, () -> PackConfiguration.setOverrides(java.util.Map.of("Battery.CellCapacity", 1234)));
        rejects(h, () -> PackConfiguration.setOverrides(java.util.Map.of("missing", 1)));
        rejects(h, () -> capacity.checked(-1));
        h.assertTrue(Config.CONFIG.Battery.CellCapacity == before, "rejected config changed capacity");
        h.succeed();
    }
    @GameTest(template="empty_7x7x7")
    public static void oreSwitchGatesNativeDecodedPlacementWithoutRemovingBlocks(GameTestHelper h) {
        var option = option("Worldgen.GenerateUranium"); String before = option.current();
        var filter = PlacementModifier.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"type\":\"biggerreactorsrework:uranium_generation\"}")).getOrThrow();
        var outputs = new java.util.ArrayList<BlockPos>(); var pos = h.absolutePos(new BlockPos(2,2,2));
        h.setBlock(2,2,2,biggerreactorsrework.blocks.materials.UraniumOre.INSTANCE);
        try {
            option.node().writeFromString("false"); filter.modify(null, h.getLevel().getRandom(), pos, outputs::add);
            h.assertTrue(outputs.isEmpty(), "disabled ore generation emitted a position");
            h.assertTrue(h.getBlockState(new BlockPos(2,2,2)).is(biggerreactorsrework.blocks.materials.UraniumOre.INSTANCE), "switch deleted existing ore");
            option.node().writeFromString("true"); filter.modify(null, h.getLevel().getRandom(), pos, outputs::add);
            h.assertTrue(outputs.equals(java.util.List.of(pos)), "enabled generation did not preserve placement origin");
        } finally { option.node().writeFromString(before); }
        h.succeed();
    }
    @GameTest(template="empty_7x7x7")
    public static void malformedFuelEntriesAndDuplicateDefinitionsAreRejected(GameTestHelper h) {
        for (String bad : new String[]{"minecraft:apple=-1", "minecraft:apple=1.5", "bad id=1", "minecraft:apple=9223372036854775807", "minecraft:apple=1;minecraft:apple=2"})
            rejects(h, () -> ReactorFuelRegistry.parse(bad));
        var entries = ReactorFuelRegistry.parse("#c:ingots/uranium=0;minecraft:apple=2500");
        h.assertTrue(entries.size()==2 && entries.getFirst().tag() && entries.getLast().amount()==2500, "valid fuel definitions failed");
        for(String bad : new String[]{"-1","1.5","1e100"})h.assertTrue(biggerreactorsrework.registries.MaterialDataMaps.ReactorFuel.CODEC
                .parse(JsonOps.INSTANCE,JsonParser.parseString("{\"amount\":"+bad+"}")).result().isEmpty(),"native fuel data map accepted an invalid amount");
        h.succeed();
    }
    @GameTest(template="empty_7x7x7",timeoutTicks=120)
    public static void customFuelUsesWholeItemsAndSimulatedTransferIsPure(GameTestHelper h) {
        for(int x=1;x<=3;x++)for(int y=1;y<=3;y++)for(int z=1;z<=3;z++)h.setBlock(x,y,z,ReactorCasing.INSTANCE);
        h.setBlock(2,2,2,ReactorFuelRod.INSTANCE); h.setBlock(2,3,2,ReactorControlRod.INSTANCE);
        h.setBlock(1,2,2,ReactorTerminal.INSTANCE); h.setBlock(2,2,1,ReactorAccessPort.INSTANCE);
        h.startSequence().thenWaitUntil(()->h.assertTrue(h.getBlockState(new BlockPos(1,2,2)).getValue(biggerreactorsrework.core.multiblock.IAssemblyStateBlock.ASSEMBLED),"reactor not assembled"))
                .thenExecute(()->{
                    var port=(ReactorAccessPortTile)GameTestSupport.be(h,new BlockPos(2,2,1));
                    var option=option("Reactor.CustomFuelEntries"); String before=option.current();
                    try {
                        option.node().writeFromString("minecraft:apple=1500;#minecraft:logs=2000");
                        var apples=new ItemStack(Items.APPLE,3);
                        h.assertTrue(port.isItemValid(ReactorAccessPortTile.FUEL_INSERT_SLOT,apples),"custom fuel rejected by slot");
                        h.assertTrue(port.insertItem(ReactorAccessPortTile.FUEL_INSERT_SLOT,apples,true).getCount()==1,"simulation did not keep whole items");
                        h.assertTrue(port.controller().simulation().fuelTank().fuel()==0&&apples.getCount()==3,"simulation mutated inventory or fuel");
                        h.assertTrue(port.insertItem(ReactorAccessPortTile.FUEL_INSERT_SLOT,apples,false).getCount()==1,"actual fuel item count wrong");
                        h.assertTrue(port.controller().simulation().fuelTank().fuel()==3000,"custom item yield was not used");
                        h.assertTrue(port.insertItem(ReactorAccessPortTile.FUEL_INSERT_SLOT,new ItemStack(Items.APPLE),false).getCount()==1,"partial fuel item was accepted");
                        h.assertTrue(ReactorFuelRegistry.amount(new ItemStack(Items.OAK_LOG))==2000,"custom item tag ignored");
                        option.node().writeFromString("minecraft:apple=0");
                        h.assertTrue(!port.isItemValid(ReactorAccessPortTile.FUEL_INSERT_SLOT,apples),"disabled fuel still accepted");
                    } finally { option.node().writeFromString(before); }
                }).thenSucceed();
    }
    @GameTest(template="empty_7x7x7")
    public static void defaultFuelsRemainAndExplicitOverridesBeatTags(GameTestHelper h) {
        var option=option("Reactor.CustomFuelEntries"); String before=option.current();
        var uranium=new ItemStack(biggerreactorsrework.items.ingots.UraniumIngot.INSTANCE);
        var blutonium=new ItemStack(biggerreactorsrework.items.ingots.BlutoniumIngot.INSTANCE);
        try {
            option.node().writeFromString("");
            long unit=Config.CONFIG.Reactor.FuelMBPerIngot;
            h.assertTrue(ReactorFuelRegistry.amount(uranium)==unit&&ReactorFuelRegistry.amount(blutonium)==unit,"ordinary ingot yield changed");
            h.assertTrue(ReactorFuelRegistry.amount(new ItemStack(biggerreactorsrework.blocks.materials.MaterialBlock.BLUTONIUM.asItem()))==9*unit,"ordinary block yield changed");
            option.node().writeFromString("#c:ingots/uranium=1500;biggerreactorsrework:uranium_ingot=2000");
            h.assertTrue(ReactorFuelRegistry.amount(uranium)==2000,"explicit fuel item did not override its tag");
            option.node().writeFromString("biggerreactorsrework:uranium_ingot=0");
            h.assertTrue(ReactorFuelRegistry.amount(uranium)==0,"disabled fuel fell through to default");
        } finally { option.node().writeFromString(before); }
        h.succeed();
    }
    @GameTest(template="empty_7x7x7",timeoutTicks=120)
    public static void lowerBatteryCapacityPreservesSavedEnergyAndBlocksInput(GameTestHelper h) {
        for(int x=1;x<=3;x++)for(int y=1;y<=3;y++)for(int z=1;z<=3;z++)h.setBlock(x,y,z,x==2&&y==2&&z==2?BatteryCell.INSTANCE:BatteryCasing.INSTANCE);
        h.setBlock(1,2,2,BatteryTerminal.INSTANCE); h.setBlock(2,2,1,BatteryEnergyPort.INSTANCE); h.setBlock(2,2,3,BatteryEnergyPort.INSTANCE);
        h.startSequence().thenWaitUntil(()->h.assertTrue(((BatteryTerminalTile)GameTestSupport.be(h,new BlockPos(1,2,2))).isBatteryAssembled(),"battery not assembled"))
                .thenExecute(()->{
                    var c=((BatteryTerminalTile)GameTestSupport.be(h,new BlockPos(1,2,2))).controller();
                    h.assertTrue(c.insert(10000,false)==10000,"initial charge failed"); c.syncCells();
                    var option=option("Battery.CellCapacity"); String before=option.current();
                    try {
                        option.node().writeFromString("1000");
                        var validate=c.getClass().getDeclaredMethod("validationPassed"); validate.setAccessible(true); validate.invoke(c);
                    } catch(ReflectiveOperationException error) { throw new AssertionError(error); }
                    finally { option.node().writeFromString(before); }
                    c.tick(); h.assertTrue(c.capacity()==1000&&c.stored()==10000,"capacity change destroyed saved energy");
                    h.assertTrue(c.insert(1,false)==0&&c.extract(1000,false)==1000,"overfull battery transfer invalid");
                    c.syncCells(); var cell=(BatteryCellTile)GameTestSupport.be(h,new BlockPos(2,2,2));
                    h.assertTrue(cell.energy()==9000&&cell.saveWithoutMetadata(h.getLevel().registryAccess()).getCompoundOrEmpty("local").getLongOr("energy",0)==9000,"overfull energy not persisted on cell");
                }).thenSucceed();
    }
    @GameTest(template="empty_7x7x7")
    public static void actualKubeJSScriptsConfigureRulesAndNativeDataMaps(GameTestHelper h) {
        if(!Boolean.getBoolean("biggerreactorsrework.testScripting")){h.succeed();return;}
        h.assertTrue(net.neoforged.fml.ModList.get().isLoaded("kubejs"),"scripting runtime absent");
        h.assertTrue(!Config.CONFIG.Worldgen.GenerateUranium&&Config.CONFIG.Battery.CellCapacity==3200001234L&&Config.CONFIG.Battery.CellTransferRate==300123,"configuration event did not apply");
        h.assertTrue(ReactorFuelRegistry.amount(new ItemStack(Items.APPLE))==2500,"script fuel event failed");
        h.assertTrue(ReactorFuelRegistry.amount(new ItemStack(Items.BEETROOT))==1234,"script fuel data map failed");
        var coil=biggerreactorsrework.registries.TurbineCoilRegistry.getCoilData(net.minecraft.world.level.block.Blocks.HONEY_BLOCK);
        h.assertTrue(coil!=null&&coil.efficiency==1.25&&coil.extractionRate==1.1,"script coil data map failed");
        var moderator=biggerreactorsrework.registries.ReactorModeratorRegistry.snapshot().get(net.minecraft.world.level.block.Blocks.SPONGE);
        h.assertTrue(moderator!=null,"script moderator data map failed");
        var transition=biggerreactorsrework.registries.FluidTransitionRegistry.liquidTransition(net.minecraft.world.level.material.Fluids.LAVA);
        h.assertTrue(transition!=null&&transition.latentHeat()==12&&transition.recovery==1,"script fluid transition failed");
        h.assertTrue(!biggerreactorsrework.registries.ReactorModeratorRegistry.snapshot().containsKey(net.minecraft.world.level.block.Blocks.LAVA),"world-space liquid moderation was re-enabled");
        biggerreactorsrework.BiggerReactors.LOGGER.info("PACK_SCRIPT_PASS: real KubeJS config/fuel/coil/solid-moderator/coolant-transition callbacks applied; server rules preserved"); h.succeed();
    }
    @GameTest(template="empty_7x7x7",timeoutTicks=500)
    public static void scriptingReloadKeepsRulesAndRegeneratesMaterialOverlays(GameTestHelper h) {
        if(!Boolean.getBoolean("biggerreactorsrework.testScripting")){h.succeed();return;}
        var future = new java.util.concurrent.atomic.AtomicReference<java.util.concurrent.CompletableFuture<Void>>();
        var before = java.util.Map.copyOf(PackConfiguration.overrides());
        h.startSequence().thenIdle(20).thenExecute(()->{
            var server=h.getLevel().getServer(); future.set(server.reloadResources(server.getPackRepository().getSelectedIds()));
        }).thenWaitUntil(()->{
            h.assertTrue(future.get()!=null&&future.get().isDone(),"resource reload incomplete");
            future.get().join();
            h.assertTrue(PackConfiguration.overrides().equals(before),"reload changed live simulation rules");
            h.assertTrue(ReactorFuelRegistry.amount(new ItemStack(Items.BEETROOT))==1234,"reload lost native fuel overlay");
            h.assertTrue(Config.CONFIG.Battery.CellCapacity==3200001234L,"reload lost scripted capacity");
        }).thenExecute(()->biggerreactorsrework.BiggerReactors.LOGGER.info("PACK_SCRIPT_RELOAD_PASS: unchanged config overrides and native material data maps survive actual resource reload"))
                .thenSucceed();
    }
}
