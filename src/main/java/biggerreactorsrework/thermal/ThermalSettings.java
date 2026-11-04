package biggerreactorsrework.thermal;

import biggerreactorsrework.core.config.ConfigValue;

// Added 2026-11-02: the single server-authoritative preset; changes require restart.
// Modified 2026-11-03: heat in kJ; renamed keys do not inherit values saved in the old unit.
public final class ThermalSettings {
    @ConfigValue(range="(0,)") public final double BaseBurn;
    @ConfigValue(range="[0,)") public final double SizeBurn;
    @ConfigValue(range="(0,)") public final double SizeConstant;
    @ConfigValue(range="(0,)") public final double KJPerFuel;
    @ConfigValue(range="(0,)") public final double KJPerCoolant;
    @ConfigValue(range="(0,)") public final double SteamBudgetKJ;
    @ConfigValue(range="[1,)") public final long HotMBPerLink;
    {
        BaseBurn=.04; SizeBurn=.04; SizeConstant=100; KJPerFuel=12500;
        KJPerCoolant=1; SteamBudgetKJ=.2; HotMBPerLink=2000;
    }

    public ThermalModel.Preset snapshot(long fuelCapacity, long coolantCapacity) {
        return new ThermalModel.Preset(fuelCapacity,coolantCapacity,BaseBurn,SizeBurn,SizeConstant,
                KJPerFuel,KJPerCoolant,SteamBudgetKJ,HotMBPerLink);
    }
}
