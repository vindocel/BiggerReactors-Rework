package biggerreactorsrework.client.guide;

//? if =26.1.2 || =26.3 {
import biggerreactorsrework.BiggerReactors;
import net.minecraft.resources.Identifier;

final class GuideIds {
    static Identifier id(String path) { return Identifier.fromNamespaceAndPath(BiggerReactors.modid, path); }
    private GuideIds() { }
//?} else if =1.21.1 {
/*import biggerreactorsrework.BiggerReactors;
import net.minecraft.resources.ResourceLocation;

final class GuideIds {
    static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(BiggerReactors.modid, path); }
    private GuideIds() { }
*/
//?}
}
