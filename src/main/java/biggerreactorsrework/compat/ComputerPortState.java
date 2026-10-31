package biggerreactorsrework.compat;

// Added 2026-10-30: optional computer integration without loading an external API in core tiles.
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.util.function.Function;

public final class ComputerPortState {
    public interface Port { ComputerPortState computerPortState(); }
    public interface Component {
        void join();
        void remove();
        void load(CompoundTag tag, HolderLookup.Provider registries);
        void save(CompoundTag tag, HolderLookup.Provider registries);
    }
    public static Function<BlockEntity, Component> factory = tile -> null;
    private final BlockEntity tile;
    private Component component;
    private CompoundTag saved = new CompoundTag();
    private boolean loaded;

    public ComputerPortState(BlockEntity tile) { this.tile = tile; }
    public Component component() {
        if (component == null && tile.getLevel() != null && !tile.getLevel().isClientSide()) {
            component = factory.apply(tile);
            if (component != null) component.load(saved, tile.getLevel().registryAccess());
        }
        return component;
    }
    public boolean loaded() { return loaded && !tile.isRemoved(); }
    public void onLoad() {
        loaded = true;
        var value = component();
        if (value != null) value.join();
    }
    public void remove() {
        loaded = false;
        if (component != null) component.remove();
    }
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        saved = tag.getCompoundOrEmpty("computerComponent").copy();
        if (component != null) component.load(saved, registries);
    }
    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        if (component != null) component.save(saved, registries);
        if (!saved.isEmpty()) tag.put("computerComponent", saved.copy());
    }
}
