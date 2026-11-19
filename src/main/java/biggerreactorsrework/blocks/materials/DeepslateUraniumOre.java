//? if =26.1.2 || =26.3 {
package biggerreactorsrework.blocks.materials;

import net.minecraft.world.level.material.MapColor;

import net.minecraft.util.valueproviders.ConstantInt;
//?} else if =1.21.1 {
/*package biggerreactorsrework.blocks.materials;

import net.minecraft.util.valueproviders.ConstantInt;
*/
//?}
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
//? if =26.1.2 || =26.3 {
import biggerreactorsrework.core.registry.RegisterBlock;


// Modified 2026-11-01: declare a map palette color without changing block behavior.
public class DeepslateUraniumOre extends DropExperienceBlock {
//?} else if =1.21.1 {
/*import biggerreactorsrework.core.registry.RegisterBlock;


public class DeepslateUraniumOre extends DropExperienceBlock {
*/
//?}

//? if =26.1.2 || =26.3 {
    @RegisterBlock(name = "deepslate_uranium_ore")
    public static final DeepslateUraniumOre INSTANCE = biggerreactorsrework.core.registry.RegistryIds.named("deepslate_uranium_ore", () -> new DeepslateUraniumOre());

    public DeepslateUraniumOre() {
        super(ConstantInt.of(0),
                biggerreactorsrework.core.registry.RegistryIds.block(Properties.of().mapColor(MapColor.DEEPSLATE)
                        .sound(SoundType.DEEPSLATE)
//?} else if =1.21.1 {
/*    @RegisterBlock(name = "deepslate_uranium_ore")
    public static final DeepslateUraniumOre INSTANCE = new DeepslateUraniumOre();

    public DeepslateUraniumOre() {
        super(ConstantInt.of(0),
                Properties.of()
                        .mapColor(net.minecraft.world.level.material.MapColor.DEEPSLATE)
                        .sound(SoundType.DEEPSLATE)
*/
//?}
                        .explosionResistance(3.0F)
                        .destroyTime(4.5f)
//? if =26.1.2 || =26.3 {
                        .requiresCorrectToolForDrops()
        ));
    }
//?} else if =1.21.1 {
/*                        .requiresCorrectToolForDrops()
        );
    }
*/
//?}
}
