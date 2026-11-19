package biggerreactorsrework.core.fluids;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
//? if =26.1.2 || =26.3 {
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;

/** Added 2026-11-01: native container transactions through a fluid port's existing direction and tank rules. */
public final class FluidPortInteraction {
//?} else if =1.21.1 {
/*import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidUtil;

/^* Added 2026-11-01: native container handling through a fluid port's existing direction and tank rules. ^/
public final class FluidPortInteraction {
*/
//?}
    private FluidPortInteraction() {}

    public static InteractionResult use(Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
//? if =26.1.2 || =26.3 {
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
                if (!fluid.isEmpty() && handler.getAmountAsLong(slot) > 0
                        && fluid.toStack(1).getFluid() != biggerreactorsrework.fluids.HeatedCoolant.INSTANCE
                        && !fluid.toStack(1).getComponentsPatch().isEmpty()) {
                    return InteractionResult.FAIL;
                }
            }
        }
        return FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection(), null)
                ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }
//?} else if =1.21.1 {
/*        var held = player.getItemInHand(hand);
        if (held.isEmpty() || FluidUtil.getFluidHandler(held.copyWithCount(1)).isEmpty()) return InteractionResult.PASS;
        // A refusal must consume the click: anything else lets the client go on to place the bucket beside the port.
        if (player.isSpectator() || !level.mayInteract(player, pos) || !player.mayUseItemAt(pos, hit.getDirection(), held)) {
            return InteractionResult.CONSUME;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        // Plain buckets cannot retain fluid components; only heated coolant has a bucket that keeps its origin.
        if (held.is(net.minecraft.world.item.Items.BUCKET)) {
            var handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, hit.getDirection());
            if (handler != null) for (int tank = 0; tank < handler.getTanks(); tank++) {
                var fluid = handler.getFluidInTank(tank);
                if (!fluid.isEmpty() && !fluid.getComponentsPatch().isEmpty() && fluid.getFluid() != biggerreactorsrework.fluids.HeatedCoolant.INSTANCE) return InteractionResult.CONSUME;
            }
        }
        return FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection())
                ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }
*/
//?}
}
