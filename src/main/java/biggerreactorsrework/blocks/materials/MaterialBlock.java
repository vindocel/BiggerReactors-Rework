//? if =26.1.2 || =26.3 {
package biggerreactorsrework.blocks.materials;

import net.minecraft.world.level.material.MapColor;

import net.minecraft.world.level.block.Block;
//?} else if =1.21.1 {
/*package biggerreactorsrework.blocks.materials;

import net.minecraft.world.level.block.Block;
*/
//?}
import net.minecraft.world.level.block.SoundType;
//? if =26.1.2 || =26.3 {
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
//?} else if =1.21.1 {
/*import biggerreactorsrework.core.registry.RegisterBlock;

public class MaterialBlock extends Block {
    @RegisterBlock(name = "blutonium_block")
    public static final MaterialBlock BLUTONIUM = new MaterialBlock(net.minecraft.world.level.material.MapColor.COLOR_BLUE);
    @RegisterBlock(name = "cyanite_block")
    public static final MaterialBlock CYANITE = new MaterialBlock(net.minecraft.world.level.material.MapColor.COLOR_CYAN);
    @RegisterBlock(name = "graphite_block")
    public static final MaterialBlock GRAPHITE = new MaterialBlock(net.minecraft.world.level.material.MapColor.COLOR_GRAY);
    @RegisterBlock(name = "ludicrite_block")
    public static final MaterialBlock LUDICRITE = new MaterialBlock(net.minecraft.world.level.material.MapColor.COLOR_PURPLE);
    @RegisterBlock(name = "uranium_block")
    public static final MaterialBlock URANIUM = new MaterialBlock(net.minecraft.world.level.material.MapColor.COLOR_GREEN);
    @RegisterBlock(name = "raw_uranium_block")
    public static final MaterialBlock RAW_URANIUM = new MaterialBlock(net.minecraft.world.level.material.MapColor.COLOR_LIGHT_GREEN);

    public MaterialBlock(net.minecraft.world.level.material.MapColor mapColor) {
        super(Properties.of()
                .mapColor(mapColor)
                .sound(SoundType.STONE)
*/
//?}
                .explosionResistance(3.0F)
//? if =26.1.2 || =26.3 {
                .destroyTime(2.0f)
                .requiresCorrectToolForDrops()));
    }
//?} else if =1.21.1 {
/*                .destroyTime(2.0f)
                .requiresCorrectToolForDrops());
    }
*/
//?}
}
