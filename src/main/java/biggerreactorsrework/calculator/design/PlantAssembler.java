package biggerreactorsrework.calculator.design;

import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;
import java.util.List;

import static biggerreactorsrework.calculator.design.MachineLayout.Role.*;

/**
 * Places the machines of a route in a row along +X, ground aligned, and puts every connected pair of ports face to
 * face. Returns null when the faces cannot hold the ports (the search then discards the combination).
 */
public final class PlantAssembler {
    private PlantAssembler() { }

    public static PlantLayout solid(ReactorDesign reactor) {
        return new PlantLayout(List.of(new PlantLayout.Placed(reactor.layout(List.of()), BlockPos.ZERO)), List.of());
    }

    /** Reactor east face against the turbine west face: steam out to steam in, water out to water in. */
    @Nullable
    public static PlantLayout water(ReactorDesign reactor, TurbineDesign turbine) {
        int tz = (reactor.depth() - turbine.depth()) / 2;
        int zLo = Math.max(1, tz + 1), zHi = Math.min(reactor.depth() - 2, tz + turbine.depth() - 2);
        int yHi = Math.min(reactor.height(), turbine.height()) - 2;
        if (zHi < zLo || yHi < 1) return null;
        BlockPos steam, water;
        if (zHi > zLo) {
            steam = new BlockPos(0, 1, zLo);
            water = new BlockPos(0, 1, zLo + 1);
        } else if (yHi >= 2) {
            steam = new BlockPos(0, 1, zLo);
            water = new BlockPos(0, 2, zLo);
        } else return null;
        int rx = reactor.width() - 1, tx = reactor.width();
        var reactorLayout = reactor.layout(List.of(new MachineLayout.Port(steam.offset(rx, 0, 0), REACTOR_HOT_OUT),
                new MachineLayout.Port(water.offset(rx, 0, 0), REACTOR_COLD_IN)));
        var turbineLayout = turbine.layout(List.of(new MachineLayout.Port(steam.offset(0, 0, -tz), TURBINE_STEAM_IN),
                new MachineLayout.Port(water.offset(0, 0, -tz), TURBINE_WATER_OUT)));
        return new PlantLayout(List.of(new PlantLayout.Placed(reactorLayout, BlockPos.ZERO), new PlantLayout.Placed(turbineLayout, new BlockPos(tx, 0, tz))),
                List.of(new PlantLayout.Connection(0, REACTOR_HOT_OUT, 1, TURBINE_STEAM_IN), new PlantLayout.Connection(1, TURBINE_WATER_OUT, 0, REACTOR_COLD_IN)));
    }

    /** Reactor | exchanger | turbine. The exchanger fixes the port positions; the others must reach them. */
    @Nullable
    public static PlantLayout advanced(ReactorDesign reactor, ExchangerDesign exchanger, TurbineDesign turbine) {
        if (exchanger.portHeight() > reactor.height() - 2 || exchanger.portHeight() > turbine.height() - 2
                || exchanger.portDepth() > reactor.depth() - 2 || exchanger.portDepth() > turbine.depth() - 2) return null;
        int ez = (reactor.depth() - 2 - exchanger.portDepth()) / 2;
        int ex = reactor.width(), tx = ex + exchanger.width();
        int tz = ez + 1 - (turbine.depth() - 2 - exchanger.portDepth()) / 2;
        int rx = reactor.width() - 1;
        if (ez + 1 < 1 || ez + 1 > reactor.depth() - 2) return null;
        var reactorLayout = reactor.layout(List.of(
                new MachineLayout.Port(new BlockPos(rx, exchanger.hotIn().getY(), ez + exchanger.hotIn().getZ()), REACTOR_HOT_OUT),
                new MachineLayout.Port(new BlockPos(rx, exchanger.coldOut().getY(), ez + exchanger.coldOut().getZ()), REACTOR_COLD_IN)));
        var turbineLayout = turbine.layout(List.of(
                new MachineLayout.Port(new BlockPos(0, exchanger.steamOut().getY(), ez + exchanger.steamOut().getZ() - tz), TURBINE_STEAM_IN),
                new MachineLayout.Port(new BlockPos(0, exchanger.waterIn().getY(), ez + exchanger.waterIn().getZ() - tz), TURBINE_WATER_OUT)));
        return new PlantLayout(List.of(new PlantLayout.Placed(reactorLayout, BlockPos.ZERO),
                new PlantLayout.Placed(exchanger.layout(), new BlockPos(ex, 0, ez)),
                new PlantLayout.Placed(turbineLayout, new BlockPos(tx, 0, tz))),
                List.of(new PlantLayout.Connection(0, REACTOR_HOT_OUT, 1, EXCHANGER_HOT_IN),
                        new PlantLayout.Connection(1, EXCHANGER_COLD_OUT, 0, REACTOR_COLD_IN),
                        new PlantLayout.Connection(1, EXCHANGER_STEAM_OUT, 2, TURBINE_STEAM_IN),
                        new PlantLayout.Connection(2, TURBINE_WATER_OUT, 1, EXCHANGER_WATER_IN)));
    }

    /** True when every connection joins two ports one block apart on opposite faces. */
    public static boolean connected(PlantLayout plant) {
        for (var c : plant.connections()) {
            var a = plant.portPosition(c.from(), c.fromRole());
            var b = plant.portPosition(c.to(), c.toRole());
            if (a.distManhattan(b) != 1) return false;
        }
        return true;
    }
}
