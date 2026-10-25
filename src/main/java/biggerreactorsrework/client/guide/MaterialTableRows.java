package biggerreactorsrework.client.guide;
import java.util.*;
/** Groups aliases only when all effective values agree. */
final class MaterialTableRows {
    record Entry(String material,String name,List<Double> values,String source){
        Entry(String material,String name,List<Double> values){this(material,name,values,"");}
        Entry{values=values.stream().map(v->v==0?0d:v).toList();}
    }
    private record Key(String material,List<Double> values){}
    static List<Entry> distinct(Collection<Entry> source){
        var result=new LinkedHashMap<Key,Entry>();
        for(var entry:source){
            if(entry.values().stream().anyMatch(v->!Double.isFinite(v)))continue;
            result.merge(new Key(entry.material(),entry.values()),entry,(a,b)->Comparator.comparing(Entry::name,String.CASE_INSENSITIVE_ORDER).thenComparing(Entry::source).compare(a,b)<=0?a:b);
        }
        return result.values().stream().sorted(Comparator.comparing(Entry::name,String.CASE_INSENSITIVE_ORDER).thenComparing(Entry::material)).toList();
    }
    static List<Entry> byEfficiency(Collection<Entry> source,int efficiencyColumn){
        return distinct(source).stream().sorted(Comparator.comparingDouble((Entry entry)->entry.values().get(efficiencyColumn))
            .thenComparing(Entry::name,String.CASE_INSENSITIVE_ORDER).thenComparing(Entry::source)).toList();
    }
    private MaterialTableRows(){}
}
