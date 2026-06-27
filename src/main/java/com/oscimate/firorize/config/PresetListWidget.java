package com.oscimate.firorize.config;

import com.oscimate.firorize.Main;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;
import org.apache.commons.collections4.map.ListOrderedMap;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;

@Environment(value= EnvType.CLIENT)
class PresetListWidget
        extends ObjectSelectionList<PresetListWidget.PresetEntry> {

    public String curPresetID;

    public PresetListWidget(Minecraft minecraft, int width, int height, int x, int y, ChangeFireColorScreen instance, Font font) {
        super(minecraft, width, height, x, y);
        this.instance = instance;
        this.font = font;

        Main.CONFIG_MANAGER.getFireColorPresets().forEach((string, map) -> {
            this.addEntry(new PresetEntry(string));
        });


        isConstruct = true;

        // Fall back to the first profile if the saved currentPreset no longer exists (e.g. it was deleted).
        int curIndex = children().stream().map(entry -> entry.languageDefinition).toList().indexOf(Main.CONFIG_MANAGER.getCurrentPreset());
        if (curIndex < 0) curIndex = 0;
        setSelected(children().get(curIndex));
    }

    private final Font font;
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
        minecraft.setScreen(new AddProfileScreen(instance));
    }

    @Override
    public void setSelected(@Nullable PresetListWidget.PresetEntry entry) {
        if (entry == null) return;
        if (!entry.equals(getSelected())) {
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

        instance.blockUnderField.setValue("");
        instance.input = instance.blockUnderField.getValue();
        instance.searchScreenListWidget.selected.clear();
        if (isConstruct) {
            instance.searchScreenListWidget.test(false);
        } else {
            // Only biomes need a world (the biome registry is world/server-provided); blocks and tags
            // are available without one. Avoid starting on the biomes tab when there is no world, but
            // still regenerate the current tab so the list reflects the newly selected profile.
            int firstOption = Main.CONFIG_MANAGER.getPriorityOrder().getFirst();
            if (minecraft.level != null) {
                instance.changeSearchOption(firstOption);
            } else {
                instance.searchScreenListWidget.test(false);
            }
        }

        instance.searchScreenListWidget.setSelected(instance.searchScreenListWidget.children().get(0));
        isConstruct = false;
        instance.cyclicalPresets.setIndex(0);

        super.setSelected(entry);
    }

    @Override
    protected void drawSelectionHighlight(GuiGraphicsExtractor context, PresetEntry entry, int color) {
        int entryWidth = getRowWidth();
        int entryHeight = entry.getHeight();
        int y = entry.getY();
        int i = this.getX() + (this.width - entryWidth) / 2;
        int j = this.getX() + (this.width + entryWidth) / 2;
        context.fill(i, y - 2, j, y + entryHeight + 2, color);
        context.fill(i + 1, y - 1, j - 1, y + entryHeight + 1, 0xFF000000);
    }

    @Override
    protected int getScrollbarX() {
        return super.scrollBarX() - 16;
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
    public void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractWidgetRenderState(context, mouseX, mouseY, delta);
        context.pose().pushMatrix();
        context.pose().scale(2f, 2f);
        context.text(font, Component.translatable("firorize.config.title.profiles"), getX() - 21, (getY()-183), Color.WHITE.getRGB());
        context.pose().popMatrix();
    }


    @Environment(value=EnvType.CLIENT)
    public class PresetEntry
            extends ObjectSelectionList.Entry<PresetListWidget.PresetEntry> {
        public final String languageDefinition;
        public PresetEntry(String languageDefinition) {
            this.languageDefinition = languageDefinition;
        }
        @Override
        public Component getNarration() {
            return Component.translatable("narrator.select", this.languageDefinition);
        }

        @Override
        public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent click, boolean doubled) {
            double mouseX = click.x();
            double mouseY = click.y();
            int x = getX();
            int y = getY();
            int entryHeight = getHeight();
            int closeWidth = entryHeight - 4;

            if (mouseX >= x+getWidth()-closeWidth-4 && mouseX <= x+getWidth()-4 && mouseY >= y && mouseY <= y+entryHeight) {
                if (!languageDefinition.equals("Initial")) {
                    // Deleting a profile is destructive and not undoable — confirm first, in a box
                    // drawn over the config screen (not a separate world-backed screen).
                    String toDelete = languageDefinition;
                    PresetListWidget.PresetEntry self = this;
                    instance.showConfirm(
                            Component.translatable("firorize.config.confirm.deleteProfile.title"),
                            Component.translatable("firorize.config.confirm.deleteProfile.message"),
                            () -> {
                                Main.CONFIG_MANAGER.getFireColorPresets().remove(toDelete);
                                Main.CONFIG_MANAGER.getImportedProfiles().remove(toDelete); // drop the online marker too
                                Main.CONFIG_MANAGER.getImportedAuthors().remove(toDelete);   // and its recorded author
                                Main.CONFIG_MANAGER.getInboxImports().remove(toDelete);      // and the friend-import flag
                                PresetListWidget.this.removeEntry(self); // removeEntry relayouts; children().remove did not refresh live
                                // setSelected updates currentPreset to the new selection; save afterwards
                                // so the persisted currentPreset never dangles at the deleted profile.
                                PresetListWidget.this.setSelected(PresetListWidget.this.children().get(0));
                                Main.CONFIG_MANAGER.save();
                            });
                    return false;
                }
            }
            setSelected(this);
            return super.mouseClicked(click, doubled);
        }
        private float alphaa;

        @Override
        public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            int x = getX();
            int y = getY();
            int entryWidth = getWidth();
            int entryHeight = getHeight();
            if (!languageDefinition.equals("Initial")) {
                if (mouseX >= x+entryWidth-entryHeight-4 && mouseX <= x+entryWidth-4 && mouseY >= y && mouseY <= y+entryHeight) {
                    alphaa = 1f;
                } else {
                    alphaa = 0.5f;
                }
                int closeWidth = entryHeight - 4;
                context.fill(x+entryWidth-closeWidth-4, y + (entryHeight / 2) - (closeWidth / 2), x+entryWidth-4, y + (entryHeight / 2) + (closeWidth / 2), new Color(1f/255*44, 1f/255*44, 1f/255*44, alphaa).getRGB());
                instance.drawX(context, y + entryHeight / 2, x + entryWidth - 4 - closeWidth / 2);
                context.outline(x+entryWidth-closeWidth-4, y + (entryHeight / 2) - (closeWidth / 2), closeWidth, closeWidth, new Color(1f/255*99, 1f/255*99, 1f/255*99, 0.8f).getRGB());
            }
            // Marker for imported profiles (drawn on the left; the name is centred). A person silhouette
            // for profiles sent by a friend via the inbox, otherwise the globe for the public gallery.
            if (Main.CONFIG_MANAGER.getImportedProfiles().contains(languageDefinition)) {
                int iconX = x + 4;
                int iconY = y + (entryHeight - 9) / 2;
                boolean fromFriend = Main.CONFIG_MANAGER.getInboxImports().contains(languageDefinition);
                if (fromFriend) {
                    instance.drawPerson(context, iconX, iconY);
                } else {
                    instance.drawGlobe(context, iconX, iconY);
                }
                // Hovering shows who the profile came from.
                if (mouseX >= iconX && mouseX <= iconX + 9 && mouseY >= iconY && mouseY <= iconY + 9) {
                    String author = Main.CONFIG_MANAGER.getImportedAuthors().get(languageDefinition);
                    if (author == null || author.isBlank()) {
                        instance.globeTooltip = Component.translatable("firorize.config.tooltip.importedOnline");
                    } else {
                        instance.globeTooltip = Component.translatable(
                                fromFriend ? "firorize.config.tooltip.sentBy" : "firorize.config.tooltip.createdBy", author);
                    }
                }
            }
            context.centeredText(PresetListWidget.this.font, Component.literal(languageDefinition), (entryWidth-6) / 2  + PresetListWidget.this.instance.wheelCoords[0], y + (entryHeight - 8) / 2 +1, 0xFFFFFFFF);
        }
    }
}