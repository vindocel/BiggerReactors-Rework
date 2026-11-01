package biggerreactorsrework.gametest;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.compat.carryon.CarryOnCompat;
import biggerreactorsrework.core.multiblock.IAssemblyStateBlock;
import biggerreactorsrework.multiblocks.reactor.blocks.ReactorCasing;
import biggerreactorsrework.util.GuideStructures;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.*;

import java.util.*;
import java.util.function.Consumer;

/** Added 2026-10-29: real clicks with Carry On present/absent, including loose pickup and carried placement. */
public final class CarryOnGameTests {
    private static final class MenuPlayer extends FakePlayer {
        int opened;
        MenuPlayer(ServerLevel level) { super(level, new GameProfile(UUID.randomUUID(), "CarryOnTest")); }
        @Override public OptionalInt openMenu(MenuProvider provider, Consumer<RegistryFriendlyByteBuf> writer) {
            opened++;
            AbstractContainerMenu menu = provider.createMenu(42, getInventory(), this);
            if (menu != null) containerMenu = menu;
            return OptionalInt.of(42);
        }
    }

    private static Object carryData(Player player) {
        try {
            return Class.forName("tschipp.carryon.common.carry.CarryOnDataManager").getMethod("getCarryData", Player.class).invoke(null, player);
        } catch (ReflectiveOperationException e) { throw new AssertionError("Carry On test API unavailable", e); }
    }

    private static Object invoke(Object target, String method, Class<?>[] types, Object... args) {
        try { return target.getClass().getMethod(method, types).invoke(target, args); }
        catch (ReflectiveOperationException e) { throw new AssertionError("Carry On test call failed: " + method, e); }
    }

    private static void pickupKey(Player player, boolean pressed) {
        if (!ModList.get().isLoaded("carryon")) return;
        var data = carryData(player);
        invoke(data, "setKeyPressed", new Class<?>[]{boolean.class}, pressed);
        invoke(data, "setTick", new Class<?>[]{int.class}, -1);
    }

    private static boolean carrying(Player player) {
        var data = carryData(player);
        return (Boolean) invoke(data, "isCarrying", new Class<?>[0]);
    }

    private static PlayerInteractEvent.RightClickBlock click(MenuPlayer player, BlockPos pos) {
        player.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        var event = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, pos,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        return NeoForge.EVENT_BUS.post(event);
    }

    private static void verify(GameTestHelper h, GuideStructures.Example example) {
        example.parts().forEach(h::setBlock);
        h.startSequence().thenWaitUntil(() -> {
            for (var entry : example.parts().entrySet()) if (entry.getValue().hasProperty(IAssemblyStateBlock.ASSEMBLED))
                h.assertTrue(h.getBlockState(entry.getKey()).getValue(IAssemblyStateBlock.ASSEMBLED), "machine not ready");
        }).thenExecute(() -> {
            var player = new MenuPlayer(h.getLevel());
            boolean installed = ModList.get().isLoaded("carryon");
            pickupKey(player, true);
            for (var entry : example.parts().entrySet()) {
                if (entry.getValue().isAir()) continue;
                var pos = h.absolutePos(entry.getKey());
                var before = h.getLevel().getBlockState(pos);
                h.assertTrue(CarryOnCompat.protectedPart(h.getLevel(), pos), "unprotected formed part " + pos);
                player.setShiftKeyDown(true);
                var event = click(player, pos);
                h.assertTrue(event.isCanceled() == installed, "optional pickup protection was not applied correctly");
                h.assertTrue(h.getLevel().getBlockState(pos).equals(before), "formed part was removed");
                if (installed) h.assertTrue(!carrying(player), "Carry On picked up a formed part");
            }
            if (installed) {
                h.assertTrue(player.opened > 0, "protection swallowed every terminal/port interaction");
                var pos = h.absolutePos(example.parts().keySet().iterator().next());
                player.setShiftKeyDown(false);
                h.assertTrue(click(player, pos).isCanceled(), "a remapped pickup key bypassed protection");
                pickupKey(player, false);
            }
            player.containerMenu = player.inventoryMenu;
        }).thenExecute(() -> h.setBlock(BlockPos.ZERO, Blocks.AIR)).thenWaitUntil(() -> {
            for (var entry : example.parts().entrySet()) {
                var state = h.getBlockState(entry.getKey());
                if (state.hasProperty(IAssemblyStateBlock.ASSEMBLED))
                    h.assertTrue(!state.getValue(IAssemblyStateBlock.ASSEMBLED), "machine still assembled after removing a corner");
            }
        }).thenExecute(() -> {
            for (var pos : example.parts().keySet())
                h.assertTrue(!CarryOnCompat.protectedPart(h.getLevel(), h.absolutePos(pos)), "protection remained after disassembly");
            if (ModList.get().isLoaded("carryon")) {
                var player = new MenuPlayer(h.getLevel());
                player.setShiftKeyDown(true);
                pickupKey(player, true);
                var pos = h.absolutePos(new BlockPos(0, 0, 1));
                click(player, pos);
                h.assertTrue(carrying(player) && h.getLevel().getBlockState(pos).isAir(), "disassembled part could not be picked up");
                invoke(carryData(player), "clear", new Class<?>[0]);
            }
        }).thenSucceed();
    }

    @GameTest(template="empty_7x7x7", timeoutTicks=200) public static void reactor(GameTestHelper h) { verify(h, GuideStructures.reactor(5, false)); }
    @GameTest(template="empty_7x7x7", timeoutTicks=200) public static void liquidReactor(GameTestHelper h) { verify(h, GuideStructures.reactor(5, true)); }
    @GameTest(template="empty_7x7x7", timeoutTicks=200) public static void turbine(GameTestHelper h) { verify(h, GuideStructures.turbine()); }
    @GameTest(template="empty_7x7x7", timeoutTicks=200) public static void exchanger(GameTestHelper h) { verify(h, GuideStructures.exchanger()); }
    @GameTest(template="empty_7x7x7", timeoutTicks=200) public static void battery(GameTestHelper h) { verify(h, GuideStructures.battery()); }

    @GameTest(template="empty_7x7x7", timeoutTicks=120)
    public static void loosePartsRemainPickable(GameTestHelper h) {
        var relative = new BlockPos(2, 2, 2);
        h.setBlock(relative, ReactorCasing.INSTANCE);
        h.startSequence().thenIdle(5).thenExecute(() -> {
            var pos = h.absolutePos(relative);
            h.assertTrue(!CarryOnCompat.protectedPart(h.getLevel(), pos), "loose part was protected");
            var player = new MenuPlayer(h.getLevel());
            player.setShiftKeyDown(true);
            pickupKey(player, true);
            var event = click(player, pos);
            if (ModList.get().isLoaded("carryon")) {
                h.assertTrue(event.isCanceled() && carrying(player) && h.getLevel().getBlockState(pos).isAir(), "Carry On could not pick up a loose part");
                var data = carryData(player);
                invoke(data, "clear", new Class<?>[0]);
            } else h.assertTrue(!event.isCanceled(), "compatibility affected play without Carry On");
        }).thenSucceed();
    }

    @GameTest(template="empty_7x7x7", timeoutTicks=200)
    public static void carriedBlockCanBePlacedBesideAFormedMachine(GameTestHelper h) {
        if (!ModList.get().isLoaded("carryon")) { h.succeed(); return; }
        var example = GuideStructures.reactor(3, false);
        example.parts().forEach(h::setBlock);
        h.startSequence().thenWaitUntil(() -> h.assertTrue(h.getBlockState(new BlockPos(0,1,1)).getValue(IAssemblyStateBlock.ASSEMBLED), "machine not ready"))
                .thenExecute(() -> {
                    var player = new MenuPlayer(h.getLevel());
                    var data = carryData(player);
                    // Modified 2026-11-01: Carry On 26.1.2 also records the player and source position.
                    invoke(data, "setBlock", new Class<?>[]{BlockState.class, BlockEntity.class, net.minecraft.server.level.ServerPlayer.class, BlockPos.class},
                            Blocks.STONE.defaultBlockState(), null, player, h.absolutePos(new BlockPos(5,1,5)));
                    var pos = h.absolutePos(new BlockPos(0,2,1));
                    click(player, pos);
                    h.assertTrue(h.getLevel().getBlockState(pos.above()).is(Blocks.STONE), "carried block placement was blocked");
                    h.assertTrue(!carrying(player), "placed block remained in Carry On inventory");
                }).thenSucceed();
    }
}
