package com.oscimate.firorize.config;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public class PlaceholderField extends TextFieldWidget {
    private Text placeholder;
    private TextRenderer textRenderer;

    public PlaceholderField(TextRenderer textRenderer, int width, int height, Text text) {
        super(textRenderer, width, height, text);
        this.textRenderer = textRenderer;
    }

    public PlaceholderField(TextRenderer textRenderer, int x, int y, int width, int height, Text text) {
        super(textRenderer, x, y, width, height, text);
        this.textRenderer = textRenderer;
    }

    public PlaceholderField(TextRenderer textRenderer, int x, int y, int width, int height, @Nullable TextFieldWidget copyFrom, Text text) {
        super(textRenderer, x, y, width, height, copyFrom, text);
        this.textRenderer = textRenderer;
    }

    @Override
    public void setPlaceholder(Text placeholder) {
        this.placeholder = placeholder;
    }

    private final int placeholderColor = new Color(128, 128, 128).getRGB();

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderWidget(context, mouseX, mouseY, delta);
        int k = this.drawsBackground() ? this.getX() + 4 : this.getX();
        int l = this.drawsBackground() ? this.getY() + (this.height - 8) / 2 : this.getY();
        int m = k;

        if (placeholder != null && getText().isEmpty() && !this.isFocused()) {
            context.drawTextWithShadow(this.textRenderer, this.placeholder, m, l, placeholderColor);
        }
    }
}
