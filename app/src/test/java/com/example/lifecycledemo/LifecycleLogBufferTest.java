package com.example.lifecycledemo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LifecycleLogBufferTest {

    @Test
    public void appendEventAddsNumberedLifecycleLine() {
        LifecycleLogBuffer buffer = new LifecycleLogBuffer();

        buffer.append("10:15:30", "onCreate");
        buffer.append("10:15:31", "onStart");

        assertEquals(
                "01. 10:15:30  MainActivity.onCreate\n"
                        + "02. 10:15:31  MainActivity.onStart",
                buffer.getText()
        );
    }

    @Test
    public void clearRemovesAllLifecycleLines() {
        LifecycleLogBuffer buffer = new LifecycleLogBuffer();

        buffer.append("10:15:30", "onCreate");
        buffer.clear();

        assertEquals("", buffer.getText());
    }
}
