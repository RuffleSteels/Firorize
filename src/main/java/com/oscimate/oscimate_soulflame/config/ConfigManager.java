package com.oscimate.oscimate_soulflame.config;

import com.google.common.base.Suppliers;
import com.google.gson.Gson;
import com.oscimate.oscimate_soulflame.ColorizeMath;
import com.oscimate.oscimate_soulflame.FireLogic;
import com.oscimate.oscimate_soulflame.Main;
import kotlin.Pair;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import org.apache.commons.collections4.map.ListOrderedMap;


import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class ConfigManager {
    public FireLogic currentFireLogic;
    public long currentFireHeightSlider;

    public HashMap<String, Pair<ArrayList<ListOrderedMap<String, int[]>>, ArrayList<Integer>>> getFireColorPresets() {
        return fireColorPresets;
    }

    public String getCurrentPreset() {
        return currentPreset;
    }

    public void setCurrentPreset(String currentPreset) {
        this.currentPreset = currentPreset;
    }

    public String currentPreset;

    public void setFireColorPresets(HashMap<String, Pair<ArrayList<ListOrderedMap<String, int[]>>, ArrayList<Integer>>> fireColorPresets) {
        this.fireColorPresets = fireColorPresets;
    }

    public HashMap<String, Pair<ArrayList<ListOrderedMap<String, int[]>>, ArrayList<Integer>>> fireColorPresets;

    public void setCurrentBlockFireColors(ArrayList<ListOrderedMap<String, int[]>> blockFireColors) {
        this.blockFireColors = blockFireColors;
    }

    public ArrayList<ListOrderedMap<String, int[]>> blockFireColors;

    public ArrayList<Integer> getPriorityOrder() {
        return priorityOrder;
    }

    public void setPriorityOrder(ArrayList<Integer> priorityOrder) {
        this.priorityOrder = priorityOrder;
    }

    public ArrayList<Integer> priorityOrder;

    public ArrayList<ListOrderedMap<String, int[]>> getCurrentBlockFireColors() {
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
        if (jsonOutput.getCurrentBlockFireColours() == null || jsonOutput.getCurrentBlockFireColours().size() == 0) {
            ArrayList<ListOrderedMap<String, int[]>> temp = new ArrayList<ListOrderedMap<String, int[]>>();
            temp.add(new ListOrderedMap<String, int[]>());
            temp.add(new ListOrderedMap<String, int[]>());
            temp.add(new ListOrderedMap<String, int[]>());
            setCurrentBlockFireColors(temp);
            save();
        } else {
            setCurrentBlockFireColors(jsonOutput.getCurrentBlockFireColours());
//            for (int i = 0; i <2;i++) {
//                int finalI = i;
//                jsonOutput.getCurrentBlockFireColours().stream().map(map -> map.keyList().stream().map(key -> Main.FIRE_SPRITES.put(key, Suppliers.memoize(() -> new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, new Identifier("oscimate_soulflame:block/fire"+finalI+"_"+ map.get(key)[0] + "_" + map.get(key)[1])).getSprite()))));
//            }
//            jsonOutput.getCurrentBlockFireColours().forEach(map -> {
//                map.valueList().forEach(ColorizeMath::create);
//            });
        }
        if (jsonOutput.getPriorityOrder() == null || jsonOutput.getPriorityOrder().size() == 0) {
            ArrayList<Integer> temp = new ArrayList<>();
            temp.add(0);
            temp.add(1);
            temp.add(2);
            setPriorityOrder(temp);
            save();
        } else {
            setPriorityOrder(jsonOutput.getPriorityOrder());
        }
        if (jsonOutput.getFireColorPresets() == null || jsonOutput.getFireColorPresets().size() == 0) {
            ArrayList<ListOrderedMap<String, int[]>> temp = new ArrayList<ListOrderedMap<String, int[]>>();
            temp.add(new ListOrderedMap<String, int[]>());
            temp.add(new ListOrderedMap<String, int[]>());
            temp.add(new ListOrderedMap<String, int[]>());
            ArrayList<Integer> temp2 = new ArrayList<>();
            temp2.add(0);
            temp2.add(1);
            temp2.add(2);

            HashMap<String, Pair<ArrayList<ListOrderedMap<String, int[]>>, ArrayList<Integer>>> map = new HashMap<>();
            Pair<ArrayList<ListOrderedMap<String, int[]>>, ArrayList<Integer>> mapp = new Pair<>(temp, temp2);

            map.put("Initial", mapp);
            setFireColorPresets(map);
            save();
        } else {
            setFireColorPresets(jsonOutput.getFireColorPresets());
        }
        if(jsonOutput.getCurrentPreset() == null || jsonOutput.getCurrentPreset().equals("")) {
            setCurrentPreset("Initial");
            save();
        } else {
            setCurrentPreset(jsonOutput.getCurrentPreset());
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
