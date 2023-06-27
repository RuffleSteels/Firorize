package com.oscimate.oscimate_soulflame.config;

import com.mojang.blaze3d.systems.RenderSystem;
import com.oscimate.oscimate_soulflame.FireLogic;
import com.oscimate.oscimate_soulflame.Main;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.List;

public class ConfigScreen extends Screen {

    private final Integer buttonWidth = 130;
    private final Screen parent;
    protected static final int windowWidth = 176;
    protected static final int windowHeight = 182;
    private final Identifier WINDOW = new Identifier("oscimate_soulflame", "textures/gui/info_box.png");
    protected int guiTop, guiLeft;

    protected ConfigScreen(Text title, Screen parent) {
        super(title);
        this.parent = parent;
    }

    @Override
    protected void init() {
        CyclingButtonWidget<FireLogic> enabledButton = CyclingButtonWidget.builder(FireLogic::getTranslatableName)
                .values(FireLogic.values())
                .initially(Main.CONFIG_MANAGER.getStartupConfig())
                .tooltip(FireLogic::getTranslatableTooltip)
                .build(width / 2 - (buttonWidth / 2), buttonWidth / 2, buttonWidth, 20, Text.literal("Fire Logic"), (button, fireLogic) -> {
                    Main.CONFIG_MANAGER.setCurrentFireLogic(fireLogic);
                });
        addDrawableChild(enabledButton);
        addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, (button) -> onClose()).dimensions(width / 2 - 100, (int) (height * 0.8F), 200, 20).build());
        super.init();
    }

    private void renderOriginWindow(DrawContext context, int mouseX, int mouseY) {
        context.drawTexture(WINDOW, width / 2 - (windowWidth / 2), height / 2 - (windowHeight / 2), 0, 0, windowWidth, windowHeight);
        Text title = Text.literal("ImprovedFireOverlay");
        context.drawCenteredTextWithShadow(textRenderer, title.getString(), width / 2, guiTop - 15, 0xFFFFFF);
        renderOriginContent(context, mouseX, mouseY);
        RenderSystem.disableBlend();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        renderOriginWindow(context, mouseX, mouseY);
        context.drawCenteredTextWithShadow(textRenderer, "Improved Fire Overlay", width / 2, 20, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    public void onClose() {
        client.setScreen(parent);
    }

    @Override
    public void removed() {
        Main.CONFIG_MANAGER.onConfigChange();
    }

    private void renderOriginContent(DrawContext context, int mouseX, int mouseY) {
        int textWidth = windowWidth - 30;
        int titleHeight = (height / 2 - (windowHeight / 2)) + 15;
        int infoHeight = (height / 2 - (windowHeight / 2)) + 15;
        int x = guiLeft + 18;
        int y = guiTop + 50;
        int startY = y;
        int endY = y - 72 + windowHeight;

        Text info = Text.translatable("oscimate_soulflame.config." + Main.CONFIG_MANAGER.getStartupConfig().toString() + ".info");

        List<OrderedText> descLines = textRenderer.wrapLines(info, textWidth);
        for (OrderedText line : descLines) {
            if (y >= startY - 18 && y <= endY + 12) {
                context.drawText(textRenderer, line, width / 2 - (windowWidth / 2) + 15, (((infoHeight + 25) - windowHeight / 2) + 30) + (y + 2), 0xCCCCCC, false);
            }
            y += 12;
        }

        MutableText title = Text.translatable("oscimate_soulflame.config." + Main.CONFIG_MANAGER.getStartupConfig().toString() + ".title");
        int titleWidth = textRenderer.getWidth(title);
        context.drawText(textRenderer, title.formatted(Formatting.UNDERLINE), width / 2 - (titleWidth / 2), titleHeight, 0xFFFFFF, false);
    }
}