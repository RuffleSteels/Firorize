package com.oscimate.oscimate_soulflame.config;

import com.oscimate.oscimate_soulflame.Main;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class UndoButton  extends ButtonWidget {
    protected UndoButton(int x, int y, int width, int height, PressAction onPress) {
        super(x, y, width, height, null, onPress, DEFAULT_NARRATION_SUPPLIER);
    }

    @Override
    public void drawMessage(DrawContext context, TextRenderer textRenderer, int color) {
        context.drawSprite(getX() + (getWidth() - Main.UNDO.getContents().getWidth())/2, getY() + (getHeight() - Main.UNDO.getContents().getHeight())/2, 10, Main.UNDO.getContents().getWidth(), Main.UNDO.getContents().getHeight(), Main.UNDO);
    }
}
