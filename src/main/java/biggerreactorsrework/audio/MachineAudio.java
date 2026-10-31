package biggerreactorsrework.audio;

// Added 2026-10-28: synchronize only terminal snapshots, once per machine state change.
import biggerreactorsrework.core.CoreEvents;
import biggerreactorsrework.core.modular.tile.ModularTile;
import biggerreactorsrework.multiblocks.reactor.tiles.ReactorTerminalTile;
import biggerreactorsrework.multiblocks.turbine.tiles.TurbineTerminalTile;
import biggerreactorsrework.multiblocks.heatexchanger.tiles.HeatExchangerTerminalTile;
import net.minecraft.core.BlockPos;
import org.joml.Vector3ic;
import java.util.Collection;
import java.util.function.Consumer;

public final class MachineAudio {
    private MachineAudio() {}
    public static Consumer<ModularTile> clientUpdate = tile -> {};

    public static boolean supported(ModularTile tile) {
        return tile instanceof ReactorTerminalTile || tile instanceof TurbineTerminalTile || tile instanceof HeatExchangerTerminalTile;
    }

    public static void publish(int kind, Vector3ic min, Vector3ic max,
                               Collection<? extends ModularTile> terminals, boolean running, double rpm) {
        double speed = Double.isFinite(rpm) && rpm >= 0 ? rpm : 0;
        var state = new MachineAudioState(kind, new BlockPos(min.x(), min.y(), min.z()),
                new BlockPos(max.x(), max.y(), max.z()), running, speed);
        for (var tile : terminals) {
            var old = tile.machineAudio;
            if (old.equals(state)) continue;
            if (old.kind() == kind && old.min().equals(state.min()) && old.max().equals(state.max())
                    && old.running() == running && CoreEvents.tickNumber() % 10 != 0) continue;
            tile.machineAudio = state;
            var level = tile.getLevel();
            if (level != null && !level.isClientSide() && !tile.isRemoved()) {
                level.sendBlockUpdated(tile.getBlockPos(), tile.getBlockState(), tile.getBlockState(), 2);
            }
        }
    }
}
