package com.oscimate.oscimate_soulflame.config;

import com.google.gson.Gson;
import com.oscimate.oscimate_soulflame.FireLogic;
import com.oscimate.oscimate_soulflame.Main;
import net.fabricmc.loader.api.FabricLoader;


import java.io.*;
import java.nio.file.Files;

public class ConfigManager {
    public FireLogic currentFireLogic;
    private static final Gson GSON = new Gson();
    public static File file = FabricLoader.getInstance().getConfigDir().resolve("oscimate_soulflame" + ".json").toFile();
    private FireLogicConfig config;

    public FireLogic getCurrentFireLogic() {
        return this.currentFireLogic;
    }
    public void setCurrentFireLogic(FireLogic fireLogic) {
        System.out.println(fireLogic);
        this.currentFireLogic = fireLogic;
        save();
    }


//    public void load() {
//        if(file.exists()) {
//            try (FileReader reader = new FileReader(file)) {
//                config = this.GSON.fromJson(reader, FireLogicConfig.class);
//            } catch (IOException e) {
//                System.out.println(e);
//            }
//        }
//        if(config == null) {
//            Main.CONFIG_MANAGER.setCurrentFireLogic(FireLogic.PERSISTENT);
//            config = new FireLogicConfig();
//            save();
//        }
//    }

    public Boolean fileExists() {
        return this.file.exists();
    }

    public FireLogic getStartupConfig() {
        FireLogicConfig jsonOutput = null;
        if(file.exists()) {
            try (Reader reader = Files.newBufferedReader(file.toPath())) {
                jsonOutput = this.GSON.fromJson(reader, FireLogicConfig.class);
                reader.close();
            } catch (IOException e) {
                    System.out.println(e);
                }
            }
            if(jsonOutput.getFireLogic() == null) {
                System.out.println("@@");
                setCurrentFireLogic(FireLogic.PERSISTENT);
                save();
        }
        return jsonOutput.getFireLogic();
    }


    public void save() {
        try (FileWriter writer = new FileWriter(file)) {
            System.out.println("WRITTEN " + Main.CONFIG_MANAGER.getCurrentFireLogic());
            writer.write(this.GSON.toJson(new FireLogicConfig()));
        } catch (IOException e) {
            System.out.println(e);
        }
    }



    public void onConfigChange() {
        save();
    }

}
