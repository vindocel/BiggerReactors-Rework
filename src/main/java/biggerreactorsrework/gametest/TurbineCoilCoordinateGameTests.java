package biggerreactorsrework.gametest;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.calculator.design.*;
import biggerreactorsrework.multiblocks.turbine.tiles.TurbineBaseTile;
import biggerreactorsrework.multiblocks.turbine.blocks.TurbineTerminal;
import biggerreactorsrework.multiblocks.turbine.state.TurbineShaftRotationState;
import biggerreactorsrework.multiblocks.turbine.simulation.modern.ModernTurbineSimulation;
import biggerreactorsrework.registries.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.gametest.*;
import java.util.*;
import static biggerreactorsrework.core.multiblock.IAssemblyStateBlock.ASSEMBLED;

// Added 2026-11-02: real turbines in every orientation must retain the calculator's exact coil radii.
@GameTestHolder(BiggerReactors.modid) @PrefixGameTestTemplate(false)
public final class TurbineCoilCoordinateGameTests {
    private static BlockPos orient(BlockPos p,int direction,int width,int height,int depth) {
        int x=p.getX(),y=p.getY(),z=p.getZ();
        return switch(direction) {
            case 0 -> new BlockPos(y,x,z);
            case 1 -> new BlockPos(height-1-y,width-1-x,z);
            case 2 -> new BlockPos(x,y,z);
            case 3 -> new BlockPos(width-1-x,height-1-y,z);
            case 4 -> new BlockPos(x,z,y);
            default -> new BlockPos(x,depth-1-z,height-1-y);
        };
    }
    @GameTest(template="empty_64x48x40",timeoutTicks=300)
    public static void allSixBuiltOrientationsMatchExactTurbineGeometry(GameTestHelper h) {
        var design=new TurbineDesign(2,2,0,9,Blocks.IRON_BLOCK,1000);
        var layout=design.layout(List.of(new MachineLayout.Port(new BlockPos(0,1,2),MachineLayout.Role.TURBINE_STEAM_IN),
                new MachineLayout.Port(new BlockPos(0,2,2),MachineLayout.Role.TURBINE_WATER_OUT)));
        var terminals=new ArrayList<BlockPos>();
        for(int direction=0;direction<6;direction++) {
            var origin=new BlockPos(1+direction*10,1,1);int which=direction;
            layout.blocks().forEach((p,state)->{
                var target=orient(p,which,layout.width(),layout.height(),layout.depth()).offset(origin);
                var axis=TurbineShaftRotationState.TURBINE_SHAFT_ROTATION_STATE_ENUM_PROPERTY;
                if(state.hasProperty(axis))state=state.setValue(axis,TurbineShaftRotationState.values()[which/2]);
                h.setBlock(target,state);if(state.is(TurbineTerminal.INSTANCE))terminals.add(target);
            });
        }
        h.startSequence().thenWaitUntil(()->{
            for(var terminal:terminals)h.assertTrue(h.getBlockState(terminal).getValue(ASSEMBLED),"Oriented turbine not assembled");
        }).thenExecute(()->{
            var steam=FluidTransitionRegistry.liquidTransition(Fluids.WATER);var coil=TurbineCoilRegistry.getCoilData(Blocks.IRON_BLOCK);
            var reference=design.simulation(coil);var actual=new ArrayList<ModernTurbineSimulation>();
            for(var terminal:terminals){var s=(ModernTurbineSimulation)((TurbineBaseTile)h.getBlockEntity(terminal)).controller().simulation();s.setNominalFlowRate(1000);s.setActive(true);s.setCoilEngaged(true);actual.add(s);}
            for(var fieldName:List.of("x","y","z","coilSize","coilFESum","coilCapacity","rotorCapacityPerRPM","rotorShafts","rotorAxialMass","rotorMass","linearBladeMetersPerRevolution","maxFlowRate"))try {
                var field=ModernTurbineSimulation.class.getDeclaredField(fieldName);field.setAccessible(true);
                var expectedValue=field.get(reference);
                for(var simulation:actual)h.assertTrue(expectedValue.equals(field.get(simulation)),
                        "Assembled turbine geometry differs from calculator: "+fieldName);
            } catch(ReflectiveOperationException e){throw new AssertionError(e);}
            double expected=0;double[] outputs=new double[actual.size()];
            for(int tick=0;tick<50000;tick++) {
                reference.idealSupply(steam);reference.tick();long value=reference.battery().extract(Long.MAX_VALUE);
                if(tick>=46000)expected+=value;
                for(int i=0;i<actual.size();i++){var s=actual.get(i);s.idealSupply(steam);s.tick();long power=s.battery().extract(Long.MAX_VALUE);if(tick>=46000)outputs[i]+=power;}
            }
            h.assertTrue(expected>0,"Reference turbine produced no power");
            for(int i=0;i<outputs.length;i++) {
                BiggerReactors.LOGGER.info("Exact coil orientation {}: actual={} reference={} RF/t; RPM={}",i,outputs[i]/4000,expected/4000,actual.get(i).RPM());
                h.assertTrue(Math.abs(outputs[i]/expected-1)<.001,"World coordinates/orientation changed coil output: "+i);
            }
        }).thenSucceed();
    }
}
