package biggerreactorsrework.gametest;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.multiblocks.battery.blocks.*;
import biggerreactorsrework.multiblocks.battery.containers.BatteryTerminalContainer;
import biggerreactorsrework.multiblocks.battery.state.BatteryState;
import biggerreactorsrework.multiblocks.battery.tiles.BatteryBaseTile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;

/** Added 2026-10-29: terminal and body menus retain their origins but share one synced appearance identity. */
public final class BatteryAppearanceGameTests {
    @GameTest(template = "empty_7x7x7", timeoutTicks = 160)
    public static void allBatteryEntryPointsShareTheTerminalAppearance(GameTestHelper h) {
        for (int x = 1; x <= 3; x++) for (int y = 1; y <= 3; y++) for (int z = 1; z <= 3; z++)
            h.setBlock(x, y, z, x == 2 && y == 2 && z == 2 ? BatteryCell.INSTANCE : BatteryCasing.INSTANCE);
        var terminal = new BlockPos(1, 2, 2);
        var secondTerminal = new BlockPos(3, 2, 2);
        var body = new BlockPos(1, 1, 1);
        var glass = new BlockPos(2, 3, 2);
        h.setBlock(terminal, BatteryTerminal.INSTANCE);
        h.setBlock(secondTerminal, BatteryTerminal.INSTANCE);
        h.setBlock(glass, BatteryGlass.INSTANCE);
        h.setBlock(2, 2, 1, BatteryEnergyPort.INSTANCE);
        h.setBlock(2, 2, 3, BatteryEnergyPort.INSTANCE);
        var sensorPos = new BlockPos(2, 1, 2);
        h.setBlock(sensorPos, BatteryRedstonePort.INSTANCE);
        h.startSequence().thenWaitUntil(() -> h.assertTrue(((BatteryBaseTile) GameTestSupport.be(h,terminal)).isBatteryAssembled(), "battery not ready"))
                .thenExecute(() -> {
                    var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(h.getLevel());
                    var expected = h.absolutePos(terminal);
                    for (var relative : java.util.List.of(terminal, secondTerminal, body, glass)) {
                        var tile = (BatteryBaseTile) GameTestSupport.be(h,relative);
                        var menu = new BatteryTerminalContainer(42, tile.getBlockPos(), player);
                        var packet = (BatteryState) menu.getGuiPacket();
                        var data = packet.write();
                        h.assertTrue(expected.asLong() == (Long) data.get("appearancePos"), "appearance depends on clicked block " + relative);
                        h.assertTrue(menu.machinePos().equals(tile.getBlockPos()), "appearance identity changed the menu/network origin");
                        var received = new BatteryState(tile);
                        h.assertTrue(!received.appearanceReady, "appearance was editable before its identity arrived");
                        received.read(data);
                        h.assertTrue(received.appearanceReady && received.appearancePos.equals(expected), "appearance identity was lost in synchronization");
                    }
                    h.assertTrue(GameTestSupport.be(h,new BlockPos(2, 2, 1)) instanceof net.minecraft.world.MenuProvider, "energy port lost its own menu");
                    var sensor = (biggerreactorsrework.multiblocks.battery.tiles.BatteryRedstonePortTile) GameTestSupport.be(h,sensorPos);
                    var sensorMenu = new biggerreactorsrework.multiblocks.battery.containers.BatteryRedstonePortContainer(43, sensor.getBlockPos(), player);
                    var snapshot = sensorMenu.getGuiPacket().write();
                    h.assertTrue(expected.asLong() == (Long) snapshot.get("appearancePos"), "sensor uses a different appearance identity");
                    var receivedSensor = biggerreactorsrework.multiblocks.battery.tiles.BatteryRedstonePortTile.SUPPLIER.create(sensor.getBlockPos(), sensor.getBlockState());
                    receivedSensor.read(snapshot);
                    h.assertTrue(receivedSensor.batteryState().appearanceReady && receivedSensor.batteryState().appearancePos.equals(expected), "sensor lost the battery theme identity on synchronization");
                    h.assertTrue(sensorMenu.machinePos().equals(sensor.getBlockPos()), "theme sharing changed the sensor request origin");
                }).thenSucceed();
    }
}
