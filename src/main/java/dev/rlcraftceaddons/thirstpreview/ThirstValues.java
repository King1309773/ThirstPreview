package dev.rlcraftceaddons.thirstpreview;

/** Per-use values; saturation is absolute SimpleDifficulty points. */
public final class ThirstValues {
    public final int amount;
    public final float saturation;
    public final float dirtyChance;

    public ThirstValues(int amount, float saturation, float dirtyChance) {
        this.amount = amount;
        this.saturation = saturation;
        this.dirtyChance = dirtyChance;
    }
}
