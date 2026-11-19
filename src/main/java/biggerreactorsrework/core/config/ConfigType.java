package biggerreactorsrework.core.config;


import net.neoforged.fml.loading.FMLEnvironment;

public enum ConfigType {
    NULL(false),
    CLIENT(isFMLClient()),
    COMMON(true),
    SERVER(true);

    public final boolean appliesToPhysicalSide;

    ConfigType(boolean appliesToPhysicalSide) {
        this.appliesToPhysicalSide = appliesToPhysicalSide;
    }

    private static boolean isFMLClient() {
//? if =26.1.2 || =26.3 {
        try {
            return FMLEnvironment.getDist().isClient();
            // in case its loaded without FML present, treat it as client
        } catch (NoClassDefFoundError e) {
//?} else if =1.21.1 {
/*        try {
            return FMLEnvironment.dist == null || FMLEnvironment.dist.isClient();
            // in case its loaded without FML present, treat it as client
        } catch (NoClassDefFoundError e) {
*/
//?}
            return true;
        }
    }

    public ConfigType from(ConfigType type) {
        return switch (this) {
            case NULL -> type;
            case CLIENT -> CLIENT;
            case COMMON -> COMMON;
            case SERVER -> SERVER;
        };
    }
}
