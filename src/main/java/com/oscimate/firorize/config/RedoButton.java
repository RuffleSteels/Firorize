package com.oscimate.firorize.config;

import com.oscimate.firorize.FireSprites;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.texture.Sprite;

public class RedoButton extends Button {
    protected RedoButton(int x, int y, int width, int height, PressAction onPress) {
        super(x, y, width, height, net.minecraft.network.chat.Component.empty(), onPress, DEFAULT_NARRATION_SUPPLIER);
    }

    @Override
    protected void drawIcon(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        this.drawButton(context); // renderWidget no longer draws the button background
        Sprite REDO = FireSprites.block(FireSprites.atlasManager(), "firorize:block/redo");
        context.drawSpriteStretched(RenderPipelines.GUI_TEXTURED, REDO,
                getX() + (getWidth() - REDO.getContents().getWidth()) / 2,
                getY() + (getHeight() - REDO.getContents().getHeight()) / 2,
                REDO.getContents().getWidth(), REDO.getContents().getHeight());
    }
}
