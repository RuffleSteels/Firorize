package com.oscimate.oscimate_soulflame.config;

import com.oscimate.oscimate_soulflame.Main;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.collections4.map.ListOrderedMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FireLogicConfig {

    public long fireHeightSlider = Main.CONFIG_MANAGER.getCurrentFireHeightSlider();
    public KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>,  int[]> blockFireColors = Main.CONFIG_MANAGER.getCurrentBlockFireColors();

    public HashMap<String, KeyValuePair<KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>,  int[]>, ArrayList<Integer>>> getFireColorPresets() {
        return fireColorPresets;
    }

    public HashMap<String, int[]> customColorPresets = Main.CONFIG_MANAGER.getCustomColorPresets();

    public HashMap<String, int[]> getCustomColorPresets() {
        return customColorPresets;
    }
    public void setCustomColorPresets(HashMap<String, int[]> customColorPresets) {
        this.customColorPresets = customColorPresets;
    }

    public String getCurrentPreset() {
        return currentPreset;
    }

    public void setCurrentPreset(String currentPreset) {
        this.currentPreset = currentPreset;
    }

    public String currentPreset = Main.CONFIG_MANAGER.getCurrentPreset();

    public void setFireColorPresets(HashMap<String, KeyValuePair<KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>,  int[]>, ArrayList<Integer>>> fireColorPresets) {
        this.fireColorPresets = fireColorPresets;
    }

    public HashMap<String, KeyValuePair<KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>,  int[]>, ArrayList<Integer>>> fireColorPresets = Main.CONFIG_MANAGER.getFireColorPresets();

    public ArrayList<Integer> getPriorityOrder() {
        return priorityOrder;
    }

    public void setPriorityOrder(ArrayList<Integer> priorityOrder) {
        this.priorityOrder = priorityOrder;
    }

    public ArrayList<Integer> priorityOrder = Main.CONFIG_MANAGER.getPriorityOrder();

    public long getFireHeightSlider() {
        return fireHeightSlider;
    }

    public KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>,  int[]> getCurrentBlockFireColours() {return blockFireColors;}
}
