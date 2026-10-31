package biggerreactorsrework.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.block.entity.BlockEntity;

final class GameTestSupport {
    private GameTestSupport() {
    }

    static void load(BlockEntity tile, CompoundTag tag, GameTestHelper helper) {
        tile.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), tag));
    }

    static void update(BlockEntity tile, CompoundTag tag, GameTestHelper helper) {
        tile.handleUpdateTag(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), tag));
    }

    @SuppressWarnings("unchecked")
    static <T extends BlockEntity> T be(GameTestHelper helper, BlockPos pos) {
        return (T) helper.getBlockEntity(pos, BlockEntity.class);
    }
}
