package com.oscimate.oscimate_soulflame.config;

import com.oscimate.oscimate_soulflame.Colors;
import com.oscimate.oscimate_soulflame.Main;
import com.oscimate.oscimate_soulflame.mixin.fire_overlays.client.BlockTagAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.MultiNoiseBiomeSourceParameterList;
import org.apache.commons.collections4.map.ListOrderedMap;
import org.apache.commons.lang3.SerializationUtils;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Environment(value= EnvType.CLIENT)
class PresetListWidget
        extends AlwaysSelectedEntryListWidget<PresetListWidget.PresetEntry> {

    public String curPresetID;

    public PresetListWidget(MinecraftClient client, int width, int height, int x, int y, ChangeFireColorScreen instance, TextRenderer textRenderer) {
        super(client, width, height, x, y);
        this.instance = instance;
        this.textRenderer = textRenderer;

        Main.CONFIG_MANAGER.getFireColorPresets().forEach((string, map) -> {
            this.addEntryToTop(new PresetEntry(string));
        });


        isConstruct = true;

        setSelected(children().get(children().stream().map(entry -> entry.languageDefinition).toList().indexOf(Main.CONFIG_MANAGER.getCurrentPreset())));
    }

    private final TextRenderer textRenderer;
    private final ChangeFireColorScreen instance;

    @Override
    public int getRowWidth() {
        return this.getWidth();
    }


    private boolean isConstruct = false;

    public void addPreset() {
        if (this.children().stream().map(entry -> entry.languageDefinition).noneMatch(string -> string.contains(" ") || string.contains("'") || string.contains("\"") || string.equals(instance.presetNameField.getText()))) {
            PresetEntry entry = new PresetEntry(instance.presetNameField.getText());
            this.addEntry(entry);



            ArrayList<ListOrderedMap<String, int[]>> temp = new ArrayList<ListOrderedMap<String, int[]>>();
            temp.add(new ListOrderedMap<String, int[]>());
            temp.add(new ListOrderedMap<String, int[]>());
            temp.add(new ListOrderedMap<String, int[]>());
            ArrayList<Integer> temp2 = new ArrayList<>();
            temp2.add(0);
            temp2.add(1);
            temp2.add(2);

            Pair<ArrayList<ListOrderedMap<String, int[]>>, ArrayList<Integer>> mapp = new Pair<>(temp, temp2);

            Main.CONFIG_MANAGER.getFireColorPresets().put(entry.languageDefinition, mapp);

            setSelected(entry);
        }
    }

    @Override
    public void setSelected(@Nullable PresetListWidget.PresetEntry entry) {

        if (!entry.equals(getSelectedOrNull())) {
            instance.hasRedo = false;
            instance.redoButton.active = false;
            instance.searchScreenListWidget.setSelected(instance.searchScreenListWidget.children().get(0));
            Main.CONFIG_MANAGER.setCurrentPreset(entry.languageDefinition);
        }

        curPresetID = entry.languageDefinition;
        Collections.copy(Main.CONFIG_MANAGER.getCurrentBlockFireColors(), Main.CONFIG_MANAGER.getFireColorPresets().get(entry.languageDefinition).getLeft());
        Collections.copy(Main.CONFIG_MANAGER.getPriorityOrder(), Main.CONFIG_MANAGER.getFireColorPresets().get(entry.languageDefinition).getRight());
        instance.blockUnderField.setText("");
        instance.input = instance.blockUnderField.getText();
        instance.searchScreenListWidget.selected.clear();
        if (isConstruct) {
            instance.searchScreenListWidget.test();
        } else {
            instance.changeSearchOption(client.world == null ? 0 : Main.CONFIG_MANAGER.getPriorityOrder().get(0));
            if (client.world == null) {
                instance.searchOptions[1].setTooltip(Tooltip.of(Text.literal("You must be loaded in a world to customize with this")));
                instance.searchOptions[1].active = false;
                instance.searchOptions[2].setTooltip(Tooltip.of(Text.literal("You must be loaded in a world to customize with this")));
                instance.searchOptions[2].active = false;
                instance.searchOptions[0].active = false;
            }
        }
        instance.searchScreenListWidget.setSelected(instance.searchScreenListWidget.children().get(0));
        isConstruct = false;
        instance.hasRedo = false;
        instance.redoButton.active = false;
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
        context.getMatrices().scale(1.5f, 1.5f, 1.5f);
        context.drawTextWithShadow(textRenderer, Text.literal("Presets"), getX() - 14, (getY()-125), Color.WHITE.getRGB());
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
        private final String languageDefinition;
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