package biggerreactorsrework.core.fluids;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidUtil;

/** Added 2026-11-01: native container handling through a fluid port's existing direction and tank rules. */
public final class FluidPortInteraction {
    private FluidPortInteraction() {}

    public static InteractionResult use(Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        var held = player.getItemInHand(hand);
        if (held.isEmpty() || FluidUtil.getFluidHandler(held.copyWithCount(1)).isEmpty()) return InteractionResult.PASS;
        // A refusal must consume the click: anything else lets the client go on to place the bucket beside the port.
        if (player.isSpectator() || !level.mayInteract(player, pos) || !player.mayUseItemAt(pos, hit.getDirection(), held)) {
            return InteractionResult.CONSUME;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        // Plain buckets cannot retain fluid components such as heated-coolant identity.
        if (held.is(net.minecraft.world.item.Items.BUCKET)) {
            var handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, hit.getDirection());
            if (handler != null) for (int tank = 0; tank < handler.getTanks(); tank++) {
                var fluid = handler.getFluidInTank(tank);
                if (!fluid.isEmpty() && !fluid.getComponentsPatch().isEmpty()) return InteractionResult.CONSUME;
            }
        }
        return FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection())
                ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }
}
