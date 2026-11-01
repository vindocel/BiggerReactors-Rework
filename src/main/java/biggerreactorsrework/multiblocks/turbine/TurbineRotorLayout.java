package biggerreactorsrework.multiblocks.turbine;

import biggerreactorsrework.Config;
import biggerreactorsrework.multiblocks.turbine.blocks.*;
import biggerreactorsrework.multiblocks.turbine.tiles.TurbineBaseTile;
import biggerreactorsrework.multiblocks.turbine.state.TurbineShaftRotationState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Added 2026-10-31: server-authoritative blade placement using real blocks and native item transactions. */
// Modified 2026-11-01: infer blade space from the base and aligned bearings before walls or roof are closed.
public final class TurbineRotorLayout {
    private TurbineRotorLayout() {}
    private record Rotor(Direction.Axis axis, List<BlockPos> shafts) {}
    private record Row(BlockPos shaft, int flags) {}
    private record Selection(BlockPos pos, Direction arm) {}

    private static int limit(Direction.Axis axis) {
        return switch (axis) {
            case X -> Config.CONFIG.Turbine.MaxLength;
            case Y -> Config.CONFIG.Turbine.MaxHeight;
            case Z -> Config.CONFIG.Turbine.MaxWidth;
        };
    }
    public static List<Direction> arms(Direction.Axis axis) {
        return Arrays.stream(Direction.values()).filter(d -> d.getAxis() != axis).toList();
    }
    private static BlockState loaded(Level level, BlockPos pos) {
        return level.hasChunkAt(pos) ? level.getBlockState(pos) : null;
    }
    /** The unassembled shaft property may still be Y on a horizontal rotor. */
    private static Direction.Axis axis(Level level, BlockPos shaft) {
        var origin = loaded(level,shaft);
        if(origin==null || !origin.is(TurbineRotorShaft.INSTANCE)) return null;
        Direction.Axis found = null;
        for (Direction d : Direction.values()) {
            var s = loaded(level, shaft.relative(d));
            if (s == null) return null;
            if (s.is(TurbineRotorShaft.INSTANCE) || s.is(TurbineRotorBearing.INSTANCE)) {
                if (found != null && found != d.getAxis()) return null;
                found = d.getAxis();
            }
        }
        return found != null ? found : Direction.Axis.values()[level.getBlockState(shaft)
                .getValue(TurbineShaftRotationState.TURBINE_SHAFT_ROTATION_STATE_ENUM_PROPERTY).ordinal()];
    }
    public static Direction.Axis shaftAxis(Level level,BlockPos shaft) {return axis(level,shaft);}
    private static BlockPos shaft(Level level, BlockPos clicked) {
        var state = loaded(level, clicked);
        if (state == null) return null;
        if (state.is(TurbineRotorShaft.INSTANCE)) return clicked;
        for (Direction d : Direction.values()) {
            BlockPos p = clicked;
            for (int n = 0; n < limit(d.getAxis()); n++) {
                p = p.relative(d);
                var s = loaded(level, p);
                if (s == null) return null;
                if (s.is(TurbineRotorShaft.INSTANCE)) {
                    var axis = axis(level, p);
                    if (axis != null && (state.is(TurbineRotorBearing.INSTANCE) ? axis == d.getAxis() : axis != d.getAxis())) return p;
                    break;
                }
                if (!state.is(TurbineRotorBlade.INSTANCE) || !s.is(TurbineRotorBlade.INSTANCE)) break;
            }
        }
        return null;
    }
    private static Rotor rotor(Level level, BlockPos shaft) {
        var axis = axis(level, shaft);
        if (axis == null) return null;
        Direction positive = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
        BlockPos first = shaft;
        for (int n = 0; n < limit(axis); n++) {
            var state = loaded(level, first.relative(positive.getOpposite()));
            if (state == null) return null;
            if (!state.is(TurbineRotorShaft.INSTANCE)) break;
            first = first.relative(positive.getOpposite());
            if (n == limit(axis)-1) return null;
        }
        var shafts = new ArrayList<BlockPos>();
        for (int n = 0; n < limit(axis); n++) {
            BlockPos p = first.relative(positive, n);
            var state = loaded(level, p);
            if (state == null) return null;
            if (!state.is(TurbineRotorShaft.INSTANCE)) return new Rotor(axis, List.copyOf(shafts));
            if (axis(level, p) != axis) return null;
            shafts.add(p);
        }
        return null;
    }

    public static InteractionResult use(Level level, BlockPos clicked, Player player, InteractionHand hand) {
        var held = player.getItemInHand(hand);
        boolean taking = hand == InteractionHand.MAIN_HAND && player.isShiftKeyDown() && held.isEmpty();
        if (!taking && (!held.is(TurbineRotorBlade.INSTANCE.asItem()) || player.isShiftKeyDown())) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer)) return InteractionResult.FAIL;
        if (!player.getAbilities().mayBuild || player.isSpectator() || !level.mayInteract(player, clicked)) return fail(player, "permission");
        BlockPos shaft = shaft(level, clicked);
        Rotor rotor = shaft == null ? null : rotor(level, shaft);
        if (rotor == null || rotor.shafts.isEmpty()) return fail(player, "rotor");
        if (!(level.getBlockEntity(shaft) instanceof TurbineBaseTile tile) || tile.nullableController() == null)
            return fail(player, "shell");
        var controller = tile.controller();
        BlockPos min = new BlockPos(controller.min().x(), controller.min().y(), controller.min().z());
        BlockPos max = new BlockPos(controller.max().x(), controller.max().y(), controller.max().z());
        for (var a : Direction.Axis.values()) if (max.get(a)-min.get(a)+1 > limit(a) || max.get(a)-min.get(a) < 2)
            return fail(player, "shell");
        if (!taking && (!bearingEnds(level, rotor, controller, min, max)
                || !baseOutline(level, controller, min, max))) return fail(player, "shell");
        var rows = rows(level, rotor, min, max);
        if (rows == null) return fail(player, "unloaded");
        var order = orderedRows(rows, rotor.axis, shaft, player.position());
        if (order.isEmpty()) return fail(player, "blocked");
        Selection selected = next(level, order, rotor.axis, min, max, taking);
        if (selected == null) return fail(player, taking ? "empty" : "full");
        if (!level.mayInteract(player, selected.pos)) return fail(player, "permission");
        if (taking) {
            if (!remove(server, player, selected.pos, shaft, true)) return fail(player, "permission");
            return InteractionResult.SUCCESS;
        }
        // ItemStack.useOn invokes NeoForge's normal capture/event/rollback path, consuming exactly one on success.
        var anchor=selected.pos.relative(selected.arm.getOpposite());
        var hit = new BlockHitResult(Vec3.atCenterOf(anchor), selected.arm, anchor, false);
        InteractionResult result = held.useOn(new UseOnContext(player, hand, hit));
        if (!result.consumesAction()) return fail(player, "placement");
        return InteractionResult.SUCCESS;
    }

    private static boolean bearingEnds(Level level, Rotor rotor, TurbineMultiblockController controller, BlockPos min, BlockPos max) {
        Direction positive = Direction.fromAxisAndDirection(rotor.axis, Direction.AxisDirection.POSITIVE);
        var first = rotor.shafts.getFirst().relative(positive.getOpposite());
        var last = rotor.shafts.getLast().relative(positive);
        if (first.get(rotor.axis) != min.get(rotor.axis) || last.get(rotor.axis) != max.get(rotor.axis)) return false;
        for (var pos : List.of(first, last)) {
            var state = loaded(level, pos);
            if (state == null || !state.is(TurbineRotorBearing.INSTANCE)
                    || !(level.getBlockEntity(pos) instanceof TurbineBaseTile tile)
                    || tile.nullableController() != controller) return false;
        }
        return true;
    }
    private static boolean baseOutline(Level level, TurbineMultiblockController controller, BlockPos min, BlockPos max) {
        // Only the horizontal base perimeter defines the footprint; the rest of the shell may still be open.
        for (int x = min.getX(); x <= max.getX(); x++) for (int z = min.getZ(); z <= max.getZ(); z++) {
            if (x != min.getX() && x != max.getX() && z != min.getZ() && z != max.getZ()) continue;
            var pos = new BlockPos(x, min.getY(), z);
            var state = loaded(level, pos);
            if (state == null || !(state.getBlock() instanceof TurbineBaseBlock block) || !block.isGoodForExterior()
                    || !(level.getBlockEntity(pos) instanceof TurbineBaseTile tile)
                    || tile.nullableController() != controller) return false;
        }
        return true;
    }
    private static List<Row> rows(Level level, Rotor rotor, BlockPos min, BlockPos max) {
        var result = new ArrayList<Row>();
        var b = Direction.Axis.values()[(rotor.axis.ordinal()+1)%3];
        var c = Direction.Axis.values()[(rotor.axis.ordinal()+2)%3];
        for (var shaft : rotor.shafts) {
            if (shaft.get(b)<=min.get(b) || shaft.get(b)>=max.get(b) || shaft.get(c)<=min.get(c) || shaft.get(c)>=max.get(c)) return null;
            int flags = 0;
            var pos=new BlockPos.MutableBlockPos();
            for (int u=min.get(b)+1;u<max.get(b);u++) for(int v=min.get(c)+1;v<max.get(c);v++) {
                int w=shaft.get(rotor.axis);
                pos.set(rotor.axis==Direction.Axis.X?w:b==Direction.Axis.X?u:v,
                        rotor.axis==Direction.Axis.Y?w:b==Direction.Axis.Y?u:v,rotor.axis==Direction.Axis.Z?w:b==Direction.Axis.Z?u:v);
                var state=loaded(level,pos);
                if(state==null)return null;
                if(pos.equals(shaft)||state.isAir())continue;
                if(state.is(TurbineRotorBlade.INSTANCE) && (u==shaft.get(b)||v==shaft.get(c)))flags|=1;
                else flags|=2;
            }
            result.add(new Row(shaft,flags));
        }
        return result;
    }
    private static List<Row> orderedRows(List<Row> rows, Direction.Axis axis, BlockPos clickedShaft, Vec3 player) {
        int firstBlocked=rows.size(),lastBlocked=-1;
        for(int i=0;i<rows.size();i++)if((rows.get(i).flags&2)!=0){firstBlocked=Math.min(firstBlocked,i);lastBlocked=i;}
        int side=0;
        for(int i=0;i<rows.size();i++)if((rows.get(i).flags&1)!=0 && lastBlocked>=0) {
            int current=i<firstBlocked?-1:i>lastBlocked?1:0;
            if(current==0 || (side!=0&&side!=current))return List.of();
            side=current;
        }
        if(lastBlocked>=0 && side==0) {
            if(firstBlocked==0)side=1;
            else if(lastBlocked==rows.size()-1)side=-1;
            else side=clickedShaft.get(axis)<=rows.get(firstBlocked).shaft.get(axis)?-1:1;
        }
        boolean reverse=lastBlocked>=0?side>0:axis!=Direction.Axis.Y &&
                Vec3.atCenterOf(rows.getLast().shaft).distanceToSqr(player)<Vec3.atCenterOf(rows.getFirst().shaft).distanceToSqr(player);
        var result=new ArrayList<Row>();
        for(int i=0;i<rows.size();i++) {
            int index=reverse?rows.size()-1-i:i;
            if((rows.get(index).flags&2)!=0 || (lastBlocked>=0&&(side<0?index>=firstBlocked:index<=lastBlocked)))continue;
            result.add(rows.get(index));
        }
        return result;
    }
    private static Selection next(Level level,List<Row> rows,Direction.Axis axis,BlockPos min,BlockPos max,boolean taking) {
        var directions=arms(axis);
        Selection last=null;
        for(var row:rows) {
            int maxRadius=0;
            for(var d:directions)maxRadius=Math.max(maxRadius,capacity(row.shaft,d,min,max));
            for(int radius=1;radius<=maxRadius;radius++)for(var d:directions) {
                if(radius>capacity(row.shaft,d,min,max))continue;
                BlockPos p=row.shaft.relative(d,radius);
                if(taking) {if(level.getBlockState(p).is(TurbineRotorBlade.INSTANCE))last=new Selection(p,d);}
                else if(level.getBlockState(p).isAir())return new Selection(p,d);
            }
        }
        return last;
    }
    private static int capacity(BlockPos shaft,Direction d,BlockPos min,BlockPos max) {
        return d.getAxisDirection()==Direction.AxisDirection.POSITIVE?max.get(d.getAxis())-shaft.get(d.getAxis())-1:shaft.get(d.getAxis())-min.get(d.getAxis())-1;
    }
    private static InteractionResult fail(Player player,String reason) {
        player.sendOverlayMessage(Component.translatable("message.biggerreactorsrework.rotor."+reason));
        // The block handled this gesture. Consuming it prevents vanilla from trying an unrelated face placement.
        return InteractionResult.CONSUME;
    }
    /** Native placement, including manual placement, receives the actual arm orientation before assembly. */
    public static BlockState placementState(BlockPlaceContext context,BlockState fallback) {
        var level=context.getLevel();var target=context.getClickedPos();
        for(var toward:Direction.values())for(int r=1;r<limit(toward.getAxis());r++) {
            var p=target.relative(toward,r);var state=loaded(level,p);
            if(state==null)break;
            if(state.is(TurbineRotorShaft.INSTANCE)) {
                var axis=axis(level,p);
                if(axis==null||axis==toward.getAxis())break;
                return fallback.setValue(TurbineShaftRotationState.TURBINE_SHAFT_ROTATION_STATE_ENUM_PROPERTY,
                        TurbineShaftRotationState.values()[axis.ordinal()]).setValue(TurbineRotorBlade.BLADE_POSITION,arms(axis).indexOf(toward.getOpposite()));
            }
            if(!state.is(TurbineRotorBlade.INSTANCE))break;
        }
        return fallback;
    }
    public static void breakShaft(Level level,BlockPos pos,Player player) {
        if(!(level instanceof ServerLevel server)||!player.getAbilities().mayBuild)return;
        var axis=axis(level,pos);if(axis==null)return;
        for(var d:arms(axis)) {
            var blades=new ArrayList<BlockPos>();
            for(int r=1;r<limit(d.getAxis());r++) {
                var p=pos.relative(d,r);var state=loaded(level,p);
                if(state==null||!state.is(TurbineRotorBlade.INSTANCE))break;
                blades.add(p);
            }
            // Outside-in avoids leaving a detached outer blade after a protection cancellation.
            Collections.reverse(blades);
            for(var p:blades)if(!remove(server,player,p,pos,false))break;
        }
    }
    private static boolean remove(ServerLevel level,Player player,BlockPos pos,BlockPos dropOrigin,boolean taking) {
        var state=loaded(level,pos);
        if(state==null||!state.is(TurbineRotorBlade.INSTANCE)||!level.mayInteract(player,pos)
                ||net.neoforged.neoforge.common.CommonHooks.fireBlockBreak(level,
                    ((ServerPlayer)player).gameMode.getGameModeForPlayer(),player,pos,state).isCanceled())return false;
        boolean drops=!player.hasInfiniteMaterials()&&(taking||player.hasCorrectToolForDrops(state));
        var items=drops?(taking?List.of(new ItemStack(TurbineRotorBlade.INSTANCE)):Block.getDrops(state,level,pos,level.getBlockEntity(pos),player,player.getMainHandItem())):List.<ItemStack>of();
        if(!level.destroyBlock(pos,false,player))return false;
        for(var stack:items) {player.getInventory().add(stack);if(!stack.isEmpty())Block.popResource(level,dropOrigin,stack);}
        return true;
    }
}
