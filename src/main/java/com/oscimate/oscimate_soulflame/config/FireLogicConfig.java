package com.oscimate.oscimate_soulflame.config;

import com.oscimate.oscimate_soulflame.FireLogic;
import com.oscimate.oscimate_soulflame.Main;

public class FireLogicConfig {
    public FireLogic fireLogic = Main.CONFIG_MANAGER.getCurrentFireLogic();

    public long fireHeightSlider = Main.CONFIG_MANAGER.getCurrentFireHeightSlider();

    public FireLogic getFireLogic() {
        return fireLogic;
    }

    public long getFireHeightSlider() {
        return fireHeightSlider;
    }
}
