package biggerreactorsrework.multiblocks.turbine.client;

import biggerreactorsrework.multiblocks.turbine.tiles.TurbineRotorBearingTile;
import biggerreactorsrework.registries.TurbineCoilRegistry;
import biggerreactorsrework.util.TurbineAtmosphere;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/** Added 2026-10-31: occasional local condensation, bounded independently of turbine size or frame rate. */
public final class TurbineAtmosphereEffects {
    private static final Set<TurbineRotorBearingTile> BEARINGS = Collections.newSetFromMap(new WeakHashMap<>());
    private static final net.minecraft.util.RandomSource RANDOM = net.minecraft.util.RandomSource.create();
    private TurbineAtmosphereEffects() {}

    static void observe(TurbineRotorBearingTile bearing) { BEARINGS.add(bearing); }

    public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        var level = mc.level;
        if (level == null) { BEARINGS.clear(); return; }
        if (mc.isPaused() || mc.player == null) return;
        BEARINGS.removeIf(b -> b.isRemoved() || b.getLevel() != level || !level.hasChunkAt(b.getBlockPos()) ||
                level.getBlockEntity(b.getBlockPos()) != b);
        if (level.getGameTime() % 20 != 0) return;
        for (var bearing : BEARINGS) {
            var bounds = bearing.AABB;
            if (!bearing.isRenderBearing || !bearing.condensationReady || bounds == null ||
                    bounds.distanceToSqr(mc.player.getEyePosition()) > 32 * 32 ||
                    RANDOM.nextFloat() > 0.2F + 0.25F * bearing.steamDensity) continue;
            int minY = (int) bounds.minY, maxY = (int) bounds.maxY;
            int width = (int) (bounds.maxX - bounds.minX) - 1;
            int depth = (int) (bounds.maxZ - bounds.minZ) - 1;
            if (width <= 0 || depth <= 0) continue;
            for (int attempt = 0; attempt < 8; attempt++) {
                int x = (int) bounds.minX + 1 + RANDOM.nextInt(width);
                int z = (int) bounds.minZ + 1 + RANDOM.nextInt(depth);
                int y = RANDOM.nextBoolean() ? maxY : maxY - 1 - RANDOM.nextInt(Math.max(1, (maxY - minY) / 2));
                var pos = new BlockPos(x, y, z);
                if (!level.hasChunkAt(pos) || !level.hasChunkAt(pos.below())) continue;
                var state = level.getBlockState(pos);
                boolean roof = y == maxY && !state.isAir();
                boolean coil = TurbineCoilRegistry.getCoilData(state.getBlock()) != null;
                if (!TurbineAtmosphere.condensationSurface(roof, coil, level.getBlockState(pos.below()).isAir(), y, minY, maxY)) continue;
                level.addParticle(ParticleTypes.DRIPPING_WATER, x + 0.2 + RANDOM.nextDouble() * 0.6,
                        y - 0.02, z + 0.2 + RANDOM.nextDouble() * 0.6, 0, 0, 0);
                break;
            }
        }
    }
}
