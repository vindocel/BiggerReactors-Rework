package biggerreactorsrework.compat.cctweaked;

// Added 2026-10-31: native CC:Tweaked callbacks; all live state stays on the server thread.
import biggerreactorsrework.multiblocks.turbine.tiles.TurbineComputerPortTile;
import biggerreactorsrework.compat.computer.access.TurbineAccess;
import dan200.computercraft.api.lua.*;

public final class TurbinePeripheral extends ComputerPeripheral {
    private final TurbineAccess access;
    public TurbinePeripheral(TurbineComputerPortTile tile) {
        super(tile, "biggerreactorsrework_turbine");
        access = TurbineAccess.create(tile::nullableController);
    }
    @LuaFunction(mainThread = true)
    public Object[] connected(IArguments args) throws LuaException {
        try {
        checkThread(); return result(available() && access.connected());
        } catch (IllegalArgumentException | IllegalStateException error) { throw new LuaException(error.getMessage()); }
    }

    @LuaFunction(mainThread = true)
    public Object[] active(IArguments args) throws LuaException {
        try {
        checkAvailable(); return result(access.active());
        } catch (IllegalArgumentException | IllegalStateException error) { throw new LuaException(error.getMessage()); }
    }

    @LuaFunction(mainThread = true)
    public Object[] setActive(IArguments args) throws LuaException {
        try {
        checkAvailable(); access.setActive(args.getBoolean(0)); tile.setChanged(); return result(true);
        } catch (IllegalArgumentException | IllegalStateException error) { throw new LuaException(error.getMessage()); }
    }

    @LuaFunction(mainThread = true)
    public Object[] coilEngaged(IArguments args) throws LuaException {
        try {
        checkAvailable(); return result(access.coilEngaged());
        } catch (IllegalArgumentException | IllegalStateException error) { throw new LuaException(error.getMessage()); }
    }

    @LuaFunction(mainThread = true)
    public Object[] setCoilEngaged(IArguments args) throws LuaException {
        try {
        checkAvailable(); access.setCoilEngaged(args.getBoolean(0)); tile.setChanged(); return result(true);
        } catch (IllegalArgumentException | IllegalStateException error) { throw new LuaException(error.getMessage()); }
    }

    @LuaFunction(mainThread = true)
    public Object[] battery(IArguments args) throws LuaException {
        try {
        checkAvailable(); var b = access.battery(); return result(table("stored", b.stored(), "capacity", b.capacity(), "producedLastTick", b.producedLastTick()));
        } catch (IllegalArgumentException | IllegalStateException error) { throw new LuaException(error.getMessage()); }
    }

    @LuaFunction(mainThread = true)
    public Object[] rotor(IArguments args) throws LuaException {
        try {
        checkAvailable(); var r = access.rotor(); return result(table("RPM", r.RPM(), "efficiencyLastTick", r.efficiencyLastTick()));
        } catch (IllegalArgumentException | IllegalStateException error) { throw new LuaException(error.getMessage()); }
    }

    @LuaFunction(mainThread = true)
    public Object[] fluidTank(IArguments args) throws LuaException {
        try {
        checkAvailable(); var t = access.fluidTank(); return result(table("input", table("name", t.input().name(), "amount", t.input().amount(), "maxAmount", t.input().maxAmount()), "output", table("name", t.output().name(), "amount", t.output().amount(), "maxAmount", t.output().maxAmount()), "flowLastTick", t.flowLastTick(), "nominalFlowRate", t.nominalFlowRate(), "flowRateLimit", t.flowRateLimit()));
        } catch (IllegalArgumentException | IllegalStateException error) { throw new LuaException(error.getMessage()); }
    }

    @LuaFunction(mainThread = true)
    public Object[] setNominalFlowRate(IArguments args) throws LuaException {
        try {
        checkAvailable(); double rate = args.getDouble(0); if (!Double.isFinite(rate) || rate != Math.rint(rate) || rate < 0 || rate > access.fluidTank().flowRateLimit()) throw new IllegalArgumentException("Flow rate must be an integer within the flow limit"); access.fluidTank().setNominalFlowRate((long) rate); tile.setChanged(); return result(true);
        } catch (IllegalArgumentException | IllegalStateException error) { throw new LuaException(error.getMessage()); }
    }

    @LuaFunction(mainThread = true)
    public Object[] ventState(IArguments args) throws LuaException {
        try {
        checkAvailable(); return result(access.ventState());
        } catch (IllegalArgumentException | IllegalStateException error) { throw new LuaException(error.getMessage()); }
    }

    @LuaFunction(mainThread = true)
    public Object[] setVentState(IArguments args) throws LuaException {
        try {
        checkAvailable(); access.setVentState(args.getString(0)); tile.setChanged(); return result(true);
        } catch (IllegalArgumentException | IllegalStateException error) { throw new LuaException(error.getMessage()); }
    }
}
