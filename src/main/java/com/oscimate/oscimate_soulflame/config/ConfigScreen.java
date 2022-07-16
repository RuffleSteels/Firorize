package com.oscimate.oscimate_soulflame.config;

import com.mojang.blaze3d.systems.RenderSystem;
import com.oscimate.oscimate_soulflame.FireLogic;
import com.oscimate.oscimate_soulflame.Main;
import com.oscimate.oscimate_soulflame.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Language;
import org.checkerframework.checker.units.qual.C;

import java.util.List;
import java.util.Optional;

public class ConfigScreen extends Screen {
    Integer buttonWidth = 130;
    private Screen parent;
    protected static final int windowWidth = 176;
    protected static final int windowHeight = 182;
    private final Identifier WINDOW = new Identifier("oscimate_soulflame", "textures/gui/info_box.png");
    protected int guiTop, guiLeft;



    protected ConfigScreen(Text title) {
        super(title);
    }

    @Override
    protected void init() {
        CyclingButtonWidget<FireLogic> enabledButton = CyclingButtonWidget.builder(FireLogic::getTranslatableName)
                .values(FireLogic.values())
                .initially(Main.CONFIG_MANAGER.getStartupConfig())
                .tooltip(value -> value.getTranslatableTooltip(client))
                .build(this.width / 2 - (buttonWidth/2), buttonWidth/2, buttonWidth, 20, Text.literal("Fire Logic"), (button, fireLogic) -> {
                    Main.CONFIG_MANAGER.setCurrentFireLogic((FireLogic) fireLogic);
                });
        this.addDrawableChild(enabledButton);
        this.addDrawableChild(new ButtonWidget(width / 2 - 100, (int) (height * 0.8F), 200, 20, ScreenTexts.DONE, (button) -> onClose()));
        super.init();
    }

    private void renderOriginWindow(MatrixStack matrices, int mouseX, int mouseY) {
        RenderSystem.enableBlend();
        RenderSystem.setShaderTexture(0, WINDOW);
        this.drawTexture(matrices, width/2 - (windowWidth/2), height/2 - (windowHeight/2), 0, 0, windowWidth, windowHeight);
        RenderSystem.setShaderTexture(0, WINDOW);
        Text title = Text.literal("ImprovedFireOverlay");
        this.drawCenteredText(matrices, this.textRenderer, title.getString(), width / 2, guiTop - 15, 0xFFFFFF);
        this.renderOriginContent(matrices, mouseX, mouseY);
        RenderSystem.disableBlend();
    }


    @Override
    public void render(MatrixStack stack, int mouseX, int mouseY, float delta) {
        this.renderBackground(stack);
        this.renderOriginWindow(stack, mouseX, mouseY);
        this.drawCenteredText(stack, this.textRenderer, "Improved Fire Overlay", this.width / 2, 20, 0xFFFFFF);
        super.render(stack, mouseX, mouseY, delta);
    }



    public List<OrderedText> getTranslatableTooltip(MinecraftClient minecraftClient) {
        return minecraftClient.textRenderer.wrapLines(Text.translatable("oscimate_soulflame.config." + Main.CONFIG_MANAGER.getStartupConfig().toString() + ".tooltip"), 200);
    }

    public void onClose() {
        client.setScreen(parent);
    }

    @Override
    public void removed() {
        Main.CONFIG_MANAGER.onConfigChange();
    }

    private void renderOriginContent(MatrixStack matrices, int mouseX, int mouseY) {

        int textWidth = windowWidth - 30;
        int titleHeight = (height/2 - (windowHeight/2)) + 15;
        int infoHeight = (height/2 - (windowHeight/2)) + 15;
        int x = guiLeft + 18;
        int y = guiTop + 50;
        int startY = y;
        int endY = y - 72 + windowHeight;

        Text info = Text.translatable("oscimate_soulflame.config." + Main.CONFIG_MANAGER.getStartupConfig().toString() + ".info");

        List<OrderedText> descLines = textRenderer.wrapLines(info, textWidth);
        for(OrderedText line : descLines) {
            if(y >= startY - 18 && y <= endY + 12) {
                int infoWidth = textRenderer.getWidth(line);
                textRenderer.draw(matrices, line, width/2 - (windowWidth/2) + 15, (((infoHeight + 25) - windowHeight/2) + 30) + (y +2), 0xCCCCCC);
            }
            y += 12;
        }

        int titleWidth = textRenderer.getWidth(Text.translatable("oscimate_soulflame.config." + Main.CONFIG_MANAGER.getStartupConfig().toString() + ".title"));

        textRenderer.draw(matrices, Text.translatable("oscimate_soulflame.config." + Main.CONFIG_MANAGER.getStartupConfig().toString() + ".title").formatted(Formatting.UNDERLINE), width/2 - (titleWidth/2), titleHeight, 0xFFFFFF);

    }

}