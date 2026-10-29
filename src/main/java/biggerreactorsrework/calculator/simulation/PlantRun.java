package biggerreactorsrework.calculator.simulation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

/**
 * Runs a virtual plant from a cold start until it is stable, then samples it. A window is a whole number of reactor
 * time-slice cycles, so every rod level is computed equally often inside it. The plant counts as stable when the
 * last {@code STABLE_WINDOWS} windows agree on power and fuel within the tolerance and no tank keeps filling or
 * draining by more than the stock tolerance of the circuit's charge.
 */
public final class PlantRun {
    public record Settings(int minWindow, long maxTicks, double tolerance, double stockTolerance, int sampleWindows) {
        /** Quick comparison between candidates. */
        public static final Settings SCREEN = new Settings(2000, 120_000, 0.01, 0.01, 1);
        /** Final check of the recommended plant. */
        public static final Settings VALIDATE = new Settings(4000, 1_200_000, 0.002, 0.002, 4);
    }

    public record Result(boolean converged, long ticks, int window, double power, double powerMin, double powerMax, double fuel,
                         double steam, double hotFlow, double rpm, double fuelTemperature, double reactorLimit, double exchangerLimit,
                         double drift, long primaryError, long waterError, VirtualPlant.Stocks stocks) {
        public double efficiency() { return fuel <= 0 ? 0 : power / fuel; }
    }

    private static final int STABLE_WINDOWS = 3;

    private PlantRun() { }

    public static Result run(PlantSpec spec, boolean reverseOrder, Settings settings, BooleanSupplier cancelled) {
        var plant = new VirtualPlant(spec, reverseOrder);
        int cycle = Math.max(1, spec.reactor().columns() * spec.reactor().y());
        int window = Math.max(cycle, (settings.minWindow() + cycle - 1) / cycle * cycle);
        var power = new ArrayList<Double>();
        var fuel = new ArrayList<Double>();
        var stocks = new ArrayList<VirtualPlant.Stocks>();
        long primaryScale = Math.max(1, spec.primaryCharge()), waterScale = Math.max(1, spec.waterCharge());
        long ticks = 0;
        long primaryError = 0, waterError = 0;
        while (ticks < settings.maxTicks()) {
            double p = 0, f = 0;
            for (int t = 0; t < window; t++) {
                if ((ticks & 1023) == 0 && cancelled.getAsBoolean()) throw new CancellationException();
                plant.tick();
                p += plant.exportedLastTick;
                f += plant.burnedLastTick;
                ticks++;
            }
            power.add(p / window);
            fuel.add(f / window);
            var s = plant.stocks();
            stocks.add(s);
            primaryError = Math.max(primaryError, Math.abs(s.primary(spec.advanced()) - plant.primaryPoured()) * (spec.solid() ? 0 : 1));
            if (spec.advanced()) waterError = Math.max(waterError, Math.abs(s.secondary() - plant.waterPoured()));
            int n = power.size();
            if (!plant.charged() || n < STABLE_WINDOWS) continue;
            if (!agree(power, n, settings.tolerance()) || !agree(fuel, n, settings.tolerance())) continue;
            if (!steadyStocks(stocks, n, settings.stockTolerance(), primaryScale, waterScale, spec.advanced())) continue;
            return sample(plant, settings, window, ticks, cancelled, primaryError, waterError, spec);
        }
        int n = power.size();
        return new Result(false, ticks, window, n == 0 ? 0 : power.get(n - 1), 0, 0, n == 0 ? 0 : fuel.get(n - 1), plant.turbineFlow(),
                plant.reactorConversion(), plant.rpm(), plant.fuelTemperature(), plant.reactorOutputLimit(), plant.exchangerOutputLimit(),
                1, primaryError, waterError, plant.stocks());
    }

    private static Result sample(VirtualPlant plant, Settings settings, int window, long ticks, BooleanSupplier cancelled,
                                 long primaryError, long waterError, PlantSpec spec) {
        List<Double> power = new ArrayList<>();
        double fuel = 0, steam = 0, hot = 0, rpm = 0, temperature = 0, reactorLimit = 0, exchangerLimit = 0;
        long samples = (long) window * settings.sampleWindows();
        for (int w = 0; w < settings.sampleWindows(); w++) {
            double p = 0;
            for (int t = 0; t < window; t++) {
                if ((ticks & 1023) == 0 && cancelled.getAsBoolean()) throw new CancellationException();
                plant.tick();
                ticks++;
                p += plant.exportedLastTick;
                fuel += plant.burnedLastTick;
                steam += spec.advanced() ? plant.exchangerSteam() : plant.turbineFlow();
                hot += plant.reactorConversion();
                rpm += plant.rpm();
                temperature += plant.fuelTemperature();
                reactorLimit += plant.reactorOutputLimit();
                exchangerLimit += plant.exchangerOutputLimit();
            }
            power.add(p / window);
            var s = plant.stocks();
            primaryError = Math.max(primaryError, Math.abs(s.primary(spec.advanced()) - plant.primaryPoured()) * (spec.solid() ? 0 : 1));
            if (spec.advanced()) waterError = Math.max(waterError, Math.abs(s.secondary() - plant.waterPoured()));
        }
        double mean = power.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double min = power.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double max = power.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        double drift = mean <= 0 ? 0 : Math.abs(power.getLast() - power.getFirst()) / mean;
        return new Result(true, ticks, window, mean, min, max, fuel / samples, steam / samples, hot / samples, rpm / samples,
                temperature / samples, reactorLimit / samples, exchangerLimit / samples, drift, primaryError, waterError, plant.stocks());
    }

    private static boolean agree(List<Double> values, int n, double tolerance) {
        double lo = Double.MAX_VALUE, hi = -Double.MAX_VALUE;
        for (int i = n - STABLE_WINDOWS; i < n; i++) {
            lo = Math.min(lo, values.get(i));
            hi = Math.max(hi, values.get(i));
        }
        return hi - lo <= tolerance * Math.max(Math.abs(hi), 1e-9);
    }

    private static boolean steadyStocks(List<VirtualPlant.Stocks> stocks, int n, double tolerance, long primary, long water, boolean advanced) {
        var a = stocks.get(n - STABLE_WINDOWS);
        var b = stocks.get(n - 1);
        double p = tolerance * primary, w = tolerance * water, t = advanced ? w : p;
        return close(a.reactorCold(), b.reactorCold(), p) && close(a.reactorHot(), b.reactorHot(), p)
                && close(a.exchangerHot(), b.exchangerHot(), p) && close(a.exchangerCold(), b.exchangerCold(), p)
                && close(a.exchangerWater(), b.exchangerWater(), w) && close(a.exchangerSteam(), b.exchangerSteam(), w)
                && close(a.turbineSteam(), b.turbineSteam(), t) && close(a.turbineWater(), b.turbineWater(), t);
    }

    private static boolean close(long a, long b, double limit) {
        return Math.abs(a - b) <= limit;
    }
}
