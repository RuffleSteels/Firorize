package com.oscimate.firorize.config;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public class PlaceholderField extends EditBox {
    private Component placeholder;
    private Font font;

    public PlaceholderField(Font font, int width, int height, Component text) {
        super(font, width, height, text);
        this.font = font;
    }

    public PlaceholderField(Font font, int x, int y, int width, int height, Component text) {
        super(font, x, y, width, height, text);
        this.font = font;
    }

    public PlaceholderField(Font font, int x, int y, int width, int height, @Nullable EditBox copyFrom, Component text) {
        super(font, x, y, width, height, copyFrom, text);
        this.font = font;
    }

    @Override
    public void setHint(Component placeholder) {
        this.placeholder = placeholder;
    }

    private final int placeholderColor = new Color(128, 128, 128).getRGB();

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractWidgetRenderState(context, mouseX, mouseY, delta);
        int k = this.isBordered() ? this.getX() + 4 : this.getX();
        int l = this.isBordered() ? this.getY() + (this.height - 8) / 2 : this.getY();
        int m = k;

        if (placeholder != null && getValue().isEmpty() && !this.isFocused()) {
            context.text(this.font, this.placeholder, m, l, placeholderColor);
        }
    }
}
