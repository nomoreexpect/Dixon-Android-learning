package com.example.lifecycledemo;

import java.util.ArrayDeque;
import java.util.Deque;

class ViewDemoLogBuffer {

    private final int maxLines;
    private final Deque<Line> lines = new ArrayDeque<>();
    private int nextNumber = 1;

    ViewDemoLogBuffer(int maxLines) {
        if (maxLines <= 0) {
            throw new IllegalArgumentException("maxLines must be greater than 0");
        }
        this.maxLines = maxLines;
    }

    void append(String eventName, String detail) {
        lines.addLast(new Line(nextNumber, eventName, detail));
        nextNumber++;

        while (lines.size() > maxLines) {
            lines.removeFirst();
        }
    }

    void clear() {
        lines.clear();
    }

    String getText() {
        if (lines.isEmpty()) {
            return "";
        }

        int eventColumnWidth = 0;
        for (Line line : lines) {
            eventColumnWidth = Math.max(eventColumnWidth, line.eventName.length());
        }

        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (Line line : lines) {
            if (!first) {
                builder.append('\n');
            }
            first = false;

            builder.append(String.format("%02d. ", line.number));
            builder.append(line.eventName);
            for (int i = line.eventName.length(); i < eventColumnWidth; i++) {
                builder.append(' ');
            }
            builder.append("  ");
            builder.append(line.detail);
        }
        return builder.toString();
    }

    private static class Line {
        final int number;
        final String eventName;
        final String detail;

        Line(int number, String eventName, String detail) {
            this.number = number;
            this.eventName = eventName == null ? "" : eventName;
            this.detail = detail == null ? "" : detail;
        }
    }
}

