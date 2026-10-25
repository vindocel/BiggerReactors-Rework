package biggerreactorsrework.client.guide;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class MaterialTableRowsTest {
    @Test void equivalentAirAndMaterialAliasesAppearOnce(){
        var p=List.of(0.1,0.25,1.1,0.05);
        var rows=MaterialTableRows.distinct(List.of(new MaterialTableRows.Entry("air","Ar",p),new MaterialTableRows.Entry("air","Ar",p),new MaterialTableRows.Entry("air","Ar",p),new MaterialTableRows.Entry("block:steel","Aço",p),new MaterialTableRows.Entry("block:steel","Aço",p)));
        assertEquals(2,rows.size());
    }
    @Test void differentValuesRemainEvenWhenRoundedDisplayMatches(){
        assertEquals(2,MaterialTableRows.distinct(List.of(new MaterialTableRows.Entry("block:steel","Aço",List.of(1.00001)),new MaterialTableRows.Entry("block:steel","Aço",List.of(1.00002)))).size());
    }
    @Test void moderatorRankingUsesEfficiencyRatherThanNameOrAbsorption(){
        var high=new MaterialTableRows.Entry("a","A",List.of(0.1,0.9));
        var low=new MaterialTableRows.Entry("z","Z",List.of(0.95,0.2));
        assertEquals(List.of(low,high),MaterialTableRows.byEfficiency(List.of(high,low),1));
    }
    @Test void coilRankingUsesItsFirstPropertyAndKeepsStableTies(){
        var high=new MaterialTableRows.Entry("a","A",List.of(0.9,0.1));
        var low=new MaterialTableRows.Entry("z","Z",List.of(0.2,0.95));
        assertEquals(List.of(low,high),MaterialTableRows.byEfficiency(List.of(high,low),0));
        var second=new MaterialTableRows.Entry("b","B",List.of(0.2,0.95));
        assertEquals(List.of(second,low),MaterialTableRows.byEfficiency(List.of(low,second),0));
    }
}
