package dev.rlcraftceaddons.thirstpreview.client;

import java.util.Iterator;
import java.util.List;

/** Self-contained hydration metadata for tooltip callers that omit the ItemStack. */
public final class TooltipHydrationSnapshot {
    // Magic DEAD, format version 01. Every token is a zero-width legacy color code.
    private static final String HEADER = "§d§e§a§d§0§1";
    private static final String AQUA = "§b";
    private static final String HEX = "0123456789abcdef";
    private static final int AMOUNT_TOKENS = 8;
    private static final int ENCODED_LENGTH = TooltipHydrationLayout.MARKER.length()
            + HEADER.length() + AMOUNT_TOKENS * 2 + AQUA.length();

    private TooltipHydrationSnapshot() { }

    /** Adds the exact signed amount without changing the reservation's measured width. */
    public static String create(int amount, int width, int spaceWidth) {
        String reservation = TooltipHydrationLayout.reservation(width, spaceWidth);
        if (reservation == null || reservation.length() <= TooltipHydrationLayout.MARKER.length()) return null;
        StringBuilder row = new StringBuilder(reservation.length() + HEADER.length() + AMOUNT_TOKENS * 2 + 2);
        row.append(TooltipHydrationLayout.MARKER).append(HEADER);
        for (int shift = 28; shift >= 0; shift -= 4) {
            row.append('§').append(HEX.charAt((amount >>> shift) & 15));
        }
        return row.append(AQUA).append(reservation, TooltipHydrationLayout.MARKER.length(), reservation.length()).toString();
    }

    /** Ambiguous or missing rows never borrow state from another tooltip. */
    public static Integer decode(List<String> lines) {
        if (lines == null) return null;
        for (String line : lines) if (line == null) return null;
        int index = TooltipHydrationLayout.findRow(lines);
        return index < 0 ? null : decode(lines.get(index));
    }

    /** Accepts a complete versioned payload, including wrapped fragments with no NBSP padding. */
    public static Integer decode(String line) {
        if (line == null) return null;
        int padding = line.indexOf('\u00a0');
        if (padding < 0) padding = line.length();
        // Amount nibbles can contain MARKER too; locate the fixed payload from its padding boundary.
        int marker = padding - ENCODED_LENGTH;
        if (marker < 0 || !formattingPrefix(line, marker)
                || !line.startsWith(TooltipHydrationLayout.MARKER, marker)
                || !line.startsWith(HEADER, marker + TooltipHydrationLayout.MARKER.length())
                || !line.startsWith(AQUA, padding - AQUA.length())) return null;
        for (int i = padding; i < line.length(); i++) if (line.charAt(i) != '\u00a0') return null;
        int amount = 0;
        int start = marker + TooltipHydrationLayout.MARKER.length() + HEADER.length();
        for (int token = 0; token < AMOUNT_TOKENS; token++) {
            int position = start + token * 2;
            if (line.charAt(position) != '§') return null;
            int nibble = HEX.indexOf(line.charAt(position + 1));
            if (nibble < 0) return null;
            amount = (amount << 4) | nibble;
        }
        return amount;
    }

    /** Also removes legacy or damaged reservations so rebuilding cannot leave duplicate rows. */
    public static void removeRows(List<String> lines) {
        for (Iterator<String> iterator = lines.iterator(); iterator.hasNext();) {
            String line = iterator.next();
            if (line != null && line.contains(TooltipHydrationLayout.MARKER)) iterator.remove();
        }
    }

    public static void upsert(List<String> lines, String row) {
        removeRows(lines);
        if (row != null) lines.add(row);
    }

    private static boolean formattingPrefix(String line, int end) {
        if ((end & 1) != 0) return false;
        for (int i = 0; i < end; i += 2) {
            if (line.charAt(i) != '§'
                    || "0123456789abcdefklmnor".indexOf(Character.toLowerCase(line.charAt(i + 1))) < 0) return false;
        }
        return true;
    }
}
