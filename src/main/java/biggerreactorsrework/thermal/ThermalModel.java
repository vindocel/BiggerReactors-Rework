package biggerreactorsrework.thermal;

import java.util.ArrayList;
import java.util.List;

// Added 2026-11-02: shared, versioned fuel and heat accounting in kJ, independent of turbine FE units.
public final class ThermalModel {
    public static final int VERSION = 1;
    public static final double FUEL_QUANTUM = 1e-9;

    private ThermalModel() { }

    public record Preset(long fuelPerRod, long coolantPerManifold, double baseBurn,
                         double sizeBurn, double sizeConstant, double heatPerFuel,
                         double heatPerCoolant, double steamBudget, long exchangerRate) {
        public static final Preset DEFAULT = new Preset(4000, 10000, .04, .04, 100,
                12500, 1, .2, 2000);

        public Preset {
            if (fuelPerRod <= 0 || coolantPerManifold <= 0 || exchangerRate <= 0
                    || !positive(baseBurn) || !Double.isFinite(sizeBurn) || sizeBurn < 0
                    || !positive(sizeConstant) || !positive(heatPerFuel) || !positive(heatPerCoolant)
                    || !positive(steamBudget) || steamBudget > heatPerCoolant)
                throw new IllegalArgumentException("Invalid thermal preset");
        }

        public double burnPerRod(long rods) {
            return rods <= 0 ? 0 : baseBurn + sizeBurn * (rods / (rods + sizeConstant));
        }

        public double fill(double usableFuel, long rods) {
            return rods <= 0 ? 0 : fraction(usableFuel / ((double) fuelPerRod * rods));
        }

        public double requestedBurn(double usableFuel, long rods, double openRodBlocks) {
            double requested = burnPerRod(rods) * fill(usableFuel, rods) * Math.max(0, openRodBlocks);
            if (requested <= 0 || !Double.isFinite(requested)) return 0;
            // A defined precision also consumes the last sub-quantum residue; it never creates unpaid heat.
            return Math.min(Math.max(0, usableFuel), Math.max(FUEL_QUANTUM,
                    Math.rint(requested / FUEL_QUANTUM) * FUEL_QUANTUM));
        }
    }

    public enum PassiveClass {
        NONE(0, 0), BASIC(.25, .04), INTERMEDIATE(.5, .08), ADVANCED(.75, .16), SUPERIOR(1, .24);
        public final double coolingWeight;
        public final double rfPerKJ;
        PassiveClass(double weight, double rate) { coolingWeight = weight; rfPerKJ = rate; }
    }

    public enum Cell { EMPTY, ROD, MANIFOLD }
    @FunctionalInterface public interface Cells { Cell at(int x, int y, int z); }
    public record Column(int x, int z, int rods, double coveredRods, int isolatedRods) { }
    public record Geometry(long rods, long usefulManifolds, long volume, int contacts,
                           double contactEfficiency, double occupancy, double efficiency,
                           List<Column> columns) {
        public Geometry { columns = List.copyOf(columns); }
    }

    /** Bounds alone determine possible lateral positions, including internal obstacles. */
    public static Geometry geometry(int x, int y, int z, Cells cells) {
        if (x <= 0 || y <= 0 || z <= 0) return new Geometry(0, 0, 0, 0, 0, 0, 0, List.of());
        List<Column> columns = new ArrayList<>();
        long rods = 0, manifolds = 0;
        double coverage = 0;
        int contacts = 0;
        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int a = 0; a < x; a++) for (int c = 0; c < z; c++) {
            int columnRods = 0, isolated = 0;
            double columnCoverage = 0;
            for (int b = 0; b < y; b++) {
                Cell cell = cells.at(a, b, c);
                int possible = 0, connected = 0;
                boolean useful = false;
                for (int[] d : directions) {
                    int nx = a + d[0], nz = c + d[1];
                    if (nx < 0 || nx >= x || nz < 0 || nz >= z) continue;
                    possible++;
                    Cell neighbour = cells.at(nx, b, nz);
                    if (neighbour == Cell.MANIFOLD) connected++;
                    if (neighbour == Cell.ROD) useful = true;
                }
                if (cell == Cell.ROD) {
                    columnRods++;
                    if (connected == 0) isolated++;
                    columnCoverage += possible == 0 ? 0 : (double) connected / possible;
                    contacts += connected;
                } else if (cell == Cell.MANIFOLD && useful) manifolds++;
            }
            if (columnRods > 0) columns.add(new Column(a, c, columnRods, columnCoverage, isolated));
            rods += columnRods;
            coverage += columnCoverage;
        }
        long volume = (long) x * y * z;
        double mean = rods == 0 ? 0 : coverage / rods;
        double occupancy = (double) (rods + manifolds) / volume;
        return new Geometry(rods, manifolds, volume, contacts, mean, occupancy,
                rods == 0 ? 0 : Math.min(mean, occupancy), columns);
    }

    public record Heat(double generated, double isolated, double structural, double capturable) { }

    public static Heat heat(double generated, double locallyEligible, double isolated, double efficiency) {
        generated = nonNegative(generated);
        isolated = Math.min(generated, nonNegative(isolated));
        double capturable = Math.min(generated - isolated,
                Math.min(nonNegative(locallyEligible), generated * fraction(efficiency)));
        return new Heat(generated, isolated, Math.max(0, generated - isolated - capturable), capturable);
    }

    public record Conversion(long volume, double processed, double useful, double processLoss,
                             double missingCoolant, double blockedOutput, double rounding) { }

    public static Conversion convert(double heat, long cold, long outputSpace, double cost, double usefulPerUnit) {
        if (!positive(cost) || !Double.isFinite(usefulPerUnit) || usefulPerUnit < 0 || usefulPerUnit > cost)
            throw new IllegalArgumentException("Invalid thermal conversion");
        heat = nonNegative(heat);
        long ideal = wholeUnits(heat / cost);
        long supplied = Math.min(ideal, Math.max(0, cold));
        long converted = Math.min(supplied, Math.max(0, outputSpace));
        double processed = converted * cost;
        double useful = converted * usefulPerUnit;
        return new Conversion(converted, processed, useful, processed - useful,
                (ideal - supplied) * cost, (supplied - converted) * cost, Math.max(0, heat - ideal * cost));
    }

    /** Unique opposite-circuit links are cached on assembly; only air may separate faces. */
    public static double gapFactor(int airBlocks) {
        return switch (airBlocks) { case 0 -> 1; case 1 -> .5; case 2 -> .25; case 3 -> .1; default -> 0; };
    }

    public static long wholeUnits(double value) {
        return Double.isFinite(value) && value > 0 ? (long) Math.min(Long.MAX_VALUE, Math.floor(value)) : 0;
    }
    public static double fraction(double value) { return Double.isFinite(value) ? Math.max(0, Math.min(1, value)) : 0; }
    private static boolean positive(double value) { return Double.isFinite(value) && value > 0; }
    private static double nonNegative(double value) { return Double.isFinite(value) ? Math.max(0, value) : 0; }
}
