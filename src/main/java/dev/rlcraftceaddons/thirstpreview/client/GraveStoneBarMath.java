package dev.rlcraftceaddons.thirstpreview.client;

/** Geometry verified against the supported GraveStone PotionBarsRenderer. */
final class GraveStoneBarMath {
    private GraveStoneBarMath() { }

    static int filledWidth(int level) {
        int clamped = Math.max(0, Math.min(20, level));
        return (clamped * 77 + 19) / 20;
    }

    static Segment between(int current, int restored) {
        int currentWidth = filledWidth(current);
        int restoredWidth = filledWidth(restored);
        if (currentWidth == restoredWidth) {
            return null;
        }
        return new Segment(79 - Math.max(currentWidth, restoredWidth),
                79 - Math.min(currentWidth, restoredWidth), restoredWidth > currentWidth);
    }

    static final class Segment {
        final int left;
        final int right;
        final int top = 1;
        final int bottom = 8;
        final boolean gain;

        private Segment(int left, int right, boolean gain) {
            this.left = left;
            this.right = right;
            this.gain = gain;
        }
    }

    static final class Anchor {
        final int x;
        final int y;

        private Anchor(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    /** A capture is valid only between one Pre(ALL) and its first render attempt. */
    static final class Frame {
        private boolean open;
        private boolean captured;
        private int width;
        private int height;
        private int leftHeight;
        private int rightHeight;
        private boolean rightSide;

        void beginFrame() {
            open = true;
            captured = false;
        }

        void capture(int width, int height, int leftHeight, int rightHeight, boolean rightSide) {
            if (!open || captured) {
                discard();
                return;
            }
            if (width <= 0 || height <= 0 || leftHeight < 0 || rightHeight < 0) {
                discard();
                return;
            }
            this.width = width;
            this.height = height;
            this.leftHeight = leftHeight;
            this.rightHeight = rightHeight;
            this.rightSide = rightSide;
            captured = true;
        }

        Anchor take(int width, int height, int leftHeight, int rightHeight, boolean rightSide) {
            boolean available = open && captured;
            discard();
            if (!available || this.width != width || this.height != height
                    || this.rightSide != rightSide) {
                return null;
            }
            long targetDelta = rightSide ? (long) rightHeight - this.rightHeight
                    : (long) leftHeight - this.leftHeight;
            boolean otherUnchanged = rightSide ? leftHeight == this.leftHeight
                    : rightHeight == this.rightHeight;
            if (targetDelta != 10L || !otherUnchanged) {
                return null;
            }
            int x = width / 2 + (rightSide ? 10 : -91);
            long y = (long) height - ((rightSide ? rightHeight : leftHeight) - 10L);
            if (x < 0 || (long) x + 81 > width || y < 0 || y + 9 > height) {
                return null;
            }
            return new Anchor(x, (int) y);
        }

        void discard() {
            open = false;
            captured = false;
        }
    }
}
