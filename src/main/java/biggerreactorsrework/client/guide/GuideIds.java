package biggerreactorsrework.client.guide;

import biggerreactorsrework.BiggerReactors;
import net.minecraft.resources.ResourceLocation;

final class GuideIds {
    static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(BiggerReactors.modid, path); }
    private GuideIds() { }
}
