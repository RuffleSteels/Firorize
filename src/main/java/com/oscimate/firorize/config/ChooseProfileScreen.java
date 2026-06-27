package com.oscimate.firorize.config;

import com.oscimate.firorize.Main;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

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
        super(Component.translatable("firorize.config.title.chooseProfile"));
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
        addRenderableWidget(list);

        int btnW = (boxW - 20 - 6) / 2;
        Button uploadButton = new Button.Builder(Component.translatable("firorize.config.button.uploadOnline"), b -> proceed(false))
                .bounds(boxX + 10, boxY + boxH - 26, btnW, 20).build();
        Button sendButton = new Button.Builder(Component.translatable("firorize.config.button.sendToFriend"), b -> proceed(true))
                .bounds(boxX + 10 + btnW + 6, boxY + boxH - 26, btnW, 20).build();
        uploadButton.active = !names.isEmpty();
        sendButton.active = !names.isEmpty();
        addRenderableWidget(uploadButton);
        addRenderableWidget(sendButton);

        addRenderableWidget(new Button.Builder(Component.literal("x"), b -> close())
                .bounds(boxX + boxW - 22, boxY + 6, 16, 16).build());

        super.init();
    }

    private void proceed(boolean privateMode) {
        String name = list.selectedName();
        if (name == null) return;
        minecraft.setScreen(new UploadPresetScreen(this, origin, online, name, privateMode));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        this.setFocused(null);
        return super.mouseClicked(click, doubled);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(origin);
    }

    @Override
    public void resize(int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        Main.setScale(width, height, minecraft);
        super.resize(minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        // Overlay the screen we came from (the config editor, or the online dialog) dimmed, rather than
        // cutting through to the blurred game.
        ChangeFireColorScreen.renderModalBackdrop(context, origin, delta);
        context.fill(0, 0, this.width, this.height, 0xB0000000);
        context.fill(boxX - 1, boxY - 1, boxX + boxW + 1, boxY + boxH + 1, 0xFF000000);
        context.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xFF1A1A1A);
        context.outline(boxX, boxY, boxW, boxH, 0xFF8B8B8B);
        context.text(font, getTitle(), boxX + 10, boxY + 9, 0xFFFFFFFF);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        if (names.isEmpty()) {
            context.centeredText(font, Component.translatable("firorize.config.status.noProfiles"),
                    width / 2, boxY + boxH / 2 - 4, 0xFFC0C0C0);
        }
    }

    /** Compact scrollable list of profile names with single-selection, styled like the online list. */
    private final class ProfileList extends AbstractWidget {
        private static final int ROW_H = 18;
        private static final int SCROLLBAR_W = 4;

        private double scrollY = 0;
        private boolean draggingScrollbar = false;
        private int selected = -1;

        ProfileList(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty());
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
        protected void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
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
                    context.outline(left, y, rowW, ROW_H, sel ? 0xFFB0C4E0 : 0xFF333333);
                    String name = font.plainSubstrByWidth(names.get(i), rowW - 12);
                    context.text(font, Component.literal(name), left + 6, y + (ROW_H - 8) / 2, 0xFFFFFFFF);
                }
                y += ROW_H;
            }
            context.disableScissor();
            renderScrollbar(context);
        }

        private void renderScrollbar(GuiGraphicsExtractor context) {
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
        public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
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
        public boolean mouseDragged(MouseButtonEvent click, double offsetX, double offsetY) {
            if (draggingScrollbar) {
                updateScrollFromMouse(click.y());
                return true;
            }
            return false;
        }

        @Override
        public boolean mouseReleased(MouseButtonEvent click) {
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
        protected void updateWidgetNarration(NarrationElementOutput builder) {
        }
    }
}
