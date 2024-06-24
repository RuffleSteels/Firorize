package com.oscimate.oscimate_soulflame.config;

import com.google.gson.Gson;
import com.oscimate.oscimate_soulflame.FireLogic;
import com.oscimate.oscimate_soulflame.Main;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;


import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    public FireLogic currentFireLogic;
    public long currentFireHeightSlider;

    public void setCurrentBlockFireColors(HashMap<String, int[]> blockFireColors) {
        this.blockFireColors = blockFireColors;
    }

    public HashMap<String, int[]> blockFireColors;

    public HashMap<String, int[]> getCurrentBlockFireColors() {
        return blockFireColors;
    }

    private static final Gson GSON = new Gson();
    public static Path file = FabricLoader.getInstance().getConfigDir().resolve("oscimate_soulflame" + ".json");

    public FireLogic getCurrentFireLogic() {
        return this.currentFireLogic;
    }
    public long getCurrentFireHeightSlider() {
        return this.currentFireHeightSlider;
    }

    public void setCurrentFireLogic(FireLogic fireLogic) {
        this.currentFireLogic = fireLogic;
    }
    public void setCurrentFireHeightSlider(long fireHeightSlider) {
        this.currentFireHeightSlider =  fireHeightSlider;
    }
    public Boolean fileExists() {
        return Files.exists(file);
    }

    public void getStartupConfig() {
        FireLogicConfig jsonOutput = null;
        if(fileExists()) {
            try (Reader reader = Files.newBufferedReader(file)) {
                jsonOutput = GSON.fromJson(reader, FireLogicConfig.class);
            } catch (IOException e) {
                System.out.println(e);
            }
        }
        if(jsonOutput.getFireLogic() == null) {
            setCurrentFireLogic(FireLogic.PERSISTENT);
            save();
        } else {
            setCurrentFireLogic(jsonOutput.getFireLogic());
        }
        if(jsonOutput.getFireHeightSlider() > 100 || jsonOutput.getFireHeightSlider() < 0) {
            setCurrentFireHeightSlider(100);
            save();
        } else {
            setCurrentFireHeightSlider(jsonOutput.getFireHeightSlider());
        }
        if (jsonOutput.getCurrentBlockFireColours() == null) {
            setCurrentBlockFireColors(new HashMap<String, int[]>());
            save();
        } else {
            setCurrentBlockFireColors(jsonOutput.getCurrentBlockFireColours());
        }
    }

    public void save() {
        try {
            Files.writeString(file, GSON.toJson(new FireLogicConfig()));
        } catch (IOException e) {
            System.out.println(e);
        }
    }
}
