package biggerreactorsrework.client.sound;

// Added 2026-10-28: one logical source per structure, independent of menus/renderers.
import biggerreactorsrework.audio.MachineAudio;
import biggerreactorsrework.audio.MachineAudioRange;
import biggerreactorsrework.audio.MachineAudioState;
import biggerreactorsrework.audio.TurbineAudioCurve;
import biggerreactorsrework.client.GuiThemes;
import biggerreactorsrework.core.modular.tile.ModularTile;
import biggerreactorsrework.registries.MachineSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;
import java.util.*;

public final class MachineSoundManager {
    private static final Set<ModularTile> SOURCES = Collections.newSetFromMap(new WeakHashMap<>());
    private record Key(int kind, BlockPos min) {}
    private static final Map<Key, Playing> PLAYING = new HashMap<>();
    private static ClientLevel world;
    private static long tick;
    private MachineSoundManager() {}
    public static void init() {
        MachineAudio.clientUpdate = SOURCES::add;
        NeoForge.EVENT_BUS.addListener(MachineSoundManager::tick);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> clear());
    }
    public static void clear() {
        PLAYING.values().forEach(Playing::stop);
        PLAYING.clear(); SOURCES.clear(); world = null;
    }
    private static void tick(ClientTickEvent.Post e) {
        var mc = Minecraft.getInstance();
        if (world != mc.level) {
            PLAYING.values().forEach(Playing::stop); PLAYING.clear(); world = mc.level;
        }
        if (world == null || mc.player == null) { SOURCES.clear(); return; }
        if (mc.isPaused()) return;
        tick++;
        var states = new HashMap<Key, MachineAudioState>();
        SOURCES.removeIf(tile -> tile.isRemoved() || tile.getLevel() != world
                || !world.hasChunkAt(tile.getBlockPos()) || world.getBlockEntity(tile.getBlockPos()) != tile);
        for (var tile : SOURCES) {
            var s = tile.machineAudio;
            if (s.kind() < 0 || !s.running()) continue;
            var key = new Key(s.kind(), s.min());
            // Multiple terminal snapshots represent one source. The latest speed wins.
            states.merge(key, s, (a, b) -> a.rpm() >= b.rpm() ? a : b);
        }
        var eye = mc.player.getEyePosition();
        var wanted = new HashSet<Key>();
        states.forEach((key, s) -> {
            Vec3 position = position(s, eye);
            int range = MachineAudioRange.trackingBlocks(s.kind());
            if (position.distanceToSqr(eye) > range * range) return;
            wanted.add(key);
            var playing = PLAYING.computeIfAbsent(key, k -> new Playing());
            playing.inactiveSince = -1;
            playing.update(s, position);
        });
        PLAYING.entrySet().removeIf(entry -> {
            if (wanted.contains(entry.getKey())) return false;
            if (entry.getValue().inactiveSince < 0) entry.getValue().inactiveSince = tick;
            entry.getValue().voices.values().forEach(MachineLoopSound::retire);
            boolean done = tick - entry.getValue().inactiveSince >= 15
                    || entry.getValue().voices.values().stream().allMatch(MachineLoopSound::isStopped);
            if (done) entry.getValue().stop();
            return done;
        });
    }
    private static Vec3 position(MachineAudioState s, Vec3 eye) {
        // Closest point on the enclosing box: large structures stay audible at their surface.
        return new Vec3(Math.clamp(eye.x, s.min().getX(), s.max().getX() + 1.0),
                Math.clamp(eye.y, s.min().getY(), s.max().getY() + 1.0),
                Math.clamp(eye.z, s.min().getZ(), s.max().getZ() + 1.0));
    }
    private static final class Playing {
        final Map<Integer, MachineLoopSound> voices = new HashMap<>();
        double rpm = Double.NaN;
        float fade;
        long nextRetry;
        long inactiveSince = -1;
        void update(MachineAudioState s, Vec3 position) {
            rpm = Double.isNaN(rpm) ? s.rpm() : rpm + (s.rpm() - rpm) * .2;
            float volume = GuiThemes.machineVolume(s) / 100f;
            fade = Math.min(1, fade + .1f);
            volume *= fade;
            var requested = new HashMap<Integer, TurbineAudioCurve.Voice>();
            if (s.kind() == MachineAudioState.TURBINE) {
                for (var v : TurbineAudioCurve.voices(rpm)) if (v.weight() > 0) requested.put(v.anchor(), v);
            } else requested.put(0, new TurbineAudioCurve.Voice(0, 1, 1));
            var manager = Minecraft.getInstance().getSoundManager();
            // Obsolete anchors reach zero at a boundary; release them before creating the next.
            voices.entrySet().removeIf(entry -> {
                if (requested.containsKey(entry.getKey())) return false;
                entry.getValue().finish(); manager.stop(entry.getValue()); return true;
            });
            for (var v : requested.values()) {
                var sound = voices.get(v.anchor());
                if (sound == null || sound.isStopped() || tick >= nextRetry && !manager.isActive(sound)) {
                    if (sound != null) { sound.finish(); manager.stop(sound); }
                    sound = new MachineLoopSound(event(s.kind(), v.anchor()));
                    voices.put(v.anchor(), sound);
                    sound.update(position, volume * v.weight(), v.pitch());
                    manager.play(sound);
                    nextRetry = tick + 20;
                } else sound.update(position, volume * v.weight(), v.pitch());
            }
        }
        void stop() {
            var manager = Minecraft.getInstance().getSoundManager();
            voices.values().forEach(s -> { s.finish(); manager.stop(s); });
        }
    }
    private static SoundEvent event(int kind, int anchor) {
        if (kind == MachineAudioState.REACTOR) return MachineSounds.REACTOR.get();
        if (kind == MachineAudioState.BOILER) return MachineSounds.BOILER.get();
        return switch (anchor) {
            case 300 -> MachineSounds.TURBINE_LOW.get();
            case 900 -> MachineSounds.TURBINE_MID.get();
            default -> MachineSounds.TURBINE_HIGH.get();
        };
    }
}
