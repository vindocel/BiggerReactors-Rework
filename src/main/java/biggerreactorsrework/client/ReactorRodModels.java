package biggerreactorsrework.client;

// Added 2026-10-28: suppress internal end plates and full-column fluid caps.
import biggerreactorsrework.multiblocks.reactor.blocks.ReactorFuelRod;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import static biggerreactorsrework.multiblocks.reactor.blocks.ReactorFuelRod.*;

public final class ReactorRodModels {
    private static final ModelProperty<Integer> CAPS = new ModelProperty<>();
    private ReactorRodModels() {}
    public static void init(IEventBus bus) { bus.addListener(ReactorRodModels::bake); }
    private static void bake(ModelEvent.ModifyBakingResult event) {
        for (var state : ReactorFuelRod.INSTANCE.getStateDefinition().getPossibleStates()) {
            var location = BlockModelShaper.stateToModelLocation(state);
            var original = event.getModels().get(location);
            if (original != null) event.getModels().put(location, new RodModel(original));
        }
    }
    private static final class RodModel extends BakedModelWrapper<BakedModel> {
        RodModel(BakedModel original) { super(original); }
        @Override public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData data) {
            var above = level.getBlockState(pos.above());
            var below = level.getBlockState(pos.below());
            boolean a = above.is(ReactorFuelRod.INSTANCE), b = below.is(ReactorFuelRod.INSTANCE);
            int mask = ReactorRodCaps.mask(state.getValue(FUEL_HEIGHT_PROPERTY), state.getValue(WASTE_HEIGHT_PROPERTY),
                    a, a ? above.getValue(FUEL_HEIGHT_PROPERTY) : 0, a ? above.getValue(WASTE_HEIGHT_PROPERTY) : 0,
                    b, b ? below.getValue(FUEL_HEIGHT_PROPERTY) : 0, b ? below.getValue(WASTE_HEIGHT_PROPERTY) : 0);
            return super.getModelData(level, pos, state, data).derive().with(CAPS, mask).build();
        }
        @Override public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random,
                                                 ModelData data, @Nullable RenderType type) {
            var quads = super.getQuads(state, side, random, data, type);
            Integer mask = data.get(CAPS);
            if (mask == null || mask == 0) return quads;
            return quads.stream().filter(quad -> !hidden(quad, mask)).toList();
        }
        private static boolean hidden(BakedQuad quad, int mask) {
            var direction = quad.getDirection();
            if (direction != Direction.UP && direction != Direction.DOWN) return false;
            boolean up = direction == Direction.UP;
            String sprite = quad.getSprite().contents().name().getPath();
            int flag = switch (sprite) {
                case "block/reactor/fuel_rod/end" -> up ? ReactorRodCaps.FRAME_UP : ReactorRodCaps.FRAME_DOWN;
                case "block/fluid/liquid_uranium_still" -> up ? ReactorRodCaps.FUEL_UP : ReactorRodCaps.FUEL_DOWN;
                case "block/fluid/fluid_cyanite_still" -> up ? ReactorRodCaps.WASTE_UP : ReactorRodCaps.WASTE_DOWN;
                default -> 0;
            };
            return (mask & flag) != 0;
        }
    }
}
