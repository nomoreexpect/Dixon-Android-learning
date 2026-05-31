package com.example.lifecycledemo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FloatWindowEdgeControllerTest {

    @Test
    public void clampMove_keepsWindowInsideSafeGestureBounds() {
        FloatWindowEdgeController controller =
                new FloatWindowEdgeController(1080, 1920, 300, 200, 24, 96);

        FloatWindowEdgeController.Position position = controller.clampMove(-120, 1880);

        assertEquals(24, position.x);
        assertEquals(1696, position.y);
    }

    @Test
    public void clampCollapsedMove_allowsBallToStayOnPhysicalHorizontalEdge() {
        FloatWindowEdgeController controller =
                new FloatWindowEdgeController(1080, 1920, 56, 56, 24, 96);

        FloatWindowEdgeController.Position left = controller.clampCollapsedMove(-20, 500);
        FloatWindowEdgeController.Position right = controller.clampCollapsedMove(1100, 500);

        assertEquals(0, left.x);
        assertEquals(500, left.y);
        assertEquals(1024, right.x);
        assertEquals(500, right.y);
    }

    @Test
    public void settleOnRelease_collapsesWhenReleasedNearLeftEdge() {
        FloatWindowEdgeController controller =
                new FloatWindowEdgeController(1080, 1920, 300, 200, 24, 96);

        FloatWindowEdgeController.SettleResult result = controller.settleOnRelease(40, 500);

        assertTrue(result.shouldCollapse);
        assertEquals(FloatWindowEdgeController.EDGE_LEFT, result.edge);
        assertEquals(24, result.position.x);
        assertEquals(500, result.position.y);
    }

    @Test
    public void settleOnRelease_collapsesWhenReleasedNearRightEdge() {
        FloatWindowEdgeController controller =
                new FloatWindowEdgeController(1080, 1920, 300, 200, 24, 96);

        FloatWindowEdgeController.SettleResult result = controller.settleOnRelease(740, 500);

        assertTrue(result.shouldCollapse);
        assertEquals(FloatWindowEdgeController.EDGE_RIGHT, result.edge);
        assertEquals(756, result.position.x);
        assertEquals(500, result.position.y);
    }

    @Test
    public void settleOnRelease_keepsExpandedWhenReleasedAwayFromEdges() {
        FloatWindowEdgeController controller =
                new FloatWindowEdgeController(1080, 1920, 300, 200, 24, 96);

        FloatWindowEdgeController.SettleResult result = controller.settleOnRelease(300, 500);

        assertFalse(result.shouldCollapse);
        assertEquals(FloatWindowEdgeController.EDGE_NONE, result.edge);
        assertEquals(300, result.position.x);
        assertEquals(500, result.position.y);
    }

    @Test
    public void settleCollapsedOnRelease_snapsBallToNearestEdge() {
        FloatWindowEdgeController controller =
                new FloatWindowEdgeController(1080, 1920, 56, 56, 24, 96);

        FloatWindowEdgeController.SettleResult result = controller.settleCollapsedOnRelease(800, 500);

        assertTrue(result.shouldCollapse);
        assertEquals(FloatWindowEdgeController.EDGE_RIGHT, result.edge);
        assertEquals(1024, result.position.x);
        assertEquals(500, result.position.y);
    }

    @Test
    public void snapToEdge_placesCollapsedBallOnPhysicalRightEdge() {
        FloatWindowEdgeController controller =
                new FloatWindowEdgeController(1080, 1920, 56, 56, 24, 96);

        FloatWindowEdgeController.Position position =
                controller.snapToEdge(FloatWindowEdgeController.EDGE_RIGHT, 1880);

        assertEquals(1024, position.x);
        assertEquals(1840, position.y);
    }

    @Test
    public void snapToEdge_placesCollapsedBallOnPhysicalLeftEdge() {
        FloatWindowEdgeController controller =
                new FloatWindowEdgeController(1080, 1920, 56, 56, 24, 96);

        FloatWindowEdgeController.Position position =
                controller.snapToEdge(FloatWindowEdgeController.EDGE_LEFT, 1880);

        assertEquals(0, position.x);
        assertEquals(1840, position.y);
    }

    @Test
    public void settleOnRelease_doesNotLetLargeThresholdCoverTheMiddle() {
        FloatWindowEdgeController controller =
                new FloatWindowEdgeController(1080, 1920, 660, 348, 72, 288);

        FloatWindowEdgeController.SettleResult result = controller.settleOnRelease(240, 480);

        assertFalse(result.shouldCollapse);
        assertEquals(FloatWindowEdgeController.EDGE_NONE, result.edge);
        assertEquals(240, result.position.x);
        assertEquals(480, result.position.y);
    }
}
