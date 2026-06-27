package com.oscimate.firorize.config;

import com.oscimate.firorize.Main;
import org.apache.commons.collections4.map.ListOrderedMap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

public class FireLogicConfig {

    public long fireHeightSlider = Main.CONFIG_MANAGER.getCurrentFireHeightSlider();
    public KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>,  int[]> blockFireColors = Main.CONFIG_MANAGER.getCurrentBlockFireColors();

    public ListOrderedMap<String, KeyValuePair<KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>,  int[]>, ArrayList<Integer>>> getFireColorPresets() {
        return fireColorPresets;
    }

    public ListOrderedMap<String, int[]> customColorPresets = Main.CONFIG_MANAGER.getCustomColorPresets();

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

    public String currentPreset = Main.CONFIG_MANAGER.getCurrentPreset();

    public void setFireColorPresets(ListOrderedMap<String, KeyValuePair<KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>,  int[]>, ArrayList<Integer>>> fireColorPresets) {
        this.fireColorPresets = fireColorPresets;
    }

    public ListOrderedMap<String, KeyValuePair<KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>,  int[]>, ArrayList<Integer>>> fireColorPresets = Main.CONFIG_MANAGER.getFireColorPresets();

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

    public LinkedHashSet<String> importedProfiles = Main.CONFIG_MANAGER.getImportedProfiles();

    public LinkedHashSet<String> getImportedProfiles() {
        return importedProfiles;
    }

    public LinkedHashMap<String, String> importedAuthors = Main.CONFIG_MANAGER.getImportedAuthors();

    public LinkedHashMap<String, String> getImportedAuthors() {
        return importedAuthors;
    }

    public LinkedHashSet<String> inboxImports = Main.CONFIG_MANAGER.getInboxImports();

    public LinkedHashSet<String> getInboxImports() {
        return inboxImports;
    }

    public long accumulatedConfigTimeMs = Main.CONFIG_MANAGER.getAccumulatedConfigTimeMs();

    public long getAccumulatedConfigTimeMs() {
        return accumulatedConfigTimeMs;
    }

    public int donationPopupsShown = Main.CONFIG_MANAGER.getDonationPopupsShown();

    public int getDonationPopupsShown() {
        return donationPopupsShown;
    }
}
