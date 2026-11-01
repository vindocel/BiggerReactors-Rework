package biggerreactorsrework.client;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.multiblocks.battery.blocks.BatteryCell;
import biggerreactorsrework.multiblocks.battery.blocks.BatteryGlass;
import biggerreactorsrework.multiblocks.heatexchanger.blocks.HeatExchangerGlassBlock;
import biggerreactorsrework.multiblocks.reactor.blocks.ReactorGlass;
import biggerreactorsrework.multiblocks.turbine.blocks.TurbineGlass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.DelegateBlockStateModel;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Completes concave glass borders using the existing straight border sprites, and the battery cells' concave corners
 * using their own corner sprites.
 * Diagonals are read while the chunk is meshed, avoiding extra persistent block states or per-frame rendering.
 */
public final class ConnectedGlassModels {
    private ConnectedGlassModels() {}

    public static void init(IEventBus bus) {
        bus.addListener(ConnectedGlassModels::bake);
    }

    private static void bake(ModelEvent.ModifyBakingResult event) {
        wrap(event, ReactorGlass.INSTANCE, "reactor/glass/connected_", false);
        wrap(event, TurbineGlass.INSTANCE, "turbine/glass/connected_", false);
        wrap(event, HeatExchangerGlassBlock.INSTANCE, "heat_exchanger/glass/glass_connected_", false);
        wrap(event, BatteryGlass.INSTANCE, "battery/glass/connected_", false);
        wrap(event, BatteryCell.INSTANCE, "battery/cell/body/corner_", true);
    }

    private static void wrap(ModelEvent.ModifyBakingResult event, Block block, String prefix, boolean overlays) {
        var models = event.getBakingResult().blockStateModels();
        var patches = new Patches(prefix, overlays);
        for (var state : block.getStateDefinition().getPossibleStates()) {
            var model = models.get(state);
            if (model != null) models.put(state, new GlassModel(model, block, patches));
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

    private static final class GlassModel extends DelegateBlockStateModel {
        private final Block glass;
        private final Patches patches;

        GlassModel(BlockStateModel original, Block glass, Patches patches) {
            super(original);
            this.glass = glass;
            this.patches = patches;
        }

        @Override
        public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> parts) {
            int start = parts.size();
            super.collectParts(level, pos, state, random, parts);
            if (parts.size() - start != 1) return;
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
            if (corners != 0) parts.set(start, new Patched(parts.get(start), patches, corners));
        }
    }

    private record Patched(BlockStateModelPart original, Patches patches, int corners) implements BlockStateModelPart {
        @Override
        public List<BakedQuad> getQuads(@Nullable Direction side) {
            var base = original.getQuads(side);
            if (side == null || base.isEmpty()) return base;
            int mask = (corners >>> (side.ordinal() * 4)) & 15;
            if (mask == 0) return base;
            var result = new ArrayList<>(base);
            result.addAll(patches.quads(side, mask, base.getFirst()));
            return result;
        }

        @Override
        public boolean useAmbientOcclusion() {
            return original.useAmbientOcclusion();
        }

        @Override
        public Material.Baked particleMaterial() {
            return original.particleMaterial();
        }

        @Override
        public int materialFlags() {
            return original.materialFlags();
        }
    }

    private static final class Patches {
        private final String prefix;
        private final boolean overlays;

        Patches(String prefix, boolean overlays) {
            this.prefix = prefix;
            this.overlays = overlays;
        }

        private TextureAtlasSprite sprite(String suffix) {
            return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS)
                    .getSprite(Identifier.fromNamespaceAndPath(BiggerReactors.modid, "block/" + prefix + suffix));
        }

        List<BakedQuad> quads(Direction face, int mask, BakedQuad base) {
            var quads = new ArrayList<BakedQuad>();
            for (int corner = 0; corner < 4; corner++) {
                if ((mask & (1 << corner)) == 0) continue;
                if (overlays) {
                    quads.add(quad(face, 0, 0, 16, 16, sprite(new String[]{"tl", "tr", "bl", "br"}[corner]), base));
                    continue;
                }
                boolean right = (corner & 1) != 0;
                boolean bottom = (corner & 2) != 0;
                float u = right ? 13 : 0, v = bottom ? 13 : 0;
                float stripeU = right ? 13 : 2, stripeV = bottom ? 13 : 2;
                float outerU = right ? 14 : 0, outerV = bottom ? 14 : 0;
                var horizontal = sprite(bottom ? "tlr" : "blr");
                var vertical = sprite(right ? "tbl" : "tbr");
                // Three disjoint rectangles form a 3x3 inward corner. The inner pixel row/column
                // keeps the hazard stripe continuous; the outer 2x2 area continues the metal rim.
                quads.add(quad(face, u, stripeV, u + 3, stripeV + 1, horizontal, base));
                quads.add(quad(face, outerU, outerV, outerU + 2, outerV + 2, horizontal, base));
                quads.add(quad(face, stripeU, outerV, stripeU + 1, outerV + 2, vertical, base));
            }
            return quads;
        }

        private static BakedQuad quad(Direction face, float u0, float v0, float u1, float v1, TextureAtlasSprite sprite, BakedQuad base) {
            var info = base.materialInfo();
            var material = new BakedQuad.MaterialInfo(sprite, net.minecraft.client.renderer.chunk.ChunkSectionLayer.byTransparency(sprite.contents().transparency()),
                    info.itemRenderType(), info.tintIndex(), info.shade(), info.lightEmission(), info.ambientOcclusion());
            return new BakedQuad(point(face, u0, v0), point(face, u0, v1), point(face, u1, v1), point(face, u1, v0),
                    uv(sprite, u0, v0), uv(sprite, u0, v1), uv(sprite, u1, v1), uv(sprite, u1, v0),
                    face, material, base.bakedNormals(), base.bakedColors());
        }

        private static long uv(TextureAtlasSprite sprite, float u, float v) {
            return UVPair.pack(sprite.getU(u / 16), sprite.getV(v / 16));
        }

        private static Vector3f point(Direction face, float u, float v) {
            var point = switch (face) {
                case NORTH -> new Vector3f(16 - u, 16 - v, 0);
                case SOUTH -> new Vector3f(u, 16 - v, 16);
                case EAST -> new Vector3f(16, 16 - v, 16 - u);
                case WEST -> new Vector3f(0, 16 - v, u);
                case UP -> new Vector3f(u, 16, v);
                case DOWN -> new Vector3f(u, 0, 16 - v);
            };
            return point.div(16);
        }
    }
}
