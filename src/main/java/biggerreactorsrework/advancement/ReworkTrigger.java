package biggerreactorsrework.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
//? if =26.1.2 {
import java.util.Optional;
import net.minecraft.advancements.criterion.SimpleCriterionTrigger;
import net.minecraft.advancements.criterion.ContextAwarePredicate;
import net.minecraft.server.level.ServerPlayer;

/** Added 2026-11-01: native server-confirmed machine milestones, without rewards. */
public final class ReworkTrigger extends SimpleCriterionTrigger<ReworkTrigger.Instance> {
//?} else if =1.21.1 {
/*import java.util.Optional;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

/^* Added 2026-11-01: native server-confirmed machine milestones, without rewards. ^/
public final class ReworkTrigger extends SimpleCriterionTrigger<ReworkTrigger.Instance> {
*/
//?} else if =26.3 {
/*import java.util.Optional;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/^* Added 2026-11-01: native server-confirmed machine milestones, without rewards. ^/
public final class ReworkTrigger extends SimpleCriterionTrigger<ReworkTrigger.Instance> {
*/
//?}
    @Override public Codec<Instance> codec() { return Instance.CODEC; }
//? if =26.1.2 {
    public void trigger(ServerPlayer player, String event) { trigger(player, instance -> instance.event().equals(event)); }
    // Modified 2026-11-01: native 26.1.2 player predicate and criterion package.
    public record Instance(Optional<ContextAwarePredicate> player, String event) implements SimpleInstance {
        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(i -> i.group(
                ContextAwarePredicate.CODEC.optionalFieldOf("player").forGetter(Instance::player),
                Codec.STRING.fieldOf("event").forGetter(Instance::event)).apply(i, Instance::new));
//?} else if =1.21.1 {
/*    public void trigger(ServerPlayer player, String event) { trigger(player, instance -> instance.event().equals(event)); }
    public record Instance(Optional<ContextAwarePredicate> player, String event) implements SimpleInstance {
        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(i -> i.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
                Codec.STRING.fieldOf("event").forGetter(Instance::event)).apply(i, Instance::new));
*/
//?} else if =26.3 {
/*    public void trigger(ServerPlayer player, String event) { trigger(player, instance -> instance.event().equals(event)); }
    public record Instance(Optional<Holder<LootItemCondition>> player, String event) implements SimpleInstance {
        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(i -> i.group(
                LootItemCondition.CODEC.optionalFieldOf("player").forGetter(Instance::player),
                Codec.STRING.fieldOf("event").forGetter(Instance::event)).apply(i, Instance::new));
*/
//?}
    }
}
