package cool.furry.mc.projectexpansion.gui;

// the emc display is stored as a corner plus an offset from it, so it stays put when the screen is resized or the text changes width
public final class EmcDisplayLayout {
    public static final int DEFAULT_OFFSET = 2;
    public static final int SNAP_DISTANCE = 4;

    private EmcDisplayLayout() {}

    public static int toScreen(boolean start, int offset, int size, int screenSize) {
        return clamp(start ? offset : screenSize - size - offset, size, screenSize);
    }

    public static int toOffset(boolean start, int pos, int size, int screenSize) {
        return start ? pos : screenSize - size - pos;
    }

    // the closer side is the one the display sticks to
    public static boolean isStart(int pos, int size, int screenSize) {
        return pos + size / 2 < screenSize / 2;
    }

    public static int clamp(int pos, int size, int screenSize) {
        return Math.max(0, Math.min(pos, screenSize - size));
    }

    // only the center snaps, the edges are left alone so it can be put right up against them
    public static int snapToCenter(int pos, int size, int screenSize) {
        int center = center(size, screenSize);
        return Math.abs(pos - center) <= SNAP_DISTANCE ? center : pos;
    }

    public static int center(int size, int screenSize) {
        return (screenSize - size) / 2;
    }
}
