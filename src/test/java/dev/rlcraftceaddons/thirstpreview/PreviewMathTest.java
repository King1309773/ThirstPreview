package dev.rlcraftceaddons.thirstpreview;

import org.junit.Test;
import static org.junit.Assert.*;

public class PreviewMathTest {
    @Test public void clipsRestorationAtFullThirst() {
        assertEquals(20, PreviewMath.restoredLevel(18, 6));
        assertEquals(20, PreviewMath.restoredLevel(20, 6));
        assertEquals(17, PreviewMath.restoredLevel(11, 6));
    }
    @Test public void oddLevelLeavesAHalfDroplet() {
        assertEquals(2, PreviewMath.fillForSlot(11, 4));
        assertEquals(1, PreviewMath.fillForSlot(11, 5));
        assertEquals(0, PreviewMath.fillForSlot(11, 6));
        assertEquals(2, PreviewMath.fillForSlot(17, 7));
        assertEquals(1, PreviewMath.fillForSlot(17, 8));
    }
    @Test public void previewCountEqualsActualRestorationForEveryLevel() {
        for (int level = 0; level <= 20; level++) {
            for (int food = 0; food <= 25; food++) {
                int previewPoints = 0;
                int target = PreviewMath.restoredLevel(level, food);
                for (int slot = 0; slot < 10; slot++) {
                    previewPoints += PreviewMath.fillForSlot(target, slot)
                            - PreviewMath.fillForSlot(level, slot);
                }
                assertEquals(Math.min(food, 20 - level), previewPoints);
            }
        }
    }
    @Test public void usesTwoPointsPerDroplet() {
        assertEquals(0.5f, PreviewMath.dropletUnits(1), 0.001f);
        assertEquals(3.0f, PreviewMath.dropletUnits(6), 0.001f);
    }
    @Test public void handlesInvalidAndExtremeLevelsWithoutOverflow() {
        assertEquals(20, PreviewMath.restoredLevel(19, Integer.MAX_VALUE));
        assertEquals(0, PreviewMath.restoredLevel(5, Integer.MIN_VALUE));
        assertEquals(0, PreviewMath.fillForSlot(-3, 0));
        assertEquals(0, PreviewMath.fillForSlot(20, -1));
        assertEquals(0, PreviewMath.fillForSlot(20, 10));
    }
}
