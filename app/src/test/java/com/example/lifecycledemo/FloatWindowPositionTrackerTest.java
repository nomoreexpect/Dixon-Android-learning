package com.example.lifecycledemo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FloatWindowPositionTrackerTest {

    @Test
    public void moveTo_returnsWindowPositionOffsetByFingerDelta() {
        FloatWindowPositionTracker tracker = new FloatWindowPositionTracker();

        tracker.startDrag(100f, 200f, 80, 240);
        FloatWindowPositionTracker.Position position = tracker.moveTo(135f, 260f);

        assertEquals(115, position.x);
        assertEquals(300, position.y);
    }

    @Test
    public void moveTo_supportsDraggingLeftAndUp() {
        FloatWindowPositionTracker tracker = new FloatWindowPositionTracker();

        tracker.startDrag(300f, 400f, 160, 220);
        FloatWindowPositionTracker.Position position = tracker.moveTo(250f, 350f);

        assertEquals(110, position.x);
        assertEquals(170, position.y);
    }

    @Test
    public void moveTo_roundsSubPixelMovementToNearestInteger() {
        FloatWindowPositionTracker tracker = new FloatWindowPositionTracker();

        tracker.startDrag(10.2f, 20.2f, 5, 7);
        FloatWindowPositionTracker.Position position = tracker.moveTo(11.8f, 23.9f);

        assertEquals(7, position.x);
        assertEquals(11, position.y);
    }
}
