package com.oscimate.firorize.config;

import com.oscimate.firorize.FireSprites;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.texture.Sprite;

public class UndoButton  extends ButtonWidget {
    protected UndoButton(int x, int y, int width, int height, PressAction onPress) {
        super(x, y, width, height, net.minecraft.text.Text.empty(), onPress, DEFAULT_NARRATION_SUPPLIER);
    }

    @Override
    protected void drawIcon(DrawContext context, int mouseX, int mouseY, float delta) {
        this.drawButton(context); // renderWidget no longer draws the button background
        Sprite UNDO = FireSprites.block(FireSprites.atlasManager(), "firorize:block/undo");
        context.drawSpriteStretched(RenderPipelines.GUI_TEXTURED, UNDO,
                getX() + (getWidth() - UNDO.getContents().getWidth()) / 2,
                getY() + (getHeight() - UNDO.getContents().getHeight()) / 2,
                UNDO.getContents().getWidth(), UNDO.getContents().getHeight());
    }
}
