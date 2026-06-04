package com.example.lifecycledemo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DebugViewStateTest {

    @Test
    public void startsInCompactModeWithCompactSize() {
        DebugViewState state = new DebugViewState(240, 160, 320, 220);

        assertFalse(state.isExpanded());
        assertEquals(240, state.getDesiredWidth());
        assertEquals(160, state.getDesiredHeight());
        assertEquals("compact", state.getSizeModeLabel());
    }

    @Test
    public void toggleSizeModeSwitchesBetweenCompactAndExpanded() {
        DebugViewState state = new DebugViewState(240, 160, 320, 220);

        state.toggleSizeMode();

        assertTrue(state.isExpanded());
        assertEquals(320, state.getDesiredWidth());
        assertEquals(220, state.getDesiredHeight());
        assertEquals("expanded", state.getSizeModeLabel());

        state.toggleSizeMode();

        assertFalse(state.isExpanded());
        assertEquals(240, state.getDesiredWidth());
        assertEquals(160, state.getDesiredHeight());
    }

    @Test
    public void recordsDrawCountAndLastTouchPoint() {
        DebugViewState state = new DebugViewState(240, 160, 320, 220);

        state.markDraw();
        state.markDraw();
        state.updateTouch(12.5f, 34.75f);

        assertEquals(2, state.getDrawCount());
        assertEquals("x=12.5, y=34.8", state.getLastTouchLabel());
    }

    @Test
    public void summaryIncludesSizeModeDrawCountAndTouch() {
        DebugViewState state = new DebugViewState(240, 160, 320, 220);

        state.toggleSizeMode();
        state.markDraw();
        state.updateTouch(10f, 20f);

        assertEquals(
                "mode=expanded, desired=320x220, drawCount=1, lastTouch=x=10.0, y=20.0",
                state.getSummary()
        );
    }
}

