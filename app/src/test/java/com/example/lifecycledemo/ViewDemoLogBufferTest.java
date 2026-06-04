package com.example.lifecycledemo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ViewDemoLogBufferTest {

    @Test
    public void appendAddsNumberedLinesWithEventAndDetail() {
        ViewDemoLogBuffer buffer = new ViewDemoLogBuffer(4);

        buffer.append("onMeasure", "width=320 height=180");
        buffer.append("onDraw", "count=1");

        assertEquals(
                "01. onMeasure  width=320 height=180\n"
                        + "02. onDraw     count=1",
                buffer.getText()
        );
    }

    @Test
    public void appendKeepsOnlyNewestLinesWhenLimitIsReached() {
        ViewDemoLogBuffer buffer = new ViewDemoLogBuffer(2);

        buffer.append("first", "1");
        buffer.append("second", "2");
        buffer.append("third", "3");

        assertEquals(
                "02. second  2\n"
                        + "03. third   3",
                buffer.getText()
        );
    }

    @Test
    public void clearRemovesAllLinesButKeepsSequenceIncreasing() {
        ViewDemoLogBuffer buffer = new ViewDemoLogBuffer(4);

        buffer.append("before", "clear");
        buffer.clear();
        buffer.append("after", "clear");

        assertEquals("02. after  clear", buffer.getText());
    }
}

