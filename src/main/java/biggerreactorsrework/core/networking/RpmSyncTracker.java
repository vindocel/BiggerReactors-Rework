package biggerreactorsrework.core.networking;

/** Suppresses identical samples; moving rotors retain the existing ten tick cadence. */
public final class RpmSyncTracker {
    private double sent = Double.NaN;
    private long lastTick = Long.MIN_VALUE;
    public void reset() { sent=Double.NaN; lastTick=Long.MIN_VALUE; }
    public boolean sample(long tick,double rpm) {
        if(!Double.isFinite(rpm)||rpm<0)return false;
        boolean initial=Double.isNaN(sent);
        boolean zeroCrossing=!initial&&(rpm==0)!=(sent==0);
        boolean due=initial||zeroCrossing||tick-lastTick>=10;
        if(!due||Double.compare(rpm,sent)==0)return false;
        lastTick=tick;sent=rpm;return true;
    }
}
