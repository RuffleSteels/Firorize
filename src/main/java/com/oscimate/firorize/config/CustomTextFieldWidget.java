package com.oscimate.firorize.config;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public class CustomTextFieldWidget extends EditBox {
    private ChangeFireColorScreen instance;
    private final boolean thing;
    public CustomTextFieldWidget(Font font, int x, int y, int width, int height, Component text, ChangeFireColorScreen instance, boolean thing) {
        super(font, x, y, width, height, text);
        this.instance = instance;
        this.thing = thing;
    }

    private void thing() {
        if (thing) {
            instance.updateCursor(this.getValue());
        } else {
            instance.input = this.getValue();
            instance.searchScreenListWidget.test();
            instance.searchScreenListWidget.selected.clear();
        }
    }

    @Override
    public void insertText(String text) {
        super.insertText(text);
        thing();
    }

    @Override
    public void deleteChars(int characterOffset) {
        super.deleteChars(characterOffset);
        thing();
    }

}
