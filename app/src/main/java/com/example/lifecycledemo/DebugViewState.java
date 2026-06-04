package com.example.lifecycledemo;

import java.util.Locale;

class DebugViewState {

    private final int compactWidth;
    private final int compactHeight;
    private final int expandedWidth;
    private final int expandedHeight;
    private boolean expanded;
    private int drawCount;
    private String lastTouchLabel = "none";
    private boolean hasTouch;
    private float lastTouchX;
    private float lastTouchY;

    DebugViewState(int compactWidth, int compactHeight, int expandedWidth, int expandedHeight) {
        this.compactWidth = compactWidth;
        this.compactHeight = compactHeight;
        this.expandedWidth = expandedWidth;
        this.expandedHeight = expandedHeight;
    }

    boolean isExpanded() {
        return expanded;
    }

    void toggleSizeMode() {
        expanded = !expanded;
    }

    int getDesiredWidth() {
        return expanded ? expandedWidth : compactWidth;
    }

    int getDesiredHeight() {
        return expanded ? expandedHeight : compactHeight;
    }

    String getSizeModeLabel() {
        return expanded ? "expanded" : "compact";
    }

    void markDraw() {
        drawCount++;
    }

    int getDrawCount() {
        return drawCount;
    }

    void updateTouch(float x, float y) {
        hasTouch = true;
        lastTouchX = x;
        lastTouchY = y;
        lastTouchLabel = String.format(Locale.US, "x=%.1f, y=%.1f", x, y);
    }

    String getLastTouchLabel() {
        return lastTouchLabel;
    }

    boolean hasTouch() {
        return hasTouch;
    }

    float getLastTouchX() {
        return lastTouchX;
    }

    float getLastTouchY() {
        return lastTouchY;
    }

    String getSummary() {
        return "mode=" + getSizeModeLabel()
                + ", desired=" + getDesiredWidth() + "x" + getDesiredHeight()
                + ", drawCount=" + drawCount
                + ", lastTouch=" + lastTouchLabel;
    }
}
