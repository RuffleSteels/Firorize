package com.oscimate.oscimate_soulflame.config;

import com.oscimate.oscimate_soulflame.Main;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

public class FireHeightSliderWidget extends SliderWidget {
    public FireHeightSliderWidget(int x, int y, int width, int height, Text text, double value) {
        super(x, y, width, height, text, value);

        this.updateMessage();
        this.applyValue();
    }

    private double getFireHeight() {
        return -Math.abs(0 - (((double) 1 /20) * Math.sqrt(getSliderValue()) - ((double) 1 /2)));
    }
    private long getSliderValue() {
        return Math.round(this.value*100);
    }

    @Override
    protected void updateMessage() {
        Main.CONFIG_MANAGER.setCurrentFireHeightSlider(getSliderValue());
        Main.currentFireHeight = getFireHeight();
    }

    @Override
    protected void applyValue() {
        long sliderValue = getSliderValue();
        Text text = sliderValue == 0   ? Text.translatable(Main.MODID+".config.fireHeight.min") :
                sliderValue == 50 ? Text.translatable(Main.MODID+".config.fireHeight.middle") :
                        sliderValue == 100 ? Text.translatable(Main.MODID+".config.fireHeight.max") :
                                Text.literal("Height: " + sliderValue);
        this.setMessage(text);
    }
}
