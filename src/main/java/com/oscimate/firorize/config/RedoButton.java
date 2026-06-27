package com.oscimate.firorize.config;

import com.oscimate.firorize.FireSprites;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public class RedoButton extends Button {
    protected RedoButton(int x, int y, int width, int height, Button.OnPress onPress) {
        super(x, y, width, height, net.minecraft.network.chat.Component.empty(), onPress, DEFAULT_NARRATION);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        TextureAtlasSprite REDO = FireSprites.block(FireSprites.atlasManager(), "firorize:block/redo");
        context.blitSprite(RenderPipelines.GUI_TEXTURED, REDO,
                getX() + (getWidth() - REDO.contents().width()) / 2,
                getY() + (getHeight() - REDO.contents().height()) / 2,
                REDO.contents().width(), REDO.contents().height());
    }
}
