package biggerreactorsrework.compat.carryon;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.core.multiblock.IAssemblyStateBlock;
import biggerreactorsrework.core.multiblock.IMultiblockBlock;
import biggerreactorsrework.core.multiblock.MultiblockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.lang.reflect.Method;

/** Added 2026-10-29: formed machines cannot be picked up; loose blocks and placement remain unaffected. */
public final class CarryOnCompat {
    private static Method carryData, isCarrying;
    private static boolean warned;

    private CarryOnCompat() { }

    /** Called only when the optional mod is present; no Carry On classes are needed to build or load the base mod. */
    public static void init() {
        try {
            carryData = Class.forName("tschipp.carryon.common.carry.CarryOnDataManager").getMethod("getCarryData", Player.class);
            isCarrying = carryData.getReturnType().getMethod("isCarrying");
        } catch (ReflectiveOperationException e) {
            BiggerReactors.LOGGER.warn("Carry On state lookup is unavailable; protecting formed machines using the default pickup gesture", e);
        }
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, CarryOnCompat::onRightClickBlock);
    }

    public static boolean protectedPart(Level level, BlockPos pos) {
        var state = level.getBlockState(pos);
        if (state.getBlock() instanceof IMultiblockBlock && state.hasProperty(IAssemblyStateBlock.ASSEMBLED)
                && state.getValue(IAssemblyStateBlock.ASSEMBLED)) return true;
        return level instanceof ServerLevel server && MultiblockRegistry.isInsideAssembledMultiblock(server, pos);
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        var player = event.getEntity();
        if (event.isCanceled() || player.isSpectator() || !player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty()) return;
        if (!protectedPart(event.getLevel(), event.getPos())) return;
        try {
            if (carryData != null && isCarrying != null) {
                if ((Boolean) isCarrying.invoke(carryData.invoke(null, player))) return;
            } else if (!player.isShiftKeyDown()) return;
        } catch (ReflectiveOperationException e) {
            if (!warned) { BiggerReactors.LOGGER.warn("Could not read Carry On state; using the default pickup gesture", e); warned = true; }
            if (!player.isShiftKeyDown()) return;
        }
        // Consume the event before pickup, but execute the block's normal interaction once so menus still open.
        var result = InteractionResult.PASS;
        if (event.getUseBlock() != TriState.FALSE) {
            var state = event.getLevel().getBlockState(event.getPos());
            var itemResult = state.useItemOn(ItemStack.EMPTY, event.getLevel(), player, event.getHand(), event.getHitVec());
            result = itemResult == ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION && event.getHand() == InteractionHand.MAIN_HAND
                    ? state.useWithoutItem(event.getLevel(), player, event.getHitVec()) : itemResult.result();
        }
        event.setCancellationResult(result.consumesAction() ? result : InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}
