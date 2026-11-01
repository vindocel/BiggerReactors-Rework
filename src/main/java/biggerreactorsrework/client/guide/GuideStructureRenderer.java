package biggerreactorsrework.client.guide;

// Modified 2026-11-01: native Minecraft 26.1.2 API.
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import org.jspecify.annotations.Nullable;

/** Draws the guide's 3D view of a structure into its own picture on the page. */
final class GuideStructureRenderer extends PictureInPictureRenderer<GuideStructureRenderer.State> {
    GuideStructureRenderer(net.minecraft.client.renderer.MultiBufferSource.BufferSource buffers) {
        super(buffers);
    }
    record State(GuideMultiblock.Preview preview, int layer, @Nullable String highlight, boolean glass, float yaw, float pitch,
                 int x0, int y0, int x1, int y1, float scale, @Nullable ScreenRectangle scissorArea,
                 @Nullable ScreenRectangle bounds) implements PictureInPictureRenderState {
        State(GuideMultiblock.Preview preview, int layer, @Nullable String highlight, boolean glass, float yaw, float pitch,
              int x0, int y0, int x1, int y1, float scale, @Nullable ScreenRectangle scissorArea) {
            this(preview, layer, highlight, glass, yaw, pitch, x0, y0, x1, y1, scale, scissorArea,
                    PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea));
        }
    }

    @Override
    public Class<State> getRenderStateClass() {
        return State.class;
    }

    @Override
    protected void renderToTexture(State state, PoseStack pose) {
        // Modified 2026-11-01: submit and flush within the native 26.1.2 picture render target.
        var dispatcher = Minecraft.getInstance().gameRenderer.getFeatureRenderDispatcher();
        var collector = dispatcher.getSubmitNodeStorage();
        Minecraft.getInstance().gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);
        var preview = state.preview();
        pose.mulPose(Axis.XP.rotationDegrees(180));
        pose.mulPose(Axis.XP.rotationDegrees(state.pitch()));
        pose.mulPose(Axis.YP.rotationDegrees(state.yaw()));
        pose.translate(-preview.width() / 2F, -preview.height() / 2F, -preview.depth() / 2F);
        RandomSource random = RandomSource.create(42L);
        for (var box : preview.boxes()) {
            if (state.layer() >= 0 && (state.layer() < box.y() || state.layer() >= box.y() + box.h())) {
                continue;
            }
            int by = state.layer() < 0 ? box.y() : state.layer();
            int bh = state.layer() < 0 ? box.h() : 1;
            BlockState block = GuideMultiblock.WALLS.equals(box.group()) && preview.glass() != null && state.glass()
                    ? preview.glass().defaultBlockState() : box.block();
            BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(block);
            List<BlockStateModelPart> parts = new ArrayList<>();
            model.collectParts(random, parts);
            pose.pushPose();
            pose.translate(box.x(), by, box.z());
            pose.scale(box.w(), bh, box.d());
            collector.submitMultiLayerBlockModel(pose, parts, model.hasMaterialFlag(BakedQuad.FLAG_TRANSLUCENT),
                    BlockModelRenderState.EMPTY_TINTS, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
            // Coolant inside, as in a working reactor; the frame alone shows only its outer faces.
            if (block.getBlock() == biggerreactorsrework.multiblocks.reactor.blocks.ReactorManifold.INSTANCE) {
                biggerreactorsrework.client.FluidBoxRenderer.draw(pose, collector, List.of(new biggerreactorsrework.client.FluidBoxRenderer.Volume(
                        new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000),
                        biggerreactorsrework.core.util.ManifoldFluidGeometry.box(0, 1), 0)), LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            }
            pose.popPose();
        }
        dispatcher.renderAllFeatures();
        if (state.highlight() != null) {
            for (var box : preview.boxes()) {
                if (!box.group().equals(state.highlight()) || state.layer() >= 0 && (state.layer() < box.y() || state.layer() >= box.y() + box.h())) {
                    continue;
                }
                int by = state.layer() < 0 ? box.y() : state.layer();
                int bh = state.layer() < 0 ? box.h() : 1;
                net.minecraft.client.renderer.ShapeRenderer.renderShape(pose, bufferSource.getBuffer(RenderTypes.lines()),
                        Shapes.box(box.x() - 0.015, by - 0.015, box.z() - 0.015, box.x() + box.w() + 0.015, by + bh + 0.015, box.z() + box.d() + 0.015),
                        0, 0, 0, 0xFFFFD91A, 2F);
            }
        }
        bufferSource.endBatch();
    }

    @Override
    protected float getTranslateY(int height, int guiScale) {
        return height / 2.0F;
    }

    @Override
    protected String getTextureLabel() {
        return "biggerreactorsrework guide structure";
    }
}
