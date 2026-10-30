package biggerreactorsrework.client.calculator;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.calculator.CalculationResult;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.*;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/**
 * Recent and saved calculations of the current player in the current world or server, kept between restarts in
 * {@code config/biggerreactorsrework/calculator/<context>.dat}. Recents keep the three newest finished results;
 * saving is explicit, independent of them and capped at {@link #SAVED_LIMIT} without ever dropping a saved entry.
 * A snapshot is kept while either list refers to it. Entries this version cannot read are written back unchanged,
 * and a file from a newer version is never overwritten.
 */
public final class CalculatorLibrary {
    // Modified 2026-10-30: only acknowledge saved entries after an atomic disk write succeeds.
    public static final int RECENT_LIMIT = 3, SAVED_LIMIT = 50;
    private static final int SCHEMA = 1;
    private static final long MAX_FILE_BYTES = 64L << 20;

    private static final Map<String, CalculatorLibrary> LIBRARIES = new HashMap<>();

    private final Path file;
    private final Deque<String> recents = new ArrayDeque<>();
    private final List<String> saved = new ArrayList<>();
    private final Map<String, CompoundTag> stored = new HashMap<>();
    private final Map<String, CalculationResult> parsed = new HashMap<>();
    private boolean readOnly;
    private boolean writeFailed;
    public boolean writeFailed() { return writeFailed; }

    private CalculatorLibrary(Path file) {
        this.file = file;
        load();
    }

    public static CalculatorLibrary get() {
        String context = context();
        return LIBRARIES.computeIfAbsent(context, k -> new CalculatorLibrary(directory().resolve(k + ".dat")));
    }

    private static Path directory() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve(BiggerReactors.modid).resolve("calculator");
    }

    /** Player and world (local save folder) or server (address), hashed: never a path taken from the server. */
    static String context() {
        var mc = Minecraft.getInstance();
        String place;
        if (mc.getSingleplayerServer() != null) place = "local:" + mc.getSingleplayerServer().getWorldData().getLevelName() + ":"
                + mc.getSingleplayerServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toAbsolutePath().normalize();
        else if (mc.getCurrentServer() != null) place = "server:" + mc.getCurrentServer().ip;
        else place = "unknown";
        String player = mc.player == null ? "none" : mc.player.getUUID().toString();
        try {
            var digest = MessageDigest.getInstance("SHA-256").digest((place + "|" + player).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 12);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean readOnly() { return readOnly; }

    public void addRecent(CalculationResult result) {
        if (readOnly) return;
        if (recents.contains(result.id())) return;
        keep(result);
        recents.addFirst(result.id());
        while (recents.size() > RECENT_LIMIT) release(recents.removeLast());
        write();
    }

    /** False when the saved list is full; nothing is removed to make room. */
    public boolean save(CalculationResult result) {
        if (readOnly) return false;
        if (saved.contains(result.id())) return true;
        if (saved.size() >= SAVED_LIMIT) return false;
        keep(result);
        saved.addFirst(result.id());
        if (write()) return true;
        saved.remove(result.id());
        release(result.id());
        return false;
    }

    public void removeSaved(String id) {
        if (readOnly) return;
        int index = saved.indexOf(id);
        if (saved.remove(id)) {
            if (write()) release(id);
            else saved.add(index, id);
        }
    }

    public boolean isSaved(String id) {
        return saved.contains(id);
    }

    public boolean savedFull() {
        return saved.size() >= SAVED_LIMIT;
    }

    private void keep(CalculationResult result) {
        stored.putIfAbsent(result.id(), result.toTag());
        parsed.putIfAbsent(result.id(), result);
    }

    private void release(String id) {
        if (!recents.contains(id) && !saved.contains(id)) {
            stored.remove(id);
            parsed.remove(id);
        }
    }

    public List<CalculationResult> recents() {
        return recents.stream().map(this::find).filter(Objects::nonNull).toList();
    }

    public List<CalculationResult> saved() {
        return saved.stream().map(this::find).filter(Objects::nonNull).toList();
    }

    @Nullable
    public CalculationResult find(String id) {
        var result = parsed.get(id);
        if (result == null && stored.containsKey(id)) {
            result = CalculationResult.fromTag(stored.get(id));
            if (result != null) parsed.put(id, result);
        }
        return result;
    }

    private void load() {
        if (!Files.isRegularFile(file)) return;
        try {
            if (Files.size(file) > MAX_FILE_BYTES) {
                readOnly = true;
                BiggerReactors.LOGGER.warn("Calculator library {} is too large; left untouched", file);
                return;
            }
            var tag = NbtIo.readCompressed(file, NbtAccounter.create(MAX_FILE_BYTES));
            if (tag.getInt("schema") > SCHEMA) {
                readOnly = true;
                BiggerReactors.LOGGER.warn("Calculator library {} comes from a newer version; left untouched", file);
                return;
            }
            var snapshots = tag.getCompound("snapshots");
            for (String id : snapshots.getAllKeys()) if (id.length() <= 64) stored.put(id, snapshots.getCompound(id));
            for (var id : tag.getList("recents", Tag.TAG_STRING))
                if (stored.containsKey(id.getAsString()) && recents.size() < RECENT_LIMIT && !recents.contains(id.getAsString())) recents.addLast(id.getAsString());
            for (var id : tag.getList("saved", Tag.TAG_STRING))
                if (stored.containsKey(id.getAsString()) && !saved.contains(id.getAsString())) saved.add(id.getAsString());
            stored.keySet().removeIf(id -> !recents.contains(id) && !saved.contains(id));
        } catch (IOException | RuntimeException e) {
            readOnly = true;
            BiggerReactors.LOGGER.error("Could not read calculator library {}; it is kept as it is", file, e);
        }
    }

    /** Written to a temporary file first and moved over the old one, so a failure never leaves a broken library. */
    private boolean write() {
        if (readOnly) return false;
        var tag = new CompoundTag();
        tag.putInt("schema", SCHEMA);
        var recentList = new ListTag();
        recents.forEach(id -> recentList.add(StringTag.valueOf(id)));
        tag.put("recents", recentList);
        var savedList = new ListTag();
        saved.forEach(id -> savedList.add(StringTag.valueOf(id)));
        tag.put("saved", savedList);
        var snapshots = new CompoundTag();
        stored.forEach(snapshots::put);
        tag.put("snapshots", snapshots);
        try {
            biggerreactorsrework.core.util.AtomicFiles.replace(file, temporary -> NbtIo.writeCompressed(tag, temporary));
            writeFailed = false;
            return true;
        } catch (IOException | RuntimeException e) {
            writeFailed = true;
            BiggerReactors.LOGGER.error("Could not save calculator library {}", file, e);
            return false;
        }
    }
}
