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

    public PresetListWidget(MinecraftClient client, int width, int height, int x, int y, ChangeFireColorScreen instance, TextRenderer textRenderer) {
        super(client, width, height, x, y);
        this.instance = instance;
        this.textRenderer = textRenderer;

        Main.CONFIG_MANAGER.getFireColorPresets().forEach((string, map) -> {
            this.addEntry(new PresetEntry(string));
        });


        isConstruct = true;

        // Fall back to the first profile if the saved currentPreset no longer exists (e.g. it was deleted).
        int curIndex = children().stream().map(entry -> entry.languageDefinition).toList().indexOf(Main.CONFIG_MANAGER.getCurrentPreset());
        if (curIndex < 0) curIndex = 0;
        setSelected(children().get(curIndex));
    }

    private final TextRenderer textRenderer;
    private final ChangeFireColorScreen instance;

    @Override
    public int getRowWidth() {
        return this.getWidth();
    }


    private boolean isConstruct = false;

    public void resetProfile() {
        // Snapshot the profile before resetting so the reset itself is a single undo step.
        instance.historyBefore();
        KeyValuePair< KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>, int[]>, ArrayList<Integer>> temp = Main.CONFIG_MANAGER.getDefaultProfile();
        int[] list = temp.getLeft().getRight();

        System.arraycopy(list, 0, Main.CONFIG_MANAGER.getCurrentBlockFireColors().getRight(), 0, list.length);
        Collections.copy(Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft(), temp.getLeft().getLeft());
        Collections.copy(Main.CONFIG_MANAGER.getPriorityOrder(), temp.getRight());
        // Commit the reset to the preset immediately (no deferral); history owns the revert.
        instance.commitToPreset();
        Main.CONFIG_MANAGER.save();
        setSelected(children().stream().filter(thing -> thing.languageDefinition.equalsIgnoreCase(curPresetID)).findFirst().get());
        instance.historyAfterReset();
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
        if (!entry.equals(getSelectedOrNull())) {
            // Switching to a different profile: undo/redo history does not carry across profiles.
            instance.clearHistory();
            instance.searchScreenListWidget.setSelected(instance.searchScreenListWidget.children().get(0));
            Main.CONFIG_MANAGER.setCurrentPreset(entry.languageDefinition);
        }

        curPresetID = entry.languageDefinition;
        int[] list = Main.CONFIG_MANAGER.getFireColorPresets().get(entry.languageDefinition).getLeft().getRight();
        System.arraycopy(list, 0, Main.CONFIG_MANAGER.getCurrentBlockFireColors().getRight(), 0, list.length);
        Collections.copy(Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft(), Main.CONFIG_MANAGER.getFireColorPresets().get(entry.languageDefinition).getLeft().getLeft());
        Collections.copy(Main.CONFIG_MANAGER.getPriorityOrder(), Main.CONFIG_MANAGER.getFireColorPresets().get(entry.languageDefinition).getRight());

        instance.blockUnderField.setText("");
        instance.input = instance.blockUnderField.getText();
        instance.searchScreenListWidget.selected.clear();
        if (isConstruct) {
            instance.searchScreenListWidget.test(false);
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
        instance.cyclicalPresets.setIndex(0);

        super.setSelected(entry);
    }

    @Override
    protected void drawSelectionHighlight(DrawContext context, int y, int entryWidth, int entryHeight, int borderColor, int fillColor) {
        int i = this.getX() + (this.width - entryWidth) / 2;
        int j = this.getX() + (this.width + entryWidth) / 2;
        context.fill(i, y - 2, j, y + entryHeight + 2, borderColor);
        context.fill(i + 1, y - 1, j - 1 - 6, y + entryHeight + 1, fillColor);
    }

    @Override
    protected int getScrollbarX() {
        return super.getScrollbarX() - 16;
    }
    @Override
    public int getX() {
        return super.getX() + instance.wheelCoords[0];
    }

    @Override
    public int getY() {
        return instance.wheelCoords[0] + instance.wheelRadius*2 + 90 + 10 + 2;
    }



    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderWidget(context, mouseX, mouseY, delta);
        context.getMatrices().push();
        context.getMatrices().scale(2f, 2f, 2f);
        context.drawTextWithShadow(textRenderer, Text.translatable("firorize.config.title.profiles"), getX() - 21, (getY()-183), Color.WHITE.getRGB());
        context.getMatrices().pop();
    }

    @Override
    protected void renderEntry(DrawContext context, int mouseX, int mouseY, float delta, int index, int x, int y, int entryWidth, int entryHeight) {
        PresetListWidget.PresetEntry entry = this.getEntry(index);
        entry.x = x;
        entry.entryHeight = entryHeight;
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

        @Override
        public Text getNarration() {
            return Text.translatable("narrator.select", this.languageDefinition);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (mouseX >= x+getWidth()-entryHeight-10 && mouseX <= x+getWidth()-10 && mouseY >= y && mouseY <= y+entryHeight) {
                if (!languageDefinition.equals("Initial")) {
                    // Deleting a profile is destructive and not undoable — confirm first, in a box
                    // drawn over the config screen (not a separate world-backed screen).
                    String toDelete = languageDefinition;
                    PresetListWidget.PresetEntry self = this;
                    instance.showConfirm(
                            Text.translatable("firorize.config.confirm.deleteProfile.title"),
                            Text.translatable("firorize.config.confirm.deleteProfile.message"),
                            () -> {
                                Main.CONFIG_MANAGER.getFireColorPresets().remove(toDelete);
                                PresetListWidget.this.children().remove(self);
                                // setSelected updates currentPreset to the new selection; save afterwards
                                // so the persisted currentPreset never dangles at the deleted profile.
                                PresetListWidget.this.setSelected(PresetListWidget.this.children().get(0));
                                Main.CONFIG_MANAGER.save();
                            });
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
                if (mouseX >= x+entryWidth-entryHeight-10 && mouseX <= x+entryWidth-10 && mouseY >= y && mouseY <= y+entryHeight) {
                    alphaa = 1f;
                } else {
                    alphaa = 0.5f;
                }
                context.fill(x+entryWidth-entryHeight-10, y, x+entryWidth-10, y + entryHeight, new Color(1f/255*44, 1f/255*44, 1f/255*44, alphaa).getRGB());
                instance.drawX(context, entryWidth, entryHeight, y, x);
                context.drawBorder(x+entryWidth-entryHeight-10, y, entryHeight, entryHeight, new Color(1f/255*99, 1f/255*99, 1f/255*99, 0.8f).getRGB());
            }
            context.drawCenteredTextWithShadow(PresetListWidget.this.textRenderer, Text.literal(languageDefinition), (entryWidth-6) / 2  + PresetListWidget.this.instance.wheelCoords[0], y+1, 0xFFFFFF);
        }
    }
}