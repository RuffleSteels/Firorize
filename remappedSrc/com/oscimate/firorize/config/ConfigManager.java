package com.oscimate.firorize.config;

import com.google.gson.Gson;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.commons.collections4.map.ListOrderedMap;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class ConfigManager {
    public long currentFireHeightSlider = -1;

    public ListOrderedMap<String, KeyValuePair<KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>,  int[]>, ArrayList<Integer>>> getFireColorPresets() {
        return fireColorPresets;
    }
    public ListOrderedMap<String, int[]> customColorPresets;

    public ListOrderedMap<String, int[]> getCustomColorPresets() {
        return customColorPresets;
    }

    public void setCustomColorPresets(ListOrderedMap<String, int[]> customColorPresets) {
        this.customColorPresets = customColorPresets;
    }

    public String getCurrentPreset() {
        return currentPreset;
    }

    public void setCurrentPreset(String currentPreset) {
        this.currentPreset = currentPreset;
    }

    public String currentPreset;

    public void setFireColorPresets(ListOrderedMap<String, KeyValuePair<KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>,  int[]>, ArrayList<Integer>>> fireColorPresets) {
        this.fireColorPresets = fireColorPresets;
    }

    public ListOrderedMap<String, KeyValuePair<KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>,  int[]>, ArrayList<Integer>>> fireColorPresets;

    public void setCurrentBlockFireColors(KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>,  int[]> blockFireColors) {
        this.blockFireColors = blockFireColors;
    }

    public KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>,  int[]> blockFireColors;

    public ArrayList<Integer> getPriorityOrder() {
        return priorityOrder;
    }

    public void setPriorityOrder(ArrayList<Integer> priorityOrder) {
        this.priorityOrder = priorityOrder;
    }

    public ArrayList<Integer> priorityOrder;

    public KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>,  int[]> getCurrentBlockFireColors() {
        return blockFireColors;
    }

    private static final Gson GSON = new Gson();
    public static Path file = FabricLoader.getInstance().getConfigDir().resolve("firorize" + ".json");

    public long getCurrentFireHeightSlider() {
        return this.currentFireHeightSlider;
    }
    public void setCurrentFireHeightSlider(long fireHeightSlider) {

        this.currentFireHeightSlider =  fireHeightSlider;
    }
    public Boolean fileExists() {
        return Files.exists(file);
    }

    public KeyValuePair<KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>, int[]>, ArrayList<Integer>> getDefaultProfile() {
        ArrayList<ListOrderedMap<String, int[]>> temp = new ArrayList<ListOrderedMap<String, int[]>>();
        ListOrderedMap<String, int[]> soulStuff = new ListOrderedMap<>();
        soulStuff.put("minecraft:soul_sand", new int[]{-15171708,-14766934});
        soulStuff.put("minecraft:soul_soil", new int[]{-15171708,-14766934});
        temp.add(soulStuff);
        temp.add(new ListOrderedMap<String, int[]>());
        temp.add(new ListOrderedMap<String, int[]>());
        ArrayList<Integer> temp2 = new ArrayList<>();
        temp2.add(0);
        temp2.add(1);
        temp2.add(2);

        return KeyValuePair.of(KeyValuePair.of(temp, new int[]{-7456000,-6456034}), temp2);
    }

    public void getStartupConfig() {
        FireLogicConfig jsonOutput = null;
        if(fileExists()) {
            try (Reader reader = Files.newBufferedReader(file)) {
                jsonOutput = GSON.fromJson(reader, FireLogicConfig.class);
            } catch (IOException e) {

            }
        }

        if(jsonOutput.getFireHeightSlider() > 100 || jsonOutput.getFireHeightSlider() < 0) {
            setCurrentFireHeightSlider(100);
            save();
        } else {
            setCurrentFireHeightSlider(jsonOutput.getFireHeightSlider());
        }
        if(jsonOutput.getCustomColorPresets() == null || jsonOutput.getCustomColorPresets().size() == 0) {
            ListOrderedMap<String, int[]> map = new ListOrderedMap<>();
            map.put("RED", new int[]{-9628645,-7655374});
            map.put("ORANGE", new int[]{-7456000,-6456034});
            map.put("GRAY", new int[]{-12569022,-11185318});
            map.put("BLUE", new int[]{-15171708,-14766934});
            map.put("YELLOW", new int[]{-6584292,-5068772});
            map.put("PURPLE", new int[]{-12446675,-10870735});

            setCustomColorPresets(map);
            save();
        } else {
            setCustomColorPresets(jsonOutput.getCustomColorPresets());
        }
        if (jsonOutput.getCurrentBlockFireColours() == null || jsonOutput.getCurrentBlockFireColours().getLeft().isEmpty()) {
            ArrayList<ListOrderedMap<String, int[]>> temp = new ArrayList<ListOrderedMap<String, int[]>>();

            temp.add(new ListOrderedMap<String, int[]>());
            temp.add(new ListOrderedMap<String, int[]>());
            temp.add(new ListOrderedMap<String, int[]>());
            setCurrentBlockFireColors(KeyValuePair.of(temp, new int[]{-7456000,-6456034}));
            save();
        } else {
            setCurrentBlockFireColors(jsonOutput.getCurrentBlockFireColours());
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


            ListOrderedMap<String, KeyValuePair<KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>,  int[]>, ArrayList<Integer>>> map = new ListOrderedMap<>();

            ListOrderedMap<String, int[]> test = new ListOrderedMap<>();
            test.put("minecraft:crimson_forest", new int[]{-10154472,-7650023});
            test.put("minecraft:warped_forest", new int[]{-14190522,-7562173});
            test.put("minecraft:basalt_deltas", new int[]{-12633022,-11185318});
            test.put("minecraft:soul_sand_valley", new int[]{-15171708,-14766934});

            ArrayList<Integer> temp22 = new ArrayList<>();
            temp22.add(2);
            temp22.add(0);
            temp22.add(1);

            ArrayList<ListOrderedMap<String, int[]>> eeka = new ArrayList<>();
            ListOrderedMap<String, int[]> soulStuff = new ListOrderedMap<>();
            soulStuff.put("minecraft:soul_sand", new int[]{-15171708,-14766934});
            soulStuff.put("minecraft:soul_soil", new int[]{-15171708,-14766934});
            eeka.add(soulStuff);
            eeka.add(new ListOrderedMap<String, int[]>());
            eeka.add(test);
            KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>, int[]> mapp2 = KeyValuePair.of(eeka,  new int[]{-7456000,-6456034});

            map.put("Initial", getDefaultProfile());
            map.put("Nether Biomes", KeyValuePair.of(mapp2, temp22));

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

        }
    }
}
