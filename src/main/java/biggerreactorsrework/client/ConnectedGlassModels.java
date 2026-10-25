package biggerreactorsrework.client;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.multiblocks.battery.blocks.BatteryCell;
import biggerreactorsrework.multiblocks.battery.blocks.BatteryGlass;
import biggerreactorsrework.multiblocks.heatexchanger.blocks.HeatExchangerGlassBlock;
import biggerreactorsrework.multiblocks.reactor.blocks.ReactorGlass;
import biggerreactorsrework.multiblocks.turbine.blocks.TurbineGlass;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Completes concave glass borders using the existing straight border sprites, and the battery cells' concave corners
 * using their own corner sprites.
 * Diagonals are chunk model data, avoiding extra persistent block states or per-frame rendering.
 */
public final class ConnectedGlassModels {
    private static final ModelProperty<Integer> CORNERS = new ModelProperty<>();

    private ConnectedGlassModels() {}

    public static void init(IEventBus bus) {
        bus.addListener(ConnectedGlassModels::bake);
    }

    private static void bake(ModelEvent.ModifyBakingResult event) {
        wrap(event, ReactorGlass.INSTANCE, "reactor/glass/connected_");
        wrap(event, TurbineGlass.INSTANCE, "turbine/glass/connected_");
        wrap(event, HeatExchangerGlassBlock.INSTANCE, "heat_exchanger/glass/glass_connected_");
        wrap(event, BatteryGlass.INSTANCE, "battery/glass/connected_");
        wrapCorners(event, BatteryCell.INSTANCE, "battery/cell/body/corner_");
    }

    private static TextureAtlasSprite sprite(ModelEvent.ModifyBakingResult event, String path) {
        return event.getTextureGetter().apply(new Material(TextureAtlas.LOCATION_BLOCKS,
                ResourceLocation.fromNamespaceAndPath(BiggerReactors.modid, "block/" + path)));
    }

    private static void wrap(ModelEvent.ModifyBakingResult event, Block block, String prefix) {
        // These sprites already belong to the ordinary glass models and are stitched by vanilla.
        var sprites = new TextureAtlasSprite[4];
        String[] suffixes = {"tbr", "tbl", "blr", "tlr"}; // left, right, top, bottom borders
        for (int i = 0; i < 4; i++) {
            sprites[i] = sprite(event, prefix + suffixes[i]);
        }
        wrap(event, block, Patches.borders(sprites));
    }

    /**
     * Blocks that draw each concave corner as a full-face overlay sprite (TL, TR, BL, BR); the block atlas stitches
     * every texture under block/, so the sprites need no model of their own.
     */
    private static void wrapCorners(ModelEvent.ModifyBakingResult event, Block block, String prefix) {
        var sprites = new TextureAtlasSprite[4];
        String[] suffixes = {"tl", "tr", "bl", "br"};
        for (int i = 0; i < 4; i++) {
            sprites[i] = sprite(event, prefix + suffixes[i]);
        }
        wrap(event, block, Patches.overlays(sprites));
    }

    private static void wrap(ModelEvent.ModifyBakingResult event, Block block, Patches patches) {
        for (var state : block.getStateDefinition().getPossibleStates()) {
            var location = BlockModelShaper.stateToModelLocation(state);
            var model = event.getModels().get(location);
            if (model != null) event.getModels().put(location, new GlassModel(model, block, patches));
        }
    }

    // Directions in the native cube face's UV coordinate system; both sides of a window are supported.
    static Direction right(Direction face) {
        return switch (face) {
            case NORTH -> Direction.WEST;
            case EAST -> Direction.NORTH;
            case WEST -> Direction.SOUTH;
            default -> Direction.EAST;
        };
    }

    static Direction down(Direction face) {
        return switch (face) {
            case UP -> Direction.SOUTH;
            case DOWN -> Direction.NORTH;
            default -> Direction.DOWN;
        };
    }

    private static final class GlassModel extends BakedModelWrapper<BakedModel> {
        private final Block glass;
        private final Patches patches;

        GlassModel(BakedModel original, Block glass, Patches patches) {
            super(original);
            this.glass = glass;
            this.patches = patches;
        }

        @Override
        public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData data) {
            int corners = 0;
            for (var face : Direction.values()) {
                if (level.getBlockState(pos.relative(face)).is(glass)) continue;
                var right = right(face);
                var down = down(face);
                Direction[] edges = {right.getOpposite(), right, down.getOpposite(), down};
                int connected = 0;
                for (int i = 0; i < 4; i++) {
                    if (level.getBlockState(pos.relative(edges[i])).is(glass)) connected |= 1 << i;
                }
                int diagonals = 0;
                for (int i = 0; i < 4; i++) {
                    if (level.getBlockState(pos.relative(edges[i & 1]).relative(edges[2 + (i >> 1)])).is(glass)) {
                        diagonals |= 1 << i;
                    }
                }
                corners |= ConnectedGlassCorners.mask(connected, diagonals) << (face.ordinal() * 4);
            }
            return super.getModelData(level, pos, state, data).derive().with(CORNERS, corners).build();
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random,
                                        ModelData data, @Nullable RenderType renderType) {
            var base = super.getQuads(state, side, random, data, renderType);
            Integer corners = data.get(CORNERS);
            if (side == null || corners == null || (renderType != null && renderType != RenderType.cutout())) return base;
            int mask = (corners >>> (side.ordinal() * 4)) & 15;
            if (mask == 0) return base;
            var result = new ArrayList<>(base);
            result.addAll(patches.faces.get(side.ordinal()).get(mask));
            return result;
        }
    }

    private static final class Patches {
        final List<List<List<BakedQuad>>> faces = new ArrayList<>();

        static Patches borders(TextureAtlasSprite[] sprites) {
            var patches = new Patches();
            for (var face : Direction.values()) {
                var corners = new ArrayList<List<BakedQuad>>();
                for (int corner = 0; corner < 4; corner++) {
                    boolean right = (corner & 1) != 0;
                    boolean bottom = (corner & 2) != 0;
                    float u = right ? 13 : 0, v = bottom ? 13 : 0;
                    float stripeU = right ? 13 : 2, stripeV = bottom ? 13 : 2;
                    float outerU = right ? 14 : 0, outerV = bottom ? 14 : 0;
                    var horizontal = sprites[bottom ? 3 : 2];
                    var vertical = sprites[right ? 1 : 0];
                    // Three disjoint rectangles form a 3x3 inward corner. The inner pixel row/column
                    // keeps the hazard stripe continuous; the outer 2x2 area continues the metal rim.
                    corners.add(List.of(quad(face, u, stripeV, u + 3, stripeV + 1, horizontal),
                            quad(face, outerU, outerV, outerU + 2, outerV + 2, horizontal),
                            quad(face, stripeU, outerV, stripeU + 1, outerV + 2, vertical)));
                }
                patches.add(corners);
            }
            return patches;
        }

        static Patches overlays(TextureAtlasSprite[] sprites) {
            var patches = new Patches();
            for (var face : Direction.values()) {
                var corners = new ArrayList<List<BakedQuad>>();
                for (var sprite : sprites) {
                    corners.add(List.of(quad(face, 0, 0, 16, 16, sprite)));
                }
                patches.add(corners);
            }
            return patches;
        }

        private void add(List<List<BakedQuad>> corners) {
            var masks = new ArrayList<List<BakedQuad>>();
            for (int mask = 0; mask < 16; mask++) {
                var quads = new ArrayList<BakedQuad>();
                for (int i = 0; i < 4; i++) if ((mask & (1 << i)) != 0) quads.addAll(corners.get(i));
                masks.add(List.copyOf(quads));
            }
            faces.add(List.copyOf(masks));
        }

        private static BakedQuad quad(Direction face, float u0, float v0, float u1, float v1, TextureAtlasSprite sprite) {
            var a = point(face, u0, v0);
            var b = point(face, u1, v1);
            var min = new Vector3f(a).min(b);
            var max = new Vector3f(a).max(b);
            return new FaceBakery().bakeQuad(min, max,
                    new BlockElementFace(face, -1, "", new BlockFaceUV(new float[]{u0, v0, u1, v1}, 0)),
                    sprite, face, BlockModelRotation.X0_Y0, null, true);
        }

        private static Vector3f point(Direction face, float u, float v) {
            return switch (face) {
                case NORTH -> new Vector3f(16 - u, 16 - v, 0);
                case SOUTH -> new Vector3f(u, 16 - v, 16);
                case EAST -> new Vector3f(16, 16 - v, 16 - u);
                case WEST -> new Vector3f(0, 16 - v, u);
                case UP -> new Vector3f(u, 16, v);
                case DOWN -> new Vector3f(u, 0, 16 - v);
            };
        }
    }
}
