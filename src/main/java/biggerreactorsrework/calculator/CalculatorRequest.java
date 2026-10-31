package biggerreactorsrework.calculator;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;

import javax.annotation.Nullable;

/**
 * Target in FE/t (shown as RF/t, the same unit in this mod) and the player's material choices: a moderator for a
 * solid reactor, or a coolant and a coil for a liquid-cooled plant.
 */
public record CalculatorRequest(long target, String fuel, boolean liquid, String material, String coil, String fingerprint) {
    public static final long MAX_TARGET = 1_000_000_000L;
    private static final int MAX_ID = 256;

    public CompoundTag toTag() {
        var tag = new CompoundTag();
        tag.putLong("target", target);
        tag.putString("fuel", fuel);
        tag.putBoolean("liquid", liquid);
        tag.putString("material", material);
        tag.putString("coil", coil);
        tag.putString("fingerprint", fingerprint);
        return tag;
    }

    /** Null when the tag is malformed or out of range. */
    @Nullable
    public static CalculatorRequest fromTag(CompoundTag tag) {
        long target = tag.getLongOr("target", 0L);
        String material = tag.getStringOr("material", ""), coil = tag.getStringOr("coil", ""), fingerprint = tag.getStringOr("fingerprint", "");
        String fuel = tag.getStringOr("fuel", "");
        if (target <= 0 || target > MAX_TARGET || material.length() > MAX_ID || coil.length() > MAX_ID || fuel.length() > MAX_ID || fingerprint.length() > 64) return null;
        if (Identifier.tryParse(material) == null || Identifier.tryParse(fuel) == null) return null;
        boolean liquid = tag.getBooleanOr("liquid", false);
        if (liquid && Identifier.tryParse(coil) == null) return null;
        return new CalculatorRequest(target, fuel, liquid, material, liquid ? coil : "", fingerprint);
    }
}
