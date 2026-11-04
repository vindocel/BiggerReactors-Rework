package biggerreactorsrework.fluids;

import biggerreactorsrework.core.registry.ModFluid;
import biggerreactorsrework.core.registry.RegisterFluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

/** Component-bearing transport fluid. The origin is preserved in tanks, never placed in the reactor world. */
// Modified 2026-11-03: every coolant heated by the reactor burns entities like lava when placed.
@RegisterFluid(name = "heated_coolant", creativeTab = false, burns = true, registerBucket = true, waterLikeMovement = true)
public class HeatedCoolant extends ModFluid {
    @RegisterFluid.Instance public static HeatedCoolant INSTANCE;
    public HeatedCoolant(BaseFlowingFluid.Properties properties) { super(properties); }

    @Override
    protected void spreadTo(net.minecraft.world.level.LevelAccessor level, net.minecraft.core.BlockPos pos,
                            net.minecraft.world.level.block.state.BlockState state, net.minecraft.core.Direction direction,
                            net.minecraft.world.level.material.FluidState target) {
        super.spreadTo(level, pos, state, direction, target);
        if (level instanceof net.minecraft.server.level.ServerLevel server && isSame(server.getFluidState(pos).getType())) {
            var from = pos.relative(direction.getOpposite());
            var origin = PlacedCoolantOrigins.get(server.getChunkAt(from), from);
            PlacedCoolantOrigins.put(server, pos, origin);
        }
    }
}
