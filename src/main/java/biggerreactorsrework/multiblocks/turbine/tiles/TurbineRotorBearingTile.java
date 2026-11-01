package biggerreactorsrework.multiblocks.turbine.tiles;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import biggerreactorsrework.core.CoreEvents;
import biggerreactorsrework.core.multiblock.common.IEventMultiblock;
import biggerreactorsrework.core.multiblock.validated.IValidatedMultiblock;
import biggerreactorsrework.core.registry.RegisterTile;
import org.joml.Vector3i;
import org.joml.Vector4i;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;

import static biggerreactorsrework.core.multiblock.IAssemblyStateBlock.ASSEMBLED;

/**
 * Bearing of an assembled turbine. On the client it holds the rotor layout and speed the rotor renderer draws;
 * see {@code TurbineRotorRenderer}.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TurbineRotorBearingTile extends TurbineBaseTile implements IEventMultiblock.AssemblyStateTransition {

    public static boolean APRIL_FOOLS_JOKE = false;

    @RegisterTile("turbine_rotor_bearing")
    public static final RegisterTile.Producer<TurbineRotorBearingTile> SUPPLIER = new RegisterTile.Producer<>(TurbineRotorBearingTile::new);

    public TurbineRotorBearingTile(BlockEntityType<?> TYPE, BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }

    public boolean isRenderBearing = false;
    public double speed = 0;
    // Modified 2026-10-31: transient atmosphere snapshot; never persisted as simulation contents.
    public float steamDensity = 0;
    public float visibleSteam = 0;
    public long lastSteamNanos = 0;
    public boolean condensationReady;
    public int rotorRevision;
    public java.util.List<biggerreactorsrework.core.util.FluidSurfaceMesh.Face> steamWindows = java.util.List.of();
    public Vector3i rotationAxis = null;
    public ArrayList<Vector4i> rotorConfiguration = null;
    public AABB AABB = null;
    private long sendFullUpdate = Long.MAX_VALUE;
    // Modified 2026-10-25: geometry synchronization must not depend on a change in RPM.
    public boolean needsFullUpdate() { return sendFullUpdate!=Long.MAX_VALUE && sendFullUpdate<=CoreEvents.tickNumber(); }

    // client render state, advanced by the renderer from real frame time
    public double renderAngle = 0;
    public long lastRenderNanos = 0;

    @Nullable
    @Override
    public CompoundTag getUpdateNBT() {
        CompoundTag nbt = new CompoundTag();
        if (nullableController() != null) {
            if (!getBlockState().getValue(ASSEMBLED)) {
                nbt.putBoolean("disassembled", true);
                return nbt;
            }
            nbt.putDouble("speed", controller().simulation().RPM());
            nbt.putFloat("steamDensity", currentSteamDensity());
            nbt.putBoolean("condensationReady", controller().condensationReady());
            if (needsFullUpdate()) {
                sendFullUpdate = Long.MAX_VALUE;
                nbt.put("config", getUpdateTag(registries()));
            }
        }
        return nbt;
    }

    @Override
    public void handleUpdateNBT(CompoundTag nbt) {
        if (!nbt.getBoolean("disassembled") && nbt.contains("speed")) {
            speed = nbt.getDouble("speed");
            steamDensity = Math.clamp(nbt.getFloat("steamDensity"), 0, 1);
            condensationReady = nbt.getBoolean("condensationReady");
            if (nbt.contains("config")) {
                handleUpdateTag(nbt.getCompound("config"), registries());
            }
        } else {
            isRenderBearing = false;
            steamDensity = 0;
            condensationReady = false;
            steamWindows = java.util.List.of();
        }
    }

    @Override
    public void handleDataNBT(CompoundTag nbt) {
        rotorRevision++;
        steamWindows = java.util.List.of();
        steamDensity = Math.clamp(nbt.getFloat("steamDensity"), 0, 1);
        condensationReady = nbt.getBoolean("condensationReady");
        if (nbt.contains("rotx")) {
            isRenderBearing = true;
            if (rotationAxis == null) {
                rotationAxis = new Vector3i();
            }
            rotationAxis.set(nbt.getInt("rotx"), nbt.getInt("roty"), nbt.getInt("rotz"));
            if (rotorConfiguration == null) {
                rotorConfiguration = new ArrayList<>();
            }
            rotorConfiguration.clear();
            int rotorShafts = nbt.getInt("shafts");
            for (int i = 0; i < rotorShafts; i++) {
                Vector4i vec = new Vector4i();
                vec.x = nbt.getInt("shaft" + i + "0");
                vec.y = nbt.getInt("shaft" + i + "1");
                vec.z = nbt.getInt("shaft" + i + "2");
                vec.w = nbt.getInt("shaft" + i + "3");
                rotorConfiguration.add(vec);
            }
            AABB = new AABB(nbt.getInt("minx"), nbt.getInt("miny"), nbt.getInt("minz"), nbt.getInt("maxx"), nbt.getInt("maxy"), nbt.getInt("maxz"));
            var p = getBlockPos();
            steamWindows = biggerreactorsrework.core.util.SteamWindowMesh.faces(
                    new biggerreactorsrework.core.util.FluidSurfaceMesh.Box((float) (AABB.minX + 1 - p.getX()),
                            (float) (AABB.minY + 1 - p.getY()), (float) (AABB.minZ + 1 - p.getZ()),
                            (float) (AABB.maxX - p.getX()), (float) (AABB.maxY - p.getY()), (float) (AABB.maxZ - p.getZ())),
                    nbt.getIntArray("steamGlass"));
        } else {
            isRenderBearing = false;
        }
    }

    @Override
    public CompoundTag getDataNBT() {
        CompoundTag nbt = super.getDataNBT();
        if (isRenderBearing && nullableController() != null) {
            nbt.putIntArray("steamGlass", controller().steamGlassPositions(getBlockPos()));
            nbt.putFloat("steamDensity", currentSteamDensity());
            nbt.putBoolean("condensationReady", controller().condensationReady());
            nbt.putInt("rotx", controller().rotationAxis.getX());
            nbt.putInt("roty", controller().rotationAxis.getY());
            nbt.putInt("rotz", controller().rotationAxis.getZ());
            nbt.putInt("minx", controller().min().x());
            nbt.putInt("miny", controller().min().y());
            nbt.putInt("minz", controller().min().z());
            nbt.putInt("maxx", controller().max().x());
            nbt.putInt("maxy", controller().max().y());
            nbt.putInt("maxz", controller().max().z());
            nbt.putInt("shafts", controller().rotorConfiguration.size());
            ArrayList<Vector4i> config = controller().rotorConfiguration;
            for (int i = 0; i < config.size(); i++) {
                Vector4i vec = config.get(i);
                nbt.putInt("shaft" + i + "0", vec.x);
                nbt.putInt("shaft" + i + "1", vec.y);
                nbt.putInt("shaft" + i + "2", vec.z);
                nbt.putInt("shaft" + i + "3", vec.w);
            }
        }
        return nbt;
    }

    public float currentSteamDensity() {
        if (nullableController() == null || !getBlockState().getValue(ASSEMBLED)) return 0;
        var simulation = controller().simulation();
        return biggerreactorsrework.util.TurbineAtmosphere.density(
                simulation.fluidTank().vaporAmount(), simulation.fluidTank().perSideCapacity(),
                simulation.active(), simulation.flowLastTick(), simulation.nominalFlowRate());
    }

    @Override
    public void onAssemblyStateTransition(IValidatedMultiblock.AssemblyState oldState, IValidatedMultiblock.AssemblyState newState) {
        sendFullUpdate = CoreEvents.tickNumber() + 10;
    }
}
