package biggerreactorsrework.client;

// Added 2026-10-28: suppress internal end plates and full-column fluid caps.
import biggerreactorsrework.multiblocks.reactor.blocks.ReactorFuelRod;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.DelegateBlockStateModel;
import org.jspecify.annotations.Nullable;
import java.util.List;
import static biggerreactorsrework.multiblocks.reactor.blocks.ReactorFuelRod.*;

public final class ReactorRodModels {
    private ReactorRodModels() {}
    public static void init(IEventBus bus) { bus.addListener(ReactorRodModels::bake); }
    private static void bake(ModelEvent.ModifyBakingResult event) {
        var models = event.getBakingResult().blockStateModels();
        for (var state : ReactorFuelRod.INSTANCE.getStateDefinition().getPossibleStates()) {
            var original = models.get(state);
            if (original != null) models.put(state, new RodModel(original));
        }
    }
    private static final class RodModel extends DelegateBlockStateModel {
        RodModel(BlockStateModel original) { super(original); }
        @Override public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> parts) {
            int start = parts.size();
            super.collectParts(level, pos, state, random, parts);
            var above = level.getBlockState(pos.above());
            var below = level.getBlockState(pos.below());
            boolean a = above.is(ReactorFuelRod.INSTANCE), b = below.is(ReactorFuelRod.INSTANCE);
            int mask = ReactorRodCaps.mask(state.getValue(FUEL_HEIGHT_PROPERTY), state.getValue(WASTE_HEIGHT_PROPERTY),
                    a, a ? above.getValue(FUEL_HEIGHT_PROPERTY) : 0, a ? above.getValue(WASTE_HEIGHT_PROPERTY) : 0,
                    b, b ? below.getValue(FUEL_HEIGHT_PROPERTY) : 0, b ? below.getValue(WASTE_HEIGHT_PROPERTY) : 0);
            if (mask == 0) return;
            for (int i = start; i < parts.size(); i++) parts.set(i, new Filtered(parts.get(i), mask));
        }
    }
    private record Filtered(BlockStateModelPart original, int mask) implements BlockStateModelPart {
        @Override public List<BakedQuad> getQuads(@Nullable Direction side) {
            return original.getQuads(side).stream().filter(quad -> !hidden(quad, mask)).toList();
        }
        @Override public boolean useAmbientOcclusion() { return original.useAmbientOcclusion(); }
        @Override public Material.Baked particleMaterial() { return original.particleMaterial(); }
        @Override public int materialFlags() { return original.materialFlags(); }
    }
    private static boolean hidden(BakedQuad quad, int mask) {
        var direction = quad.direction();
        if (direction != Direction.UP && direction != Direction.DOWN) return false;
        boolean up = direction == Direction.UP;
        String sprite = quad.materialInfo().sprite().contents().name().getPath();
        int flag = switch (sprite) {
            case "block/reactor/fuel_rod/end" -> up ? ReactorRodCaps.FRAME_UP : ReactorRodCaps.FRAME_DOWN;
            case "block/fluid/liquid_uranium_still" -> up ? ReactorRodCaps.FUEL_UP : ReactorRodCaps.FUEL_DOWN;
            case "block/fluid/fluid_cyanite_still" -> up ? ReactorRodCaps.WASTE_UP : ReactorRodCaps.WASTE_DOWN;
            default -> 0;
        };
        return (mask & flag) != 0;
    }
}
