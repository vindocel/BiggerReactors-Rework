package biggerreactorsrework.gametest;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.Config;
import biggerreactorsrework.core.multiblock.IAssemblyStateBlock;
import biggerreactorsrework.multiblocks.battery.blocks.BatteryRedstonePort;
import biggerreactorsrework.multiblocks.battery.tiles.BatteryCellTile;
import biggerreactorsrework.multiblocks.battery.tiles.BatteryRedstonePortTile;
import biggerreactorsrework.multiblocks.battery.tiles.BatteryRedstonePortTile.Mode;
import biggerreactorsrework.multiblocks.battery.containers.BatteryRedstonePortContainer;
import biggerreactorsrework.multiblocks.turbine.blocks.TurbineRedstonePort;
import biggerreactorsrework.multiblocks.turbine.tiles.TurbineRedstonePortTile;
import biggerreactorsrework.util.GuideStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;

import java.util.List;
import java.util.Map;

/** Added 2026-10-30: charge boundaries, independent ports, persistence and actual turbine automation. */
@GameTestHolder(BiggerReactors.modid)
@PrefixGameTestTemplate(false)
public final class BatteryRedstoneGameTests {
    private static void charge(GameTestHelper h, double fraction) {
        for (int x = 1; x <= 3; x++) for (int y = 1; y <= 3; y++) for (int z = 1; z <= 3; z++)
            ((BatteryCellTile) h.getBlockEntity(new BlockPos(x, y, z))).loadEnergyFromItem(Math.round(Config.CONFIG.Battery.CellCapacity * fraction));
    }
    private static BatteryRedstonePortTile tile(GameTestHelper h, BlockPos pos) { return (BatteryRedstonePortTile) h.getBlockEntity(pos); }
    private static void ready(GameTestHelper h, BlockPos pos) {
        h.assertTrue(h.getBlockState(pos).getValue(IAssemblyStateBlock.ASSEMBLED), "machine not ready");
    }
    private static void config(GameTestHelper h, BatteryRedstonePortTile tile, Mode mode, double threshold) {
        h.assertTrue(tile.configure(Map.of("mode", mode.ordinal(), "threshold", threshold)), "valid sensor configuration rejected");
    }
    private static void outputs(GameTestHelper h, List<BlockPos> positions, int... signals) {
        for (int i = 0; i < positions.size(); i++) {
            var pos = h.absolutePos(positions.get(i));
            var state = h.getLevel().getBlockState(pos);
            for (var side : Direction.values()) {
                int expected = side == Direction.EAST ? signals[i] : 0;
                h.assertTrue(state.getSignal(h.getLevel(), pos, side) == expected, "wrong outward signal " + i + "/" + side);
                h.assertTrue(state.getDirectSignal(h.getLevel(), pos, side) == expected, "wrong strong signal");
            }
            h.assertTrue(state.getValue(BatteryRedstonePort.IS_LIT) == (signals[i] > 0), "lit texture disagrees with signal");
        }
    }

    @GameTest(template = "empty_7x7x7")
    public static void craftingAndMiningResources(GameTestHelper h) {
        var casing = biggerreactorsrework.multiblocks.battery.blocks.BatteryCasing.INSTANCE.asItem();
        var items = new java.util.ArrayList<net.minecraft.world.item.ItemStack>();
        for (var item : List.of(casing, net.minecraft.world.item.Items.REDSTONE, casing,
                net.minecraft.world.item.Items.REDSTONE, net.minecraft.world.item.Items.GOLD_INGOT, net.minecraft.world.item.Items.REDSTONE,
                casing, net.minecraft.world.item.Items.REDSTONE, casing)) items.add(new net.minecraft.world.item.ItemStack(item));
        var input = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, items);
        var recipe = h.getLevel().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, h.getLevel());
        h.assertTrue(recipe.isPresent(), "sensor craft did not match its 3x3 ingredients");
        var result = recipe.orElseThrow().value().assemble(input, h.getLevel().registryAccess());
        h.assertTrue(result.is(BatteryRedstonePort.INSTANCE.asItem()) && result.getCount() == 1, "craft returned the wrong block or count");
        h.assertTrue(BatteryRedstonePort.INSTANCE.defaultBlockState().is(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE), "sensor missing pickaxe tag");
        h.assertTrue(BatteryRedstonePort.INSTANCE.defaultBlockState().is(net.minecraft.tags.BlockTags.NEEDS_IRON_TOOL), "sensor missing tool tier tag");
        h.setBlock(new BlockPos(2,2,2), BatteryRedstonePort.INSTANCE);
        var pos = h.absolutePos(new BlockPos(2,2,2));
        var drops = net.minecraft.world.level.block.Block.getDrops(h.getLevel().getBlockState(pos), h.getLevel(), pos, h.getLevel().getBlockEntity(pos));
        h.assertTrue(drops.size() == 1 && drops.getFirst().is(BatteryRedstonePort.INSTANCE.asItem()), "sensor did not drop itself");
        h.succeed();
    }

    @GameTest(template = "empty_7x7x7", timeoutTicks = 240)
    public static void independentChargeSensorsAndDisassembly(GameTestHelper h) {
        GuideStructures.battery().parts().forEach(h::setBlock);
        var positions = List.of(new BlockPos(0,1,1), new BlockPos(0,2,1), new BlockPos(0,3,1), new BlockPos(0,1,2), new BlockPos(0,3,2));
        positions.forEach(pos -> h.setBlock(pos, BatteryRedstonePort.INSTANCE));
        h.startSequence().thenWaitUntil(() -> ready(h, positions.getFirst())).thenExecute(() -> {
            for (int i = 0; i < positions.size(); i++) config(h, tile(h, positions.get(i)), Mode.values()[(i + 1) % 5], 90);
        }).thenIdle(3).thenExecute(() -> {
            outputs(h, positions, 0,15,0,15,0);
            charge(h, .9);
        }).thenIdle(3).thenExecute(() -> {
            outputs(h, positions, 15,15,0,0,0);
            charge(h, 1);
        }).thenIdle(3).thenExecute(() -> {
            outputs(h, positions, 15,0,15,0,0);
            charge(h, .999999999);
        }).thenIdle(3).thenExecute(() -> {
            outputs(h, positions, 15,0,0,0,0);
            h.setBlock(BlockPos.ZERO, Blocks.AIR);
        }).thenIdle(5).thenExecute(() -> outputs(h, positions, 0,0,0,0,0)).thenSucceed();
    }

    @GameTest(template = "empty_17x17x17", timeoutTicks = 240)
    public static void batterySensorSwitchesAnAdjacentTurbine(GameTestHelper h) {
        GuideStructures.battery().parts().forEach(h::setBlock);
        GuideStructures.turbine().parts().forEach((pos, state) -> h.setBlock(pos.offset(5,0,0), state));
        var sensorPos = new BlockPos(4,2,2);
        var commandPos = new BlockPos(5,2,2);
        h.setBlock(sensorPos, BatteryRedstonePort.INSTANCE);
        h.setBlock(commandPos, TurbineRedstonePort.INSTANCE);
        h.startSequence().thenWaitUntil(() -> { ready(h, sensorPos); ready(h, commandPos); }).thenExecute(() -> {
            config(h, tile(h, sensorPos), Mode.BELOW, 90);
            var input = (TurbineRedstonePortTile) h.getBlockEntity(commandPos);
            h.assertTrue(input.configure(Map.of("mode", 1, "behavior", 0, "low", 0d, "high", 0d)), "turbine command rejected");
        }).thenIdle(4).thenExecute(() -> {
            h.assertTrue(((TurbineRedstonePortTile) h.getBlockEntity(commandPos)).controller().simulation().active(), "low battery did not start turbine");
            charge(h, .95);
        }).thenIdle(4).thenExecute(() -> {
            h.assertFalse(((TurbineRedstonePortTile) h.getBlockEntity(commandPos)).controller().simulation().active(), "high battery did not stop turbine");
            charge(h, .85);
        }).thenIdle(4).thenExecute(() -> {
            h.assertTrue(((TurbineRedstonePortTile) h.getBlockEntity(commandPos)).controller().simulation().active(), "falling battery did not restart turbine");
        }).thenSucceed();
    }

    @GameTest(template = "empty_7x7x7", timeoutTicks = 160)
    public static void settingsSurviveSaveAndRejectMalformedRequests(GameTestHelper h) {
        GuideStructures.battery().parts().forEach(h::setBlock);
        var pos = new BlockPos(0,1,1);
        h.setBlock(pos, BatteryRedstonePort.INSTANCE);
        h.startSequence().thenWaitUntil(() -> ready(h, pos)).thenExecute(() -> {
            var sensor = tile(h, pos);
            config(h, sensor, Mode.ABOVE, 42.5);
            for (double bad : new double[]{-1, 100.01, Double.NaN, Double.POSITIVE_INFINITY})
                h.assertFalse(sensor.configure(Map.of("mode", 1, "threshold", bad)), "invalid threshold accepted");
            for (Object bad : List.of("bad", Map.of(), Map.of("mode", 99, "threshold", 50d), Map.of("mode", 1.5, "threshold", 50d)))
                h.assertFalse(sensor.configure(bad), "malformed request accepted");
            h.assertTrue(sensor.mode() == Mode.ABOVE && sensor.threshold() == 42.5, "invalid packet mutated configuration");
            var saved = sensor.saveWithFullMetadata(h.getLevel().registryAccess());
            var restored = BatteryRedstonePortTile.SUPPLIER.create(sensor.getBlockPos(), sensor.getBlockState());
            restored.loadWithComponents(saved, h.getLevel().registryAccess());
            h.assertTrue(restored.mode() == Mode.ABOVE && restored.threshold() == 42.5, "configuration lost on save/load");
            restored.read(sensor.write());
            h.assertTrue(restored.settingsReady && restored.mode() == Mode.ABOVE && restored.threshold() == 42.5, "menu snapshot lost configuration");
            var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(h.getLevel());
            player.setPos(sensor.getBlockPos().getX() + .5, sensor.getBlockPos().getY(), sensor.getBlockPos().getZ() + .5);
            var menu = new BatteryRedstonePortContainer(42, sensor.getBlockPos(), player);
            h.assertTrue(menu.stillValid(player) && menu.getGuiPacket() == sensor && menu.machinePos().equals(sensor.getBlockPos()), "sensor did not expose its own valid menu");
            h.assertTrue(!BatteryRedstonePortTile.matches(Mode.FULL, 90, Long.MAX_VALUE - 1, Long.MAX_VALUE), "almost full rounded to full");
            h.assertTrue(!BatteryRedstonePortTile.matches(Mode.ABOVE, 100, Long.MAX_VALUE - 1, Long.MAX_VALUE), "almost full rounded to 100 percent");
            h.assertTrue(BatteryRedstonePortTile.matches(Mode.ABOVE, 50, Long.MAX_VALUE, Long.MAX_VALUE), "large energy overflowed");
            h.assertFalse(BatteryRedstonePortTile.matches(Mode.EMPTY, 90, 0, 0), "zero capacity emitted empty signal");
            h.setBlock(BlockPos.ZERO, Blocks.AIR);
        }).thenIdle(5).thenExecute(() -> {
            var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(h.getLevel());
            h.assertFalse(new BatteryRedstonePortContainer(42, h.absolutePos(pos), player).stillValid(player), "menu remained valid after disassembly");
        }).thenSucceed();
    }
}
