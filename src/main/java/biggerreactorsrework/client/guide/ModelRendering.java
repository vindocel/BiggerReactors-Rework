package biggerreactorsrework.client.guide;

//? if =26.1.2 || =26.3 {
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

/** Helpers to draw standalone models (loaded through ModelEvent.RegisterStandalone) from renderers. */
public final class ModelRendering {
    private static final Map<Identifier, StandaloneModelKey<BlockStateModelPart>> KEYS = new ConcurrentHashMap<>();

    private ModelRendering() {
    }

    public static StandaloneModelKey<BlockStateModelPart> standalone(Identifier id) {
        return KEYS.computeIfAbsent(id, key -> new StandaloneModelKey<>((ModelDebugName) key::toString));
    }

    public static void register(ModelEvent.RegisterStandalone event, Identifier id) {
        event.register(standalone(id), SimpleUnbakedStandaloneModel.simpleModelWrapper(id));
    }

    public static BlockStateModelPart model(Identifier id) {
        return Minecraft.getInstance().getModelManager().getStandaloneModel(standalone(id));
    }

    public static void render(PoseStack pose, SubmitNodeCollector collector, BlockStateModelPart model, int light, int overlay) {
        collector.submitMultiLayerBlockModel(pose, List.of(model), false, BlockModelRenderState.EMPTY_TINTS, light, overlay, 0);
    }
//?} else if =1.21.1 {
/*import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.data.ModelData;

/^* Helpers to draw standalone baked models (loaded through ModelEvent.RegisterAdditional) from renderers. ^/
public final class ModelRendering {
    private ModelRendering() {
    }

    public static ModelResourceLocation standalone(ResourceLocation id) {
        return ModelResourceLocation.standalone(id);
    }

    public static BakedModel model(ResourceLocation id) {
        return Minecraft.getInstance().getModelManager().getModel(standalone(id));
    }

    public static void render(PoseStack pose, MultiBufferSource buffers, BakedModel model, int light, int overlay) {
        RenderType type = RenderType.cutout();
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(pose.last(), buffers.getBuffer(type), null,
                model, 1.0F, 1.0F, 1.0F, light, overlay, ModelData.EMPTY, type);
    }
*/
//?}

    /** Blockstate-style Y rotation (clockwise seen from above) of a model whose native front is north. */
    public static float blockstateY(Direction facing) {
        return switch (facing) {
            case EAST -> 90;
            case SOUTH -> 180;
            case WEST -> 270;
            default -> 0;
        };
    }
}
