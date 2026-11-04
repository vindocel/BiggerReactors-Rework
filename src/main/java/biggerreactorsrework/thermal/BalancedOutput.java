package biggerreactorsrework.thermal;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongUnaryOperator;

// Added 2026-11-02: distribute equally, reoffer refusals, rotate indivisible remainders.
public final class BalancedOutput {
    public record Result(long accepted,int nextCursor) { }
    private record Sink(int index,LongUnaryOperator receive) { }
    public static Result distribute(long amount,List<LongUnaryOperator> destinations,int cursor) {
        if(amount<=0 || destinations.isEmpty())return new Result(0,cursor);
        int start=Math.floorMod(cursor,destinations.size());
        var active=new ArrayList<Sink>();
        for(int i=0;i<destinations.size();i++) {
            int index=(start+i)%destinations.size();active.add(new Sink(index,destinations.get(index)));
        }
        int nextCursor=start;
        long remaining=amount;
        while(remaining>0 && !active.isEmpty()) {
            long share=remaining/active.size(),extra=remaining%active.size();
            var next=new ArrayList<Sink>();
            for(int i=0;i<active.size();i++) {
                long offered=share+(i<extra?1:0);
                if(offered==0) { next.add(active.get(i));continue; }
                long accepted=Math.max(0,Math.min(offered,active.get(i).receive.applyAsLong(offered)));
                remaining-=accepted;
                if(i<extra && accepted>share)nextCursor=(active.get(i).index+1)%destinations.size();
                if(accepted==offered)next.add(active.get(i));
            }
            if(next.size()==active.size())break;
            active=next;
        }
        return new Result(amount-remaining,nextCursor);
    }
    private BalancedOutput() { }
}
