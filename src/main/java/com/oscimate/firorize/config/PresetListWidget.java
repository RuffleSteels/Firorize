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

import net.minecraft.client.input.MouseButtonEvent;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Environment(value= EnvType.CLIENT)
class PresetListWidget
        extends ObjectSelectionList<PresetListWidget.PresetEntry> {

    public String curPresetID;

    // Type badge labels + accent colours, indexed 0 block / 1 tag / 2 biome.
    private static final String[] TYPE_BADGE_KEYS = {
            "firorize.config.type.block", "firorize.config.type.tag", "firorize.config.type.biome"
    };
    private static final int[][] TYPE_BADGE_COLORS = {
            {143, 208, 143}, {143, 175, 224}, {224, 176, 112}
    };
    // Explanatory tooltip shown when the type badge is hovered, indexed 0 block / 1 tag / 2 biome.
    private static final String[] TYPE_BADGE_TOOLTIP_KEYS = {
            "firorize.config.tooltip.badge.block", "firorize.config.tooltip.badge.tag", "firorize.config.tooltip.badge.biome"
    };
    // Standard (delayed) hover for the type badge: it isn't a widget, so the delay is tracked by hand —
    // the tooltip only shows once the pointer has rested on the same badge for BADGE_TOOLTIP_DELAY_MS.
    private static final long BADGE_TOOLTIP_DELAY_MS = 500L;
    private String badgeHoverKey = null;
    private long badgeHoverStartMs = 0L;

    // Reorder mode: toggled by the reorder button next to the "Profiles" title. While on, rows can be
    // dragged to change the list order (which is the inter-profile priority — top wins), and the
    // active checkboxes / delete buttons are hidden so they aren't hit by accident.
    public boolean reorderMode = false;
    private int dragFromIndex = -1;
    private boolean orderChanged = false;

    // Vertical space reserved between the "Profiles" title and the first list row for the explanatory
    // subtitle below the title. The list is nudged down by this much (and shortened to match, see the
    // constructor in ChangeFireColorScreen) while the title stays put, so the description sits in the gap.
    static final int DESC_GAP = 44;

    // Extra gap above the whole profiles section, separating it from the colour-picker / base-overlay
    // controls above. The profile title, buttons and list all shift down by this; the list is shortened
    // to match so its bottom (and the import/share rows below) stay put. Keep even (the 2× title uses /2).
    static final int TOP_GAP = 18;

    public PresetListWidget(Minecraft minecraft, int width, int height, int x, int y, ChangeFireColorScreen instance, Font font) {
        super(minecraft, width, height, x, y);
        this.instance = instance;
        this.font = font;

        Main.CONFIG_MANAGER.getFireColorPresets().forEach((string, map) -> {
            this.addEntry(new PresetEntry(string));
        });


        isConstruct = true;

        // With no profiles at all there's nothing to edit; leave the editor unselected (curPresetID
        // stays null and the editor buffers stay empty). Otherwise fall back to the first profile if
        // the saved currentPreset no longer exists (e.g. it was deleted).
        if (!children().isEmpty()) {
            int curIndex = children().stream().map(entry -> entry.languageDefinition).toList().indexOf(Main.CONFIG_MANAGER.getCurrentPreset());
            if (curIndex < 0) curIndex = 0;
            setSelected(children().get(curIndex));
        } else {
            isConstruct = false;
        }
    }

    private final Font font;
    private final ChangeFireColorScreen instance;

    @Override
    public int getRowWidth() {
        return this.getWidth();
    }


    private boolean isConstruct = false;

    public void resetProfile() {
        // Nothing selected (the list is empty) → no profile to reset.
        if (curPresetID == null) return;
        // Snapshot the profile before resetting so the reset itself is a single undo step.
        instance.historyBefore();
        // Reset = empty this profile's entries and restore the default base colour. The profile keeps
        // its type (a tag/biome profile must not be repopulated with the block default).
        for (ListOrderedMap<String, int[]> map : Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft()) {
            map.clear();
        }
        System.arraycopy(new int[]{-7456000, -6456034}, 0, Main.CONFIG_MANAGER.getCurrentBlockFireColors().getRight(), 0, 2);
        // Commit the reset to the preset immediately (no deferral); history owns the revert.
        instance.commitToPreset();
        Main.CONFIG_MANAGER.save();
        setSelected(children().stream().filter(thing -> thing.languageDefinition.equalsIgnoreCase(curPresetID)).findFirst().get());
        instance.historyAfterReset();
        instance.resetProfileButton.setFocused(false);
    }

    public void addProfile(String presetName, KeyValuePair<KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>, int[]>, ArrayList<Integer>> newProfile, int type) {
        PresetEntry entry = new PresetEntry(presetName);
        addEntry(entry);

        Main.CONFIG_MANAGER.getFireColorPresets().put(presetName, newProfile);
        Main.CONFIG_MANAGER.getProfileTypes().put(presetName, type);
        // A freshly created/imported profile starts active so it's used right away (mirrors the old
        // behaviour where adding a profile made it the one applied in-world).
        Main.CONFIG_MANAGER.getActiveProfiles().add(presetName);

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
            // Each profile is a single category; lock the editor to this profile's type. (A biome
            // profile in the main menu lists nothing until a world provides the biome registry.)
            instance.changeSearchOption(Main.CONFIG_MANAGER.getProfileType(entry.languageDefinition));
        }

        instance.searchScreenListWidget.setSelected(instance.searchScreenListWidget.children().get(0));
        isConstruct = false;
        instance.cyclicalPresets.setIndex(0);

        super.setSelected(entry);
    }

    /** Clears the editor when the last profile is deleted: no profile is selected, the live editing
     *  buffer is emptied, and the search list refreshes (showing only the base-fire row). The colour
     *  editor controls go inert until a profile is added again. */
    public void clearSelection() {
        instance.clearHistory();
        curPresetID = null;
        Main.CONFIG_MANAGER.setCurrentPreset("");
        for (ListOrderedMap<String, int[]> map : Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft()) {
            map.clear();
        }
        instance.searchScreenListWidget.selected.clear();
        instance.searchScreenListWidget.test(false);
        instance.searchScreenListWidget.setSelected(instance.searchScreenListWidget.children().get(0));
        super.setSelected(null);
    }

    @Override
    protected void extractSelection(GuiGraphicsExtractor context, PresetEntry entry, int color) {
        int entryWidth = getRowWidth();
        int entryHeight = entry.getHeight();
        int y = entry.getY();
        int i = this.getX() + (this.width - entryWidth) / 2;
        int j = this.getX() + (this.width + entryWidth) / 2;
        // Outline extends 1px above and below the entry (2px taller than the flush vanilla default).
        context.fill(i, y - 1, j, y + entryHeight + 1, color);
        context.fill(i + 1, y, j - 1, y + entryHeight, 0xFF000000);
    }

    @Override
    protected int scrollBarX() {
        return super.scrollBarX() - 16;
    }
    @Override
    public int getX() {
        return super.getX() + instance.wheelCoords[0];
    }

    @Override
    public int getY() {
        return instance.wheelCoords[0] + instance.wheelRadius*2 + 90 + 10 + 2 + DESC_GAP + TOP_GAP;
    }



    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractWidgetRenderState(context, mouseX, mouseY, delta);
        context.pose().pushMatrix();
        context.pose().scale(2f, 2f);
        // Title is drawn at 2×, anchored to the profile button row (screen-space profileButtonY) so it
        // stays aligned with the reorder/reset/add buttons regardless of the DESC_GAP / TOP_GAP pushes.
        int titleLeft = getX() - 21;
        int titleTop = instance.profileButtonY / 2 + 2;
        context.text(font, Component.translatable("firorize.config.title.profiles"), titleLeft, titleTop, Color.WHITE.getRGB());
        context.pose().popMatrix();

        // Small grey subtitle in the gap below the title, explaining what profiles are and how to reorder.
        // Drawn unscaled (1×) and wrapped to the list width so it reads as a caption under the 2× title.
        int descX = titleLeft * 2;
        int descY = (titleTop + font.lineHeight) * 2 + 3;
        int descW = (getX() + getWidth()) - descX - 2;
        for (net.minecraft.util.FormattedCharSequence line : font.split(
                Component.translatable("firorize.config.subtitle.profiles"), descW)) {
            context.text(font, line, descX, descY, 0xFF9A9A9A);
            descY += font.lineHeight;
        }

        // With every profile deleted the list is blank; point the player at the + button so the empty
        // state reads as intentional rather than broken.
        if (children().isEmpty()) {
            int cx = getX() + getWidth() / 2;
            int cy = getY() + 6;
            for (net.minecraft.util.FormattedCharSequence line : font.split(
                    Component.translatable("firorize.config.status.noProfiles"), getWidth() - 16)) {
                context.centeredText(font, line, cx, cy, 0xFF9A9A9A);
                cy += font.lineHeight + 1;
            }
        }
    }


    // ---- Reorder (drag) handling ----

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (reorderMode && click.button() == 0) {
            PresetEntry e = getEntryAtPosition(click.x(), click.y());
            if (e != null) {
                dragFromIndex = children().indexOf(e);
                orderChanged = false;
                return true; // grab the row; don't select/edit while reordering
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
        if (reorderMode && dragFromIndex >= 0) {
            PresetEntry over = getEntryAtPosition(click.x(), click.y());
            int target = over != null ? children().indexOf(over)
                    : (click.y() < getY() ? 0 : children().size() - 1);
            if (target >= 0 && target != dragFromIndex) {
                moveEntry(dragFromIndex, target);
                dragFromIndex = target;
                orderChanged = true;
            }
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if (dragFromIndex >= 0) {
            dragFromIndex = -1;
            if (orderChanged) {
                Main.CONFIG_MANAGER.save();
                orderChanged = false;
            }
            return true;
        }
        return super.mouseReleased(click);
    }

    /** Moves a row to a new index, rebuilding both the visible list and the persisted profile order
     *  (the same profile objects, just re-keyed in the new sequence). Selection is preserved. */
    private void moveEntry(int from, int to) {
        PresetEntry selected = getSelected();
        List<PresetEntry> order = new ArrayList<>(children());
        order.add(to, order.remove(from));
        clearEntries();
        for (PresetEntry e : order) addEntry(e);
        if (selected != null) super.setSelected(selected); // restore highlight without re-copying buffers

        ListOrderedMap<String, KeyValuePair<KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>, int[]>, ArrayList<Integer>>> old = Main.CONFIG_MANAGER.getFireColorPresets();
        ListOrderedMap<String, KeyValuePair<KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>, int[]>, ArrayList<Integer>>> rebuilt = new ListOrderedMap<>();
        for (PresetEntry e : order) rebuilt.put(e.languageDefinition, old.get(e.languageDefinition));
        Main.CONFIG_MANAGER.setFireColorPresets(rebuilt);
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

            // While reordering the list, rows are grabbed for dragging (handled at the widget level);
            // a plain click here does nothing.
            if (reorderMode) return false;

            // Active toggle (green/red checkbox on the left): flips whether this profile is applied
            // in-world. Independent of which profile is selected for editing, so don't change selection.
            int cbSize = 10;
            int cbX = x + 4;
            int cbY = y + (entryHeight - cbSize) / 2;
            if (mouseX >= cbX && mouseX <= cbX + cbSize && mouseY >= cbY && mouseY <= cbY + cbSize) {
                java.util.LinkedHashSet<String> active = Main.CONFIG_MANAGER.getActiveProfiles();
                if (!active.remove(languageDefinition)) active.add(languageDefinition);
                Main.CONFIG_MANAGER.save();
                return false;
            }

            if (mouseX >= x+getWidth()-closeWidth-4 && mouseX <= x+getWidth()-4 && mouseY >= y && mouseY <= y+entryHeight) {
                // Every profile is deletable. Deleting is destructive and not undoable — confirm first,
                // in a box drawn over the config screen (not a separate world-backed screen).
                String toDelete = languageDefinition;
                PresetListWidget.PresetEntry self = this;
                instance.showConfirm(
                        Component.translatable("firorize.config.confirm.deleteProfile.title"),
                        Component.translatable("firorize.config.confirm.deleteProfile.message"),
                        () -> {
                            Main.CONFIG_MANAGER.getFireColorPresets().remove(toDelete);
                            Main.CONFIG_MANAGER.getActiveProfiles().remove(toDelete);    // and the active flag
                            Main.CONFIG_MANAGER.getImportedProfiles().remove(toDelete); // drop the online marker too
                            Main.CONFIG_MANAGER.getImportedAuthors().remove(toDelete);   // and its recorded author
                            Main.CONFIG_MANAGER.getInboxImports().remove(toDelete);      // and the friend-import flag
                            Main.CONFIG_MANAGER.getBuiltinImports().remove(toDelete);    // and the built-in flag
                            Main.CONFIG_MANAGER.getProfileTypes().remove(toDelete);      // and its recorded type
                            PresetListWidget.this.removeEntry(self); // removeEntry relayouts; children().remove did not refresh live
                            // Re-point the editor at the new first profile, or clear it if that was the
                            // last profile. Save afterwards so the persisted currentPreset never dangles.
                            if (!PresetListWidget.this.children().isEmpty()) {
                                PresetListWidget.this.setSelected(PresetListWidget.this.children().get(0));
                            } else {
                                PresetListWidget.this.clearSelection();
                            }
                            Main.CONFIG_MANAGER.save();
                        });
                return false;
            }
            setSelected(this);
            return super.mouseClicked(click, doubled);
        }
        private float alphaa;
        // Marquee state: when the hovered row's name is too long to fit, it auto-scrolls sideways.
        // marqueeStart is reset each time the hover begins so the scroll always starts from the front.
        private long marqueeStart;
        private boolean wasHovered;

        @Override
        public void extractContent(GuiGraphicsExtractor context, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            int x = getX();
            int y = getY();
            int entryWidth = getWidth();
            int entryHeight = getHeight();
            boolean active = Main.CONFIG_MANAGER.getActiveProfiles().contains(languageDefinition);

            if (reorderMode) {
                // Drag handle (three lines) replaces the checkbox; the whole row reads as draggable.
                int hx = x + 5;
                int hy = y + entryHeight / 2 - 3;
                for (int r = 0; r < 3; r++) context.fill(hx, hy + r * 3, hx + 9, hy + r * 3 + 1, 0xFFB8B8B8);
            } else {
                // Active checkbox: green when applied, red when not. Dimmed rows make inactive obvious.
                int cbSize = 10;
                int cbX = x + 4;
                int cbY = y + (entryHeight - cbSize) / 2;
                boolean cbHover = mouseX >= cbX && mouseX <= cbX + cbSize && mouseY >= cbY && mouseY <= cbY + cbSize;
                context.fill(cbX, cbY, cbX + cbSize, cbY + cbSize, active ? 0xFF2E7D32 : 0xFF7A2E2E);
                context.outline(cbX, cbY, cbSize, cbSize, cbHover ? 0xFFFFFFFF : (active ? 0xFF59C24E : 0xFFC25555));
                if (active) instance.drawCheckmark(context, cbX, cbY, 0xFFFFFFFF);
                // Hovering the checkbox explains what it does (and which way it'll flip).
                if (cbHover) {
                    instance.globeTooltip = Component.translatable(
                            active ? "firorize.config.tooltip.profileActive" : "firorize.config.tooltip.profileInactive");
                }

                // Delete button (red X) on every profile, hidden only while reordering.
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

            // Marker for imported profiles (drawn after the checkbox; the name is centred). Built-in
            // profiles get a star, profiles sent by a friend via the inbox get a person silhouette,
            // everything else from the gallery gets the globe.
            if (Main.CONFIG_MANAGER.getImportedProfiles().contains(languageDefinition)) {
                int iconX = x + 17;
                int iconY = y + (entryHeight - 9) / 2;
                boolean isBuiltin = Main.CONFIG_MANAGER.getBuiltinImports().contains(languageDefinition);
                boolean fromFriend = Main.CONFIG_MANAGER.getInboxImports().contains(languageDefinition);
                if (isBuiltin) {
                    instance.drawBuiltin(context, iconX, iconY);
                } else if (fromFriend) {
                    instance.drawPerson(context, iconX, iconY);
                } else {
                    instance.drawGlobe(context, iconX, iconY);
                }
                // Hovering shows where the profile came from.
                if (mouseX >= iconX && mouseX <= iconX + 9 && mouseY >= iconY && mouseY <= iconY + 9) {
                    if (isBuiltin) {
                        instance.globeTooltip = Component.translatable("firorize.config.tooltip.builtinProfile");
                    } else {
                        String author = Main.CONFIG_MANAGER.getImportedAuthors().get(languageDefinition);
                        if (author == null || author.isBlank()) {
                            instance.globeTooltip = Component.translatable("firorize.config.tooltip.importedOnline");
                        } else {
                            instance.globeTooltip = Component.translatable(
                                    fromFriend ? "firorize.config.tooltip.sentBy" : "firorize.config.tooltip.createdBy", author);
                        }
                    }
                }
            }
            boolean imported = Main.CONFIG_MANAGER.getImportedProfiles().contains(languageDefinition);
            int type = Main.CONFIG_MANAGER.getProfileType(languageDefinition);

            // Type badge (second column): a small pill on the right, colour-coded per type, sitting
            // left of the delete button (every profile has one).
            int deleteReserve = (entryHeight - 4) + 8;
            net.minecraft.network.chat.Component badge = Component.translatable(TYPE_BADGE_KEYS[type]);
            int badgeW = PresetListWidget.this.font.width(badge) + 6;
            int badgeX = x + entryWidth - deleteReserve - badgeW;
            int badgeY = y + (entryHeight - 11) / 2;
            float dim = active ? 1f : 0.5f;
            context.fill(badgeX, badgeY, badgeX + badgeW, badgeY + 11, new Color(0x2A / 255f, 0x2A / 255f, 0x2A / 255f, dim).getRGB());
            context.outline(badgeX, badgeY, badgeW, 11, new Color(0x5A / 255f, 0x5A / 255f, 0x5A / 255f, dim).getRGB());
            int[] tc = TYPE_BADGE_COLORS[type];
            context.text(PresetListWidget.this.font, badge, badgeX + 3, badgeY + 2,
                    new Color(tc[0] / 255f, tc[1] / 255f, tc[2] / 255f, dim).getRGB(), false);

            // Hovering the badge explains what its type recolours by, but only after a short rest on it
            // (a standard delayed tooltip), so it doesn't flash while the pointer sweeps down the list.
            boolean badgeHover = !reorderMode && mouseX >= badgeX && mouseX <= badgeX + badgeW
                    && mouseY >= badgeY && mouseY <= badgeY + 11;
            if (badgeHover) {
                if (!languageDefinition.equals(badgeHoverKey)) {
                    badgeHoverKey = languageDefinition;
                    badgeHoverStartMs = System.currentTimeMillis();
                }
                if (System.currentTimeMillis() - badgeHoverStartMs >= BADGE_TOOLTIP_DELAY_MS) {
                    instance.globeTooltip = Component.translatable(TYPE_BADGE_TOOLTIP_KEYS[type]);
                }
            } else if (languageDefinition.equals(badgeHoverKey)) {
                // Pointer left the badge we were timing — reset so re-entering waits the full delay again.
                badgeHoverKey = null;
            }

            // Name (left column): left-aligned after the checkbox/marker. When it doesn't fit the
            // space before the badge it marquee-scrolls while the row is hovered (clipped to its
            // column); otherwise it's truncated.
            int nameX = x + (imported ? 29 : 18);
            int nameColor = active ? 0xFFFFFFFF : 0xFF707070;
            int nameMax = Math.max(8, badgeX - 4 - nameX);
            int nameY = y + (entryHeight - 8) / 2 + 1;
            int fullW = PresetListWidget.this.font.width(languageDefinition);
            if (hovered && fullW > nameMax) {
                if (!wasHovered) marqueeStart = System.currentTimeMillis();
                int off = MarqueeText.offset(fullW, nameMax, System.currentTimeMillis() - marqueeStart);
                context.enableScissor(nameX, y, nameX + nameMax, y + entryHeight);
                context.text(PresetListWidget.this.font, Component.literal(languageDefinition), nameX - off, nameY, nameColor);
                context.disableScissor();
            } else {
                String shown = PresetListWidget.this.font.plainSubstrByWidth(languageDefinition, nameMax);
                context.text(PresetListWidget.this.font, Component.literal(shown), nameX, nameY, nameColor);
            }
            wasHovered = hovered;
        }
    }
}