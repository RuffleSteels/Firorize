package com.oscimate.oscimate_soulflame.config;

import com.oscimate.oscimate_soulflame.FireLogic;
import com.oscimate.oscimate_soulflame.Main;
import com.oscimate.oscimate_soulflame.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.*;
import net.minecraft.util.Language;
import org.checkerframework.checker.units.qual.C;

import java.util.List;

public class ConfigScreen extends Screen {
    Integer buttonWidth = 130;
    private Screen parent;



    protected ConfigScreen(Text title) {
        super(title);
    }

    @Override
    protected void init() {
        CyclingButtonWidget<FireLogic> enabledButton = CyclingButtonWidget.builder(FireLogic::getTranslatableName)
                .values(FireLogic.values())
                .initially(Main.CONFIG_MANAGER.getCurrentFireLogic())
                .tooltip(client -> value -> value.getTranslatableTooltip(client))
                .build(this.width / 2 - (buttonWidth/2), buttonWidth/2, buttonWidth, 20, new LiteralText("Fire Logic"), (button, fireLogic) -> {
                    Main.CONFIG_MANAGER.setCurrentFireLogic((FireLogic) fireLogic);
                });
        this.addDrawableChild(enabledButton);
        this.addDrawableChild(new ButtonWidget(width / 2 - 100, (int) (height * 0.8F), 200, 20, ScreenTexts.DONE, (button) -> onClose()));
        super.init();
    }

    @Override
    public void render(MatrixStack stack, int mouseX, int mouseY, float delta) {
        this.renderBackground(stack);
        drawCenteredText(stack, this.textRenderer, "Improved Fire Overlay", this.width / 2, 20, 0xFFFFFF);
        super.render(stack, mouseX, mouseY, delta);
    }


    public void onClose() {
        client.setScreen(parent);
    }

    @Override
    public void removed() {
        Main.CONFIG_MANAGER.onConfigChange();
    }
}