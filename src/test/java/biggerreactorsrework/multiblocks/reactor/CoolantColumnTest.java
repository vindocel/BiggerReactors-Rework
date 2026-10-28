package biggerreactorsrework.multiblocks.reactor;

import biggerreactorsrework.core.util.CoolantColumn;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CoolantColumnTest {
    @Test void abruptStockDropsLeaveNoGapOrOverlapAlongTheColumn() {
        for(double[] stocks:new double[][]{{.8,.2},{.08,.2},{0,.2},{.08,0},{.000001,.000002},{1,1}})
            for(int i=0;i<7;i++) {
                var cold=CoolantColumn.slice(stocks[0],stocks[1],i,7,false);
                var hot=CoolantColumn.slice(stocks[0],stocks[1],i,7,true);
                assertEquals(0,hot.bottom());assertEquals(1,cold.top());
                assertEquals(hot.top(),cold.bottom(),1e-6,"fluid boundary left an empty interval");
            }
    }
    @Test void fullColdHasNoGapsBetweenBlocks() {
        for(int i=0;i<4;i++) {
            var slice=CoolantColumn.slice(1,0,i,4,false);
            assertEquals(0,slice.bottom());assertEquals(1,slice.top());
        }
    }
    @Test void halfHotHalfColdSharesTheColumnRatherThanEachBlock() {
        assertEquals(1,CoolantColumn.slice(.5,.5,0,3,true).top());
        assertEquals(.5,CoolantColumn.slice(.5,.5,1,3,true).top());
        assertEquals(.5,CoolantColumn.slice(.5,.5,1,3,false).bottom());
        assertEquals(0,CoolantColumn.slice(.5,.5,2,3,false).bottom());
        assertEquals(0,CoolantColumn.slice(.5,.5,2,3,true).top());
    }
    @Test void independentBuffersCannotOverlapVisually() {
        for(int i=0;i<5;i++) {
            var hot=CoolantColumn.slice(1,1,i,5,true);
            var cold=CoolantColumn.slice(1,1,i,5,false);
            assertEquals(1,(hot.top()-hot.bottom())+(cold.top()-cold.bottom()),1e-6);
            assertTrue(hot.top()<=cold.bottom());
        }
        assertEquals(1,CoolantColumn.slice(0,0,0,1,false).bottom());
        assertEquals(0,CoolantColumn.slice(0,0,0,1,true).top());
    }
}
