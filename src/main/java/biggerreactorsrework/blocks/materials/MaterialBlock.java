package biggerreactorsrework.blocks.materials;

import net.minecraft.world.level.material.MapColor;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import biggerreactorsrework.core.registry.RegisterBlock;

// Modified 2026-11-01: each registered material supplies its own map palette color.
public class MaterialBlock extends Block {
    @RegisterBlock(name = "blutonium_block")
    public static final MaterialBlock BLUTONIUM = biggerreactorsrework.core.registry.RegistryIds.named("blutonium_block", () -> new MaterialBlock(MapColor.COLOR_BLUE));
    @RegisterBlock(name = "cyanite_block")
    public static final MaterialBlock CYANITE = biggerreactorsrework.core.registry.RegistryIds.named("cyanite_block", () -> new MaterialBlock(MapColor.COLOR_CYAN));
    @RegisterBlock(name = "graphite_block")
    public static final MaterialBlock GRAPHITE = biggerreactorsrework.core.registry.RegistryIds.named("graphite_block", () -> new MaterialBlock(MapColor.COLOR_GRAY));
    @RegisterBlock(name = "ludicrite_block")
    public static final MaterialBlock LUDICRITE = biggerreactorsrework.core.registry.RegistryIds.named("ludicrite_block", () -> new MaterialBlock(MapColor.COLOR_PURPLE));
    @RegisterBlock(name = "uranium_block")
    public static final MaterialBlock URANIUM = biggerreactorsrework.core.registry.RegistryIds.named("uranium_block", () -> new MaterialBlock(MapColor.COLOR_GREEN));
    @RegisterBlock(name = "raw_uranium_block")
    public static final MaterialBlock RAW_URANIUM = biggerreactorsrework.core.registry.RegistryIds.named("raw_uranium_block", () -> new MaterialBlock(MapColor.COLOR_LIGHT_GREEN));
    
    public MaterialBlock(MapColor mapColor) {
        super(biggerreactorsrework.core.registry.RegistryIds.block(Properties.of().mapColor(mapColor)
                .sound(SoundType.STONE)
                .explosionResistance(3.0F)
                .destroyTime(2.0f)
                .requiresCorrectToolForDrops()));
    }
}
