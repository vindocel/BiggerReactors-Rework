//? if =26.1.2 || =26.3 {
package biggerreactorsrework.client.guide;

import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;

public final class GuidePictures {
    private GuidePictures() {
    }

    public static void register(RegisterPictureInPictureRenderersEvent event) {
        event.register(GuideStructureRenderer.State.class, GuideStructureRenderer::new);
    }
}
//?} else if =1.21.1 {
//?}
