package biggerreactorsrework.client.guide;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Draws the guide from the book model's parts: standing upright with the cover to the front, and opening like the
 * model's animation when the local player uses it (see {@link GuideBookAnimation}). Every part pivots on the spine.
 */
public class GuideBookRenderer implements SpecialModelRenderer<GuideBookAnimation.Pose> {
    public static final Identifier ID = GuideIds.id("guide_book");
    public static final Identifier BACK_COVER = part("back_cover");
    public static final Identifier FRONT_COVER = part("front_cover");
    public static final Identifier LEAF_1 = part("leaf_1");
    public static final Identifier LEAF_2 = part("leaf_2");
    public static final Identifier SPINE = part("spine");
    /** Half the width of the closed book, in pixels: the closed book is centered, the open one on its spine. */
    private static final float CLOSED_HALF_WIDTH = 4.1F;

    private static Identifier part(String name) {
        return GuideIds.id("item/guide_book/" + name);
    }

    public static List<Identifier> models() {
        return List.of(BACK_COVER, FRONT_COVER, LEAF_1, LEAF_2, SPINE);
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked<GuideBookAnimation.Pose> {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

        @Override
        public SpecialModelRenderer<GuideBookAnimation.Pose> bake(SpecialModelRenderer.BakingContext context) {
            return new GuideBookRenderer();
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }

    @Override
    public GuideBookAnimation.Pose extractArgument(ItemStack stack) {
        return heldByLocalPlayer(stack) ? GuideBookAnimation.pose() : GuideBookAnimation.Pose.CLOSED;
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        for (int corner = 0; corner < 8; corner++) {
            output.accept(new Vector3f(corner & 1, (corner >> 1) & 1, (corner >> 2) & 1));
        }
    }

    @Override
    public void submit(GuideBookAnimation.Pose p, PoseStack pose, SubmitNodeCollector buffers, int light, int overlay, boolean hasFoil, int outlineColor) {
        pose.pushPose();
        // The parts are modeled lying on the table with the cover up; stand the book up with the cover to the front.
        pose.translate(0.5, 0.5, 0.5);
        pose.rotate(Axis.XP.rotationDegrees(90));
        pose.translate(-CLOSED_HALF_WIDTH / 16.0 * (1.0F - p.openness()), 0, 0);
        pose.translate(-0.5, -0.5, -0.5);
        part(pose, buffers, BACK_COVER, 0, 0, light, overlay);
        part(pose, buffers, FRONT_COVER, p.cover(), 0, light, overlay);
        part(pose, buffers, LEAF_1, p.leaf1(), 0, light, overlay);
        part(pose, buffers, LEAF_2, p.leaf2(), 0, light, overlay);
        part(pose, buffers, SPINE, p.spine(), p.spineDrop(), light, overlay);
        pose.popPose();
    }

    /** A part turned around the spine (the block center) by {@code angle} degrees and moved down by {@code drop} pixels. */
    private static void part(PoseStack pose, SubmitNodeCollector buffers, Identifier model, float angle, float drop, int light, int overlay) {
        pose.pushPose();
        pose.translate(0.5, 0.5 + drop / 16.0, 0.5);
        pose.rotate(Axis.ZP.rotationDegrees(angle));
        pose.translate(-0.5, -0.5, -0.5);
        ModelRendering.render(pose, buffers, ModelRendering.model(model), light, overlay);
        pose.popPose();
    }

    /** Only the book in the local player's own hand opens. */
    private static boolean heldByLocalPlayer(ItemStack stack) {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && (stack == player.getMainHandItem() || stack == player.getOffhandItem());
    }
}
