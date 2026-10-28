package biggerreactorsrework.client.sound;

// Added 2026-10-28: persistent positional loop with a bounded gain envelope.
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

final class MachineLoopSound extends AbstractTickableSoundInstance {
    private float target;
    private boolean retiring;
    MachineLoopSound(SoundEvent event) {
        super(event, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        looping = true;
        delay = 0;
        volume = 0;
    }
    void update(Vec3 position, float gain, float speed) {
        x = position.x; y = position.y; z = position.z;
        target = Math.clamp(gain, 0, 1);
        volume = target;
        pitch = Math.clamp(speed, .5f, 2f);
        retiring = false;
    }
    void retire() { target = 0; retiring = true; }
    void finish() { stop(); }
    @Override public void tick() {
        if (retiring) volume = Math.max(0, volume - 1f / 15);
        if (retiring && volume <= .0001f) stop();
    }
    @Override public boolean canStartSilent() { return true; }
}
