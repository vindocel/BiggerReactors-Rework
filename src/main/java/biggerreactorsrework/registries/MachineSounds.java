package biggerreactorsrework.registries;

// Added 2026-10-28: calibrated mono loops; runtime gain 1 is the approved maximum.
import biggerreactorsrework.BiggerReactors;
//? if =26.1.2 || =26.3 {
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
//?} else if =1.21.1 {
/*import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
*/
//?}
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

public final class MachineSounds {
    private static final DeferredRegister<SoundEvent> EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, BiggerReactors.modid);
    public static final Supplier<SoundEvent> REACTOR = sound("machine.reactor.loop");
    public static final Supplier<SoundEvent> BOILER = sound("machine.boiler.loop");
    public static final Supplier<SoundEvent> TURBINE_LOW = sound("machine.turbine.low_loop");
    public static final Supplier<SoundEvent> TURBINE_MID = sound("machine.turbine.mid_loop");
    public static final Supplier<SoundEvent> TURBINE_HIGH = sound("machine.turbine.high_loop");
//? if =26.1.2 || =26.3 {
    private static Supplier<SoundEvent> sound(String id) {
        return EVENTS.register(id, () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(BiggerReactors.modid, id)));
    }
//?} else if =1.21.1 {
/*    private static Supplier<SoundEvent> sound(String id) {
        return EVENTS.register(id, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(BiggerReactors.modid, id)));
    }
*/
//?}
    public static void register(IEventBus bus) { EVENTS.register(bus); }
    private MachineSounds() {}
}
