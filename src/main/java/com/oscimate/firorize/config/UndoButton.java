package com.oscimate.firorize.config;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.util.Identifier;

public class UndoButton  extends ButtonWidget {
    protected UndoButton(int x, int y, int width, int height, PressAction onPress) {
        super(x, y, width, height, null, onPress, DEFAULT_NARRATION_SUPPLIER);
    }

    @Override
    public void drawMessage(DrawContext context, TextRenderer textRenderer, int color) {
        Sprite UNDO = new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, Identifier.of("firorize:block/undo")).getSprite();
        context.drawSprite(getX() + (getWidth() - UNDO.getContents().getWidth())/2, getY() + (getHeight() - UNDO.getContents().getHeight())/2, 10, UNDO.getContents().getWidth(), UNDO.getContents().getHeight(), UNDO);
    }
}
