package com.oscimate.firorize.config;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.util.Identifier;

public class RedoButton extends ButtonWidget {
    protected RedoButton(int x, int y, int width, int height, PressAction onPress) {
        super(x, y, width, height, null, onPress, DEFAULT_NARRATION_SUPPLIER);
    }

    @Override
    @SuppressWarnings("deprecation") // SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE is deprecated but still the supported atlas id in 1.21
    public void drawMessage(DrawContext context, TextRenderer textRenderer, int color) {
        // Reuse the undo sprite, drawn horizontally mirrored (via swapped U coords) so it reads as a
        // redo arrow. Flipping the UVs instead of the matrix keeps the quad winding intact, so the
        // GUI does not cull it (a matrix scale(-1,1,1) renders blank).
        Sprite UNDO = new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, Identifier.of("firorize:block/undo")).getSprite();
        int w = UNDO.getContents().getWidth();
        int h = UNDO.getContents().getHeight();
        int dx = getX() + (getWidth() - w) / 2;
        int dy = getY() + (getHeight() - h) / 2;
        context.drawTexturedQuad(UNDO.getAtlasId(), dx, dx + w, dy, dy + h, 10,
                UNDO.getMaxU(), UNDO.getMinU(), UNDO.getMinV(), UNDO.getMaxV());
    }
}
