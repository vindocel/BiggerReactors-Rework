package biggerreactorsrework.client;

import biggerreactorsrework.BiggerReactors;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.fml.loading.FMLPaths;

/** Per-player themes, keyed by save/server, dimension and machine position. No network packets. */
public final class GuiThemes {
    private static GuiThemePreferences preferences;

    private GuiThemes() {}

    private static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve("biggerreactorsrework-themes.json");
    }

    private static GuiThemePreferences preferences() {
        if (preferences != null) return preferences;
        preferences = new GuiThemePreferences();
        if (Files.exists(path())) {
            try (var reader = Files.newBufferedReader(path(), StandardCharsets.UTF_8)) {
                preferences = GuiThemePreferences.read(reader);
            } catch (IOException | RuntimeException e) {
                BiggerReactors.LOGGER.warn("Could not read GUI themes; using the light default", e);
            }
        }
        return preferences;
    }

    public static boolean dark(BlockPos pos) { return preferences().dark(appearanceKey(pos)); }
    public static boolean applyToAll() { return preferences().applyToAll(); }
    // Modified 2026-10-28: every terminal of a multiblock shares its canonical position.
    public static int volume(BlockPos pos) { return preferences().volume(volumeKey(pos)); }
    public static void setVolume(BlockPos pos, int percent) {
        preferences().setVolume(volumeKey(pos), percent);
        save();
    }
    private static String volumeKey(BlockPos pos) {
        var level = Minecraft.getInstance().level;
        if (level != null && level.getBlockEntity(pos) instanceof biggerreactorsrework.core.modular.tile.ModularTile tile
                && tile.machineAudio.kind() >= 0) {
            return key(tile.machineAudio.min()) + "|audio:" + tile.machineAudio.kind();
        }
        return key(pos) + "|audio";
    }
    public static int machineVolume(biggerreactorsrework.audio.MachineAudioState state) {
        return preferences().volume(key(state.min()) + "|audio:" + state.kind());
    }

    public static void choose(BlockPos pos, boolean dark) {
        preferences().choose(appearanceKey(pos), dark);
        save();
    }

    public static void setApplyToAll(BlockPos pos, boolean value) {
        preferences().setApplyToAll(appearanceKey(pos), value);
        save();
    }

    // Modified 2026-10-30: every resource port follows its machine's synced appearance identity.
    private static String appearanceKey(BlockPos pos) {
        var level = Minecraft.getInstance().level;
        if (level != null && level.getBlockEntity(pos) instanceof biggerreactorsrework.multiblocks.reactor.tiles.ReactorBaseTile reactor)
            return key(reactor.appearancePos);
        if (level != null && level.getBlockEntity(pos) instanceof biggerreactorsrework.multiblocks.turbine.tiles.TurbineBaseTile turbine)
            return key(turbine.appearancePos);
        if (level != null && level.getBlockEntity(pos) instanceof biggerreactorsrework.multiblocks.heatexchanger.tiles.HeatExchangerBaseTile exchanger)
            return key(exchanger.appearancePos);
        if (level != null && level.getBlockEntity(pos) instanceof biggerreactorsrework.multiblocks.battery.tiles.BatteryBaseTile tile)
            return key(tile.batteryState().appearancePos);
        return key(pos);
    }

    private static String key(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        String world = "unknown";
        if (mc.getSingleplayerServer() != null) {
            world = "local:" + mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
        } else if (mc.getCurrentServer() != null) {
            world = "server:" + mc.getCurrentServer().ip;
        }
        String dimension = mc.level == null ? "unknown" : mc.level.dimension().location().toString();
        return world + "|" + dimension + "|" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    private static void save() {
        Path target = path();
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        try {
            Files.createDirectories(target.getParent());
            try (var writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                preferences().write(writer);
            }
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | RuntimeException e) {
            BiggerReactors.LOGGER.warn("Could not save GUI themes", e);
        }
    }
}
