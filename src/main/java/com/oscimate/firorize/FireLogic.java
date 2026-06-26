package com.oscimate.firorize;

import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

public enum FireLogic {
    PERSISTENT,
    CONSISTENT;

    public Component getTranslatableName() {
        return Component.translatable("firorize.config." + this.name());
    }

    public Tooltip getTranslatableTooltip() {
        return Tooltip.create(Component.translatable("firorize.config." + name() + ".tooltip"));
    }


}
