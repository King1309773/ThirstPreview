package dev.rlcraftceaddons.thirstpreview.client;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class TooltipHydrationLayoutTest {
    @Test public void hydrationUsesFullAndHalfDrops() {
        assertEquals(4, TooltipHydrationLayout.forAmount(8).iconCount);
        TooltipHydrationLayout.Row row = TooltipHydrationLayout.forAmount(3);
        assertEquals(2, row.iconCount);
        assertEquals(36, row.textureU(0));
        assertEquals(45, row.textureU(1));
        assertEquals("", row.counter);
    }
    @Test public void tenDropsFitButLargerValuesUseExactCompactCount() {
        assertEquals(10, TooltipHydrationLayout.forAmount(20).iconCount);
        assertEquals(1, TooltipHydrationLayout.forAmount(21).iconCount);
        assertEquals("×10.5", TooltipHydrationLayout.forAmount(21).counter);
        assertEquals("×1073741823.5", TooltipHydrationLayout.forAmount(Integer.MAX_VALUE).counter);
    }
    @Test public void zeroAndLossAreDistinctAndDoNotOverflow() {
        assertEquals(0, TooltipHydrationLayout.forAmount(0).textureU(0));
        assertEquals("0", TooltipHydrationLayout.forAmount(0).counter);
        assertTrue(TooltipHydrationLayout.forAmount(-3).loss);
        assertEquals("-1.5", TooltipHydrationLayout.forAmount(-3).counter);
        assertEquals("Red tint requires the native grayscale drop, not the blue filled sprite", 9,
                TooltipHydrationLayout.forAmount(-3).textureU(0));
        assertEquals("-1073741824", TooltipHydrationLayout.forAmount(Integer.MIN_VALUE).counter);
    }
    @Test public void reservationIsMeasuredAndWrappingIsDetected() {
        String reserve = TooltipHydrationLayout.reservation(35, 4);
        assertEquals(TooltipHydrationLayout.MARKER + repeat('\u00a0', 9), reserve);
        assertTrue(TooltipHydrationLayout.complete("§7" + reserve, reserve));
        assertFalse(TooltipHydrationLayout.complete(reserve.substring(0, reserve.length()-1), reserve));
        assertNull(TooltipHydrationLayout.reservation(20, 0));
    }
    @Test public void finalWrappedLinesDetermineIndexAndAmbiguitySkipsDrawing() {
        String reserve = TooltipHydrationLayout.reservation(20, 4);
        assertEquals(3, TooltipHydrationLayout.findRow(Arrays.asList("title", "wrapped title", "food", reserve)));
        assertEquals(-1, TooltipHydrationLayout.findRow(Arrays.asList("title", reserve, reserve)));
        assertEquals(-1, TooltipHydrationLayout.findRow(Arrays.asList("title", "no hydration")));
    }
    @Test public void ninePixelIconsStayWithinMeasuredTooltipAndScreen() {
        assertEquals(51, TooltipHydrationLayout.iconTop(20, 40, 3, 4));
        assertEquals(-1, TooltipHydrationLayout.iconTop(20, 38, 3, 4));
        assertEquals(-1, TooltipHydrationLayout.iconTop(20, 40, 0, 4));
        assertTrue(TooltipHydrationLayout.fits(4, 51, 45, 9, 4, 20, 50, 40, 100, 100));
        assertFalse(TooltipHydrationLayout.fits(4, 51, 51, 9, 4, 20, 50, 40, 100, 100));
        assertFalse(TooltipHydrationLayout.fits(4, -1, 45, 9, 4, -32, 50, 40, 100, 100));
    }
    @Test public void preflightRejectsClippedAndWrappedRowsBeforeBlankTextIsDrawn() {
        String reserve = TooltipHydrationLayout.reservation(48, 4);
        List<String> lines = Arrays.asList("title", "details", reserve);
        TooltipHydrationLayout.Metrics metrics = new TooltipHydrationLayout.Metrics() {
            public int width(String value) { return value.replaceAll("§.", "").length() * 4; }
            public List<String> wrap(String value, int width) {
                if (width(value) <= width) return Collections.singletonList(value);
                return Arrays.asList(value.substring(0, value.length()-1), "§b\u00a0");
            }
        };
        assertTrue(TooltipHydrationLayout.preflight(lines, reserve, 48, 90, 80, 320, 240, -1, metrics));
        assertFalse(TooltipHydrationLayout.preflight(lines, reserve, 48, 90, 80, 320, 240, 20, metrics));
        assertFalse(TooltipHydrationLayout.preflight(lines, reserve, 48, 90, 80, 320, 20, -1, metrics));
        assertFalse(TooltipHydrationLayout.preflight(Arrays.asList("title", reserve, reserve), reserve,
                48, 90, 80, 320, 240, -1, metrics));
    }
    private String repeat(char c, int count) { char[] chars = new char[count]; Arrays.fill(chars, c); return new String(chars); }
}
