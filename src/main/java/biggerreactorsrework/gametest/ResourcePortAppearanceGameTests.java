package biggerreactorsrework.gametest;

// Added 2026-10-30: resource-port snapshots share terminal appearance while keeping their own controls/origins.
import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.core.client.gui.GuiSync;
import biggerreactorsrework.core.client.gui.api.IHasUpdatableState;
import biggerreactorsrework.core.multiblock.IAssemblyStateBlock;
import biggerreactorsrework.multiblocks.battery.blocks.*;
import biggerreactorsrework.multiblocks.battery.tiles.*;
import biggerreactorsrework.multiblocks.battery.containers.BatteryTerminalContainer;
import biggerreactorsrework.multiblocks.reactor.blocks.*;
import biggerreactorsrework.multiblocks.reactor.tiles.*;
import biggerreactorsrework.multiblocks.turbine.blocks.*;
import biggerreactorsrework.multiblocks.turbine.tiles.*;
import biggerreactorsrework.multiblocks.heatexchanger.blocks.*;
import biggerreactorsrework.multiblocks.heatexchanger.tiles.*;
import biggerreactorsrework.util.GuideStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.*;
import java.util.List;

public final class ResourcePortAppearanceGameTests {
    private static GuiSync.IGUIPacket packet(BlockEntity tile) {
        if (tile instanceof BatteryBaseTile && !(tile instanceof BatteryEnergyPortTile))
            return ((BatteryBaseTile)tile).batteryState();
        return (GuiSync.IGUIPacket)((IHasUpdatableState<?>)tile).getState();
    }
    private static BlockPos appearance(BlockEntity tile) {
        if (tile instanceof ReactorBaseTile reactor) return reactor.appearancePos;
        if (tile instanceof TurbineBaseTile turbine) return turbine.appearancePos;
        if (tile instanceof HeatExchangerBaseTile exchanger) return exchanger.appearancePos;
        return ((BatteryBaseTile)tile).batteryState().appearancePos;
    }
    private static void check(GameTestHelper h,BlockPos expected,List<BlockPos> positions) {
        var player=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(h.getLevel());
        for (var relative:positions) {
            var tile=GameTestSupport.be(h,relative);
            var menu=tile instanceof BatteryTerminalTile
                    ? new BatteryTerminalContainer(42,tile.getBlockPos(),player)
                    : ((MenuProvider)tile).createMenu(42,player.getInventory(),player);
            var provider=(GuiSync.IGUIPacketProvider)menu;
            var data=provider.getGuiPacket().write();
            h.assertTrue(Long.valueOf(h.absolutePos(expected).asLong()).equals(data.get("appearancePos")),"separate resource-port appearance at "+relative);
            h.assertTrue(provider.machinePos().equals(tile.getBlockPos()),"appearance sharing moved command/menu origin");
            var received=tile.getType().create(tile.getBlockPos(),tile.getBlockState());
            packet(received).read(data);
            h.assertTrue(appearance(received).equals(h.absolutePos(expected)),"client lost shared appearance at "+relative);
            // Both input and output modes must carry the same anchor and preserve port-specific controls.
            if (!(tile instanceof ReactorTerminalTile || tile instanceof TurbineTerminalTile
                    || tile instanceof HeatExchangerTerminalTile || tile instanceof BatteryTerminalTile)) {
                for(int direction:new int[]{0,1}) {
                    provider.executeRequest("setDirection",direction);
                    var changed=provider.getGuiPacket().write();
                    h.assertTrue(changed.get("direction").equals(direction==0),"appearance sync changed direction semantics");
                    h.assertTrue(data.get("appearancePos").equals(changed.get("appearancePos")),"port direction changed theme identity");
                    packet(received).read(changed);
                    h.assertTrue(appearance(received).equals(h.absolutePos(expected)),"client theme changed with port mode");
                }
            }
        }
    }
    private static void verify(GameTestHelper h,BlockPos first,BlockPos second,Block casing,List<BlockPos> ports) {
        var all=new java.util.ArrayList<>(ports);all.add(first);all.add(second);
        h.startSequence().thenWaitUntil(() -> h.assertTrue(h.getBlockState(first).getValue(IAssemblyStateBlock.ASSEMBLED),"machine not assembled"))
            .thenExecute(() -> { check(h,first,all); h.setBlock(first,casing); })
            .thenIdle(5).thenWaitUntil(() -> h.assertTrue(h.getBlockState(second).getValue(IAssemblyStateBlock.ASSEMBLED),"machine not reassembled"))
            .thenExecute(() -> { all.remove(first);check(h,second,all); }).thenSucceed();
    }
    @GameTest(template="empty_7x7x7",timeoutTicks=240)
    public static void reactorItemAndCoolantPortsFollowTerminalAppearance(GameTestHelper h) {
        GuideStructures.reactor(5,true).parts().forEach(h::setBlock);
        var first=new BlockPos(0,1,2); var second=new BlockPos(4,2,2);
        var items=new BlockPos(0,2,1); var coolant=new BlockPos(0,2,3);
        h.setBlock(second,ReactorTerminal.INSTANCE);h.setBlock(items,ReactorAccessPort.INSTANCE);h.setBlock(coolant,ReactorCoolantPort.INSTANCE);
        verify(h,first,second,ReactorCasing.INSTANCE,List.of(items,coolant));
    }
    @GameTest(template="empty_7x7x7",timeoutTicks=240)
    public static void turbineFluidPortsFollowTerminalAppearance(GameTestHelper h) {
        GuideStructures.turbine().parts().forEach(h::setBlock);
        var second=new BlockPos(4,2,2);h.setBlock(second,TurbineTerminal.INSTANCE);
        verify(h,new BlockPos(0,1,2),second,TurbineCasing.INSTANCE,List.of(new BlockPos(2,1,0),new BlockPos(2,1,4)));
    }
    @GameTest(template="empty_7x7x7",timeoutTicks=240)
    public static void exchangerBothCircuitsFollowTerminalAppearance(GameTestHelper h) {
        GuideStructures.exchanger().parts().forEach(h::setBlock);
        var second=new BlockPos(2,1,3);h.setBlock(second,HeatExchangerTerminalBlock.INSTANCE);
        verify(h,new BlockPos(1,1,0),second,HeatExchangerCasingBlock.INSTANCE,
                List.of(new BlockPos(0,1,1),new BlockPos(3,1,1),new BlockPos(0,1,2),new BlockPos(3,1,2)));
    }
    @GameTest(template="empty_7x7x7",timeoutTicks=240)
    public static void batteryEnergyPortsFollowTerminalAppearance(GameTestHelper h) {
        GuideStructures.battery().parts().forEach(h::setBlock);
        var second=new BlockPos(4,2,2);h.setBlock(second,BatteryTerminal.INSTANCE);
        verify(h,new BlockPos(0,2,2),second,BatteryCasing.INSTANCE,List.of(new BlockPos(2,2,0),new BlockPos(2,2,4)));
    }
}
