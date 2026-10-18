package dev.rlcraftceaddons.thirstpreview.client;

import org.junit.Test;
import static org.junit.Assert.*;

public class GraveStoneBarMathTest {
    @Test
    public void sevenToFifteenAddsOnlyTheNewRightAlignedColumns() throws Exception {
        assertSegment(7, 15, 21, 52, true);
    }

    @Test
    public void nineteenToTwentyAddsTheLastThreeColumns() throws Exception {
        assertSegment(19, 20, 2, 5, true);
    }

    @Test
    public void emptyToFullStaysInsideTheSeventySevenPixelInterior() throws Exception {
        assertSegment(0, 20, 2, 79, true);
    }

    @Test
    public void negativeHydrationMarksOnlyTheLostColumns() throws Exception {
        assertSegment(15, 7, 21, 52, false);
    }

    @Test
    public void equalAndSaturatedFullLevelsDoNotDraw() throws Exception {
        assertNull(segment(20, 20));
        assertNull(segment(20, Integer.MAX_VALUE));
        assertNull(segment(Integer.MIN_VALUE, -1));
    }

    @Test
    public void extremeInputsSaturateBeforeMultiplication() throws Exception {
        assertSegment(Integer.MIN_VALUE, Integer.MAX_VALUE, 2, 79, true);
        assertEquals(0, width(Integer.MIN_VALUE));
        assertEquals(77, width(Integer.MAX_VALUE));
    }

    @Test
    public void everyHydrationLevelUsesTheTargetsCeilingRule() throws Exception {
        for (int level = 0; level <= 20; level++) {
            assertEquals((int) Math.ceil(level * 77D / 20D), width(level));
        }
    }

    @Test
    public void rightBarUsesTheHeightReservedByTheTarget() throws Exception {
        GraveStoneBarMath.Frame frame = captured(true);
        assertAnchor(take(frame, 320, 240, 39, 49, true), 170, 201);
    }

    @Test
    public void leftBarUsesTheLeftHeightAndOffset() throws Exception {
        GraveStoneBarMath.Frame frame = captured(false);
        assertAnchor(take(frame, 320, 240, 49, 39, false), 69, 201);
    }

    @Test
    public void oddScreenWidthsUseIntegerCentering() throws Exception {
        GraveStoneBarMath.Frame frame = frame();
        begin(frame);
        capture(frame, 321, 241, 39, 39, true);
        assertAnchor(take(frame, 321, 241, 39, 49, true), 170, 202);
    }

    @Test
    public void targetSideMustIncreaseByExactlyTen() throws Exception {
        for (int after : new int[] {39, 48, 50, 59}) {
            assertNull(take(captured(true), 320, 240, 39, after, true));
        }
    }

    @Test
    public void anyChangeOnTheOtherSideMakesThePositionUncertain() throws Exception {
        assertNull(take(captured(true), 320, 240, 49, 49, true));
        assertNull(take(captured(false), 320, 240, 49, 49, false));
    }

    @Test
    public void changedHydrationSideDoesNotReuseTheCapture() throws Exception {
        assertNull(take(captured(true), 320, 240, 49, 39, false));
    }

    @Test
    public void changedWidthOrHeightDoesNotReuseTheCapture() throws Exception {
        assertNull(take(captured(true), 321, 240, 39, 49, true));
        assertNull(take(captured(true), 320, 241, 39, 49, true));
    }

    @Test
    public void renderWithoutCaptureDoesNothing() throws Exception {
        GraveStoneBarMath.Frame frame = frame();
        begin(frame);
        assertNull(take(frame, 320, 240, 39, 49, true));
    }

    @Test
    public void captureWithoutBeginningAFrameDoesNothing() throws Exception {
        GraveStoneBarMath.Frame frame = frame();
        capture(frame, 320, 240, 39, 39, true);
        assertNull(take(frame, 320, 240, 39, 49, true));
    }

    @Test
    public void aNewFrameDiscardsThePreviousCapture() throws Exception {
        GraveStoneBarMath.Frame frame = captured(true);
        begin(frame);
        assertNull(take(frame, 320, 240, 39, 49, true));
    }

    @Test
    public void aCaptureCanOnlyBeConsumedOnce() throws Exception {
        GraveStoneBarMath.Frame frame = captured(true);
        assertNotNull(take(frame, 320, 240, 39, 49, true));
        assertNull(take(frame, 320, 240, 39, 49, true));
    }

    @Test
    public void aFailedValidationAlsoConsumesTheCapture() throws Exception {
        GraveStoneBarMath.Frame frame = captured(true);
        assertNull(take(frame, 320, 240, 39, 48, true));
        assertNull(take(frame, 320, 240, 39, 49, true));
    }

    @Test
    public void duplicateCaptureMakesEventOrderingUncertain() throws Exception {
        GraveStoneBarMath.Frame frame = captured(true);
        capture(frame, 320, 240, 39, 39, true);
        assertNull(take(frame, 320, 240, 39, 49, true));
    }

    @Test
    public void heightOverflowCannotImpersonateAValidReservation() throws Exception {
        GraveStoneBarMath.Frame frame = frame();
        begin(frame);
        capture(frame, 320, 240, 39, Integer.MAX_VALUE - 4, true);
        assertNull(take(frame, 320, 240, 39, Integer.MIN_VALUE + 5, true));
    }

    @Test
    public void anchorsOutsideTheViewportAreRejected() throws Exception {
        GraveStoneBarMath.Frame frame = frame();
        begin(frame);
        capture(frame, 100, 240, 39, 39, false);
        assertNull(take(frame, 100, 240, 49, 39, false));
        frame = frame();
        begin(frame);
        capture(frame, 320, 20, 39, 39, true);
        assertNull(take(frame, 320, 20, 39, 49, true));
    }

    private static int width(int level) {
        return GraveStoneBarMath.filledWidth(level);
    }

    private static GraveStoneBarMath.Segment segment(int current, int restored) {
        return GraveStoneBarMath.between(current, restored);
    }

    private static void assertSegment(int current, int restored, int left, int right, boolean gain)
            throws Exception {
        GraveStoneBarMath.Segment segment = segment(current, restored);
        assertNotNull(segment);
        assertEquals(left, segment.left);
        assertEquals(right, segment.right);
        assertEquals(1, segment.top);
        assertEquals(8, segment.bottom);
        assertEquals(gain, segment.gain);
    }

    private static GraveStoneBarMath.Frame frame() {
        return new GraveStoneBarMath.Frame();
    }

    private static GraveStoneBarMath.Frame captured(boolean right) {
        GraveStoneBarMath.Frame frame = frame();
        begin(frame);
        capture(frame, 320, 240, 39, 39, right);
        return frame;
    }

    private static void begin(GraveStoneBarMath.Frame frame) {
        frame.beginFrame();
    }

    private static void capture(GraveStoneBarMath.Frame frame, int width, int height,
                                int left, int right, boolean side) {
        frame.capture(width, height, left, right, side);
    }

    private static GraveStoneBarMath.Anchor take(GraveStoneBarMath.Frame frame, int width, int height,
                                                int left, int right, boolean side) {
        return frame.take(width, height, left, right, side);
    }

    private static void assertAnchor(GraveStoneBarMath.Anchor anchor, int x, int y) {
        assertNotNull(anchor);
        assertEquals(x, anchor.x);
        assertEquals(y, anchor.y);
    }
}
