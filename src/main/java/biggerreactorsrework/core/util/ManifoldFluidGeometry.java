package biggerreactorsrework.core.util;

// Added 2026-10-28: contained coolant sits just behind the manifold's outer grille.
public final class ManifoldFluidGeometry {
    // Match the fuel volume's 0.02 model-pixel clearance, rather than an inset inner tube.
    private static final float INSET = .02f / 16;
    private ManifoldFluidGeometry() {}
    public static FluidSurfaceMesh.Box box(float bottom, float top) {
        return new FluidSurfaceMesh.Box(INSET, bottom, INSET, 1 - INSET, top, 1 - INSET);
    }
}
