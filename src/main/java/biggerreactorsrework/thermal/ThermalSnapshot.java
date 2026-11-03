package biggerreactorsrework.thermal;

// Added 2026-11-02: immutable per-tick telemetry. Heat quantities are kJ/t; power quantities are RF/t.
public record ThermalSnapshot(double generated, double capturable, double isolated, double structural,
                              double processed, double useful, double processLoss, double missingCoolant,
                              double blockedOutput, double rounding, double structuralEfficiency,
                              double contactEfficiency, double occupancy, long rods, long usefulManifolds,
                              double usableFuel, double burnPerRod, double requestedBurn,
                              double potentialRF, long acceptedRF, double discardedRF) {
    public static final ThermalSnapshot EMPTY = new ThermalSnapshot(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0);
    public java.util.Map<String,Object> values() {
        var result=new java.util.LinkedHashMap<String,Object>();
        for(var component:ThermalSnapshot.class.getRecordComponents())try {
            result.put(component.getName(),component.getAccessor().invoke(this));
        } catch(ReflectiveOperationException error) { throw new IllegalStateException(error); }
        return result;
    }
}

