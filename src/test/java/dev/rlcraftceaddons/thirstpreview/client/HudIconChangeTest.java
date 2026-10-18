package dev.rlcraftceaddons.thirstpreview.client;

import org.junit.Test;
import static org.junit.Assert.*;

public class HudIconChangeTest {
    @Test
    public void unchangedDropletsAreNeverFlashed() {
        assertNull(HudIconChange.between(0, 0));
        assertNull(HudIconChange.between(1, 1));
        assertNull(HudIconChange.between(2, 2));
    }

    @Test
    public void firstHalfUsesNativeRightHalfSprite() {
        HudIconChange change = HudIconChange.between(0, 1);
        assertNotNull(change);
        assertEquals(45, change.textureU);
        assertEquals(9, change.width);
        assertTrue(change.gain);
    }

    @Test
    public void fullGainUsesWholeNativeDroplet() {
        HudIconChange change = HudIconChange.between(0, 2);
        assertNotNull(change);
        assertEquals(36, change.textureU);
        assertEquals(9, change.width);
    }

    @Test
    public void fillingSecondHalfLeavesExistingRightHalfUntouched() {
        HudIconChange change = HudIconChange.between(1, 2);
        assertNotNull(change);
        assertEquals(36, change.textureU);
        assertEquals(5, change.width);
        assertTrue(change.gain);
    }

    @Test
    public void losingSecondHalfOnlyHighlightsLeftHalf() {
        HudIconChange change = HudIconChange.between(2, 1);
        assertNotNull(change);
        assertEquals(36, change.textureU);
        assertEquals(5, change.width);
        assertFalse(change.gain);
    }

    @Test
    public void losingFirstHalfUsesNativeRightHalfSprite() {
        HudIconChange change = HudIconChange.between(1, 0);
        assertNotNull(change);
        assertEquals(45, change.textureU);
        assertEquals(9, change.width);
        assertFalse(change.gain);
    }
}
