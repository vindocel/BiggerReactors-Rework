package biggerreactorsrework.machine.side;

import java.util.List;

/**
 * Which routes a machine offers for a face and transmission (an empty list means the face is locked).
 */
public interface SideRules {
    List<SideRoute> allowed(RelativeFace face, Transmission transmission);

    /**
     * Route a newly placed machine starts with on that face.
     */
    default SideRoute defaultRoute(RelativeFace face, Transmission transmission) {
        return SideRoute.NONE;
    }

    List<SideRoute> ITEM_BASIC = List.of(SideRoute.NONE, SideRoute.ITEM_INPUT, SideRoute.ITEM_OUTPUT, SideRoute.ITEM_INPUT_OUTPUT);
    List<SideRoute> FLUID_IN = List.of(SideRoute.NONE, SideRoute.FLUID_INPUT);
    List<SideRoute> ENERGY = List.of(SideRoute.NONE, SideRoute.ENERGY_INPUT);

    /**
     * Cyanite Reprocessor: FRONT locked; the other faces take cyanite in, give blutonium out, take water in and take
     * energy in. Every configurable face starts accepting energy.
     */
    SideRules CYANITE_REPROCESSOR = new SideRules() {
        @Override
        public List<SideRoute> allowed(RelativeFace face, Transmission transmission) {
            if (face == RelativeFace.FRONT) {
                return List.of();
            }
            return switch (transmission) {
                case ITEM -> ITEM_BASIC;
                case FLUID -> FLUID_IN;
                case ENERGY -> ENERGY;
            };
        }

        @Override
        public SideRoute defaultRoute(RelativeFace face, Transmission transmission) {
            return transmission == Transmission.ENERGY && face != RelativeFace.FRONT ? SideRoute.ENERGY_INPUT : SideRoute.NONE;
        }
    };
}
