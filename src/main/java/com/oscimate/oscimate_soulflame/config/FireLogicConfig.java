package com.oscimate.oscimate_soulflame.config;

import com.oscimate.oscimate_soulflame.FireLogic;
import com.oscimate.oscimate_soulflame.Main;
import net.minecraft.block.Block;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class FireLogicConfig {
    public FireLogic fireLogic = Main.CONFIG_MANAGER.getCurrentFireLogic();

    public long fireHeightSlider = Main.CONFIG_MANAGER.getCurrentFireHeightSlider();
    public HashMap<String, int[]> blockFireColors = Main.CONFIG_MANAGER.getCurrentBlockFireColors();

    public FireLogic getFireLogic() {
        return fireLogic;
    }

    public long getFireHeightSlider() {
        return fireHeightSlider;
    }

    public HashMap<String, int[]> getCurrentBlockFireColours() {return blockFireColors;}
}
