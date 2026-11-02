package biggerreactorsrework.core.fluids;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;

/** Added 2026-11-01: native container transactions through a fluid port's existing direction and tank rules. */
public final class FluidPortInteraction {
    private FluidPortInteraction() {}

    public static InteractionResult use(Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        var held = player.getItemInHand(hand);
        if (held.isEmpty() || ItemAccess.forPlayerInteraction(player, hand).getCapability(Capabilities.Fluid.ITEM) == null) {
            return InteractionResult.PASS;
        }
        // A rejected container must not fall through to bucket placement in the world beside the port.
        if (player.isSpectator() || !level.mayInteract(player, pos) || !player.mayUseItemAt(pos, hit.getDirection(), held)) {
            return InteractionResult.FAIL;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        // Vanilla bucket capabilities cannot retain fluid components such as heated-coolant identity.
        // Refuse their extraction rather than silently returning an unidentified bucket.
        if (held.is(net.minecraft.world.item.Items.BUCKET)) {
            var handler = level.getCapability(Capabilities.Fluid.BLOCK, pos, hit.getDirection());
            if (handler != null) for (int slot = 0; slot < handler.size(); slot++) {
                var fluid = handler.getResource(slot);
                if (!fluid.isEmpty() && handler.getAmountAsLong(slot) > 0 && !fluid.toStack(1).getComponentsPatch().isEmpty()) {
                    return InteractionResult.FAIL;
                }
            }
        }
        return FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection(), null)
                ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }
}
