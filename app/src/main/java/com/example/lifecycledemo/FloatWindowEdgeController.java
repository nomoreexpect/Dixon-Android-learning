package com.example.lifecycledemo;

/**
 * Keeps floating-window coordinates away from system gesture edges and decides
 * whether a released window should collapse into the edge ball.
 *
 * This class intentionally has no Android View dependency, so the math is easy
 * to verify with local unit tests.
 */
public class FloatWindowEdgeController {

    public static final int EDGE_NONE = 0;
    public static final int EDGE_LEFT = 1;
    public static final int EDGE_RIGHT = 2;

    private final int screenWidth;
    private final int screenHeight;
    private final int windowWidth;
    private final int windowHeight;
    private final int safeInset;
    private final int snapThreshold;

    public FloatWindowEdgeController(
            int screenWidth,
            int screenHeight,
            int windowWidth,
            int windowHeight,
            int safeInset,
            int snapThreshold
    ) {
        this.screenWidth = Math.max(1, screenWidth);
        this.screenHeight = Math.max(1, screenHeight);
        this.windowWidth = Math.max(1, windowWidth);
        this.windowHeight = Math.max(1, windowHeight);
        this.safeInset = Math.max(0, safeInset);
        this.snapThreshold = Math.max(0, snapThreshold);
    }

    public Position clampMove(int x, int y) {
        return new Position(
                clamp(x, minX(), maxX()),
                clamp(y, minY(), maxY())
        );
    }

    public Position clampCollapsedMove(int x, int y) {
        return new Position(
                clamp(x, 0, maxPhysicalX()),
                clamp(y, minY(), maxY())
        );
    }

    public SettleResult settleOnRelease(int x, int y) {
        Position position = clampMove(x, y);
        int threshold = effectiveSnapThreshold();

        if (position.x <= minX() + threshold) {
            return new SettleResult(new Position(minX(), position.y), true, EDGE_LEFT);
        }

        if (position.x >= maxX() - threshold) {
            return new SettleResult(new Position(maxX(), position.y), true, EDGE_RIGHT);
        }

        return new SettleResult(position, false, EDGE_NONE);
    }

    public SettleResult settleCollapsedOnRelease(int x, int y) {
        Position position = clampMove(x, y);
        int edge = position.x <= (minX() + maxX()) / 2 ? EDGE_LEFT : EDGE_RIGHT;
        return new SettleResult(snapToEdge(edge, position.y), true, edge);
    }

    public Position snapToEdge(int edge, int y) {
        int targetX = edge == EDGE_RIGHT ? maxPhysicalX() : 0;
        return new Position(targetX, clamp(y, minY(), maxY()));
    }

    private int minX() {
        return safeInset;
    }

    private int minY() {
        return safeInset;
    }

    private int maxX() {
        return Math.max(minX(), screenWidth - windowWidth - safeInset);
    }

    private int maxY() {
        return Math.max(minY(), screenHeight - windowHeight - safeInset);
    }

    private int maxPhysicalX() {
        return Math.max(0, screenWidth - windowWidth);
    }

    private int effectiveSnapThreshold() {
        return Math.min(snapThreshold, Math.max(0, (maxX() - minX()) / 3));
    }

    private int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    public static final class Position {
        public final int x;
        public final int y;

        public Position(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    public static final class SettleResult {
        public final Position position;
        public final boolean shouldCollapse;
        public final int edge;

        public SettleResult(Position position, boolean shouldCollapse, int edge) {
            this.position = position;
            this.shouldCollapse = shouldCollapse;
            this.edge = edge;
        }
    }
}
