package biggerreactorsrework.multiblocks.turbine;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.advancement.ReworkAdvancements;
import biggerreactorsrework.multiblocks.turbine.simulation.ITurbineSimulation;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector4i;

/** Added 2026-11-01: server-side rotating blade envelopes, cached once per assembly. */
public final class TurbineRotorHazard {
    public static final ResourceKey<DamageType> DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(BiggerReactors.modid, "turbine_rotor"));
    public record Sweep(Vec3 center, Direction.Axis axis, double radius) {
        public boolean intersects(AABB body) {
            double axialMin = axis == Direction.Axis.X ? body.minX : axis == Direction.Axis.Y ? body.minY : body.minZ;
            double axialMax = axis == Direction.Axis.X ? body.maxX : axis == Direction.Axis.Y ? body.maxY : body.maxZ;
            double axialCenter = axis == Direction.Axis.X ? center.x : axis == Direction.Axis.Y ? center.y : center.z;
            if (axialMax <= axialCenter - 0.35 || axialMin >= axialCenter + 0.35) return false;
            double a = axis == Direction.Axis.X ? center.y : center.x;
            double b = axis == Direction.Axis.Z ? center.y : center.z;
            double minA = axis == Direction.Axis.X ? body.minY : body.minX;
            double maxA = axis == Direction.Axis.X ? body.maxY : body.maxX;
            double minB = axis == Direction.Axis.Z ? body.minY : body.minZ;
            double maxB = axis == Direction.Axis.Z ? body.maxY : body.maxZ;
            double da = Math.max(minA - a, Math.max(0, a - maxA));
            double db = Math.max(minB - b, Math.max(0, b - maxB));
            return da * da + db * db < radius * radius;
        }
    }
    private List<Sweep> sweeps = List.of();
    public void rebuild(BlockPos bearing, Vec3i axis, List<Vector4i> configuration) {
        var result = new ArrayList<Sweep>();
        Direction.Axis shaftAxis = axis.getX()!=0 ? Direction.Axis.X : axis.getY()!=0 ? Direction.Axis.Y : Direction.Axis.Z;
        BlockPos position = bearing;
        for (var row : configuration) {
            position = position.offset(axis);
            int length = Math.max(Math.max(row.x, row.y), Math.max(row.z, row.w));
            if (length > 0) result.add(new Sweep(Vec3.atCenterOf(position), shaftAxis, length + 0.5));
        }
        sweeps = List.copyOf(result);
    }
    public void clear() { sweeps = List.of(); }
    public static float damagePerHit(double rpm) {
        return biggerreactorsrework.util.TurbineRotorDamage.perHit(rpm, ITurbineSimulation.MAX_RPM);
    }
    public void tick(ServerLevel level, AABB interior, double rpm) {
        if (sweeps.isEmpty() || level.getGameTime() % 10 != 0 || damagePerHit(rpm) == 0) return;
        for (var player : level.getEntitiesOfClass(ServerPlayer.class, interior, p -> p.isAlive() && !p.isSpectator())) hit(level, player, rpm);
    }
    public boolean hit(ServerLevel level, ServerPlayer player, double rpm) {
        float damage = damagePerHit(rpm);
        if (damage == 0 || !player.isAlive() || player.isSpectator()) return false;
        for (Sweep sweep : sweeps) {
            if (!sweep.intersects(player.getBoundingBox())) continue;
            var source = new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DAMAGE), sweep.center());
            boolean hurt = player.hurt(source, damage);
            if (hurt && player.isDeadOrDying()) ReworkAdvancements.award(player, "turbine_accident");
            return hurt;
        }
        return false;
    }
}
