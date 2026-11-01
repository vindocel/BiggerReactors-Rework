package biggerreactorsrework.compat.iris;

/** Added 2026-10-31: the affected Iris custom program owns its sampler bindings outside RenderPearl. */
public final class IrisCompositeUniforms {
    private IrisCompositeUniforms() {}
    private static final ClassValue<java.util.Optional<java.lang.reflect.Method>> CUSTOM_PASS = new ClassValue<>() {
        @Override protected java.util.Optional<java.lang.reflect.Method> computeValue(Class<?> type) {
            try { return java.util.Optional.of(type.getMethod("iris$getCustomPass")); }
            catch (NoSuchMethodException e) { return java.util.Optional.empty(); }
        }
    };

    public static boolean hasCustomPass(Object pass) {
        var method = CUSTOM_PASS.get(pass.getClass());
        if (method.isEmpty()) return false;
        try { return method.get().invoke(pass) != null; }
        catch (ReflectiveOperationException e) { return false; }
    }

    public static boolean usesCustomSampler(String pipeline, boolean customPass, String irisVersion) {
        return customPass && "iris:composite".equals(pipeline)
                && ("1.11.7".equals(irisVersion) || irisVersion.startsWith("1.11.7+"));
    }
}
