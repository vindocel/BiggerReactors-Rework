package biggerreactorsrework.thermal;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// Added 2026-11-02: structural, conservation and low-power regressions from the thermal contract.
class ThermalModelTest {
    private static ThermalModel.Geometry layout(int x, int y, int z, boolean missing) {
        return ThermalModel.geometry(x, y, z, (a, b, c) -> {
            if (missing && a == 1 && b == 0 && c == 2) return ThermalModel.Cell.EMPTY;
            return (a + c) % 2 == 0 ? ThermalModel.Cell.ROD : ThermalModel.Cell.MANIFOLD;
        });
    }

    @Test void completeCheckerboardsHaveNoStructuralLossAtTheirWalls() {
        for (int[] size : new int[][]{{3,3,3}, {5,5,5}, {9,9,9}, {4,2,7}, {1,3,2}}) {
            var g = layout(size[0], size[1], size[2], false);
            assertEquals(1, g.efficiency());
            assertEquals(g.volume(), g.rods() + g.usefulManifolds());
            for (double power : new double[]{.0001, 1, 500, 1e9})
                assertEquals(power, ThermalModel.heat(power, power, 0, g.efficiency()).capturable());
        }
    }

    @Test void missingManifoldKeepsSeventyFivePercentAtAnyPowerAndInsertion() {
        var g = layout(3, 1, 3, true);
        assertEquals(5, g.rods()); assertEquals(3, g.usefulManifolds());
        assertEquals(.75, g.contactEfficiency()); assertEquals(8d / 9, g.occupancy());
        assertEquals(.75, g.efficiency());
        assertEquals(750, ThermalModel.heat(1000, 750, 0, g.efficiency()).capturable());
        assertEquals(375, ThermalModel.heat(500, 375, 0, g.efficiency()).capturable());
        // Only the perfect upper rods burn: global structural cap still applies.
        assertEquals(150, ThermalModel.heat(200, 200, 0, g.efficiency()).capturable());
        var tall = layout(3, 3, 3, true);
        assertEquals(1 - .25 / 3, tall.efficiency(), 1e-12);
    }

    @Test void remoteManifoldsDoNotAddCapacityAndInternalObstaclesDoNotChangeDenominators() {
        var g = ThermalModel.geometry(5,1,1,(x,y,z) -> switch(x) {
            case 0 -> ThermalModel.Cell.ROD;
            case 1,4 -> ThermalModel.Cell.MANIFOLD;
            default -> ThermalModel.Cell.EMPTY;
        });
        assertEquals(1, g.usefulManifolds()); assertEquals(.4, g.efficiency());
        assertEquals(0, ThermalModel.heat(100,0,100,1).capturable());
        var noSpace = layout(1,1,1,false);
        assertEquals(0, noSpace.efficiency());
        assertEquals(0, ThermalModel.geometry(0,0,0,(x,y,z)->ThermalModel.Cell.EMPTY).efficiency());
    }

    @Test void fixedFuelHeatAndSmoothSizeCurveDoNotCreateEnergy() {
        var p = ThermalModel.Preset.DEFAULT;
        assertEquals(4.5217391304, 100*p.burnPerRod(15), 1e-8);
        assertEquals(5.5757575758, 100*p.burnPerRod(65), 1e-8);
        assertEquals(7.1471215352, 100*p.burnPerRod(369), 1e-8);
        for(int n=1;n<1000;n++) {
            assertTrue(p.burnPerRod(n) >= .04 && p.burnPerRod(n) < .08);
            assertTrue(p.burnPerRod(n+1) > p.burnPerRod(n));
        }
        for(double fill : new double[]{1,.5,.1}) for(double insertion : new double[]{0,25,42,50,75,100}) {
            double expected=65*p.burnPerRod(65)*fill*(1-insertion/100);
            assertEquals(expected,p.requestedBurn(4000*65*fill,65,65*(1-insertion/100)),1e-9);
        }
        assertEquals(0,p.requestedBurn(4000,0,0));
        assertEquals(1e-12,p.requestedBurn(1e-12,1,1));
    }

    @Test void conversionLossesPartitionHeatWithoutStoringRounding() {
        for(long cold : new long[]{0,3,20}) for(long space : new long[]{0,2,30}) {
            var c=ThermalModel.convert(107,cold,space,10,2);
            assertEquals(107,c.useful()+c.processLoss()+c.missingCoolant()+c.blockedOutput()+c.rounding(),1e-12);
            assertTrue(c.volume()<=cold && c.volume()<=space);
        }
        assertEquals(0,ThermalModel.convert(9,100,100,10,2).volume());
        assertEquals(0,ThermalModel.convert(1,100,100,10,2).volume());
        assertEquals(80,ThermalModel.convert(100,10,10,10,2).processLoss());
    }

    @Test void exchangerDistanceAndPassiveCapsAreBounded() {
        assertEquals(1,ThermalModel.gapFactor(0)); assertEquals(.5,ThermalModel.gapFactor(1));
        assertEquals(.25,ThermalModel.gapFactor(2)); assertEquals(.1,ThermalModel.gapFactor(3));
        assertEquals(0,ThermalModel.gapFactor(4));
        assertEquals(0,ThermalModel.PassiveClass.NONE.coolingWeight);
        for(var c:ThermalModel.PassiveClass.values()) {
            assertTrue(c.coolingWeight<=1); assertTrue(c.rfPerKJ<=.24);
        }
        assertThrows(IllegalArgumentException.class,()->ThermalModel.convert(1,1,1,10,11));
    }

    @Test void balancedOutputsRedistributeRefusalsAndRotateIntegerRemainders() {
        long[] received=new long[3];int cursor=0;
        var targets=java.util.List.<java.util.function.LongUnaryOperator>of(n->{received[0]+=n;return n;},
                n->0,n->{received[2]+=n;return n;});
        for(int i=0;i<12;i++) {
            var result=BalancedOutput.distribute(7,targets,cursor);cursor=result.nextCursor();
            assertEquals(7,result.accepted());
        }
        assertEquals(0,received[1]);assertEquals(84,received[0]+received[2]);
        assertTrue(Math.abs(received[0]-received[2])<=2);
        assertEquals(0,BalancedOutput.distribute(20,java.util.List.of(n->0,n->0),0).accepted());
    }

    @Test void exchangerScansOnlyTheFirstUnobstructedOppositeChannel() {
        var hot=java.util.List.of(new ExchangerLinks.Position(0,0,0));
        for(int gap=0;gap<=4;gap++) {
            final int distance=gap+1;
            var links=ExchangerLinks.scan(hot,(x,y,z)-> y!=0 || z!=0 || x<=0?3:x==distance?2:0);
            assertEquals(ThermalModel.gapFactor(gap),links.weighted(),1e-12);
        }
        assertEquals(0,ExchangerLinks.scan(hot,(x,y,z)->x==1?1:x==2?2:3).count());
        assertEquals(0,ExchangerLinks.scan(hot,(x,y,z)->x==1?3:x==2?2:0).count());
        assertEquals(1,ExchangerLinks.scan(hot,(x,y,z)->y!=0 || z!=0 || x<=0?3:x==1 || x==3?2:0).count());
    }
}
