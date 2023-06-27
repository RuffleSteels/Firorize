package com.oscimate.oscimate_soulflame.config;

import com.google.gson.Gson;
import com.oscimate.oscimate_soulflame.FireLogic;
import com.oscimate.oscimate_soulflame.Main;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;

public class ConfigManager {

    public FireLogic currentFireLogic;
    private static final Gson GSON = new Gson();
    public static File file = FabricLoader.getInstance().getConfigDir().resolve("oscimate_soulflame" + ".json").toFile();

    public FireLogic getCurrentFireLogic() {
        return currentFireLogic;
    }

    public void setCurrentFireLogic(FireLogic fireLogic) {
        System.out.println(fireLogic);
        currentFireLogic = fireLogic;
        save();
    }

    public Boolean fileExists() {
        return file.exists();
    }

    public FireLogic getStartupConfig() {
        FireLogicConfig jsonOutput = null;
        if (file.exists()) {
            try (Reader reader = Files.newBufferedReader(file.toPath())) {
                jsonOutput = GSON.fromJson(reader, FireLogicConfig.class);
                reader.close();
            }
            catch (IOException e) {
                System.out.println(e);
            }
        }
        if (jsonOutput.getFireLogic() == null) {
            System.out.println("Its null");
            setCurrentFireLogic(FireLogic.PERSISTENT);
            save();
        }
        currentFireLogic = jsonOutput.getFireLogic();
        return jsonOutput.getFireLogic();
    }

    public void save() {
        try (FileWriter writer = new FileWriter(file)) {
            System.out.println("WRITTEN " + Main.CONFIG_MANAGER.getCurrentFireLogic());
            writer.write(GSON.toJson(new FireLogicConfig()));
        }
        catch (IOException e) {
            System.out.println(e);
        }
    }

    public void onConfigChange() {
        save();
    }
}
