package biggerreactorsrework.multiblocks.heatexchanger.client;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.multiblocks.heatexchanger.blocks.HeatExchangerCondenserChannelBlock;
//? if =26.1.2 || =26.3 {
import biggerreactorsrework.multiblocks.heatexchanger.blocks.HeatExchangerEvaporatorChannelBlock;
import biggerreactorsrework.client.guide.ModelRendering;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.cuboid.FaceBakery;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
//?} else if =1.21.1 {
/*import biggerreactorsrework.multiblocks.heatexchanger.blocks.HeatExchangerEvaporatorChannelBlock;
import com.mojang.math.Transformation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.FaceInfo;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
*/
//?}
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
//? if =26.1.2 || =26.3 {
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.DelegateBlockStateModel;
import net.neoforged.neoforge.client.model.quad.BakedNormals;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
//?} else if =1.21.1 {
/*import net.neoforged.bus.api.IEventBus;
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
*/
//?}
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
//? if =26.1.2 || =26.3 {
    }

    private static final Identifier CONNECTION_ERROR = model("block/heat_exchanger/casing/corner");
    private static final Identifier CONDENSER_STRAIGHT = model("block/heat_exchanger/hot_channel/connected_tb");
    private static final Identifier CONDENSER_CORNER = model("block/heat_exchanger/hot_channel/connected_bn");
    private static final Identifier EVAPORATOR_STRAIGHT = model("block/heat_exchanger/cold_channel/connected_tb");
    private static final Identifier EVAPORATOR_CORNER = model("block/heat_exchanger/cold_channel/connected_bn");

    private static Identifier model(String path) {
        return Identifier.fromNamespaceAndPath(BiggerReactors.modid, path);
    }
//?} else if =1.21.1 {
/*    }

    private static final ModelResourceLocation CONNECTION_ERROR = model("block/heat_exchanger/casing/corner");
    private static final ModelResourceLocation CONDENSER_STRAIGHT = model("block/heat_exchanger/hot_channel/connected_tb");
    private static final ModelResourceLocation CONDENSER_CORNER = model("block/heat_exchanger/hot_channel/connected_bn");
    private static final ModelResourceLocation EVAPORATOR_STRAIGHT = model("block/heat_exchanger/cold_channel/connected_tb");
    private static final ModelResourceLocation EVAPORATOR_CORNER = model("block/heat_exchanger/cold_channel/connected_bn");

    private static ModelResourceLocation model(String path) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(BiggerReactors.modid, path));
    }
*/
//?}

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

//? if =26.1.2 || =26.3 {
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
//?} else if =1.21.1 {
/*    public static void init(IEventBus modBus) {
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
*/
//?}

//? if =26.1.2 || =26.3 {
    private static void modifyBakingResult(ModelEvent.ModifyBakingResult event) {
        final var models = event.getBakingResult().blockStateModels();
        replaceStates(models, HeatExchangerCondenserChannelBlock.INSTANCE, true);
//?} else if =1.21.1 {
/*    private static void modifyBakingResult(ModelEvent.ModifyBakingResult event) {
        final var models = event.getModels();
        replaceStates(models, HeatExchangerCondenserChannelBlock.INSTANCE, true);
*/
//?}
        replaceStates(models, HeatExchangerEvaporatorChannelBlock.INSTANCE, false);
//? if =26.1.2 || =26.3 {
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
//?} else if =1.21.1 {
/*    }

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
*/
//?}

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
//? if =26.1.2 || =26.3 {
                || flags == (EAST_CONNECTED_BIT | WEST_CONNECTED_BIT);
        final Identifier model;
        if (straight) {
//?} else if =1.21.1 {
/*                || flags == (EAST_CONNECTED_BIT | WEST_CONNECTED_BIT);
        final ModelResourceLocation model;
        if (straight) {
*/
//?}
            model = condenser ? CONDENSER_STRAIGHT : EVAPORATOR_STRAIGHT;
        } else {
            model = condenser ? CONDENSER_CORNER : EVAPORATOR_CORNER;
        }
        return new Selection(model, ROTATIONS[flags]);
//? if =26.1.2 || =26.3 {
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
//?} else if =1.21.1 {
/*    }

    /^*
     * Base model with every quad transformed once at bake time. Rotated quads lose their original cull face,
     * so all of them go to the unculled list; channels sit inside the structure, so this costs nothing visible.
     ^/
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
                collected.replaceAll(quad -> canonical(transformer.process(quad)));
            }
            this.quads = List.copyOf(collected);
            this.renderTypes = base.getRenderTypes(Blocks.AIR.defaultBlockState(), RandomSource.create(42), ModelData.EMPTY);
        }

        private final ChunkRenderTypeSet renderTypes;

        // Smooth lighting reads corners by vertex index, so rotated quads take the facing and vertex order of a baked face.
        private static BakedQuad canonical(BakedQuad quad) {
            final int[] source = quad.getVertices();
            final int stride = source.length / 4;
            final int[] min = {FaceInfo.Constants.MIN_X, FaceInfo.Constants.MIN_Y, FaceInfo.Constants.MIN_Z};
            final int[] max = {FaceInfo.Constants.MAX_X, FaceInfo.Constants.MAX_Y, FaceInfo.Constants.MAX_Z};
            final float[] bounds = new float[6];
            for (int axis = 0; axis < 3; axis++) {
                bounds[min[axis]] = 999;
                bounds[max[axis]] = -999;
            }
            for (int i = 0; i < 4; i++) {
                for (int axis = 0; axis < 3; axis++) {
                    final float value = snap(Float.intBitsToFloat(source[i * stride + axis]));
                    source[i * stride + axis] = Float.floatToRawIntBits(value);
                    bounds[min[axis]] = Math.min(bounds[min[axis]], value);
                    bounds[max[axis]] = Math.max(bounds[max[axis]], value);
                }
            }
            final Direction direction = FaceBakery.calculateFacing(source);
            final int[] vertices = source.clone();
            final FaceInfo info = FaceInfo.fromFacing(direction);
            for (int i = 0; i < 4; i++) {
                final FaceInfo.VertexInfo corner = info.getVertexInfo(i);
                final float x = bounds[corner.xFace], y = bounds[corner.yFace], z = bounds[corner.zFace];
                for (int k = 0; k < 4; k++) {
                    if (Float.intBitsToFloat(source[k * stride]) == x && Float.intBitsToFloat(source[k * stride + 1]) == y
                            && Float.intBitsToFloat(source[k * stride + 2]) == z) {
                        System.arraycopy(source, k * stride, vertices, i * stride, stride);
                        break;
                    }
                }
            }
            return new BakedQuad(vertices, quad.getTintIndex(), direction, quad.getSprite(), quad.isShade(), quad.hasAmbientOcclusion());
        }

        private static float snap(float value) {
            final float pixel = Math.round(value * 16) / 16f;
            return Math.abs(pixel - value) < 1.0E-4F ? pixel : value;
        }

        @Override
        public @NotNull ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data) {
            return renderTypes;
        }
*/
//?}

//? if =26.1.2 || =26.3 {
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
            collected.replaceAll(quad -> rotate(quad, rotation));
        }
        return List.copyOf(collected);
    }

    /**
     * Smooth lighting reads corner light by vertex index and face normal, so a rotated quad needs the same
     * vertex order and normals the model baker produces for an unrotated face in that direction.
     */
    private static BakedQuad rotate(BakedQuad quad, Matrix4f rotation) {
        final var positions = new Vector3fc[4];
        final var uvs = new long[4];
        for (int i = 0; i < 4; i++) {
            final var position = rotation.transformPosition(new Vector3f(quad.position(i)));
            positions[i] = position.set(snap(position.x), snap(position.y), snap(position.z));
            uvs[i] = quad.packedUV(i);
        }
        final var direction = Direction.rotate(rotation, quad.direction());
        try {
            FaceBakery.recalculateWinding(positions, uvs, direction);
        } catch (IllegalStateException ignored) {
        }
        return new BakedQuad(positions[0], positions[1], positions[2], positions[3], uvs[0], uvs[1], uvs[2], uvs[3], direction,
                quad.materialInfo(), BakedNormals.of(BakedNormals.computeQuadNormal(positions[0], positions[1], positions[2], positions[3])),
                quad.bakedColors());
    }

    private static float snap(float value) {
        final float pixel = Math.round(value * 16) / 16f;
        return Math.abs(pixel - value) < 1.0E-4F ? pixel : value;
    }

    private record Transformed(List<BakedQuad> quads, BlockStateModelPart base) implements BlockStateModelPart {
        @Override
        public List<BakedQuad> getQuads(@Nullable Direction side) {
            return side == null ? quads : List.of();
//?} else if =1.21.1 {
/*        @Override
        public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData extraData, @Nullable RenderType renderType) {
            return side == null ? quads : List.of();
*/
//?}
        }

//? if =26.1.2 || =26.3 {
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
//?} else if =1.21.1 {
/*        @Override
        public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand) {
            return side == null ? quads : List.of();
        }
*/
//?}
    }
}
