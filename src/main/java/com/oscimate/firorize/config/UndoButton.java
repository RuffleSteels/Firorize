package com.oscimate.firorize.config;

import com.oscimate.firorize.FireSprites;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public class UndoButton  extends Button {
    protected UndoButton(int x, int y, int width, int height, Button.OnPress onPress) {
        super(x, y, width, height, net.minecraft.network.chat.Component.empty(), onPress, DEFAULT_NARRATION);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractContents(context, mouseX, mouseY, delta); // extractWidgetRenderState no longer draws the button background
        TextureAtlasSprite UNDO = FireSprites.block(FireSprites.atlasManager(), "firorize:block/undo");
        context.blitSprite(RenderPipelines.GUI_TEXTURED, UNDO,
                getX() + (getWidth() - UNDO.contents().width()) / 2,
                getY() + (getHeight() - UNDO.contents().height()) / 2,
                UNDO.contents().width(), UNDO.contents().height());
    }
}
