package biggerreactorsrework.compat.cctweaked;

// Added 2026-10-31: native CC:Tweaked callbacks; all live state stays on the server thread.
import biggerreactorsrework.multiblocks.heatexchanger.tiles.HeatExchangerComputerPortTile;
import biggerreactorsrework.compat.computer.access.HeatExchangerAccess;
import dan200.computercraft.api.lua.*;

public final class HeatExchangerPeripheral extends ComputerPeripheral {
    private final HeatExchangerAccess access;
    public HeatExchangerPeripheral(HeatExchangerComputerPortTile tile) {
        super(tile, "biggerreactorsrework_heat_exchanger");
        access = HeatExchangerAccess.create(tile::nullableController);
    }
    @LuaFunction(mainThread = true)
    public Object[] connected(IArguments args) throws LuaException {
        try {
        checkThread(); return result(available() && access.connected());
        } catch (IllegalArgumentException | IllegalStateException error) { throw new LuaException(error.getMessage()); }
    }

    @LuaFunction(mainThread = true)
    public Object[] condenser(IArguments args) throws LuaException {
        try {
        checkAvailable(); var c = access.condenser(); return result(table("temperature", c.temperature(), "rfPerKelvin", c.rfPerKelvin(), "transitionedLastTick", c.transitionedLastTick(), "maxTransitionedLastTick", c.maxTransitionedLastTick(), "transitionEnergy", c.transitionEnergy(), "input", table("name", c.input().name(), "amount", c.input().amount(), "maxAmount", c.input().maxAmount()), "output", table("name", c.output().name(), "amount", c.output().amount(), "maxAmount", c.output().maxAmount())));
        } catch (IllegalArgumentException | IllegalStateException error) { throw new LuaException(error.getMessage()); }
    }

    @LuaFunction(mainThread = true)
    public Object[] dumpCondenser(IArguments args) throws LuaException {
        try {
        checkAvailable(); access.condenser().dump(); tile.setChanged(); return result(true);
        } catch (IllegalArgumentException | IllegalStateException error) { throw new LuaException(error.getMessage()); }
    }

    @LuaFunction(mainThread = true)
    public Object[] evaporator(IArguments args) throws LuaException {
        try {
        checkAvailable(); var c = access.evaporator(); return result(table("temperature", c.temperature(), "rfPerKelvin", c.rfPerKelvin(), "transitionedLastTick", c.transitionedLastTick(), "maxTransitionedLastTick", c.maxTransitionedLastTick(), "transitionEnergy", c.transitionEnergy(), "input", table("name", c.input().name(), "amount", c.input().amount(), "maxAmount", c.input().maxAmount()), "output", table("name", c.output().name(), "amount", c.output().amount(), "maxAmount", c.output().maxAmount())));
        } catch (IllegalArgumentException | IllegalStateException error) { throw new LuaException(error.getMessage()); }
    }

    @LuaFunction(mainThread = true)
    public Object[] dumpEvaporator(IArguments args) throws LuaException {
        try {
        checkAvailable(); access.evaporator().dump(); tile.setChanged(); return result(true);
        } catch (IllegalArgumentException | IllegalStateException error) { throw new LuaException(error.getMessage()); }
    }

    @LuaFunction(mainThread = true)
    public Object[] internalEnvironment(IArguments args) throws LuaException {
        try {
        checkAvailable(); var e = access.internalEnvironment(); return result(table("temperature", e.temperature(), "rfPerKelvin", e.rfPerKelvin()));
        } catch (IllegalArgumentException | IllegalStateException error) { throw new LuaException(error.getMessage()); }
    }

    @LuaFunction(mainThread = true)
    public Object[] ambientTemperature(IArguments args) throws LuaException {
        try {
        checkAvailable(); return result(access.ambientTemperature());
        } catch (IllegalArgumentException | IllegalStateException error) { throw new LuaException(error.getMessage()); }
    }

    @LuaFunction(mainThread = true)
    public Object[] ambientInternalRFKT(IArguments args) throws LuaException {
        try {
        checkAvailable(); return result(access.ambientInternalRFKT());
        } catch (IllegalArgumentException | IllegalStateException error) { throw new LuaException(error.getMessage()); }
    }

    @LuaFunction(mainThread = true)
    public Object[] condenserInternalRFKT(IArguments args) throws LuaException {
        try {
        checkAvailable(); return result(access.condenserInternalRFKT());
        } catch (IllegalArgumentException | IllegalStateException error) { throw new LuaException(error.getMessage()); }
    }

    @LuaFunction(mainThread = true)
    public Object[] evaporatorInternalRFKT(IArguments args) throws LuaException {
        try {
        checkAvailable(); return result(access.evaporatorInternalRFKT());
        } catch (IllegalArgumentException | IllegalStateException error) { throw new LuaException(error.getMessage()); }
    }

    @LuaFunction(mainThread = true)
    public Object[] condenserEvaporatorRFKT(IArguments args) throws LuaException {
        try {
        checkAvailable(); return result(access.condenserEvaporatorRFKT());
        } catch (IllegalArgumentException | IllegalStateException error) { throw new LuaException(error.getMessage()); }
    }
}
