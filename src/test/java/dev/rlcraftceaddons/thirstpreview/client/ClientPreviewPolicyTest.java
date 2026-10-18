package dev.rlcraftceaddons.thirstpreview.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class ClientPreviewPolicyTest {
    @Test
    public void nonDrinkableMainHandDoesNotHideOffhandDrink() {
        List<String> visited = new ArrayList<>();
        String selected = ClientPreviewPolicy.resolveHeld("sword", "water", stack -> {
            visited.add(stack);
            return "water".equals(stack) ? "water values" : null;
        });
        assertEquals("water values", selected);
        assertEquals(Arrays.asList("sword", "water"), visited);
    }

    @Test
    public void mainHandDrinkTakesPriorityOverOffhandDrink() {
        List<String> visited = new ArrayList<>();
        String selected = ClientPreviewPolicy.resolveHeld("juice", "water", stack -> {
            visited.add(stack);
            return stack;
        });
        assertEquals("juice", selected);
        assertEquals(Arrays.asList("juice"), visited);
    }

    @Test
    public void activelyUsedOffhandDrinkTakesPriorityOverHeldMainHandDrink() {
        assertEquals("offhand water", ClientPreviewPolicy.resolveHeld(
                "offhand water", "main juice", "offhand water", stack -> stack));
    }

    @Test
    public void nonDrinkableActiveItemFallsBackToHeldDrinks() {
        assertEquals("offhand water", ClientPreviewPolicy.resolveHeld(
                "bow", "bow", "offhand water", stack -> stack.contains("water") ? stack : null));
    }

    @Test
    public void previewRequiresEveryNativeVisibilityCondition() {
        assertTrue(ClientPreviewPolicy.shouldShowHud(true, false, true, true, true));
        assertFalse(ClientPreviewPolicy.shouldShowHud(false, false, true, true, true));
        assertFalse(ClientPreviewPolicy.shouldShowHud(true, true, true, true, true));
        assertFalse(ClientPreviewPolicy.shouldShowHud(true, false, false, true, true));
        assertFalse(ClientPreviewPolicy.shouldShowHud(true, false, true, false, true));
        assertFalse(ClientPreviewPolicy.shouldShowHud(true, false, true, true, false));
    }

    @Test
    public void thirstRowUsesHeightAfterNativeThirstReservedItsTenPixels() {
        assertEquals(201, ClientPreviewPolicy.thirstY(240, 49));
        assertEquals(191, ClientPreviewPolicy.thirstY(240, 59));
        assertEquals(400, ClientPreviewPolicy.thirstY(449, 59));
    }

    @Test
    public void dropletSlotsFollowScaledScreenAndRunFromRightToLeft() {
        assertEquals(242, ClientPreviewPolicy.dropletX(320, 0));
        assertEquals(170, ClientPreviewPolicy.dropletX(320, 9));
        assertEquals(402, ClientPreviewPolicy.dropletX(640, 0));
    }

    @Test
    public void positiveSaturationAndStableTicksKeepNativeRowStill() {
        assertArrayEquals(new int[10], ClientPreviewPolicy.nativeYOffsets(0, 3, 0.1F));
        assertArrayEquals(new int[10], ClientPreviewPolicy.nativeYOffsets(1, 3, 0F));
    }

    @Test
    public void emptyThirstUsesSameTenDeterministicNativeShakeOffsets() {
        assertArrayEquals(new int[] {-1, 0, 0, 1, 1, 1, 1, -1, -1, 1},
                ClientPreviewPolicy.nativeYOffsets(0, 0, 0F));
    }
}
