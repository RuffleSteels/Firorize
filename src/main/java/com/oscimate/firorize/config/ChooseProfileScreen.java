package com.oscimate.firorize.config;

import com.oscimate.firorize.Main;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * First step of the share flow: pick which local profile to share from a selectable list, then choose
 * <b>Upload online</b> (public gallery) or <b>Send to a friend</b> (private). The chosen action drops
 * straight into {@link UploadPresetScreen} pre-set to that mode, replacing the old per-screen
 * "Private" toggle. Reached from the colour editor's share button and the online screen's
 * Upload/Send buttons.
 */
public class ChooseProfileScreen extends Screen {
    private final Screen origin;               // returned to on cancel/success
    private final OnlinePresetsScreen online;  // non-null when reached from the online screen; refreshed on success
    private final List<String> names;

    private ProfileList list;
    private int boxX, boxY, boxW, boxH;

    public ChooseProfileScreen(Screen origin, OnlinePresetsScreen online) {
        super(Text.translatable("firorize.config.title.chooseProfile"));
        this.origin = origin;
        this.online = online;
        this.names = new ArrayList<>(Main.CONFIG_MANAGER.getFireColorPresets().keySet());
    }

    @Override
    protected void init() {
        Main.inConfig = true;
        boxW = Math.min(300, width - 40);
        boxH = Math.min(240, height - 40);
        boxX = (width - boxW) / 2;
        boxY = (height - boxH) / 2;

        int listY = boxY + 28;
        int listH = boxH - 28 - 34;
        list = new ProfileList(boxX + 10, listY, boxW - 20, listH);
        int idx = names.indexOf(Main.CONFIG_MANAGER.getCurrentPreset());
        list.select(idx >= 0 ? idx : 0);
        addDrawableChild(list);

        int btnW = (boxW - 20 - 6) / 2;
        ButtonWidget uploadButton = new ButtonWidget.Builder(Text.translatable("firorize.config.button.uploadOnline"), b -> proceed(false))
                .dimensions(boxX + 10, boxY + boxH - 26, btnW, 20).build();
        ButtonWidget sendButton = new ButtonWidget.Builder(Text.translatable("firorize.config.button.sendToFriend"), b -> proceed(true))
                .dimensions(boxX + 10 + btnW + 6, boxY + boxH - 26, btnW, 20).build();
        uploadButton.active = !names.isEmpty();
        sendButton.active = !names.isEmpty();
        addDrawableChild(uploadButton);
        addDrawableChild(sendButton);

        addDrawableChild(new ButtonWidget.Builder(Text.literal("x"), b -> close())
                .dimensions(boxX + boxW - 22, boxY + 6, 16, 16).build());

        super.init();
    }

    private void proceed(boolean privateMode) {
        String name = list.selectedName();
        if (name == null) return;
        client.setScreen(new UploadPresetScreen(this, origin, online, name, privateMode));
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        this.setFocused(null);
        return super.mouseClicked(click, doubled);
    }

    @Override
    public void close() {
        client.setScreen(origin);
    }

    @Override
    public void resize(int width, int height) {
        MinecraftClient client = MinecraftClient.getInstance();
        Main.setScale(width, height, client);
        super.resize(client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight());
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
        context.fill(0, 0, this.width, this.height, 0x50000000);
        context.fill(boxX - 1, boxY - 1, boxX + boxW + 1, boxY + boxH + 1, 0xFF000000);
        context.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xFF1A1A1A);
        context.drawStrokedRectangle(boxX, boxY, boxW, boxH, 0xFF8B8B8B);
        context.drawTextWithShadow(textRenderer, getTitle(), boxX + 10, boxY + 9, 0xFFFFFFFF);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        if (names.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, Text.translatable("firorize.config.status.noProfiles"),
                    width / 2, boxY + boxH / 2 - 4, 0xFFC0C0C0);
        }
    }

    /** Compact scrollable list of profile names with single-selection, styled like the online list. */
    private final class ProfileList extends ClickableWidget {
        private static final int ROW_H = 18;
        private static final int SCROLLBAR_W = 4;

        private double scrollY = 0;
        private boolean draggingScrollbar = false;
        private int selected = -1;

        ProfileList(int x, int y, int width, int height) {
            super(x, y, width, height, Text.empty());
        }

        void select(int index) {
            selected = index;
        }

        String selectedName() {
            return selected >= 0 && selected < names.size() ? names.get(selected) : null;
        }

        private int contentHeight() {
            return names.size() * ROW_H;
        }

        private int maxScroll() {
            return Math.max(0, contentHeight() - getHeight());
        }

        private void clampScroll() {
            if (scrollY < 0) scrollY = 0;
            if (scrollY > maxScroll()) scrollY = maxScroll();
        }

        @Override
        protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            clampScroll();
            int left = getX(), top = getY(), right = getX() + getWidth(), bottom = getY() + getHeight();
            context.fill(left, top, right, bottom, 0xFF141414);
            context.enableScissor(left, top, right, bottom);
            int y = top - (int) scrollY;
            int rowW = getWidth() - SCROLLBAR_W - 2;
            for (int i = 0; i < names.size(); i++) {
                if (y + ROW_H >= top && y <= bottom) {
                    boolean sel = i == selected;
                    boolean hover = mouseX >= left && mouseX <= left + rowW && mouseY >= y && mouseY <= y + ROW_H
                            && mouseY >= top && mouseY <= bottom;
                    if (sel || hover) context.fill(left, y, left + rowW, y + ROW_H, sel ? 0xFF3A5A8A : 0xFF262626);
                    context.drawStrokedRectangle(left, y, rowW, ROW_H, sel ? 0xFFB0C4E0 : 0xFF333333);
                    String name = textRenderer.trimToWidth(names.get(i), rowW - 12);
                    context.drawTextWithShadow(textRenderer, Text.literal(name), left + 6, y + (ROW_H - 8) / 2, 0xFFFFFFFF);
                }
                y += ROW_H;
            }
            context.disableScissor();
            renderScrollbar(context);
        }

        private void renderScrollbar(DrawContext context) {
            int max = maxScroll();
            if (max <= 0) return;
            int viewport = getHeight();
            int total = contentHeight();
            int sbX = getX() + getWidth() - SCROLLBAR_W;
            context.fill(sbX, getY(), sbX + SCROLLBAR_W, getY() + viewport, 0x40FFFFFF);
            int handleH = Math.max(20, (int) ((long) viewport * viewport / total));
            int handleY = getY() + (int) ((viewport - handleH) * (scrollY / max));
            context.fill(sbX, handleY, sbX + SCROLLBAR_W, handleY + handleH, 0xFFB0B0B0);
        }

        @Override
        public boolean mouseClicked(Click click, boolean doubled) {
            double mx = click.x(), my = click.y();
            if (click.button() != 0 || !isMouseOver(mx, my)) return false;
            int max = maxScroll();
            if (max > 0 && mx >= getX() + getWidth() - SCROLLBAR_W) {
                draggingScrollbar = true;
                updateScrollFromMouse(my);
                return true;
            }
            int rel = (int) (my - getY() + scrollY);
            int idx = rel / ROW_H;
            if (rel >= 0 && idx >= 0 && idx < names.size()) {
                selected = idx;
                return true;
            }
            return false;
        }

        @Override
        public boolean mouseDragged(Click click, double offsetX, double offsetY) {
            if (draggingScrollbar) {
                updateScrollFromMouse(click.y());
                return true;
            }
            return false;
        }

        @Override
        public boolean mouseReleased(Click click) {
            draggingScrollbar = false;
            return super.mouseReleased(click);
        }

        private void updateScrollFromMouse(double my) {
            int max = maxScroll();
            if (max <= 0) return;
            int viewport = getHeight();
            int total = contentHeight();
            int handleH = Math.max(20, (int) ((long) viewport * viewport / total));
            double track = viewport - handleH;
            if (track <= 0) return;
            double r = (my - getY() - handleH / 2.0) / track;
            scrollY = Math.max(0, Math.min(max, r * max));
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
            if (!isMouseOver(mouseX, mouseY)) return false;
            scrollY -= verticalAmount * 16;
            clampScroll();
            return true;
        }

        @Override
        protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        }
    }
}
