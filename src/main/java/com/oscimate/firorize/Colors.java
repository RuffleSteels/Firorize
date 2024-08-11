package com.oscimate.firorize;

import net.minecraft.text.Text;
import net.minecraft.util.StringIdentifiable;

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
