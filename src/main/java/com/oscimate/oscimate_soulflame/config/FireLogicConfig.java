package com.oscimate.oscimate_soulflame.config;

import com.oscimate.oscimate_soulflame.FireLogic;
import com.oscimate.oscimate_soulflame.Main;
import kotlin.Pair;
import net.minecraft.block.Block;
import org.apache.commons.collections4.map.ListOrderedMap;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class FireLogicConfig {

    public long fireHeightSlider = Main.CONFIG_MANAGER.getCurrentFireHeightSlider();
    public ArrayList<ListOrderedMap<String, int[]>> blockFireColors = Main.CONFIG_MANAGER.getCurrentBlockFireColors();

    public HashMap<String, Pair<ArrayList<ListOrderedMap<String, int[]>>, ArrayList<Integer>>> getFireColorPresets() {
        return fireColorPresets;
    }

    public String getCurrentPreset() {
        return currentPreset;
    }

    public void setCurrentPreset(String currentPreset) {
        this.currentPreset = currentPreset;
    }

    public String currentPreset = Main.CONFIG_MANAGER.getCurrentPreset();

    public void setFireColorPresets(HashMap<String, Pair<ArrayList<ListOrderedMap<String, int[]>>, ArrayList<Integer>>> fireColorPresets) {
        this.fireColorPresets = fireColorPresets;
    }

    public HashMap<String, Pair<ArrayList<ListOrderedMap<String, int[]>>, ArrayList<Integer>>> fireColorPresets = Main.CONFIG_MANAGER.getFireColorPresets();

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

    public ArrayList<ListOrderedMap<String, int[]>> getCurrentBlockFireColours() {return blockFireColors;}
}
