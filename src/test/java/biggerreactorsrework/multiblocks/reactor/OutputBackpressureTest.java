package biggerreactorsrework.multiblocks.reactor;

import biggerreactorsrework.core.util.OutputBackpressure;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Modified 2026-10-26. */
class OutputBackpressureTest {
    @Test void retainedOutputUsesALinearLimitAfterOneSecond() {
        for(int stock:new int[]{0,250,500,750}) {
            var pressure=new OutputBackpressure();
            for(int tick=0;tick<100;tick++)pressure.tick(stock,1000);
            assertEquals(1-stock/1000d,pressure.limit(),1e-12);
        }
    }
    @Test void completeExtractionInBatchesDoesNotPenalizeEitherTickOrder() {
        for(int period:new int[]{1,2,10,20})for(boolean afterProduction:new boolean[]{false,true}) {
            var pressure=new OutputBackpressure();long stock=0;
            for(int tick=0;tick<300;tick++) {
                if(!afterProduction && tick%period==0){stock=0;pressure.released(stock,10000);}
                assertEquals(1,pressure.tick(stock,10000),1e-12);
                stock+=100;
                if(afterProduction && tick%period==period-1){stock=0;pressure.released(stock,10000);}
            }
        }
    }
    @Test void FullOutputStopsImmediatelyAndRecoveryTakesAtMostOneSecond() {
        var pressure=new OutputBackpressure();
        assertEquals(0,pressure.tick(1000,1000));
        pressure.released(0,1000);
        for(int tick=0;tick<20;tick++)pressure.tick(0,1000);
        assertEquals(1,pressure.limit(),1e-12);
    }
    @Test void StableBacklogAndSaveLoadCannotEvadeTheLimit() {
        var pressure=new OutputBackpressure();for(int tick=0;tick<30;tick++)pressure.tick(800,1000);
        var restored=new OutputBackpressure();restored.load(pressure.save());
        assertEquals(.2,restored.tick(800,1000),1e-12);
        restored.released(0,1000);for(int tick=0;tick<20;tick++)restored.tick(0,1000);
        assertEquals(1,restored.limit(),1e-12);
    }
}
