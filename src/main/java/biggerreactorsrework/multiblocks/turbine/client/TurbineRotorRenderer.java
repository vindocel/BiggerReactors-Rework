package biggerreactorsrework.multiblocks.turbine.client;

import biggerreactorsrework.multiblocks.turbine.blocks.TurbineRotorBlade;
import biggerreactorsrework.multiblocks.turbine.blocks.TurbineRotorShaft;
import biggerreactorsrework.multiblocks.turbine.tiles.TurbineRotorBearingTile;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;
import org.joml.Vector3i;
import org.joml.Vector4f;
import org.joml.Vector4i;

/**
 * Draws the spinning rotor (shafts and blades) of an assembled turbine from its bearing.
 * <p>
 * The transforms are the ones the rotor has always used: an orientation matrix for the rotor axis,
 * a spin about the rotor axis advanced from real frame time at {@code RPM / 10}, and a per-blade placement matrix.
 */
public class TurbineRotorRenderer implements BlockEntityRenderer<TurbineRotorBearingTile> {

    public TurbineRotorRenderer(BlockEntityRendererProvider.Context context) {
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

        final BlockState shaftState = TurbineRotorShaft.INSTANCE.defaultBlockState();
        final BlockState bladeState = TurbineRotorBlade.INSTANCE.defaultBlockState();
        final var bearingPos = tile.getBlockPos();
        final var localMatrix = new Matrix4f();
        final var mutablePos = new BlockPos.MutableBlockPos();

        Vector3i worldPos = new Vector3i(bearingPos.getX(), bearingPos.getY(), bearingPos.getZ());
        for (Vector4i bladeCounts : rotorConfiguration) {
            worldPos.add(rotationAxis);

            localMatrix.identity();
            drawPart(poseStack, buffers, packedOverlay, shaftState, bearingPos, worldPos, spinMatrix, localMatrix, mutablePos);

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

                    drawPart(poseStack, buffers, packedOverlay, bladeState, bearingPos, instancePos, spinMatrix, localMatrix, mutablePos);
                }
                i++;
            }
        }
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

    private static void drawPart(PoseStack poseStack, MultiBufferSource buffers, int packedOverlay, BlockState state,
                                 BlockPos bearingPos, Vector3i partPos, Matrix4f spinMatrix, Matrix4f localMatrix,
                                 BlockPos.MutableBlockPos mutablePos) {
        final var level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        mutablePos.set(partPos.x, partPos.y, partPos.z);
        final int light = LevelRenderer.getLightColor(level, mutablePos);
        poseStack.pushPose();
        poseStack.translate(partPos.x - bearingPos.getX(), partPos.y - bearingPos.getY(), partPos.z - bearingPos.getZ());
        poseStack.mulPose(spinMatrix);
        poseStack.mulPose(localMatrix);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, poseStack, buffers, light, packedOverlay, ModelData.EMPTY, null);
        poseStack.popPose();
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
