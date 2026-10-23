package dev.rlcraftceaddons.thirstpreview.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class TooltipHydrationSnapshotTest {
    @Test public void signedAmountsRoundTripWithoutItemOrWorldState() {
        int[] amounts = { Integer.MIN_VALUE, Integer.MAX_VALUE, -21, -3, 0, 1, 3, 20, 21, 0x0123b000 };
        for (int amount : amounts) {
            String row = TooltipHydrationSnapshot.create(amount, 35, 4);
            assertNotNull("A usable reservation is required for " + amount, row);
            assertEquals(Integer.valueOf(amount), TooltipHydrationSnapshot.decode(row));
            assertEquals(Integer.valueOf(amount), TooltipHydrationSnapshot.decode(Arrays.asList("title", row)));
        }
    }

    @Test public void encodedDataUsesOnlyInvisibleLegacyFormattingAndPreservesPadding() {
        String row = TooltipHydrationSnapshot.create(Integer.MIN_VALUE, 35, 4);
        assertNotNull(row);
        assertTrue(row.startsWith(TooltipHydrationLayout.MARKER));
        assertTrue(row.matches("(?:§[0-9a-fk-or])+\\u00a0+"));
        assertEquals(repeat('\u00a0', 9), row.replaceAll("§[0-9a-fk-or]", ""));
        assertTrue("The last formatting code must restore aqua", row.endsWith("§b" + repeat('\u00a0', 9)));
        assertTrue(TooltipHydrationLayout.complete("§7" + row, row));
    }

    @Test public void invalidMeasurementsDoNotReserveRows() {
        assertNull(TooltipHydrationSnapshot.create(3, 0, 4));
        assertNull(TooltipHydrationSnapshot.create(3, -1, 4));
        assertNull(TooltipHydrationSnapshot.create(3, 35, 0));
        assertNull(TooltipHydrationSnapshot.create(3, 35, -1));
    }

    @Test public void copiedAndInterleavedTooltipsKeepTheirOwnAmounts() {
        List<String> first = tooltip(3);
        List<String> copied = Collections.unmodifiableList(new ArrayList<String>(first));
        List<String> other = Collections.unmodifiableList(tooltip(-3));
        for (int i = 0; i < 3; i++) {
            assertEquals(Integer.valueOf(-3), TooltipHydrationSnapshot.decode(other));
            assertEquals(Integer.valueOf(3), TooltipHydrationSnapshot.decode(copied));
            assertEquals(Integer.valueOf(3), TooltipHydrationSnapshot.decode(first));
        }
        TooltipHydrationSnapshot.upsert(first, TooltipHydrationSnapshot.create(21, 35, 4));
        assertEquals(Integer.valueOf(21), TooltipHydrationSnapshot.decode(first));
        assertEquals(Integer.valueOf(3), TooltipHydrationSnapshot.decode(copied));
        assertEquals(Integer.valueOf(-3), TooltipHydrationSnapshot.decode(other));
        assertNull(TooltipHydrationSnapshot.decode(Arrays.asList("title", "ordinary tooltip")));
    }

    @Test public void prefixedFormattingDoesNotChangeDecodedAmount() {
        String row = TooltipHydrationSnapshot.create(3, 35, 4);
        assertEquals(Integer.valueOf(3), TooltipHydrationSnapshot.decode("§7§o" + row));
        assertNull(TooltipHydrationSnapshot.decode("visible prefix" + row));
        assertNull(TooltipHydrationSnapshot.decode("§x" + row));
    }

    @Test public void completeMetadataSurvivesWrappingBeforeItsFirstPaddingCharacter() {
        String row = TooltipHydrationSnapshot.create(-3, 35, 4);
        assertNotNull(row);
        String metadata = row.substring(0, row.indexOf('\u00a0'));
        assertEquals(Integer.valueOf(-3), TooltipHydrationSnapshot.decode(metadata));
        assertEquals(Integer.valueOf(-3), TooltipHydrationSnapshot.decode("§7" + metadata));
        List<String> wrapped = Collections.unmodifiableList(new ArrayList<String>(Arrays.asList(
                "title", "details", metadata, "§b\u00a0\u00a0", "§b\u00a0\u00a0")));
        assertEquals(2, TooltipHydrationLayout.findRow(wrapped));
        assertEquals(Integer.valueOf(-3), TooltipHydrationSnapshot.decode(wrapped));
        assertNull(TooltipHydrationSnapshot.decode(metadata.substring(0, metadata.length() - 1)));
        assertNull(TooltipHydrationSnapshot.decode(metadata + "visible suffix"));
        assertNull(TooltipHydrationSnapshot.decode(metadata + "§x"));
        assertNull(TooltipHydrationSnapshot.decode(metadata + "§r"));
    }

    @Test public void onlyAUniqueNonTitleOwnedRowCanBeDecoded() {
        String first = TooltipHydrationSnapshot.create(3, 35, 4);
        String second = TooltipHydrationSnapshot.create(20, 35, 4);
        assertNull(TooltipHydrationSnapshot.decode(Arrays.asList("title", first, second)));
        assertNull(TooltipHydrationSnapshot.decode(Arrays.asList("title", first, first)));
        assertNull(TooltipHydrationSnapshot.decode(Collections.singletonList(first)));
        assertNull(TooltipHydrationSnapshot.decode(Arrays.asList("title", first,
                TooltipHydrationLayout.reservation(35, 4))));
        assertEquals(Integer.valueOf(3), TooltipHydrationSnapshot.decode(Arrays.asList("title", "details", first)));
    }

    @Test public void upsertReplacesOwnedRowsAndIsIdempotent() {
        String first = TooltipHydrationSnapshot.create(3, 35, 4);
        String second = TooltipHydrationSnapshot.create(-3, 35, 4);
        List<String> lines = new ArrayList<String>(Arrays.asList("title", first, "details", first));
        TooltipHydrationSnapshot.upsert(lines, second);
        assertEquals(Arrays.asList("title", "details", second), lines);
        TooltipHydrationSnapshot.upsert(lines, second);
        assertEquals(Arrays.asList("title", "details", second), lines);
        assertEquals(Integer.valueOf(-3), TooltipHydrationSnapshot.decode(lines));
        TooltipHydrationSnapshot.upsert(lines, null);
        assertEquals(Arrays.asList("title", "details"), lines);
    }

    @Test public void cleanupRemovesOnlyMarkedRowsIncludingLegacyAndDamagedData() {
        String valid = TooltipHydrationSnapshot.create(3, 35, 4);
        assertNotNull(valid);
        String legacy = TooltipHydrationLayout.reservation(35, 4);
        List<String> lines = new ArrayList<String>(Arrays.asList("title", "§bordinary", valid,
                "\u00a0\u00a0", legacy, valid.substring(0, valid.length() - 9)));
        TooltipHydrationSnapshot.removeRows(lines);
        assertEquals(Arrays.asList("title", "§bordinary", "\u00a0\u00a0"), lines);
        TooltipHydrationSnapshot.removeRows(lines);
        assertEquals(Arrays.asList("title", "§bordinary", "\u00a0\u00a0"), lines);
    }

    @Test public void malformedRowsCannotReuseAnEarlierTooltipAmount() {
        String valid = TooltipHydrationSnapshot.create(21, 35, 4);
        assertEquals(Integer.valueOf(21), TooltipHydrationSnapshot.decode(Arrays.asList("title", valid)));
        int payloadStart = TooltipHydrationLayout.MARKER.length();
        String badMagic = valid.substring(0, payloadStart + 1) + "x" + valid.substring(payloadStart + 2);
        String changedMagic = valid.substring(0, payloadStart + 1)
                + (valid.charAt(payloadStart + 1) == '0' ? '1' : '0') + valid.substring(payloadStart + 2);
        List<String> invalid = Arrays.asList("", "ordinary", "\u00a0", TooltipHydrationLayout.reservation(35, 4),
                badMagic, changedMagic, valid + "visible suffix", valid + "§r",
                valid.replace('\u00a0', ' '), valid + valid);
        for (String row : invalid) {
            assertNull("Malformed string must not decode: " + row, TooltipHydrationSnapshot.decode(row));
            assertNull(TooltipHydrationSnapshot.decode(Arrays.asList("title", row)));
        }
    }

    @Test public void truncatedOrExtendedPayloadsAreRejected() {
        String row = TooltipHydrationSnapshot.create(Integer.MIN_VALUE, 35, 4);
        assertNotNull(row);
        int end = row.indexOf('\u00a0');
        for (int i = TooltipHydrationLayout.MARKER.length(); i < end; i++) {
            assertNull("Incomplete payload at " + i, TooltipHydrationSnapshot.decode(row.substring(0, i) + "\u00a0"));
        }
        String extraToken = row.substring(0, end - 2) + "§0" + row.substring(end - 2);
        assertNull(TooltipHydrationSnapshot.decode(extraToken));
        int versionCode = end - 2 - 8 * 2 - 1;
        String otherVersion = row.substring(0, versionCode)
                + (row.charAt(versionCode) == 'f' ? 'e' : 'f') + row.substring(versionCode + 1);
        assertNull(TooltipHydrationSnapshot.decode(otherVersion));
        String nonHexToken = row.substring(0, end - 4) + "§l" + row.substring(end - 2);
        assertNull(TooltipHydrationSnapshot.decode(nonHexToken));
        String invalidToken = row.substring(0, end - 4) + "§x" + row.substring(end - 2);
        assertNull(TooltipHydrationSnapshot.decode(invalidToken));
    }

    @Test public void nullAndAbsentDataReturnNoAmount() {
        assertNull(TooltipHydrationSnapshot.decode((String) null));
        assertNull(TooltipHydrationSnapshot.decode((List<String>) null));
        assertNull(TooltipHydrationSnapshot.decode(Collections.<String>emptyList()));
        assertNull(TooltipHydrationSnapshot.decode(Arrays.asList("title", null)));
    }

    private static List<String> tooltip(int amount) {
        return new ArrayList<String>(Arrays.asList("title", TooltipHydrationSnapshot.create(amount, 35, 4)));
    }

    private static String repeat(char c, int count) {
        char[] chars = new char[count];
        Arrays.fill(chars, c);
        return new String(chars);
    }
}
