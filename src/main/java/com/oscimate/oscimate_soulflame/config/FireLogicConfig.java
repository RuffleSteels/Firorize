package com.oscimate.oscimate_soulflame.config;

import com.oscimate.oscimate_soulflame.FireLogic;
import com.oscimate.oscimate_soulflame.Main;
import net.minecraft.block.Block;
import org.apache.commons.collections4.map.ListOrderedMap;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class FireLogicConfig {
    public FireLogic fireLogic = Main.CONFIG_MANAGER.getCurrentFireLogic();

    public long fireHeightSlider = Main.CONFIG_MANAGER.getCurrentFireHeightSlider();
    public ArrayList<ListOrderedMap<String, int[]>> blockFireColors = Main.CONFIG_MANAGER.getCurrentBlockFireColors();

    public ArrayList<Integer> getPriorityOrder() {
        return priorityOrder;
    }

    public void setPriorityOrder(ArrayList<Integer> priorityOrder) {
        this.priorityOrder = priorityOrder;
    }

    public ArrayList<Integer> priorityOrder = Main.CONFIG_MANAGER.getPriorityOrder();
    public FireLogic getFireLogic() {
        return fireLogic;
    }

    public long getFireHeightSlider() {
        return fireHeightSlider;
    }

    public ArrayList<ListOrderedMap<String, int[]>> getCurrentBlockFireColours() {return blockFireColors;}
}
