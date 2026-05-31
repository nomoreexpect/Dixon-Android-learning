package com.example.lifecycledemo;

/**
 * Calculates floating-window movement from raw touch coordinates.
 *
 * This class has no Android View dependency, so the drag math can be tested with
 * normal local unit tests. WindowManager still owns the real on-screen update.
 */
public class FloatWindowPositionTracker {

    private float downRawX;
    private float downRawY;
    private int downWindowX;
    private int downWindowY;

    /**
     * Records the finger position and the window position at ACTION_DOWN.
     */
    public void startDrag(float rawX, float rawY, int windowX, int windowY) {
        downRawX = rawX;
        downRawY = rawY;
        downWindowX = windowX;
        downWindowY = windowY;
    }

    /**
     * Returns the new window position for the current finger location.
     */
    public Position moveTo(float rawX, float rawY) {
        int nextX = downWindowX + Math.round(rawX - downRawX);
        int nextY = downWindowY + Math.round(rawY - downRawY);
        return new Position(nextX, nextY);
    }

    public static final class Position {
        public final int x;
        public final int y;

        public Position(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
