package com.oscimate.oscimate_soulflame;

import net.minecraft.text.Text;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.function.ValueLists;
import net.minecraft.world.Difficulty;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.function.IntFunction;

public class Colors implements StringIdentifiable {
//    CUSTOM(0, "CUSTOM", new int[]{3, 3}),
//    GREEN(1, "GREEN", new int[]{Color.GREEN.getRGB(), Color.GREEN.getRGB()}),
//    BLUE(2, "BLUE", new int[]{Color.BLUE.getRGB(), Color.BLUE.getRGB()});

//    public static final StringIdentifiable.EnumCodec<Colors> CODEC = StringIdentifiable.createCodec(Colors::values);
//    private static final IntFunction<Colors> BY_ID = ValueLists.createIdToValueFunction(Colors::getId, values(), ValueLists.OutOfBoundsHandling.WRAP);
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
        return Text.literal("Color: " + this.name);
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
