package com.oscimate.oscimate_soulflame;

import com.oscimate.oscimate_soulflame.config.ConfigManager;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.checkerframework.checker.units.qual.C;

public enum FireLogic {
    PERSISTENT,
    CONSISTENT;



    public Text getTranslatableName() {
        return new TranslatableText("oscimate_soulflame.config." + this.name());
    }

}
