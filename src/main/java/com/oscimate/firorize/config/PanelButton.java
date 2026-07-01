package com.oscimate.firorize.config;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;

/**
 * A flat, panel-styled button matching the dark Firorize dialogs (background {@code 0xFF3A3A3A},
 * {@code 0xFF8B8B8B} outline that brightens on hover) instead of the raised vanilla button sprite.
 * Used across the Import Profiles hub so the whole section reads as one cohesive surface. The label is
 * drawn centred, matching the other custom buttons on this screen.
 */
public class PanelButton extends ButtonWidget {
    // net.minecraft.text.Text is fully-qualified because ButtonWidget has a nested Text type that
    // shadows the import inside subclasses (same reason KofiBannerButton fully-qualifies it).
    public PanelButton(int x, int y, int width, int height, net.minecraft.text.Text label, PressAction onPress) {
        super(x, y, width, height, label, onPress, DEFAULT_NARRATION_SUPPLIER);
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean hover = isHovered() || isFocused();
        int bg = !active ? 0xFF2A2A2A : (hover ? 0xFF4A4A4A : 0xFF3A3A3A);
        int border = !active ? 0xFF555555 : (hover ? 0xFFFFFFFF : 0xFF8B8B8B);
        context.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), bg);
        context.drawBorder(getX(), getY(), getWidth(), getHeight(), border);
        context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, getMessage(),
                getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2, active ? 0xFFFFFFFF : 0xFFA0A0A0);
    }
}
