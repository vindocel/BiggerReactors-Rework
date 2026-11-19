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
public class UraniumOre extends DropExperienceBlock {
//?} else if =1.21.1 {
/*import biggerreactorsrework.core.registry.RegisterBlock;

public class UraniumOre extends DropExperienceBlock {
*/
//?}

//? if =26.1.2 || =26.3 {
    @RegisterBlock(name = "uranium_ore")
    public static final UraniumOre INSTANCE = biggerreactorsrework.core.registry.RegistryIds.named("uranium_ore", () -> new UraniumOre());

    public UraniumOre() {
        super(ConstantInt.of(0),
                biggerreactorsrework.core.registry.RegistryIds.block(Properties.of().mapColor(MapColor.STONE)
                        .sound(SoundType.STONE)
//?} else if =1.21.1 {
/*    @RegisterBlock(name = "uranium_ore")
    public static final UraniumOre INSTANCE = new UraniumOre();

    public UraniumOre() {
        super(ConstantInt.of(0),
                Properties.of()
                        .mapColor(net.minecraft.world.level.material.MapColor.STONE)
                        .sound(SoundType.STONE)
*/
//?}
                        .explosionResistance(3.0F)
                        .destroyTime(3.0f)
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
