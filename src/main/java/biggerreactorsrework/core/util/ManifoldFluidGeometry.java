package biggerreactorsrework.core.util;

// Added 2026-10-28: contained coolant sits just behind the manifold's outer grille.
public final class ManifoldFluidGeometry {
    // Modified 2026-11-03: half a model pixel keeps bright coolants from flickering against the grille at a distance.
    private static final float INSET = .5f / 16;
    private ManifoldFluidGeometry() {}
    public static FluidSurfaceMesh.Box box(float bottom, float top) {
        return new FluidSurfaceMesh.Box(INSET, bottom, INSET, 1 - INSET, top, 1 - INSET);
    }
}
