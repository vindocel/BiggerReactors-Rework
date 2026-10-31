package biggerreactorsrework.multiblocks.heatexchanger.client;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.multiblocks.heatexchanger.blocks.HeatExchangerCondenserChannelBlock;
import biggerreactorsrework.multiblocks.heatexchanger.blocks.HeatExchangerEvaporatorChannelBlock;
import biggerreactorsrework.client.guide.ModelRendering;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.DelegateBlockStateModel;
import net.neoforged.neoforge.client.model.quad.BakedNormals;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

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

    private static final Identifier CONNECTION_ERROR = model("block/heat_exchanger/casing/corner");
    private static final Identifier CONDENSER_STRAIGHT = model("block/heat_exchanger/hot_channel/connected_tb");
    private static final Identifier CONDENSER_CORNER = model("block/heat_exchanger/hot_channel/connected_bn");
    private static final Identifier EVAPORATOR_STRAIGHT = model("block/heat_exchanger/cold_channel/connected_tb");
    private static final Identifier EVAPORATOR_CORNER = model("block/heat_exchanger/cold_channel/connected_bn");

    private static Identifier model(String path) {
        return Identifier.fromNamespaceAndPath(BiggerReactors.modid, path);
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
        modBus.addListener(HeatExchangerChannelModels::registerStandalone);
        modBus.addListener(HeatExchangerChannelModels::modifyBakingResult);
    }

    private static void registerStandalone(ModelEvent.RegisterStandalone event) {
        ModelRendering.register(event, CONNECTION_ERROR);
        ModelRendering.register(event, CONDENSER_STRAIGHT);
        ModelRendering.register(event, CONDENSER_CORNER);
        ModelRendering.register(event, EVAPORATOR_STRAIGHT);
        ModelRendering.register(event, EVAPORATOR_CORNER);
    }

    private static void modifyBakingResult(ModelEvent.ModifyBakingResult event) {
        final var models = event.getBakingResult().blockStateModels();
        replaceStates(models, HeatExchangerCondenserChannelBlock.INSTANCE, true);
        replaceStates(models, HeatExchangerEvaporatorChannelBlock.INSTANCE, false);
    }

    private static void replaceStates(Map<BlockState, BlockStateModel> models, Block block, boolean condenser) {
        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            final var original = models.get(state);
            if (original != null) {
                models.put(state, new TransformedModel(original, select(state, condenser)));
            }
        }
    }

    private record Selection(Identifier model, @Nullable Matrix4f rotation) {
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
        final Identifier model;
        if (straight) {
            model = condenser ? CONDENSER_STRAIGHT : EVAPORATOR_STRAIGHT;
        } else {
            model = condenser ? CONDENSER_CORNER : EVAPORATOR_CORNER;
        }
        return new Selection(model, ROTATIONS[flags]);
    }

    /**
     * Base model with every quad transformed once on first use. Rotated quads lose their original cull face,
     * so all of them go to the unculled list; channels sit inside the structure, so this costs nothing visible.
     */
    private static final class TransformedModel extends DelegateBlockStateModel {
        private final Selection selection;
        private volatile BlockStateModelPart baseUsed;
        private volatile Transformed part;

        TransformedModel(BlockStateModel original, Selection selection) {
            // the original state model keeps providing particles and item/display properties
            super(original);
            this.selection = selection;
        }

        private Transformed part() {
            var base = ModelRendering.model(selection.model);
            if (part == null || baseUsed != base) {
                part = new Transformed(transform(base, selection.rotation), base);
                baseUsed = base;
            }
            return part;
        }

        @Override
        public void collectParts(RandomSource random, List<BlockStateModelPart> parts) {
            parts.add(part());
        }

        @Override
        public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> parts) {
            parts.add(part());
        }
    }

    private static List<BakedQuad> transform(BlockStateModelPart base, @Nullable Matrix4f rotation) {
        final var collected = new ArrayList<BakedQuad>();
        for (Direction side : Direction.values()) {
            collected.addAll(base.getQuads(side));
        }
        collected.addAll(base.getQuads(null));
        if (rotation != null) {
            collected.replaceAll(quad -> new BakedQuad(rotation.transformPosition(new Vector3f(quad.position(0))),
                    rotation.transformPosition(new Vector3f(quad.position(1))), rotation.transformPosition(new Vector3f(quad.position(2))),
                    rotation.transformPosition(new Vector3f(quad.position(3))), quad.packedUV(0), quad.packedUV(1), quad.packedUV(2), quad.packedUV(3),
                    Direction.rotate(rotation, quad.direction()), quad.materialInfo(), BakedNormals.UNSPECIFIED, quad.bakedColors()));
        }
        return List.copyOf(collected);
    }

    private record Transformed(List<BakedQuad> quads, BlockStateModelPart base) implements BlockStateModelPart {
        @Override
        public List<BakedQuad> getQuads(@Nullable Direction side) {
            return side == null ? quads : List.of();
        }

        @Override
        public boolean useAmbientOcclusion() {
            return base.useAmbientOcclusion();
        }

        @Override
        public Material.Baked particleMaterial() {
            return base.particleMaterial();
        }

        @Override
        public int materialFlags() {
            return base.materialFlags();
        }
    }
}
