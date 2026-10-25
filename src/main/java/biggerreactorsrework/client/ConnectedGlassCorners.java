package biggerreactorsrework.client;

/** Missing diagonal corners of a face. */
public final class ConnectedGlassCorners {
    private ConnectedGlassCorners() {}

    /** Edges: left, right, top, bottom. Diagonals/result: TL, TR, BL, BR. */
    public static int mask(int edges, int diagonals) {
        int result = 0;
        if ((edges & 5) == 5 && (diagonals & 1) == 0) result |= 1;
        if ((edges & 6) == 6 && (diagonals & 2) == 0) result |= 2;
        if ((edges & 9) == 9 && (diagonals & 4) == 0) result |= 4;
        if ((edges & 10) == 10 && (diagonals & 8) == 0) result |= 8;
        return result;
    }
}
