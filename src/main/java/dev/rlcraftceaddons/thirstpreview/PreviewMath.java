package dev.rlcraftceaddons.thirstpreview;

public final class PreviewMath {
    private PreviewMath() { }
    public static int restoredLevel(int current, int amount) {
        return (int) Math.max(0L, Math.min(20L, (long) current + amount));
    }

    /** Slot zero is the rightmost droplet; each full droplet holds two points. */
    public static int fillForSlot(int level, int slot) {
        if (slot < 0 || slot >= 10) {
            return 0;
        }
        return Math.max(0, Math.min(2, level - slot * 2));
    }

    public static float dropletUnits(int amount) {
        return amount / 2.0F;
    }
}
