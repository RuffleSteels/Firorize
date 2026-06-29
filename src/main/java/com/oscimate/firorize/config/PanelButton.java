package com.oscimate.firorize.config;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * A flat, panel-styled button matching the dark Firorize dialogs (background {@code 0xFF1A1A1A},
 * {@code 0xFF8B8B8B} outline that brightens on hover) instead of the raised vanilla button sprite.
 * Used across the Import Profiles hub so the whole section reads as one cohesive surface. The label is
 * still drawn by the vanilla centred-text path; only the background/outline are customised.
 */
public class PanelButton extends Button {
    public PanelButton(int x, int y, int width, int height, Component label, OnPress onPress) {
        super(x, y, width, height, label, onPress, DEFAULT_NARRATION);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        boolean hover = isHoveredOrFocused();
        int bg = !active ? 0xFF2A2A2A : (hover ? 0xFF4A4A4A : 0xFF3A3A3A);
        int border = !active ? 0xFF555555 : (hover ? 0xFFFFFFFF : 0xFF8B8B8B);
        context.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), bg);
        context.outline(getX(), getY(), getWidth(), getHeight(), border);
        // Draw the label exactly like a vanilla Button (centred, correct active/inactive colour);
        // overriding extractContents otherwise drops the default label entirely.
        extractDefaultLabel(context.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE));
    }
}
