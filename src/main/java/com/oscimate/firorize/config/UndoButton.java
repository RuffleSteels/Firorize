package com.oscimate.firorize.config;

import com.oscimate.firorize.FireSprites;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.texture.Sprite;

public class UndoButton  extends Button {
    protected UndoButton(int x, int y, int width, int height, PressAction onPress) {
        super(x, y, width, height, net.minecraft.network.chat.Component.empty(), onPress, DEFAULT_NARRATION_SUPPLIER);
    }

    @Override
    protected void drawIcon(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        this.drawButton(context); // renderWidget no longer draws the button background
        Sprite UNDO = FireSprites.block(FireSprites.atlasManager(), "firorize:block/undo");
        context.drawSpriteStretched(RenderPipelines.GUI_TEXTURED, UNDO,
                getX() + (getWidth() - UNDO.getContents().getWidth()) / 2,
                getY() + (getHeight() - UNDO.getContents().getHeight()) / 2,
                UNDO.getContents().getWidth(), UNDO.getContents().getHeight());
    }
}
