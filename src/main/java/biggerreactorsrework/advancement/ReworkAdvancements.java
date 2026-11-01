package biggerreactorsrework.advancement;

import biggerreactorsrework.BiggerReactors;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.advancements.triggers.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Added 2026-11-01: nearby witnesses receive machine milestones; persistence belongs to vanilla advancements. */
public final class ReworkAdvancements {
    private static final DeferredRegister<CriterionTrigger<?>> TRIGGERS = DeferredRegister.create(Registries.TRIGGER_TYPE, BiggerReactors.modid);
    public static final net.neoforged.neoforge.registries.DeferredHolder<CriterionTrigger<?>, ReworkTrigger> EVENT = TRIGGERS.register("machine_event", ReworkTrigger::new);
    private ReworkAdvancements() {}
    public static void register(IEventBus bus) { TRIGGERS.register(bus); }
    public static void award(ServerPlayer player, String event) { EVENT.get().trigger(player, event); }
    public static final class Reporter {
        private final Map<String, Long> last = new HashMap<>();
        public void report(Level level, org.joml.Vector3ic min, org.joml.Vector3ic max, String event) {
            Long previous = last.get(event);
            if (previous != null && level.getGameTime() >= previous && level.getGameTime() - previous < 100) return;
            report(level, new AABB(min.x(), min.y(), min.z(), max.x()+1, max.y()+1, max.z()+1), event);
        }
        public void report(Level level, AABB machine, String event) {
            if (!(level instanceof ServerLevel server)) return;
            long now = level.getGameTime();
            var previous = last.get(event);
            if (previous != null && now >= previous && now - previous < 100) return;
            last.put(event, now);
            AABB vicinity = machine.inflate(32);
            for (ServerPlayer player : server.players()) {
                if (player.isAlive() && !player.isSpectator() && vicinity.contains(player.position())) award(player, event);
            }
        }
    }
}
