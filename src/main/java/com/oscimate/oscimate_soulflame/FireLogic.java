package com.oscimate.oscimate_soulflame;

import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.text.Text;

public enum FireLogic {
    PERSISTENT,
    CONSISTENT;

    public Text getTranslatableName() {
        return Text.translatable("oscimate_soulflame.config." + this.name());
    }

    public Tooltip getTranslatableTooltip() {
        return Tooltip.of(Text.translatable("oscimate_soulflame.config." + name() + ".tooltip"));
    }


}
