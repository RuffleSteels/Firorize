package com.oscimate.firorize.config;

import com.oscimate.firorize.FireSprites;
import com.oscimate.firorize.Main;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Component;

import java.time.Duration;

/**
 * Gentle Ko-fi donation popup, surfaced by {@link DonationTracker} after every 20 minutes of total
 * config time. Styled as a centred dark box over the dimmed config screen, matching
 * {@link AddProfileScreen}/{@link ChangeFireColorScreen#renderConfirm}. Easily dismissible — the
 * "x", "Maybe later", ESC, and clicking outside the box all return to the config screen the player
 * was on; the "Support" button opens the Ko-fi page and likewise returns there afterwards.
 */
public class DonatePopupScreen extends Screen {
    private final Screen parent;
    private int boxX, boxY, boxW, boxH;

    public DonatePopupScreen(Screen parent) {
        super(Component.translatable("firorize.donate.popup.title"));
        this.parent = parent;
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent click, boolean doubled) {
        this.setFocused(null);
        // MouseButtonEvent outside the dialog box dismisses it (returns to the config screen).
        if (click.x() < boxX || click.x() > boxX + boxW || click.y() < boxY || click.y() > boxY + boxH) {
            close();
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    protected void init() {
        int pad = 12;
        boxW = Math.min(264, width - 40);
        boxH = 118;
        boxX = (width - boxW) / 2;
        boxY = (height - boxH) / 2;

        Button support = new Button.Builder(
                Component.translatable("firorize.donate.popup.support"),
                button -> ConfirmLinkScreen.open(parent, DonationTracker.KOFI_URL))
                .dimensions(boxX + pad, boxY + boxH - 50, boxW - pad * 2, 20).build();
        support.setTooltip(Tooltip.of(Component.translatable("firorize.donate.tooltip")));
        support.setTooltipDelay(Duration.ofMillis(750L));

        Button later = new Button.Builder(
                Component.translatable("firorize.donate.popup.later"), button -> close())
                .dimensions(boxX + pad, boxY + boxH - 26, boxW - pad * 2, 18).build();

        this.addDrawableChild(new Button.Builder(Component.literal("x"), button -> close())
                .dimensions(boxX + boxW - 22, boxY + 6, 16, 16).build());
        this.addDrawableChild(support);
        this.addDrawableChild(later);
        super.init();
        Main.inConfig = true;
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    @Override
    public void resize(int width, int height) {
        Minecraft client = Minecraft.getInstance();
        Main.setScale(width, height, client);
        super.resize(client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight());
    }

    @Override
    @SuppressWarnings("deprecation") // FireSprites uses the still-supported BLOCK_ATLAS_TEXTURE id
    public void renderBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        // Draw the config screen behind (deferred 3D/colour-wheel elements suppressed so they don't
        // composite over this popup), then dim and the box.
        ChangeFireColorScreen.renderModalBackdrop(context, parent, delta);
        context.fill(0, 0, this.width, this.height, 0xB0000000);
        context.fill(boxX - 1, boxY - 1, boxX + boxW + 1, boxY + boxH + 1, 0xFF000000);
        context.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xFF1A1A1A);
        context.drawStrokedRectangle(boxX, boxY, boxW, boxH, 0xFF8B8B8B);

        // Ko-fi cup icon next to the heading.
        TextureAtlasSprite kofi = FireSprites.block(FireSprites.atlasManager(), "firorize:block/kofi");
        context.drawSpriteStretched(RenderPipelines.GUI_TEXTURED, kofi, boxX + 12, boxY + 9, 12, 12);
        context.drawTextWithShadow(font, getTitle(), boxX + 28, boxY + 11, 0xFFFFFFFF);
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int ty = boxY + 34;
        for (FormattedCharSequence line : font.wrapLines(Component.translatable("firorize.donate.popup.body"), boxW - 24)) {
            context.drawCenteredTextWithShadow(font, line, width / 2, ty, 0xFFC0C0C0);
            ty += 11;
        }
    }
}
