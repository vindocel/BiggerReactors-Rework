package biggerreactorsrework.multiblocks.turbine.client;

import biggerreactorsrework.multiblocks.turbine.blocks.TurbineRotorBlade;
import biggerreactorsrework.multiblocks.turbine.blocks.TurbineRotorShaft;
import biggerreactorsrework.multiblocks.turbine.tiles.TurbineRotorBearingTile;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.data.ModelData;
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
public class TurbineRotorRenderer implements BlockEntityRenderer<TurbineRotorBearingTile> {
    // Modified 2026-10-31: keep baked parts and static rotor transforms between frames, not world lighting.
    private record Baked(BlockState state, BakedModel model, List<RenderType> layers) {}
    private record Part(Baked baked, int x, int y, int z, Matrix4f local) {}
    private record Layout(int revision, boolean joke, BakedModel shaft, BakedModel blade, List<Part> parts) {}
    private final java.util.Map<TurbineRotorBearingTile, Layout> layouts = new java.util.WeakHashMap<>();

    public TurbineRotorRenderer(BlockEntityRendererProvider.Context context) {
    }

    private static Baked baked(BlockState state) {
        var model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
        var layers = new ArrayList<RenderType>();
        model.getRenderTypes(state, RandomSource.create(42), ModelData.EMPTY).forEach(layers::add);
        return new Baked(state, model, List.copyOf(layers));
    }

    @Override
    public void render(TurbineRotorBearingTile tile, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        final var rotationAxis = tile.rotationAxis;
        final var rotorConfiguration = tile.rotorConfiguration;
        final var level = tile.getLevel();
        if (!tile.isRenderBearing || rotationAxis == null || rotorConfiguration == null || level == null) {
            return;
        }

        advanceSpin(tile);
        TurbineAtmosphereEffects.observe(tile);

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

        final var bearingPos = tile.getBlockPos();
        final var dispatcher = Minecraft.getInstance().getBlockRenderer();
        final var shaftModel = dispatcher.getBlockModel(TurbineRotorShaft.INSTANCE.defaultBlockState());
        final var bladeModel = dispatcher.getBlockModel(TurbineRotorBlade.INSTANCE.defaultBlockState());
        var layout = layouts.get(tile);
        if (layout == null || layout.revision != tile.rotorRevision || layout.joke != TurbineRotorBearingTile.APRIL_FOOLS_JOKE ||
                layout.shaft != shaftModel || layout.blade != bladeModel) {
            layout = new Layout(tile.rotorRevision, TurbineRotorBearingTile.APRIL_FOOLS_JOKE, shaftModel, bladeModel,
                    layoutParts(bearingPos, rotationAxis, rotorConfiguration, blade180RotationMultiplier));
            layouts.put(tile, layout);
        }

        final var lightPos = new BlockPos.MutableBlockPos();
        final var modelRenderer = dispatcher.getModelRenderer();
        for (Part part : layout.parts) {
            lightPos.set(bearingPos.getX() + part.x, bearingPos.getY() + part.y, bearingPos.getZ() + part.z);
            final int light = LevelRenderer.getLightColor(level, lightPos);
            poseStack.pushPose();
            poseStack.translate(part.x, part.y, part.z);
            poseStack.mulPose(spinMatrix);
            poseStack.mulPose(part.local);
            for (RenderType layer : part.baked.layers) {
                modelRenderer.renderModel(poseStack.last(), buffers.getBuffer(layer), part.baked.state, part.baked.model,
                        1, 1, 1, light, packedOverlay, ModelData.EMPTY, layer);
            }
            poseStack.popPose();
        }

        final long now = System.nanoTime();
        if (tile.lastSteamNanos != 0) {
            tile.visibleSteam = biggerreactorsrework.util.TurbineAtmosphere.fade(tile.visibleSteam, tile.steamDensity, (now - tile.lastSteamNanos) / 1.0E9F);
        }
        tile.lastSteamNanos = now;
        if (tile.visibleSteam > 0) {
            TurbineSteamRenderer.draw(poseStack, buffers, tile.steamWindows, tile.visibleSteam,
                    biggerreactorsrework.core.util.SteamWindowMesh.frame(level.getGameTime() + partialTick));
        }
    }

    private static List<Part> layoutParts(BlockPos bearingPos, Vector3i rotationAxis, List<Vector4i> rotorConfiguration, int blade180RotationMultiplier) {
        final var shaft = baked(TurbineRotorShaft.INSTANCE.defaultBlockState());
        final var blade = baked(TurbineRotorBlade.INSTANCE.defaultBlockState());
        final var parts = new ArrayList<Part>();
        final var localMatrix = new Matrix4f();

        Vector3i worldPos = new Vector3i(bearingPos.getX(), bearingPos.getY(), bearingPos.getZ());
        for (Vector4i bladeCounts : rotorConfiguration) {
            worldPos.add(rotationAxis);

            localMatrix.identity();
            addPart(parts, shaft, bearingPos, worldPos, localMatrix);

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

                    addPart(parts, blade, bearingPos, instancePos, localMatrix);
                }
                i++;
            }
        }
        return List.copyOf(parts);
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

    private static void addPart(List<Part> parts, Baked baked, BlockPos bearingPos, Vector3i partPos, Matrix4f localMatrix) {
        parts.add(new Part(baked, partPos.x - bearingPos.getX(), partPos.y - bearingPos.getY(), partPos.z - bearingPos.getZ(), new Matrix4f(localMatrix)));
    }

    @Override
    public boolean shouldRenderOffScreen(TurbineRotorBearingTile tile) {
        return true;
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
