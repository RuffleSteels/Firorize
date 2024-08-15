package com.oscimate.firorize.config;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class CustomTextFieldWidget extends TextFieldWidget {
    private ChangeFireColorScreen instance;
    private final boolean thing;
    public CustomTextFieldWidget(TextRenderer textRenderer, int x, int y, int width, int height, Text text, ChangeFireColorScreen instance, boolean thing) {
        super(textRenderer, x, y, width, height, text);
        this.instance = instance;
        this.thing = thing;
    }

    private void thing() {
        if (thing) {
            instance.updateCursor(this.getText());
        } else {
            instance.input = this.getText();
            instance.searchScreenListWidget.test();
            instance.setRedo(false);
            instance.searchScreenListWidget.selected.clear();
        }
    }

    @Override
    public void write(String text) {
        super.write(text);
        thing();
    }

    @Override
    public void eraseCharacters(int characterOffset) {
        super.eraseCharacters(characterOffset);
        thing();
    }

}
