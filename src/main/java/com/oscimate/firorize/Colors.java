package com.oscimate.firorize;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

public class Colors implements StringRepresentable {
    private final String name;
    private final int[] colors;

    public Colors(String name, int[] colors) {
        this.name = name;
        this.colors = colors;
    }


    public int[] getColors() {
        return colors;
    }

    public Component getTranslatableName() {
        return Component.translatable("firorize.config.title.color").append(": " + this.name);
    }

    public Component getInfo() {
        return Component.translatable("options.difficulty." + this.name + ".info");
    }


    public String getName() {
        return this.name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
