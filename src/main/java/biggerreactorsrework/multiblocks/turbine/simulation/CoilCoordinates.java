package biggerreactorsrework.multiblocks.turbine.simulation;

// Added 2026-11-02: exact radial coordinates; subtract world positions before projecting onto the shaft axes.
public final class CoilCoordinates {
    public record Radial(int first,int second) { }
    public static Radial relative(int x,int y,int z,int bx,int by,int bz,int shaftAxis) {
        int dx=x-bx,dy=y-by,dz=z-bz;
        return switch(shaftAxis) {
            case 0 -> new Radial(dy,dz);
            case 1 -> new Radial(dx,dz);
            case 2 -> new Radial(dx,dy);
            default -> throw new IllegalArgumentException("Invalid shaft axis");
        };
    }
    private CoilCoordinates() { }
}
