package com.oscimate.firorize.config;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;

public class InvisibleTextFieldWidget extends PlaceholderField {
    private final ChangeFireColorScreen instance;

    public InvisibleTextFieldWidget(ChangeFireColorScreen instance, TextRenderer textRenderer, int x, int y, int width, int height, Text text) {
        super(textRenderer, x, y, width, height, text);
        this.instance = instance;
    }


    @Override
    public void setCursor(int cursor, boolean shiftKeyPressed) {
        super.setCursor(cursor, shiftKeyPressed);
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharInput input) {
        char chr = (char) input.codepoint();
        if (!String.valueOf(chr).matches("[^A-Za-z0-9 ]") && !String.valueOf(chr).equals(" ")) {
            return super.charTyped(input);
        }
        return false;
    }
}
