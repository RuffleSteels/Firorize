package com.oscimate.firorize.config;

/**
 * Shared horizontal auto-scroll ("marquee") math for one-line titles that are too long for their
 * column. Given the rendered text width, the available width and how long the row has been hovered,
 * returns the pixel offset to <b>subtract</b> from the text's draw X so the title holds at the start,
 * scrolls left to reveal the end, holds there, then loops back. Returns 0 when the text already fits,
 * so callers can fall back to a plain (truncated) draw. Pure integer math — identical across versions.
 */
final class MarqueeText {
    private MarqueeText() {}

    private static final int PAUSE_MS = 900;         // hold at each end before/after scrolling
    private static final int SPEED_PX_PER_SEC = 24;  // scroll speed while revealing the tail

    static int offset(int textWidth, int availWidth, long elapsedMs) {
        int overflow = textWidth - availWidth;
        if (overflow <= 0) return 0;
        int scrollMs = Math.max(1, overflow * 1000 / SPEED_PX_PER_SEC);
        int cycle = PAUSE_MS + scrollMs + PAUSE_MS;
        long t = ((elapsedMs % cycle) + cycle) % cycle;
        if (t < PAUSE_MS) return 0;
        t -= PAUSE_MS;
        if (t < scrollMs) return (int) (overflow * t / scrollMs);
        return overflow;
    }
}
