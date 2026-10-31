package biggerreactorsrework.blocks.materials;

import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import biggerreactorsrework.core.registry.RegisterBlock;


public class DeepslateUraniumOre extends DropExperienceBlock {

    @RegisterBlock(name = "deepslate_uranium_ore")
    public static final DeepslateUraniumOre INSTANCE = biggerreactorsrework.core.registry.RegistryIds.named("deepslate_uranium_ore", () -> new DeepslateUraniumOre());

    public DeepslateUraniumOre() {
        super(ConstantInt.of(0),
                biggerreactorsrework.core.registry.RegistryIds.block(Properties.of()
                        .sound(SoundType.DEEPSLATE)
                        .explosionResistance(3.0F)
                        .destroyTime(4.5f)
                        .requiresCorrectToolForDrops()
        ));
    }
}
