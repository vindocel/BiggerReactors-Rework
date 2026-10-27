package biggerreactorsrework.multiblocks.reactor.simulation.base;

/** Conserves sub-mB heat instead of losing it on every tick. Modified 2026-10-25. */
public final class PhaseChange {
    private PhaseChange() { }
    public record Result(long units, double accepted, double remainder) { }
    public static Result accept(double energy, double latent, long available, double remainder) {
        if (!Double.isFinite(energy) || energy <= 0 || !Double.isFinite(latent) || latent <= 0 || available <= 0)
            return new Result(0,0,remainder);
        remainder = Double.isFinite(remainder)?Math.max(0,Math.min(Math.nextDown(latent),remainder)):0;
        double total=energy+remainder;
        long units=Math.min(available,(long)(total/latent));
        // Modified 2026-10-27: no fractional extra unit beyond the available batch/output space.
        double rest=units==available?0:Math.min(Math.nextDown(latent),Math.max(0,total-units*latent));
        return new Result(units,Math.max(0,units*latent+rest-remainder),rest);
    }
}
