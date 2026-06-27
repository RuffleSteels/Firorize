package com.oscimate.firorize.config;

import com.oscimate.firorize.Main;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.apache.commons.collections4.map.ListOrderedMap;

import java.util.ArrayList;
import java.util.List;

/**
 * Scrollable list of {@link OnlinePreset}s with per-entry expand/collapse. Each entry is a card
 * showing a chevron, title and creation date; clicking the header toggles a description block plus
 * one or two action buttons, growing that card and pushing the cards below it down (layout sums the
 * live per-entry heights each frame). Styled like the in-screen confirm dialog
 * ({@link ChangeFireColorScreen#renderConfirm}).
 *
 * <p>Each row carries its own {@link RowKind}, so a single list can mix sections: {@link #setBrowse}
 * builds a "My Uploads" (Delete) section followed by a "Community" (Import) section, and
 * {@link #setInbox} builds "Received" (Import / Dismiss) followed by "Sent" (Cancel). Non-interactive
 * <b>HEADER</b> rows separate the groups.
 *
 * <p>This deliberately does NOT extend {@code AlwaysSelectedEntryListWidget} (which assumes uniform
 * entry heights) — it manages its own scroll offset and variable-height layout.
 */
public class OnlinePresetListWidget extends ClickableWidget {
    private static final int PAD = 6;
    private static final int GAP = 3;
    private static final int HEADER_H = 22;
    private static final int SECTION_H = 14;
    private static final int LINE_H = 11;
    private static final int IMPORT_BTN_H = 16;
    private static final int SCROLLBAR_W = 4;

    /** Per-row behaviour. HEADER is a non-interactive section label. */
    private enum RowKind { HEADER, BROWSE_IMPORT, UPLOAD_DELETE, INBOX_IMPORT, SENT_CANCEL }

    private static final int ACTION_BTN_W = 64;
    private static final int BTN_GAP = 6;
    private static final int CONFIRM_DELETE_MS = 3000;

    private final OnlinePresetsScreen screen;
    private final TextRenderer textRenderer;
    private final List<Row> rows = new ArrayList<>();

    private double scrollY = 0;
    private boolean draggingScrollbar = false;

    public OnlinePresetListWidget(int x, int y, int width, int height, OnlinePresetsScreen screen, TextRenderer textRenderer) {
        super(x, y, width, height, Text.empty());
        this.screen = screen;
        this.textRenderer = textRenderer;
    }

    /**
     * Browse view: the player's own uploads first (under a "My Uploads" header, Delete action), then
     * everyone else's (under a "Community" header, Import action). When the player has no uploads the
     * community list is shown header-less.
     */
    public void setBrowse(List<OnlinePreset> mine, List<OnlinePreset> community) {
        rows.clear();
        boolean haveMine = mine != null && !mine.isEmpty();
        if (haveMine) {
            rows.add(new Row(RowKind.HEADER, null, Text.translatable("firorize.config.label.myUploads")));
            for (OnlinePreset p : mine) rows.add(new Row(RowKind.UPLOAD_DELETE, p, null));
        }
        if (community != null && !community.isEmpty()) {
            if (haveMine) rows.add(new Row(RowKind.HEADER, null, Text.translatable("firorize.config.label.community")));
            for (OnlinePreset p : community) rows.add(new Row(RowKind.BROWSE_IMPORT, p, null));
        }
        scrollY = 0;
    }

    /** Inbox view: "Received" (Import/Dismiss) then "Sent" (Cancel). */
    public void setInbox(List<OnlinePreset> received, List<OnlinePreset> sent) {
        rows.clear();
        if (received != null && !received.isEmpty()) {
            rows.add(new Row(RowKind.HEADER, null, Text.translatable("firorize.config.label.inboxReceived")));
            for (OnlinePreset p : received) rows.add(new Row(RowKind.INBOX_IMPORT, p, null));
        }
        if (sent != null && !sent.isEmpty()) {
            rows.add(new Row(RowKind.HEADER, null, Text.translatable("firorize.config.label.inboxSent")));
            for (OnlinePreset p : sent) rows.add(new Row(RowKind.SENT_CANCEL, p, null));
        }
        scrollY = 0;
    }

    public boolean isEmpty() {
        return rows.stream().noneMatch(r -> r.kind != RowKind.HEADER);
    }

    // ---- geometry ----

    private int cardWidth() {
        return getWidth() - SCROLLBAR_W - 2;
    }

    /** Wrap width available for the description text inside a card. */
    private int wrapWidth() {
        return cardWidth() - PAD * 2;
    }

    private int rowHeight(Row row) {
        if (row.kind == RowKind.HEADER) return SECTION_H;
        if (!row.expanded) return HEADER_H;
        int descBlock = row.descriptionLines().isEmpty() ? 0 : row.descriptionLines().size() * LINE_H + 2;
        int authorBlock = LINE_H + 2; // "by <author>" / "to <recipient>" line
        return HEADER_H + descBlock + authorBlock + IMPORT_BTN_H + 8;
    }

    private int totalContentHeight() {
        int total = 0;
        for (Row row : rows) total += rowHeight(row) + GAP;
        return total;
    }

    private int maxScroll() {
        return Math.max(0, totalContentHeight() - getHeight());
    }

    private void clampScroll() {
        if (scrollY < 0) scrollY = 0;
        if (scrollY > maxScroll()) scrollY = maxScroll();
    }

    // ---- rendering ----

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        clampScroll();
        int left = getX();
        int top = getY();
        int right = getX() + getWidth();
        int bottom = getY() + getHeight();

        context.enableScissor(left, top, right, bottom);
        int y = top - (int) scrollY;
        for (Row row : rows) {
            int h = rowHeight(row);
            if (y + h >= top && y <= bottom) {
                if (row.kind == RowKind.HEADER) {
                    renderHeader(context, row, left, y);
                } else {
                    renderRow(context, row, left, y, h, mouseX, mouseY);
                }
            }
            y += h + GAP;
        }
        context.disableScissor();

        renderScrollbar(context);
    }

    private void renderHeader(DrawContext context, Row row, int x, int y) {
        context.drawTextWithShadow(textRenderer, row.headerLabel, x + 1, y + SECTION_H - 10, 0xFF9090A0);
    }

    private void renderRow(DrawContext context, Row row, int x, int y, int h, int mouseX, int mouseY) {
        int w = cardWidth();
        boolean headerHover = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + HEADER_H
                && mouseY >= getY() && mouseY <= getY() + getHeight();

        context.fill(x, y, x + w, y + h, 0xFF1A1A1A);
        context.drawBorder(x, y, w, h, headerHover ? 0xFFB0B0B0 : 0xFF454545);

        // Chevron
        drawChevron(context, x + PAD, y + (HEADER_H - 7) / 2, row.expanded, 0xFFC0C0C0);

        // Relative time, right-aligned
        Text time = row.preset.relativeTime();
        int timeWidth = textRenderer.getWidth(time);
        int titleX = x + PAD + 11;
        int titleMax = w - PAD - timeWidth - 6 - (titleX - x);
        String title = textRenderer.trimToWidth(row.preset.displayTitle(), Math.max(8, titleMax));
        context.drawTextWithShadow(textRenderer, Text.literal(title), titleX, y + (HEADER_H - 8) / 2, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, time, x + w - PAD - timeWidth, y + (HEADER_H - 8) / 2, 0xFF909090);

        if (row.expanded) {
            int ty = y + HEADER_H;
            for (OrderedText line : row.descriptionLines()) {
                context.drawTextWithShadow(textRenderer, line, x + PAD, ty, 0xFFC0C0C0);
                ty += LINE_H;
            }
            if (!row.descriptionLines().isEmpty()) ty += 2;
            Text attribution = row.kind == RowKind.SENT_CANCEL
                    ? Text.translatable("firorize.config.label.toRecipient", row.preset.displayRecipient())
                    : Text.translatable("firorize.config.label.byAuthor", row.preset.displayAuthor());
            context.drawTextWithShadow(textRenderer, attribution, x + PAD, ty, 0xFF7090C0);

            // Primary (rightmost) and optional secondary action button.
            int[] primary = primaryRect(x, y, h, w);
            drawButton(context, primary, primaryLabel(row), isDanger(row) && row.confirmActionUntil > System.currentTimeMillis(),
                    mouseX, mouseY);
            Text secondary = secondaryLabel(row);
            if (secondary != null) {
                int[] sec = secondaryRect(x, y, h, w);
                drawButton(context, sec, secondary, false, mouseX, mouseY);
            }
        }
    }

    private void drawButton(DrawContext context, int[] btn, Text label, boolean confirming, int mouseX, int mouseY) {
        boolean hover = mouseX >= btn[0] && mouseX <= btn[0] + btn[2] && mouseY >= btn[1] && mouseY <= btn[1] + btn[3]
                && mouseY >= getY() && mouseY <= getY() + getHeight();
        context.fill(btn[0], btn[1], btn[0] + btn[2], btn[1] + btn[3], hover ? 0xFF505050 : 0xFF383838);
        context.drawBorder(btn[0], btn[1], btn[2], btn[3],
                confirming ? 0xFFE08080 : (hover ? 0xFFFFFFFF : 0xFF8B8B8B));
        context.drawCenteredTextWithShadow(textRenderer, label, btn[0] + btn[2] / 2, btn[1] + (btn[3] - 8) / 2,
                confirming ? 0xFFE08080 : 0xFFFFFFFF);
    }

    private int[] primaryRect(int x, int y, int h, int w) {
        return new int[]{x + w - PAD - ACTION_BTN_W, y + h - IMPORT_BTN_H - 6, ACTION_BTN_W, IMPORT_BTN_H};
    }

    private int[] secondaryRect(int x, int y, int h, int w) {
        return new int[]{x + w - PAD - ACTION_BTN_W * 2 - BTN_GAP, y + h - IMPORT_BTN_H - 6, ACTION_BTN_W, IMPORT_BTN_H};
    }

    private Text primaryLabel(Row row) {
        return switch (row.kind) {
            case BROWSE_IMPORT, INBOX_IMPORT -> Text.translatable("firorize.config.button.importPreset");
            case UPLOAD_DELETE -> row.confirmActionUntil > System.currentTimeMillis()
                    ? Text.translatable("firorize.config.button.confirmDelete")
                    : Text.translatable("firorize.config.button.deletePreset");
            case SENT_CANCEL -> row.confirmActionUntil > System.currentTimeMillis()
                    ? Text.translatable("firorize.config.button.confirmDelete")
                    : Text.translatable("firorize.config.button.cancelSend");
            case HEADER -> Text.empty();
        };
    }

    /** Inbox rows get a secondary "Dismiss" button; everything else has just the primary. */
    private Text secondaryLabel(Row row) {
        return row.kind == RowKind.INBOX_IMPORT ? Text.translatable("firorize.config.button.dismiss") : null;
    }

    private boolean isDanger(Row row) {
        return row.kind == RowKind.UPLOAD_DELETE || row.kind == RowKind.SENT_CANCEL;
    }

    /** Filled triangle: pointing down when expanded, right when collapsed. */
    private void drawChevron(DrawContext context, int x, int y, boolean expanded, int color) {
        for (int r = 0; r < 4; r++) {
            if (expanded) {
                context.fill(x + r, y + r, x + 7 - r, y + r + 1, color);
            } else {
                context.fill(x + r, y + r, x + r + 1, y + 7 - r, color);
            }
        }
    }

    private void renderScrollbar(DrawContext context) {
        int max = maxScroll();
        if (max <= 0) return;
        int viewport = getHeight();
        int total = totalContentHeight();
        int sbX = getX() + getWidth() - SCROLLBAR_W;
        context.fill(sbX, getY(), sbX + SCROLLBAR_W, getY() + viewport, 0x40FFFFFF);
        int handleH = Math.max(20, (int) ((long) viewport * viewport / total));
        int handleY = getY() + (int) ((viewport - handleH) * (scrollY / max));
        context.fill(sbX, handleY, sbX + SCROLLBAR_W, handleY + handleH, 0xFFB0B0B0);
    }

    // ---- input ----

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double mx = mouseX;
        double my = mouseY;
        if (button != 0 || !isMouseOver(mx, my)) return false;

        // Scrollbar handle
        int max = maxScroll();
        if (max > 0 && mx >= getX() + getWidth() - SCROLLBAR_W) {
            draggingScrollbar = true;
            updateScrollFromMouse(my);
            return true;
        }

        int y = getY() - (int) scrollY;
        for (Row row : rows) {
            int h = rowHeight(row);
            int w = cardWidth();
            if (row.kind == RowKind.HEADER) {
                y += h + GAP;
                continue;
            }
            // Action buttons (only when expanded) take priority over the header toggle.
            if (row.expanded) {
                int[] primary = primaryRect(getX(), y, h, w);
                if (inRect(primary, mx, my)) {
                    onPrimary(row);
                    return true;
                }
                if (secondaryLabel(row) != null && inRect(secondaryRect(getX(), y, h, w), mx, my)) {
                    onSecondary(row);
                    return true;
                }
            }
            if (mx >= getX() && mx <= getX() + w && my >= y && my <= y + HEADER_H) {
                row.expanded = !row.expanded;
                clampScroll();
                return true;
            }
            y += h + GAP;
        }
        return false;
    }

    private boolean inRect(int[] r, double mx, double my) {
        return mx >= r[0] && mx <= r[0] + r[2] && my >= r[1] && my <= r[1] + r[3];
    }

    private void onPrimary(Row row) {
        switch (row.kind) {
            case BROWSE_IMPORT -> importPreset(row.preset, row.preset.displayAuthor(), 0);
            case INBOX_IMPORT -> importPreset(row.preset, row.preset.displayAuthor(), row.preset.id());
            case UPLOAD_DELETE -> confirmThen(row, () -> screen.deleteUpload(row.preset.id()));
            case SENT_CANCEL -> confirmThen(row, () -> screen.deleteSend(row.preset.id()));
            case HEADER -> { }
        }
    }

    private void onSecondary(Row row) {
        if (row.kind == RowKind.INBOX_IMPORT) screen.deleteSend(row.preset.id()); // Dismiss
    }

    /** Two-step confirm: first click arms a short window, second click within it runs the action. */
    private void confirmThen(Row row, Runnable action) {
        long now = System.currentTimeMillis();
        if (row.confirmActionUntil > now) {
            row.confirmActionUntil = 0;
            action.run();
        } else {
            row.confirmActionUntil = now + CONFIRM_DELETE_MS;
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double offsetX, double offsetY) {
        if (draggingScrollbar) {
            updateScrollFromMouse(mouseY);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingScrollbar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void updateScrollFromMouse(double my) {
        int max = maxScroll();
        if (max <= 0) return;
        int viewport = getHeight();
        int total = totalContentHeight();
        int handleH = Math.max(20, (int) ((long) viewport * viewport / total));
        double track = viewport - handleH;
        if (track <= 0) return;
        double rel = (my - getY() - handleH / 2.0) / track;
        scrollY = Math.max(0, Math.min(max, rel * max));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!isMouseOver(mouseX, mouseY)) return false;
        scrollY -= verticalAmount * 16;
        clampScroll();
        return true;
    }

    /**
     * Deserializes the preset into a new local profile, recording its author for the globe tooltip.
     * When {@code dismissSendId > 0} (an inbox import) the originating send is removed afterwards.
     */
    private void importPreset(OnlinePreset preset, String author, int dismissSendId) {
        KeyValuePair<KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>, int[]>, ArrayList<Integer>> profile =
                AddProfileScreen.deserializeFromString(preset.data());
        if (profile == null) {
            screen.flashMessage(Text.translatable("firorize.config.status.importFailed"), true);
            return;
        }
        String name = uniqueName(preset.displayTitle());
        screen.parent.presetListWidget.addProfile(name, profile);
        // Mark this local profile as imported so the preset list shows the online marker (persisted),
        // and remember who it came from for the tooltip. Inbox imports get the person/"Sent by" marker.
        Main.CONFIG_MANAGER.getImportedProfiles().add(name);
        Main.CONFIG_MANAGER.getImportedAuthors().put(name, author);
        if (dismissSendId > 0) Main.CONFIG_MANAGER.getInboxImports().add(name);
        Main.CONFIG_MANAGER.save();
        screen.flashMessage(Text.translatable("firorize.config.status.imported", name), false);
        if (dismissSendId > 0) screen.deleteSend(dismissSendId);
    }

    private String uniqueName(String base) {
        var keys = Main.CONFIG_MANAGER.getFireColorPresets().keySet();
        if (keys.stream().noneMatch(base::equalsIgnoreCase)) return base;
        int n = 2;
        String candidate;
        do {
            candidate = base + " (" + n++ + ")";
        } while (keys.stream().anyMatch(candidate::equalsIgnoreCase));
        return candidate;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        // Narration intentionally minimal; the dialog title narrates the context.
    }

    private final class Row {
        final RowKind kind;
        final OnlinePreset preset;   // null for HEADER
        final Text headerLabel;      // non-null only for HEADER
        boolean expanded = false;
        long confirmActionUntil = 0;
        private List<OrderedText> cachedLines;
        private int cachedWidth = -1;

        private Row(RowKind kind, OnlinePreset preset, Text headerLabel) {
            this.kind = kind;
            this.preset = preset;
            this.headerLabel = headerLabel;
        }

        List<OrderedText> descriptionLines() {
            String desc = preset.displayDescription();
            if (desc.isBlank()) return List.of();
            int w = wrapWidth();
            if (cachedLines == null || cachedWidth != w) {
                cachedLines = textRenderer.wrapLines(Text.literal(desc), w);
                cachedWidth = w;
            }
            return cachedLines;
        }
    }
}
