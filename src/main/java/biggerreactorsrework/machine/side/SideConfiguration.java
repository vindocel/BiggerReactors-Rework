package biggerreactorsrework.machine.side;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.List;

/**
 * Per-transmission side configuration of a single-block machine. Every face starts with the machine's default route.
 */
public final class SideConfiguration {
    private static final int FACES = RelativeFace.VALUES.length;
    /**
     * Bits per face when a transmission's routes are packed in an int for the screen.
     */
    private static final int PACK_BITS = 4;

    private final SideRules rules;
    private final SideRoute[][] routes = new SideRoute[Transmission.VALUES.length][FACES];

    public SideConfiguration(SideRules rules) {
        this.rules = rules;
        resetToDefaults();
    }

    /**
     * Applies the machine's default routes (for example energy accepted on every allowed face).
     */
    public void resetToDefaults() {
        for (Transmission t : Transmission.VALUES) {
            for (RelativeFace face : RelativeFace.VALUES) {
                routes[t.ordinal()][face.ordinal()] = rules.defaultRoute(face, t);
            }
        }
    }

    public SideRules rules() {
        return rules;
    }

    public SideRoute get(RelativeFace face, Transmission transmission) {
        SideRoute route = routes[transmission.ordinal()][face.ordinal()];
        List<SideRoute> allowed = rules.allowed(face, transmission);
        if (allowed.contains(route)) {
            return route;
        }
        SideRoute fallback = rules.defaultRoute(face, transmission);
        return allowed.contains(fallback) ? fallback : SideRoute.NONE;
    }

    public boolean set(RelativeFace face, Transmission transmission, SideRoute route) {
        if (!rules.allowed(face, transmission).contains(route)) {
            return false;
        }
        routes[transmission.ordinal()][face.ordinal()] = route;
        return true;
    }

    /**
     * Moves to the next (or previous) supported route of that face/transmission.
     */
    public boolean cycle(RelativeFace face, Transmission transmission, boolean forward) {
        List<SideRoute> allowed = rules.allowed(face, transmission);
        if (allowed.size() < 2) {
            return false;
        }
        int index = allowed.indexOf(get(face, transmission));
        int next = Math.floorMod(index + (forward ? 1 : -1), allowed.size());
        return set(face, transmission, allowed.get(next));
    }

    /**
     * Sets every configurable face of the transmission back to NONE (fixed routes are kept).
     */
    public boolean clear(Transmission transmission) {
        boolean changed = false;
        for (RelativeFace face : RelativeFace.VALUES) {
            if (rules.allowed(face, transmission).contains(SideRoute.NONE) && get(face, transmission) != SideRoute.NONE) {
                routes[transmission.ordinal()][face.ordinal()] = SideRoute.NONE;
                changed = true;
            }
        }
        return changed;
    }

    /**
     * Route the batch button moves every configurable face (two or more allowed routes) to: the next (or previous)
     * route after the one they all share, or NONE when they differ.
     */
    public SideRoute batchTarget(Transmission transmission, boolean forward) {
        SideRoute common = null;
        boolean[] supported = new boolean[SideRoute.VALUES.length];
        for (RelativeFace face : RelativeFace.VALUES) {
            List<SideRoute> allowed = rules.allowed(face, transmission);
            if (allowed.size() < 2) {
                continue;
            }
            for (SideRoute route : allowed) {
                supported[route.ordinal()] = true;
            }
            SideRoute current = get(face, transmission);
            if (common == null) {
                common = current;
            } else if (common != current) {
                return SideRoute.NONE;
            }
        }
        if (common == null) {
            return SideRoute.NONE;
        }
        int step = forward ? 1 : -1;
        for (int i = 1; i <= SideRoute.VALUES.length; i++) {
            SideRoute candidate = SideRoute.VALUES[Math.floorMod(common.ordinal() + i * step, SideRoute.VALUES.length)];
            if (supported[candidate.ordinal()]) {
                return candidate;
            }
        }
        return common;
    }

    /**
     * Sets {@code route} on every configurable face that allows it; returns whether anything changed.
     */
    public boolean applyAll(Transmission transmission, SideRoute route) {
        boolean changed = false;
        for (RelativeFace face : RelativeFace.VALUES) {
            List<SideRoute> allowed = rules.allowed(face, transmission);
            if (allowed.size() >= 2 && allowed.contains(route) && get(face, transmission) != route) {
                routes[transmission.ordinal()][face.ordinal()] = route;
                changed = true;
            }
        }
        return changed;
    }

    public boolean anyFace(Transmission transmission, SideRoute route) {
        for (RelativeFace face : RelativeFace.VALUES) {
            if (get(face, transmission) == route) {
                return true;
            }
        }
        return false;
    }

    /**
     * Routes of a transmission packed in an int ({@value #PACK_BITS} bits per face), for the screen.
     */
    public int pack(Transmission transmission) {
        int packed = 0;
        for (RelativeFace face : RelativeFace.VALUES) {
            packed |= get(face, transmission).ordinal() << (face.ordinal() * PACK_BITS);
        }
        return packed;
    }

    public void unpack(Transmission transmission, int packed) {
        for (RelativeFace face : RelativeFace.VALUES) {
            int ordinal = (packed >>> (face.ordinal() * PACK_BITS)) & ((1 << PACK_BITS) - 1);
            if (ordinal < SideRoute.VALUES.length) {
                routes[transmission.ordinal()][face.ordinal()] = SideRoute.VALUES[ordinal];
            }
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        for (Transmission t : Transmission.VALUES) {
            ListTag list = new ListTag();
            for (SideRoute route : routes[t.ordinal()]) {
                list.add(StringTag.valueOf(route.name()));
            }
            tag.put(t.name().toLowerCase(java.util.Locale.ROOT), list);
        }
        return tag;
    }

    /**
     * Loads saved routes; transmissions missing from the tag keep the machine defaults.
     */
    public void load(CompoundTag tag) {
        resetToDefaults();
        for (Transmission t : Transmission.VALUES) {
            ListTag list = tag.getList(t.name().toLowerCase(java.util.Locale.ROOT), Tag.TAG_STRING);
            SideRoute[] target = routes[t.ordinal()];
            for (int i = 0; i < target.length && i < list.size(); i++) {
                try {
                    target[i] = SideRoute.valueOf(list.getString(i));
                } catch (IllegalArgumentException ignored) {
                    // Unknown route name: keep the default for that face.
                }
            }
        }
    }
}
