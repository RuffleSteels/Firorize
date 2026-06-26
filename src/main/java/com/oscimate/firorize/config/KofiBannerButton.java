package com.oscimate.firorize.config;

import com.oscimate.firorize.FireSprites;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * Slim, persistent "Support Firorize on Ko-fi" banner on the entry {@link ConfigScreen}. Designed to
 * be noticeable but unobtrusive: a dark pill with the Ko-fi-red accent border and cup icon that fills
 * with Ko-fi red on hover to invite a click. Pressing it opens the Ko-fi page via the vanilla
 * confirm-link dialog.
 */
public class KofiBannerButton extends Button {
    private static final int KOFI_RED = 0xFFFF5E5B;
    private static final int IDLE_BG = 0xFF2A1416;

    protected KofiBannerButton(Screen parent, int x, int y, int width, int height) {
        super(x, y, width, height, net.minecraft.network.chat.Component.translatable("firorize.donate.banner"),
                openKofi(parent), DEFAULT_NARRATION);
    }

    // Concretely-typed Button.OnPress so the super(...) call doesn't trip lambda overload inference.
    private static Button.OnPress openKofi(Screen parent) {
        return button -> ConfirmLinkScreen.open(parent, DonationTracker.KOFI_URL);
    }

    @Override
    @SuppressWarnings("deprecation") // FireSprites uses the still-supported BLOCK_ATLAS_TEXTURE id
    protected void drawIcon(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        int x1 = getX(), y1 = getY(), x2 = getX() + getWidth(), y2 = getY() + getHeight();
        boolean hovered = isHovered();

        context.fill(x1, y1, x2, y2, hovered ? KOFI_RED : IDLE_BG);
        context.drawStrokedRectangle(x1, y1, getWidth(), getHeight(), KOFI_RED);

        int iconSize = 12;
        int iconX = x1 + 8;
        int iconY = y1 + (getHeight() - iconSize) / 2;
        TextureAtlasSprite kofi = FireSprites.block(FireSprites.atlasManager(), "firorize:block/kofi");
        context.drawSpriteStretched(RenderPipelines.GUI_TEXTURED, kofi, iconX, iconY, iconSize, iconSize);

        int centerX = (iconX + iconSize + x2) / 2;
        context.drawCenteredTextWithShadow(Minecraft.getInstance().font, getMessage(),
                centerX, y1 + (getHeight() - 8) / 2, hovered ? 0xFFFFFFFF : 0xFFFFC8C6);
    }
}
