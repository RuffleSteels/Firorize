package com.oscimate.oscimate_soulflame;

import com.oscimate.oscimate_soulflame.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.List;

public enum FireLogic {
    PERSISTENT,
    CONSISTENT;

    public Text getTranslatableName() {
        return Text.translatable("oscimate_soulflame.config." + this.name());
    }

    public List<OrderedText> getTranslatableTooltip(MinecraftClient minecraftClient) {
        return minecraftClient.textRenderer.wrapLines(Text.translatable("oscimate_soulflame.config." + this.name() + ".tooltip"), 200);
    }


}
