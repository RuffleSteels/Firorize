package com.oscimate.firorize.config;

import com.oscimate.firorize.Main;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.text.Text;
import org.apache.commons.collections4.map.ListOrderedMap;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;

@Environment(value= EnvType.CLIENT)
class PresetListWidget
        extends AlwaysSelectedEntryListWidget<PresetListWidget.PresetEntry> {

    public String curPresetID;
    private final int width;
    private final int x;
    private final int y;


    public PresetListWidget(MinecraftClient client, int width, int height, int x, int y, ChangeFireColorScreen instance, TextRenderer textRenderer) {
        super(client, width, height, instance.wheelCoords[0] + instance.wheelRadius*2 + 90 + 10 + 2 , instance.wheelCoords[0] + instance.wheelRadius*2 + 90 + 10 + 2 + height, y);
        this.instance = instance;
        this.textRenderer = textRenderer;

        this.width = width;
        this.x = x + instance.wheelCoords[0];
        this.y = instance.wheelCoords[0] + instance.wheelRadius*2 + 90 + 10 + 2;

        setLeftPos(x);
        setRenderHorizontalShadows(false);


        Main.CONFIG_MANAGER.getFireColorPresets().forEach((string, map) -> {
            this.addEntry(new PresetEntry(string));
        });


        isConstruct = true;

        setSelected(children().get(children().stream().map(entry -> entry.languageDefinition).toList().indexOf(Main.CONFIG_MANAGER.getCurrentPreset())));
    }

    @Override
    public int getRowLeft() {
        return 0;
    }

    private final TextRenderer textRenderer;
    private final ChangeFireColorScreen instance;

    @Override
    public int getRowWidth() {
        return width;
    }


    private boolean isConstruct = false;

    public void resetProfile() {
//        instance.resetBuffer = true;
        KeyValuePair< KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>, int[]>, ArrayList<Integer>> temp = Main.CONFIG_MANAGER.getDefaultProfile();
        int[] list = temp.getLeft().getRight();

        System.arraycopy(list, 0, Main.CONFIG_MANAGER.getCurrentBlockFireColors().getRight(), 0, list.length);
        Collections.copy(Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft(), temp.getLeft().getLeft());
        Collections.copy(Main.CONFIG_MANAGER.getPriorityOrder(), temp.getRight());

        instance.isReset = true;
        setSelected(children().stream().filter(thing -> thing.languageDefinition.equalsIgnoreCase(curPresetID)).findFirst().get());
        instance.setRedo(true);
        instance.resetBuffer = true;
        instance.resetProfileButton.setFocused(false);
    }

    public void addProfile(String presetName, KeyValuePair<KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>, int[]>, ArrayList<Integer>> newProfile) {
        PresetEntry entry = new PresetEntry(presetName);
        addEntry(entry);

        Main.CONFIG_MANAGER.getFireColorPresets().put(presetName, newProfile);

        Main.CONFIG_MANAGER.save();

        setSelected(entry);
    }

    public void addPreset() {
        instance.isPresetAdd = true;
        client.setScreen(new AddProfileScreen(instance));
    }

    @Override
    public void setSelected(@Nullable PresetListWidget.PresetEntry entry) {
        if (!instance.isReset) {
            if (!entry.equals(getSelectedOrNull())) {
                instance.setRedo(false);
                instance.searchScreenListWidget.setSelected(instance.searchScreenListWidget.children().get(0));
                Main.CONFIG_MANAGER.setCurrentPreset(entry.languageDefinition);
            }


            curPresetID = entry.languageDefinition;
            int[] list = Main.CONFIG_MANAGER.getFireColorPresets().get(entry.languageDefinition).getLeft().getRight();
            System.arraycopy(list, 0, Main.CONFIG_MANAGER.getCurrentBlockFireColors().getRight(), 0, list.length);
            Collections.copy(Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft(), Main.CONFIG_MANAGER.getFireColorPresets().get(entry.languageDefinition).getLeft().getLeft());
            Collections.copy(Main.CONFIG_MANAGER.getPriorityOrder(), Main.CONFIG_MANAGER.getFireColorPresets().get(entry.languageDefinition).getRight());
        }
        instance.blockUnderField.setText("");
        instance.input = instance.blockUnderField.getText();
        instance.searchScreenListWidget.selected.clear();
        if (isConstruct) {
            instance.searchScreenListWidget.test();
        } else {
            instance.changeSearchOption(client.world == null ? 0 : Main.CONFIG_MANAGER.getPriorityOrder().get(0));
            if (client.world == null) {
                instance.searchOptions[1].setTooltip(Tooltip.of(Text.translatable("firorize.config.tooltip.priorityArrow")));
                instance.searchOptions[1].active = false;
                instance.searchOptions[2].setTooltip(Tooltip.of(Text.translatable("firorize.config.tooltip.priorityArrow")));
                instance.searchOptions[2].active = false;
                instance.searchOptions[0].active = false;
            }
        }

        instance.searchScreenListWidget.setSelected(instance.searchScreenListWidget.children().get(0));
        isConstruct = false;
        instance.setRedo(false, false);
        instance.cyclicalPresets.setIndex(0);

        if (instance.isReset) {
            instance.setRedo(true, false);
        }

        super.setSelected(entry);
    }

    @Override
    protected int getScrollbarPositionX() {
        return super.getScrollbarPositionX() + 20 + instance.blockSearchCoords[0];
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.getMatrices().push();
        context.getMatrices().scale(2f, 2f, 2f);
        context.drawTextWithShadow(textRenderer, Text.translatable("firorize.config.title.profiles"), x - 63, (y-183), Color.WHITE.getRGB());
        context.getMatrices().pop();
    }

    @Override
    protected void renderEntry(DrawContext context, int mouseX, int mouseY, float delta, int index, int x, int y, int entryWidth, int entryHeight) {
        PresetListWidget.PresetEntry entry = this.getEntry(index);
        entry.x = x;
        entry.entryHeight = entryHeight;
        entry.entryWidth = entryWidth;
        entry.y = y;
        super.renderEntry(context, mouseX, mouseY, delta, index, x, y, entryWidth, entryHeight);
    }



    @Environment(value=EnvType.CLIENT)
    public class PresetEntry
            extends AlwaysSelectedEntryListWidget.Entry<PresetListWidget.PresetEntry> {
        public final String languageDefinition;
        public PresetEntry(String languageDefinition) {
            this.languageDefinition = languageDefinition;
        }
        private int x;
        private int y;
        private int entryHeight;
        private int entryWidth;

        @Override
        public Text getNarration() {
            return Text.translatable("narrator.select", this.languageDefinition);
        }
        private int buffer = 30;

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (mouseX >= x+entryWidth-entryHeight-10+buffer && mouseX <= x+entryWidth-10+buffer && mouseY >= y && mouseY <= y+entryHeight) {
                if (!languageDefinition.equals("Initial")) {
                    Main.CONFIG_MANAGER.getFireColorPresets().remove(languageDefinition);
                    PresetListWidget.this.children().remove(this);
                    PresetListWidget.this.setSelected(PresetListWidget.this.children().get(0));
                    return false;
                }
            }
            setSelected(this);
            return super.mouseClicked(mouseX, mouseY, button);
        }
        private float alphaa;

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            if (!languageDefinition.equals("Initial")) {
                if (mouseX >= x+entryWidth-entryHeight-10+buffer && mouseX <= x+entryWidth-10+buffer && mouseY >= y && mouseY <= y+entryHeight) {
                    alphaa = 1f;
                } else {
                    alphaa = 0.5f;
                }
                context.fill(x+entryWidth-entryHeight-10+buffer, y, x+entryWidth-10+buffer, y + entryHeight, new Color(1f/255*44, 1f/255*44, 1f/255*44, alphaa).getRGB());
                instance.drawX(context, entryWidth, entryHeight, y, x+buffer);
                context.drawBorder(x+entryWidth-entryHeight-10+buffer, y, entryHeight, entryHeight, new Color(1f/255*99, 1f/255*99, 1f/255*99, 0.8f).getRGB());
            }
            context.drawCenteredTextWithShadow(PresetListWidget.this.textRenderer, Text.literal(languageDefinition), (entryWidth-6) / 2  + PresetListWidget.this.instance.wheelCoords[0], y+1, 0xFFFFFF);
        }
    }
}