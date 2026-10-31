package biggerreactorsrework.multiblocks.reactor.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import biggerreactorsrework.core.registry.RegisterTile;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class ReactorManifoldTile extends ReactorBaseTile {
    
    @RegisterTile("reactor_manifold")
    public static final RegisterTile.Producer<ReactorManifoldTile> SUPPLIER = new RegisterTile.Producer<>(ReactorManifoldTile::new);
    
    public ReactorManifoldTile(BlockEntityType<?> TYPE, BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }
    
    public long lastCheckedTick = Long.MIN_VALUE;
    public BlockPos coolantAnchor;
    public int coolantColumnBottom=getBlockPos().getY(),coolantColumnHeight=1;
    @Override protected void readNBT(net.minecraft.nbt.CompoundTag tag){super.readNBT(tag);coolantAnchor=tag.contains("coolantAnchor")?BlockPos.of(tag.getLongOr("coolantAnchor", 0L)):null;coolantColumnBottom=tag.contains("coolantColumnBottom")?tag.getIntOr("coolantColumnBottom", 0):getBlockPos().getY();coolantColumnHeight=Math.max(1,tag.getIntOr("coolantColumnHeight", 0));}
    @Override protected net.minecraft.nbt.CompoundTag writeNBT(){var tag=super.writeNBT();if(coolantAnchor!=null)tag.putLong("coolantAnchor",coolantAnchor.asLong());tag.putInt("coolantColumnBottom",coolantColumnBottom);tag.putInt("coolantColumnHeight",coolantColumnHeight);return tag;}
    @Override protected net.minecraft.nbt.CompoundTag getUpdateNBT(){return writeNBT();}
    @Override protected void handleUpdateNBT(net.minecraft.nbt.CompoundTag tag){readNBT(tag);}
}
