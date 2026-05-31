package com.example.lifecycledemo;

/**
 * Separates a plain tap from a real drag for the floating window.
 */
public class FloatWindowDragGestureTracker {

    private final int tapSlop;
    private float downRawX;
    private float downRawY;
    private boolean dragged;

    public FloatWindowDragGestureTracker(int tapSlop) {
        this.tapSlop = Math.max(0, tapSlop);
    }

    public void start(float rawX, float rawY) {
        downRawX = rawX;
        downRawY = rawY;
        dragged = false;
    }

    public boolean updateMove(float rawX, float rawY) {
        if (dragged) {
            return true;
        }

        dragged = distanceFromDown(rawX, rawY) > tapSlop;
        return dragged;
    }

    public boolean isTap(float rawX, float rawY) {
        return !dragged && distanceFromDown(rawX, rawY) <= tapSlop;
    }

    public boolean hasDragged() {
        return dragged;
    }

    private float distanceFromDown(float rawX, float rawY) {
        float dx = rawX - downRawX;
        float dy = rawY - downRawY;
        return (float) Math.hypot(dx, dy);
    }
}
