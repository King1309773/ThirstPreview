package dev.rlcraftceaddons.thirstpreview.client;

import java.util.function.Function;
import java.util.Random;

final class ClientPreviewPolicy {
    private ClientPreviewPolicy() {}

    static <T, V> V resolveHeld(T mainHand, T offHand, Function<T, V> resolver) {
        V mainValue = resolver.apply(mainHand);
        return mainValue != null ? mainValue : resolver.apply(offHand);
    }

    static <T, V> V resolveHeld(T activeStack, T mainHand, T offHand, Function<T, V> resolver) {
        if (activeStack != null) {
            V activeValue = resolver.apply(activeStack);
            if (activeValue != null) {
                return activeValue;
            }
        }
        return resolveHeld(mainHand, offHand, resolver);
    }

    static boolean shouldShowHud(boolean configured, boolean hideGui, boolean nativeHud,
                                 boolean thirstEnabled, boolean nativeThirstDisplay) {
        return configured && !hideGui && nativeHud && thirstEnabled && nativeThirstDisplay;
    }

    static int thirstY(int scaledHeight, int rightHeightAfterThirst) {
        return scaledHeight - rightHeightAfterThirst + 10;
    }

    static int dropletX(int scaledWidth, int slot) {
        return scaledWidth / 2 + 82 - slot * 8;
    }

    static int[] nativeYOffsets(int updateCounter, int thirst, float saturation) {
        int[] offsets = new int[10];
        if (saturation <= 0F && updateCounter % (thirst * 3 + 1) == 0) {
            // Match SimpleDifficulty's integer multiplication and per-slot RNG calls.
            Random random = new Random((long) (updateCounter * 445));
            for (int i = 0; i < offsets.length; i++) {
                offsets[i] = random.nextInt(3) - 1;
            }
        }
        return offsets;
    }
}
