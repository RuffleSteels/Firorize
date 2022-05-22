package com.oscimate.oscimate_soulflame;

import java.util.List;

import com.oscimate.oscimate_soulflame.Main;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

public class ConfigScreen extends Screen {

    protected ConfigScreen(Text title) {
        super(title);
    }

    @Override
    protected void init() {
        CyclingButtonWidget<Boolean> enabledButton = new CyclingButtonWidget.Builder<Boolean>(enabled -> new TranslatableText(enabled ? "On" : "Off"))
                .initially(Main.isEnabled).build(this.width / 2 - 50, 50, 100, 20, new TranslatableText("Enabled"), (button, value) -> Main.isEnabled = value);
        this.addDrawableChild(enabledButton);
        super.init();
    }

    @Override
    public void render(MatrixStack stack, int mouseX, int mouseY, float delta) {
        this.renderBackground(stack);
        drawCenteredText(stack, this.textRenderer, "ImprovedFireOverlay Config", this.width / 2, 20, 0xFFFFFF);
        super.render(stack, mouseX, mouseY, delta);
    }
}