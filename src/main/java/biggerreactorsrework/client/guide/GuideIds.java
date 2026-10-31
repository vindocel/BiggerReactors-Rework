package biggerreactorsrework.client.guide;

import biggerreactorsrework.BiggerReactors;
import net.minecraft.resources.Identifier;

final class GuideIds {
    static Identifier id(String path) { return Identifier.fromNamespaceAndPath(BiggerReactors.modid, path); }
    private GuideIds() { }
}
