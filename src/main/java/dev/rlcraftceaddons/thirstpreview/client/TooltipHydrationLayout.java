package dev.rlcraftceaddons.thirstpreview.client;

import java.util.List;
import java.util.ArrayList;

/** Pure measurements in Forge's scaled GUI coordinates. */
public final class TooltipHydrationLayout {
    // Multiple color resets: continuation fragments inherit only the final aqua code.
    public static final String MARKER = "§0§1§2§3§b";
    private TooltipHydrationLayout() { }

    public static Row forAmount(int amount) {
        if (amount > 0 && amount <= 20) {
            return new Row(amount, (amount + 1) / 2, false, "");
        }
        String drops = Long.toString(Math.abs((long) amount) / 2L)
                + (Math.abs((long) amount) % 2L == 0 ? "" : ".5");
        return new Row(amount, 1, amount < 0, amount == 0 ? "0" : (amount < 0 ? "-" : "×") + drops);
    }

    public static String reservation(int width, int spaceWidth) {
        if (width <= 0 || spaceWidth <= 0) return null;
        int count = (width + spaceWidth - 1) / spaceWidth;
        StringBuilder result = new StringBuilder(MARKER);
        for (int i = 0; i < count; i++) result.append('\u00a0');
        return result.toString();
    }

    public static int findRow(List<String> lines) {
        int found = -1;
        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).contains(MARKER)) {
                if (found >= 0) return -1;
                found = i;
            }
        }
        return found;
    }

    public static boolean complete(String line, String expected) {
        if (expected == null || !line.endsWith(expected)) return false;
        String prefix = line.substring(0, line.length() - expected.length());
        return prefix.replaceAll("§[0-9a-fk-orA-FK-OR]", "").isEmpty();
    }

    public static int iconTop(int y, int height, int index, int count) {
        if (index < 1 || index >= count || (long) height != 10L * count) return -1;
        long top = (long) y + 10L * index + 1;
        return top < 0 || top > Integer.MAX_VALUE ? -1 : (int) top;
    }

    public static boolean fits(int x, int y, int width, int height,
                               int boxX, int boxY, int boxWidth, int boxHeight,
                               int screenWidth, int screenHeight) {
        return width > 0 && height > 0 && x >= 0 && y >= 0 && x >= boxX && y >= boxY
                && (long) x + width <= (long) boxX + boxWidth
                && (long) y + height <= (long) boxY + boxHeight
                && (long) x + width <= screenWidth && (long) y + height <= screenHeight;
    }

    public interface Metrics {
        int width(String text);
        List<String> wrap(String text, int width);
    }

    /** Forge 2860 GuiUtils preflight. Only native Forge still performs the real drawing. */
    public static boolean preflight(List<String> original, String expected, int rowWidth,
                                     int mouseX, int mouseY, int screenWidth, int screenHeight,
                                     int maxWidth, Metrics font) {
        if (expected == null || findRow(original) < 0) return false;
        int width = 0;
        for (String line : original) width = Math.max(width, font.width(line));
        boolean wrap = false;
        int x = mouseX + 12;
        if ((long) x + width + 4 > screenWidth) {
            x = mouseX - 16 - width;
            if (x < 4) {
                width = mouseX > screenWidth / 2 ? mouseX - 20 : screenWidth - 16 - mouseX;
                wrap = true;
            }
        }
        if (maxWidth > 0 && width > maxWidth) { width = maxWidth; wrap = true; }
        if (width <= 0) return false;
        List<String> lines = original;
        if (wrap) {
            lines = new ArrayList<>();
            int wrappedWidth = 0;
            for (String line : original) {
                for (String fragment : font.wrap(line, width)) {
                    lines.add(fragment);
                    wrappedWidth = Math.max(wrappedWidth, font.width(fragment));
                }
            }
            width = wrappedWidth;
            x = mouseX > screenWidth / 2 ? mouseX - 16 - width : mouseX + 12;
        }
        int index = findRow(lines);
        if (index < 0 || !complete(lines.get(index), expected)) return false;
        // Our row is always after the entire title group: its final height is 10*n.
        if (lines.size() > Integer.MAX_VALUE / 10) return false;
        int height = lines.size() * 10;
        int y = mouseY - 12;
        if (y < 4) y = 4;
        else if ((long) y + height + 4 > screenHeight) y = screenHeight - height - 4;
        if (x < 0 || y < 0 || (long) x + width > screenWidth || (long) y + height > screenHeight) return false;
        int top = iconTop(y, height, index, lines.size());
        return top >= 0 && fits(x, top, rowWidth, 9, x, y, width, height, screenWidth, screenHeight);
    }

    public static final class Row {
        private final int amount;
        public final int iconCount;
        public final boolean loss;
        public final String counter;
        private Row(int amount, int iconCount, boolean loss, String counter) {
            this.amount = amount;
            this.iconCount = iconCount;
            this.loss = loss;
            this.counter = counter;
        }
        public int textureU(int icon) {
            if (icon < 0 || icon >= iconCount) throw new IndexOutOfBoundsException();
            if (amount == 0) return 0;
            if (amount < 0) return 9; // Native grayscale silhouette: tinting the blue fill cannot produce red.
            return amount > 0 && amount <= 20 && icon * 2 + 1 == amount ? 45 : 36;
        }
        public int width(int counterWidth) {
            return iconCount * 9 + (counter.isEmpty() ? 0 : 3 + counterWidth);
        }
    }
}
