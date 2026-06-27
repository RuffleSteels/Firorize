package com.oscimate.firorize.config;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

public class InvisibleTextFieldWidget extends PlaceholderField {
    private final ChangeFireColorScreen instance;

    public InvisibleTextFieldWidget(ChangeFireColorScreen instance, Font font, int x, int y, int width, int height, Component text) {
        super(font, x, y, width, height, text);
        this.instance = instance;
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharacterEvent input) {
        char chr = (char) input.codepoint();
        if (!String.valueOf(chr).matches("[^A-Za-z0-9 ]") && !String.valueOf(chr).equals(" ")) {
            return super.charTyped(input);
        }
        return false;
    }
}
