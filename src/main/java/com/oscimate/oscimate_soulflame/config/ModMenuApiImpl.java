package com.oscimate.oscimate_soulflame.config;

import com.oscimate.oscimate_soulflame.config.ConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.terraformersmc.modmenu.gui.ModMenuOptionsScreen;
import net.minecraft.text.Text;

public class ModMenuApiImpl implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ConfigScreen::new;
    }
}
