package biggerreactorsrework.multiblocks.heatexchanger.client;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.multiblocks.heatexchanger.blocks.HeatExchangerCondenserChannelBlock;
import biggerreactorsrework.multiblocks.heatexchanger.blocks.HeatExchangerEvaporatorChannelBlock;
import com.mojang.math.Transformation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.QuadTransformers;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static biggerreactorsrework.core.modular.block.IConnectedTexture.Module.*;

/**
 * Picks the heat exchanger channel model (straight, corner or connection error) and its orientation from the
 * block state, and bakes the result into the chunk mesh. The selection rules and rotations are the ones the
 * channels always used; only the rendering path changed from per-instance drawing to static baked quads.
 */
public final class HeatExchangerChannelModels {
    private HeatExchangerChannelModels() {
    }

    private static final ModelResourceLocation CONNECTION_ERROR = model("block/heat_exchanger/casing/corner");
    private static final ModelResourceLocation CONDENSER_STRAIGHT = model("block/heat_exchanger/hot_channel/connected_tb");
    private static final ModelResourceLocation CONDENSER_CORNER = model("block/heat_exchanger/hot_channel/connected_bn");
    private static final ModelResourceLocation EVAPORATOR_STRAIGHT = model("block/heat_exchanger/cold_channel/connected_tb");
    private static final ModelResourceLocation EVAPORATOR_CORNER = model("block/heat_exchanger/cold_channel/connected_bn");

    private static ModelResourceLocation model(String path) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(BiggerReactors.modid, path));
    }

    private static final int TOP_CONNECTED_BIT = 1;
    private static final int BOTTOM_CONNECTED_BIT = 2;
    private static final int NORTH_CONNECTED_BIT = 4;
    private static final int SOUTH_CONNECTED_BIT = 8;
    private static final int EAST_CONNECTED_BIT = 16;
    private static final int WEST_CONNECTED_BIT = 32;

    private static final Matrix4f[] ROTATIONS = new Matrix4f[64];

    static {
        for (int i = 0; i < ROTATIONS.length; i++) {
            ROTATIONS[i] = new Matrix4f();
        }
        final var preTranslate = new Vector3f(0.5f);
        final var postTranslate = new Vector3f(-0.5f);
        ROTATIONS[NORTH_CONNECTED_BIT | SOUTH_CONNECTED_BIT].translate(preTranslate).rotate((float) Math.toRadians(90), 1, 0, 0).translate(postTranslate);
        ROTATIONS[NORTH_CONNECTED_BIT].set(ROTATIONS[NORTH_CONNECTED_BIT | SOUTH_CONNECTED_BIT]);
        ROTATIONS[SOUTH_CONNECTED_BIT].set(ROTATIONS[NORTH_CONNECTED_BIT | SOUTH_CONNECTED_BIT]);
        ROTATIONS[EAST_CONNECTED_BIT | WEST_CONNECTED_BIT].translate(preTranslate).rotate((float) Math.toRadians(90), 0, 0, 1).translate(postTranslate);
        ROTATIONS[EAST_CONNECTED_BIT].set(ROTATIONS[EAST_CONNECTED_BIT | WEST_CONNECTED_BIT]);
        ROTATIONS[WEST_CONNECTED_BIT].set(ROTATIONS[EAST_CONNECTED_BIT | WEST_CONNECTED_BIT]);

        ROTATIONS[BOTTOM_CONNECTED_BIT | NORTH_CONNECTED_BIT].identity();
        ROTATIONS[BOTTOM_CONNECTED_BIT | WEST_CONNECTED_BIT].translate(preTranslate).rotate((float) Math.toRadians(90), 0, 1, 0).translate(postTranslate);
        ROTATIONS[BOTTOM_CONNECTED_BIT | SOUTH_CONNECTED_BIT].translate(preTranslate).rotate((float) Math.toRadians(180), 0, 1, 0).translate(postTranslate);
        ROTATIONS[BOTTOM_CONNECTED_BIT | EAST_CONNECTED_BIT].translate(preTranslate).rotate((float) Math.toRadians(-90), 0, 1, 0).translate(postTranslate);

        ROTATIONS[TOP_CONNECTED_BIT | NORTH_CONNECTED_BIT].translate(preTranslate).rotate((float) Math.toRadians(90), 1, 0, 0).translate(postTranslate);
        ROTATIONS[TOP_CONNECTED_BIT | WEST_CONNECTED_BIT].translate(preTranslate).rotate((float) Math.toRadians(180), 1, 0, 0).rotate((float) Math.toRadians(90), 0, 1, 0).translate(postTranslate);
        ROTATIONS[TOP_CONNECTED_BIT | SOUTH_CONNECTED_BIT].translate(preTranslate).rotate((float) Math.toRadians(180), 1, 0, 0).translate(postTranslate);
        ROTATIONS[TOP_CONNECTED_BIT | EAST_CONNECTED_BIT].translate(preTranslate).rotate((float) Math.toRadians(180), 1, 0, 0).rotate((float) Math.toRadians(-90), 0, 1, 0).translate(postTranslate);

        ROTATIONS[SOUTH_CONNECTED_BIT | WEST_CONNECTED_BIT].translate(preTranslate).rotate((float) Math.toRadians(-90), 1, 0, 0).rotate((float) Math.toRadians(90), 0, 1, 0).translate(postTranslate);
        ROTATIONS[NORTH_CONNECTED_BIT | EAST_CONNECTED_BIT].translate(preTranslate).rotate((float) Math.toRadians(90), 1, 0, 0).rotate((float) Math.toRadians(-90), 0, 1, 0).translate(postTranslate);

        ROTATIONS[SOUTH_CONNECTED_BIT | EAST_CONNECTED_BIT].translate(preTranslate).rotate((float) Math.toRadians(-90), 1, 0, 0).rotate((float) Math.toRadians(-90), 0, 1, 0).translate(postTranslate);
        ROTATIONS[NORTH_CONNECTED_BIT | WEST_CONNECTED_BIT].translate(preTranslate).rotate((float) Math.toRadians(90), 1, 0, 0).rotate((float) Math.toRadians(90), 0, 1, 0).translate(postTranslate);
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(HeatExchangerChannelModels::registerAdditional);
        modBus.addListener(HeatExchangerChannelModels::modifyBakingResult);
    }

    private static void registerAdditional(ModelEvent.RegisterAdditional event) {
        event.register(CONNECTION_ERROR);
        event.register(CONDENSER_STRAIGHT);
        event.register(CONDENSER_CORNER);
        event.register(EVAPORATOR_STRAIGHT);
        event.register(EVAPORATOR_CORNER);
    }

    private static void modifyBakingResult(ModelEvent.ModifyBakingResult event) {
        final var models = event.getModels();
        replaceStates(models, HeatExchangerCondenserChannelBlock.INSTANCE, true);
        replaceStates(models, HeatExchangerEvaporatorChannelBlock.INSTANCE, false);
    }

    private static void replaceStates(Map<ModelResourceLocation, BakedModel> models, Block block, boolean condenser) {
        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            final var selection = select(state, condenser);
            final var base = models.get(selection.model);
            if (base == null) {
                continue;
            }
            final var stateLocation = BlockModelShaper.stateToModelLocation(state);
            final var original = models.get(stateLocation);
            models.put(stateLocation, new TransformedModel(original != null ? original : base, base, selection.rotation));
        }
    }

    private record Selection(ModelResourceLocation model, @Nullable Matrix4f rotation) {
    }

    private static Selection select(BlockState state, boolean condenser) {
        int connectedSides = 0;
        connectedSides += state.getValue(TOP_CONNECTED_PROPERTY) ? 1 : 0;
        connectedSides += state.getValue(BOTTOM_CONNECTED_PROPERTY) ? 1 : 0;
        connectedSides += state.getValue(NORTH_CONNECTED_PROPERTY) ? 1 : 0;
        connectedSides += state.getValue(SOUTH_CONNECTED_PROPERTY) ? 1 : 0;
        connectedSides += state.getValue(EAST_CONNECTED_PROPERTY) ? 1 : 0;
        connectedSides += state.getValue(WEST_CONNECTED_PROPERTY) ? 1 : 0;

        if (connectedSides > 2) {
            return new Selection(CONNECTION_ERROR, null);
        }
        int flags = 0;
        flags |= state.getValue(TOP_CONNECTED_PROPERTY) ? TOP_CONNECTED_BIT : 0;
        flags |= state.getValue(BOTTOM_CONNECTED_PROPERTY) ? BOTTOM_CONNECTED_BIT : 0;
        flags |= state.getValue(NORTH_CONNECTED_PROPERTY) ? NORTH_CONNECTED_BIT : 0;
        flags |= state.getValue(SOUTH_CONNECTED_PROPERTY) ? SOUTH_CONNECTED_BIT : 0;
        flags |= state.getValue(EAST_CONNECTED_PROPERTY) ? EAST_CONNECTED_BIT : 0;
        flags |= state.getValue(WEST_CONNECTED_PROPERTY) ? WEST_CONNECTED_BIT : 0;
        final boolean straight = connectedSides <= 1
                || flags == (TOP_CONNECTED_BIT | BOTTOM_CONNECTED_BIT)
                || flags == (NORTH_CONNECTED_BIT | SOUTH_CONNECTED_BIT)
                || flags == (EAST_CONNECTED_BIT | WEST_CONNECTED_BIT);
        final ModelResourceLocation model;
        if (straight) {
            model = condenser ? CONDENSER_STRAIGHT : EVAPORATOR_STRAIGHT;
        } else {
            model = condenser ? CONDENSER_CORNER : EVAPORATOR_CORNER;
        }
        return new Selection(model, ROTATIONS[flags]);
    }

    /**
     * Base model with every quad transformed once at bake time. Rotated quads lose their original cull face,
     * so all of them go to the unculled list; channels sit inside the structure, so this costs nothing visible.
     */
    private static final class TransformedModel extends BakedModelWrapper<BakedModel> {
        private final List<BakedQuad> quads;

        TransformedModel(BakedModel original, BakedModel base, @Nullable Matrix4f rotation) {
            // the original state model keeps providing particles and item/display properties
            super(original);
            final IQuadTransformer transformer = rotation == null ? null : QuadTransformers.applying(new Transformation(rotation));
            final var random = RandomSource.create(42);
            final var collected = new ArrayList<BakedQuad>();
            for (Direction side : Direction.values()) {
                random.setSeed(42);
                collected.addAll(base.getQuads(null, side, random, ModelData.EMPTY, null));
            }
            random.setSeed(42);
            collected.addAll(base.getQuads(null, null, random, ModelData.EMPTY, null));
            if (transformer != null) {
                collected.replaceAll(transformer::process);
            }
            this.quads = List.copyOf(collected);
            this.renderTypes = base.getRenderTypes(Blocks.AIR.defaultBlockState(), RandomSource.create(42), ModelData.EMPTY);
        }

        private final ChunkRenderTypeSet renderTypes;

        @Override
        public @NotNull ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data) {
            return renderTypes;
        }

        @Override
        public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData extraData, @Nullable RenderType renderType) {
            return side == null ? quads : List.of();
        }

        @Override
        public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand) {
            return side == null ? quads : List.of();
        }
    }
}
