package com.oscimate.firorize.config;

import com.oscimate.firorize.Main;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

/**
 * Tracks the total time the player spends on Firorize config screens and surfaces a gentle Ko-fi
 * donation popup once every {@link #THRESHOLD_MS} of accumulated time. The running total is persisted
 * in {@code firorize.json} (see {@link ConfigManager#accumulatedConfigTimeMs}) so it carries across
 * game restarts.
 *
 * <p>{@link #onConfigFrame()} is called once per rendered frame from the config screens the player
 * actually dwells on (entry screen, colour editor, height editor, online gallery). Time is measured
 * from the wall-clock delta between frames, with large gaps discarded so only <em>active</em> config
 * time counts. The frame-timestamp approach is idempotent within a single frame, so a modal that
 * renders its parent as a backdrop will not double-count.
 */
public final class DonationTracker {
    public static final String KOFI_URL = "https://ko-fi.com/rufflesteels";
    public static final long THRESHOLD_MS = 20L * 60L * 1000L; // 20 minutes
    private static final long MAX_FRAME_DELTA_MS = 2000L; // discard alt-tab / between-session gaps

    private static long lastFrameMs = -1L;
    private static boolean popupQueued = false;

    private DonationTracker() {}

    public static void onConfigFrame() {
        MinecraftClient client = MinecraftClient.getInstance();

        // The popup renders its parent config screen as a backdrop, which re-enters this method. Don't
        // accumulate (or re-trigger) while the popup is up; reset so its open duration isn't counted.
        if (client.currentScreen instanceof DonatePopupScreen) {
            lastFrameMs = -1L;
            return;
        }

        long now = System.currentTimeMillis();
        if (lastFrameMs > 0L) {
            long delta = now - lastFrameMs;
            if (delta > 0L && delta < MAX_FRAME_DELTA_MS) {
                Main.CONFIG_MANAGER.accumulatedConfigTimeMs += delta;
            }
        }
        lastFrameMs = now;

        if (popupQueued) return;

        long shown = Main.CONFIG_MANAGER.getDonationPopupsShown();
        if (Main.CONFIG_MANAGER.accumulatedConfigTimeMs >= (shown + 1L) * THRESHOLD_MS) {
            Screen current = client.currentScreen;
            // Catch the shown-count up to however many full thresholds have actually elapsed, not just
            // shown+1. Otherwise any banked time beyond one threshold (e.g. a long single session, or
            // lowering THRESHOLD_MS) leaves the condition true on the next frame, re-firing the popup.
            long elapsed = Main.CONFIG_MANAGER.accumulatedConfigTimeMs / THRESHOLD_MS;
            Main.CONFIG_MANAGER.setDonationPopupsShown((int) elapsed);
            Main.CONFIG_MANAGER.save();
            popupQueued = true;
            // Defer the screen swap to the end of the frame — mutating screens mid-render is unsafe.
            client.execute(() -> {
                client.setScreen(new DonatePopupScreen(current));
                popupQueued = false;
            });
        }
    }
}
