package biggerreactorsrework.calculator.search;

import biggerreactorsrework.Config;

/**
 * Structure sizes follow server configuration. Work is bounded by evaluations and time, not fixed machine sizes.
 */
public final class SearchDomain {
    /** Reactor height as a multiple of the base side. */
    static final double[] REACTOR_ASPECTS = {1, 2, 0.5};
    /** Plant evaluations and wall time per calculation before the search stops with what it has. */
    static final int MAX_EVALUATIONS = 1500;
    static final long MAX_MILLIS = 120_000;

    private SearchDomain() { }

    static int reactorSide() {
        var c = Config.CONFIG.Reactor;
        return Math.max(0, Math.min(c.MaxLength, c.MaxWidth) - 2);
    }

    static int reactorHeight() {
        return Math.max(0, Config.CONFIG.Reactor.MaxHeight - 2);
    }

    static int turbineArm() {
        var c = Config.CONFIG.Turbine;
        return Math.max(0, (Math.min(c.MaxLength, c.MaxWidth) - 3) / 2);
    }

    static int turbineShaft() {
        return Math.max(0, Config.CONFIG.Turbine.MaxHeight - 2);
    }

    static int exchangerLength() {
        return Math.max(0, Config.CONFIG.HeatExchanger.MaxLength - 2);
    }

    static int exchangerRows() {
        int rows = Math.max(0, (Config.CONFIG.HeatExchanger.MaxHeight - 1) / 2);
        return rows - rows % 2;
    }

    static int exchangerLayers() {
        return Math.max(0, (Config.CONFIG.HeatExchanger.MaxWidth - 1) / 3);
    }
}
