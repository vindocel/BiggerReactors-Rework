package biggerreactorsrework.compat.opencomputers;

// Added 2026-10-30: component lifetime, persistence and server-thread boundary.
import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.compat.ComputerPortState;
import li.cil.oc.api.Network;
import li.cil.oc.api.machine.*;
import li.cil.oc.api.network.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class PortComponent implements Environment, ComputerPortState.Component {
    protected final BlockEntity tile;
    private final Node node;
    protected PortComponent(BlockEntity tile, String name) {
        this.tile = tile;
        node = Network.newNode(this, Visibility.Network).withComponent(name, Visibility.Network).create();
    }
    @Override public final Node node() { return node; }
    @Override public final void join() { Network.joinOrCreateNetwork(tile); tile.setChanged(); }
    @Override public final void remove() { node.remove(); }
    @Override public final void onConnect(Node other) { if (other == node) tile.setChanged(); }
    @Override public final void onDisconnect(Node other) {}
    @Override public final void onMessage(Message message) {}
    @Override public final void load(CompoundTag tag, HolderLookup.Provider registries) {
        try { node.loadData(tag, registries); }
        catch (li.cil.oc.api.UnrecoverablePersistanceException error) {
            org.apache.logging.log4j.LogManager.getLogger("BiggerReactors-Rework/OpenComputers")
                    .warn("Cannot restore computer component address at {}; a new address will be assigned", tile.getBlockPos(), error);
        }
    }
    @Override public final void save(CompoundTag tag, HolderLookup.Provider registries) { node.saveData(tag, registries); }
    protected final boolean available() {
        var level = tile.getLevel();
        return level != null && !level.isClientSide && ((ComputerPortState.Port) tile).computerPortState().loaded()
                && level.hasChunkAt(tile.getBlockPos()) && level.getBlockEntity(tile.getBlockPos()) == tile;
    }
    protected final void checkThread() {
        var server = tile.getLevel() == null ? null : tile.getLevel().getServer();
        if (server == null || !server.isSameThread()) throw new IllegalStateException("Computer callbacks require the server thread");
    }
    protected final void checkAvailable() {
        checkThread();
        if (!available()) throw new IllegalStateException("Computer port is unloaded or removed");
    }
    protected static Map<String, Object> table(Object... pairs) {
        var result = new LinkedHashMap<String, Object>();
        for (int i = 0; i < pairs.length; i += 2) result.put((String) pairs[i], pairs[i + 1]);
        return result;
    }
    protected static Object[] result(Object value) { return new Object[]{value}; }
    protected static double percentage(Arguments args, int index) {
        double value = args.checkDouble(index);
        if (!Double.isFinite(value) || value < 0 || value > 100) throw new IllegalArgumentException("Expected a finite percentage from 0 to 100");
        return value;
    }
    protected static int rodIndex(Arguments args) {
        double value = args.checkDouble(0);
        if (!Double.isFinite(value) || value < 0 || value > Integer.MAX_VALUE || value != Math.rint(value))
            throw new IllegalArgumentException("Expected a nonnegative integer rod index");
        return (int) value;
    }
    @Callback(doc = "function():number -- Component API schema version (1).")
    public final Object[] apiVersion(Context context, Arguments args) { return result(1); }
    @Callback(doc = "function():string -- Installed mod version.")
    public final Object[] modVersion(Context context, Arguments args) { return result(BiggerReactors.modVersion()); }
}
