package biggerreactorsrework.multiblocks.turbine.client;

import biggerreactorsrework.multiblocks.turbine.blocks.TurbineRotorBlade;
import biggerreactorsrework.multiblocks.turbine.blocks.TurbineRotorShaft;
import biggerreactorsrework.multiblocks.turbine.tiles.TurbineRotorBearingTile;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Vector3i;
import org.joml.Vector4f;
import org.joml.Vector4i;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws the spinning rotor (shafts and blades) of an assembled turbine from its bearing.
 * <p>
 * The transforms are the ones the rotor has always used: an orientation matrix for the rotor axis,
 * a spin about the rotor axis advanced from real frame time at {@code RPM / 10}, and a per-blade placement matrix.
 */
public class TurbineRotorRenderer implements BlockEntityRenderer<TurbineRotorBearingTile, TurbineRotorRenderer.State> {
    // Modified 2026-10-31: keep baked parts and static rotor transforms between frames, not world lighting.
    private final java.util.Map<net.minecraft.client.renderer.block.dispatch.BlockStateModel,List<BlockStateModelPart>> models = new java.util.IdentityHashMap<>();
    private record Layout(int revision,boolean joke,List<BlockStateModelPart> shafts,List<BlockStateModelPart> blades,List<Part> parts) {}
    private final java.util.Map<TurbineRotorBearingTile,Layout> layouts = new java.util.WeakHashMap<>();

    private record Part(List<BlockStateModelPart> model, int x, int y, int z, Matrix4f spin, Matrix4f local, int light) {
    }

    public static final class State extends BlockEntityRenderState {
        final List<Part> parts = new ArrayList<>();
        List<biggerreactorsrework.core.util.FluidSurfaceMesh.Face> steamWindows = List.of();
        float steamDensity;
        int steamFrame;
    }

    private List<BlockStateModelPart> model(BlockState state) {
        var model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state);
        var cached = models.get(model);
        if (cached != null) return cached;
        if (models.size() >= 2) models.clear();
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(RandomSource.create(42), parts);
        var baked = List.copyOf(parts);
        models.put(model,baked);
        return baked;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        for (Part part : state.parts) {
            poseStack.pushPose();
            poseStack.translate(part.x, part.y, part.z);
            poseStack.mulPose(part.spin);
            poseStack.mulPose(part.local);
            collector.submitMultiLayerBlockModel(poseStack, part.model, false, BlockModelRenderState.EMPTY_TINTS, part.light, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
        TurbineSteamRenderer.submit(poseStack, collector, state.steamWindows, state.steamDensity, state.steamFrame);
    }


    public TurbineRotorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void extractRenderState(TurbineRotorBearingTile tile, State state, float partialTick, Vec3 cameraPosition, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTick, cameraPosition, breakProgress);
        state.parts.clear();
        state.steamWindows = List.of();
        state.steamDensity = 0;
        final var rotationAxis = tile.rotationAxis;
        final var rotorConfiguration = tile.rotorConfiguration;
        final var level = tile.getLevel();
        if (!tile.isRenderBearing || rotationAxis == null || rotorConfiguration == null || level == null) {
            return;
        }

        advanceSpin(tile);
        TurbineAtmosphereEffects.observe(tile);
        if (tile.visibleSteam > 0) {
            state.steamWindows = tile.steamWindows;
            state.steamDensity = tile.visibleSteam;
            state.steamFrame = biggerreactorsrework.core.util.SteamWindowMesh.frame(level.getGameTime() + partialTick);
        }

        final int blade180RotationMultiplier = -rotationAxis.x() | -rotationAxis.y() | rotationAxis.z();
        double initialAngle = 0;
        if (blade180RotationMultiplier > 0) {
            initialAngle += 180;
        }
        if (rotationAxis.x() != 0) {
            initialAngle += 180;
        }

        final var spinMatrix = new Matrix4f();
        spinMatrix.translate(0.5f, 0.5f, 0.5f);
        if (rotationAxis.x() != 0) {
            spinMatrix.rotate((float) Math.toRadians(-90.0f * rotationAxis.x()), 0, 0, 1);
            initialAngle -= 90;
        } else if (rotationAxis.z() != 0) {
            spinMatrix.rotate((float) Math.toRadians(90.0f * rotationAxis.z()), 1, 0, 0);
        } else if (rotationAxis.y() != 1) {
            spinMatrix.rotate((float) Math.toRadians(180), 1, 0, 0);
        }
        spinMatrix.rotate((float) Math.toRadians(initialAngle), 0, 1, 0);
        spinMatrix.translate(-0.5f, -0.5f, -0.5f);
        spinMatrix.translate(0.5f, 0.5f, 0.5f);
        spinMatrix.rotate((float) Math.toRadians(tile.renderAngle), 0, 1, 0);
        spinMatrix.translate(-0.5f, -0.5f, -0.5f);

        final List<BlockStateModelPart> shaftState = model(TurbineRotorShaft.INSTANCE.defaultBlockState());
        final List<BlockStateModelPart> bladeState = model(TurbineRotorBlade.INSTANCE.defaultBlockState());
        final var bearingPos = tile.getBlockPos();
        var cached = layouts.get(tile);
        if (cached != null && cached.revision == tile.rotorRevision && cached.joke == TurbineRotorBearingTile.APRIL_FOOLS_JOKE &&
                cached.shafts == shaftState && cached.blades == bladeState) {
            var lightPos = new BlockPos.MutableBlockPos();
            for (var part : cached.parts) {
                lightPos.set(bearingPos.getX()+part.x,bearingPos.getY()+part.y,bearingPos.getZ()+part.z);
                state.parts.add(new Part(part.model,part.x,part.y,part.z,spinMatrix,part.local,
                        net.minecraft.util.LightCoordsUtil.getLightCoords(level,lightPos)));
            }
            return;
        }
        final var localMatrix = new Matrix4f();
        final var mutablePos = new BlockPos.MutableBlockPos();

        Vector3i worldPos = new Vector3i(bearingPos.getX(), bearingPos.getY(), bearingPos.getZ());
        for (Vector4i bladeCounts : rotorConfiguration) {
            worldPos.add(rotationAxis);

            localMatrix.identity();
            drawPart(state, shaftState, bearingPos, worldPos, spinMatrix, localMatrix, mutablePos);

            int i = 0;
            for (Direction direction : Direction.values()) {
                switch (direction) {
                    case UP, DOWN -> {
                        if (rotationAxis.y() != 0) {
                            continue;
                        }
                    }
                    case NORTH, SOUTH -> {
                        if (rotationAxis.z() != 0) {
                            continue;
                        }
                    }
                    case WEST, EAST -> {
                        if (rotationAxis.x() != 0) {
                            continue;
                        }
                    }
                }
                for (int j = 0; j < bladeCounts.get(i); j++) {
                    localMatrix.identity();
                    localMatrix.translate(0.5f, 0.5f, 0.5f);
                    localMatrix.rotate((float) Math.toRadians(180 * (i & 1)), 0, 1, 0);
                    localMatrix.rotate((float) Math.toRadians(blade180RotationMultiplier * 135 * (i & 2)), 0, 1, 0);
                    localMatrix.translate(-0.5f, -0.5f, -0.5f);
                    localMatrix.translate(0, 0, -(j + 1));
                    localMatrix.translate(0.5f, 0.5f, 0.5f);
                    localMatrix.rotate((float) Math.toRadians(180), 0, 0, 1);
                    localMatrix.translate(-0.5f, -0.5f, -0.5f);

                    var instancePos = new Vector3i();
                    if (TurbineRotorBearingTile.APRIL_FOOLS_JOKE) {
                        var pos = new Vector4f(0.5f, 0.5f, -(j + 1) + 0.5f, 1).mul(localMatrix).sub(0.5f, 0.5f, 0.5f, 0);
                        instancePos.add((int) pos.x, (int) pos.y, (int) pos.z);
                        if (rotationAxis.x() != 0) {
                            instancePos.x ^= instancePos.y;
                            instancePos.y ^= instancePos.x;
                            instancePos.x ^= instancePos.y;
                        } else if (rotationAxis.z() != 0) {
                            instancePos.z ^= instancePos.y;
                            instancePos.y ^= instancePos.z;
                            instancePos.z ^= instancePos.y;
                        }
                    }
                    instancePos.add(worldPos);

                    drawPart(state, bladeState, bearingPos, instancePos, spinMatrix, localMatrix, mutablePos);
                }
                i++;
            }
        }
        layouts.put(tile,new Layout(tile.rotorRevision,TurbineRotorBearingTile.APRIL_FOOLS_JOKE,shaftState,bladeState,List.copyOf(state.parts)));
    }

    private static void advanceSpin(TurbineRotorBearingTile tile) {
        final long now = System.nanoTime();
        if (tile.lastRenderNanos != 0) {
            double speed = tile.speed / 10f;
            if (speed > 0.001f) {
                double elapsedMillis = (now - tile.lastRenderNanos) / 1_000_000.0;
                // RPM * time in minutes * 360 degrees per rotation
                tile.renderAngle = (tile.renderAngle + speed * (elapsedMillis / 60000.0) * 360.0) % 360.0;
            }
        }
        tile.lastRenderNanos = now;
    }

    private static void drawPart(State out, List<BlockStateModelPart> model, BlockPos bearingPos, Vector3i partPos, Matrix4f spinMatrix, Matrix4f localMatrix,
                                 BlockPos.MutableBlockPos mutablePos) {
        final var level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        mutablePos.set(partPos.x, partPos.y, partPos.z);
        out.parts.add(new Part(model, partPos.x - bearingPos.getX(), partPos.y - bearingPos.getY(), partPos.z - bearingPos.getZ(),
                spinMatrix, new Matrix4f(localMatrix), net.minecraft.util.LightCoordsUtil.getLightCoords(level, mutablePos)));
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return false;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

    @Override
    public AABB getRenderBoundingBox(TurbineRotorBearingTile tile) {
        if (tile.AABB != null) {
            return tile.AABB.inflate(1);
        }
        return new AABB(tile.getBlockPos());
    }
}
