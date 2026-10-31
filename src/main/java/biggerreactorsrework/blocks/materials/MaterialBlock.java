package biggerreactorsrework.blocks.materials;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import biggerreactorsrework.core.registry.RegisterBlock;

public class MaterialBlock extends Block {
    @RegisterBlock(name = "blutonium_block")
    public static final MaterialBlock BLUTONIUM = biggerreactorsrework.core.registry.RegistryIds.named("blutonium_block", () -> new MaterialBlock());
    @RegisterBlock(name = "cyanite_block")
    public static final MaterialBlock CYANITE = biggerreactorsrework.core.registry.RegistryIds.named("cyanite_block", () -> new MaterialBlock());
    @RegisterBlock(name = "graphite_block")
    public static final MaterialBlock GRAPHITE = biggerreactorsrework.core.registry.RegistryIds.named("graphite_block", () -> new MaterialBlock());
    @RegisterBlock(name = "ludicrite_block")
    public static final MaterialBlock LUDICRITE = biggerreactorsrework.core.registry.RegistryIds.named("ludicrite_block", () -> new MaterialBlock());
    @RegisterBlock(name = "uranium_block")
    public static final MaterialBlock URANIUM = biggerreactorsrework.core.registry.RegistryIds.named("uranium_block", () -> new MaterialBlock());
    @RegisterBlock(name = "raw_uranium_block")
    public static final MaterialBlock RAW_URANIUM = biggerreactorsrework.core.registry.RegistryIds.named("raw_uranium_block", () -> new MaterialBlock());
    
    public MaterialBlock() {
        super(biggerreactorsrework.core.registry.RegistryIds.block(Properties.of()
                .sound(SoundType.STONE)
                .explosionResistance(3.0F)
                .destroyTime(2.0f)
                .requiresCorrectToolForDrops()));
    }
}
