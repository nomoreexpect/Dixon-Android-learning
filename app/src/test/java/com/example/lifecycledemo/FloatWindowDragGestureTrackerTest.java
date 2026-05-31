package com.example.lifecycledemo;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FloatWindowDragGestureTrackerTest {

    @Test
    public void updateMove_doesNotStartDragInsideTapSlop() {
        FloatWindowDragGestureTracker tracker = new FloatWindowDragGestureTracker(24);

        tracker.start(100f, 200f);

        assertFalse(tracker.updateMove(110f, 210f));
        assertTrue(tracker.isTap(110f, 210f));
        assertFalse(tracker.hasDragged());
    }

    @Test
    public void updateMove_startsDragOutsideTapSlop() {
        FloatWindowDragGestureTracker tracker = new FloatWindowDragGestureTracker(24);

        tracker.start(100f, 200f);

        assertTrue(tracker.updateMove(140f, 200f));
        assertFalse(tracker.isTap(140f, 200f));
        assertTrue(tracker.hasDragged());
    }
}
