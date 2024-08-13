package com.oscimate.firorize;

import net.minecraft.text.Text;
import net.minecraft.util.StringIdentifiable;

public class Colors implements StringIdentifiable {






    private final String name;
    private final int[] colors;

    public Colors(String name, int[] colors) {
        this.name = name;
        this.colors = colors;
    }


    public int[] getColors() {
        return colors;
    }

    public Text getTranslatableName() {
        return Text.translatable("firorize.config.title.color").append(": " + this.name);
    }

    public Text getInfo() {
        return Text.translatable("options.difficulty." + this.name + ".info");
    }


    public String getName() {
        return this.name;
    }

    @Override
    public String asString() {
        return this.name;
    }
}
