package biggerreactorsrework.blocks.materials;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import biggerreactorsrework.core.registry.RegisterBlock;

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
                .explosionResistance(3.0F)
                .destroyTime(2.0f)
                .requiresCorrectToolForDrops());
    }
}
