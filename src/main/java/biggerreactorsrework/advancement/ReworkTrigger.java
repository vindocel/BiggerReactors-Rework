package biggerreactorsrework.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

/** Added 2026-11-01: native server-confirmed machine milestones, without rewards. */
public final class ReworkTrigger extends SimpleCriterionTrigger<ReworkTrigger.Instance> {
    @Override public Codec<Instance> codec() { return Instance.CODEC; }
    public void trigger(ServerPlayer player, String event) { trigger(player, instance -> instance.event().equals(event)); }
    public record Instance(Optional<ContextAwarePredicate> player, String event) implements SimpleInstance {
        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(i -> i.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
                Codec.STRING.fieldOf("event").forGetter(Instance::event)).apply(i, Instance::new));
    }
}
