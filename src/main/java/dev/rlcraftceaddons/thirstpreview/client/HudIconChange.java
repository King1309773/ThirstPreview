package dev.rlcraftceaddons.thirstpreview.client;

final class HudIconChange {
    final int textureU;
    final int width;
    final boolean gain;

    private HudIconChange(int textureU, int width, boolean gain) {
        this.textureU = textureU;
        this.width = width;
        this.gain = gain;
    }

    static HudIconChange between(int currentFill, int previewFill) {
        if (currentFill == previewFill) {
            return null;
        }
        int highestFill = Math.max(currentFill, previewFill);
        int lowestFill = Math.min(currentFill, previewFill);
        // The first native half fills the right side; the second adds columns 0..4.
        return new HudIconChange(highestFill == 1 ? 45 : 36,
                lowestFill == 1 ? 5 : 9, previewFill > currentFill);
    }
}
