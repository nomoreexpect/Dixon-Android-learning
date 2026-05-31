package com.example.lifecycledemo;

public class LifecycleLogBuffer {
    private final StringBuilder builder = new StringBuilder();
    private int eventCount = 0;

    public void append(String timeText, String eventName) {
        eventCount++;
        if (builder.length() > 0) {
            builder.append('\n');
        }
        builder.append(String.format(
                "%02d. %s  MainActivity.%s",
                eventCount,
                timeText,
                eventName
        ));
    }

    public void clear() {
        builder.setLength(0);
        eventCount = 0;
    }

    public String getText() {
        return builder.toString();
    }
}
