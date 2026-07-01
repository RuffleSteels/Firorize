package com.oscimate.firorize.config;

import com.mojang.blaze3d.systems.RenderSystem;
import com.oscimate.firorize.CustomRenderLayer;
import com.oscimate.firorize.GameRendererSetting;
import com.oscimate.firorize.Main;
import com.oscimate.firorize.mixin.fire_overlays.client.FireBlockInvoker;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.EmptyBlockView;
import net.minecraft.world.biome.Biome;
import org.apache.commons.collections4.map.ListOrderedMap;
import org.apache.commons.lang3.SerializationUtils;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.time.Duration;
import java.util.List;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ChangeFireColorScreen extends Screen {
    private Screen parent;
    private boolean clicked = false;
    private boolean sliderClicked = false;
    public double clickedX = 95.0;
    public List<Integer> lastSelected = new ArrayList<>();
    public double clickedY = 95.0;
    public void drawX(DrawContext context, int entryWidth, int entryHeight, int y, int x) {
        int colorInt = new Color(1f/255*150, 1f/255*150, 1f/255*150, 1f).getRGB();
        context.fill(x+entryWidth-entryHeight-entryHeight/2 + 1, y+entryHeight/2 + 1, x+entryWidth-entryHeight-entryHeight/2, y+entryHeight/2, colorInt);
        context.fill(x+entryWidth-entryHeight-entryHeight/2 + 2, y+entryHeight/2, x+entryWidth-entryHeight-entryHeight/2 + 1, y+entryHeight/2 - 1, colorInt);
        context.fill(x+entryWidth-entryHeight-entryHeight/2, y+entryHeight/2, x+entryWidth-entryHeight-entryHeight/2 - 1, y+entryHeight/2 - 1, colorInt);
        context.fill(x+entryWidth-entryHeight-entryHeight/2 + 2, y+entryHeight/2 + 2, x+entryWidth-entryHeight-entryHeight/2 + 1, y+entryHeight/2 + 1, colorInt);
        context.fill(x+entryWidth-entryHeight-entryHeight/2, y+entryHeight/2 + 2, x+entryWidth-entryHeight-entryHeight/2 - 1, y+entryHeight/2 + 1, colorInt);
        context.fill(x+entryWidth-entryHeight-entryHeight/2 + 3, y+entryHeight/2 - 1, x+entryWidth-entryHeight-entryHeight/2 + 2, y+entryHeight/2 - 2, colorInt);
        context.fill(x+entryWidth-entryHeight-entryHeight/2 - 1, y+entryHeight/2 - 1, x+entryWidth-entryHeight-entryHeight/2 - 2, y+entryHeight/2 - 2, colorInt);
        context.fill(x+entryWidth-entryHeight-entryHeight/2 + 3, y+entryHeight/2 + 3, x+entryWidth-entryHeight-entryHeight/2 + 2, y+entryHeight/2 + 2, colorInt);
        context.fill(x+entryWidth-entryHeight-entryHeight/2 - 1, y+entryHeight/2 + 3, x+entryWidth-entryHeight-entryHeight/2 - 2, y+entryHeight/2 + 2, colorInt);
    }

    private static final String[] REORDER_GLYPH = {
            ".......",
            "...X...",
            "..XXX..",
            ".......",
            "XXXXXXX",
            ".......",
            "XXXXXXX",
            ".......",
            "XXXXXXX",
            ".......",
            "..XXX..",
            "...X...",
            ".......",
    };

    // Magnifying glass (6×7), drawn at the right of the search field so it's obvious the field is a search box.
    private static final String[] SEARCH_GLYPH = {
            ".XXX..",
            "X...X.",
            "X...X.",
            "X...X.",
            ".XXX..",
            "....X.",
            ".....X",
    };

    /** Draws the magnifying-glass glyph top-left at (px,py) in the given ARGB colour. */
    private void drawSearchIcon(DrawContext context, int px, int py, int color) {
        for (int gy = 0; gy < SEARCH_GLYPH.length; gy++) {
            for (int gx = 0; gx < SEARCH_GLYPH[gy].length(); gx++) {
                if (SEARCH_GLYPH[gy].charAt(gx) == 'X') {
                    context.fill(px + gx, py + gy, px + gx + 1, py + gy + 1, color);
                }
            }
        }
    }

    /** Draws the reorder glyph centred in the 20×20 reorder button. Tinted brighter (and with a lit
     *  interior) while reorder mode is active so the toggle state is obvious. */
    public void drawReorderIcon(DrawContext context, int px, int py) {
        boolean on = presetListWidget != null && presetListWidget.reorderMode;
        if (on) {
            context.fill(px + 1, py + 1, px + 19, py + 19, 0x40FFFFFF);
        }
        int color = on ? 0xFFFFFFFF : 0xFFBFBFBF;
        int offX = px + (20 - 7) / 2;
        int offY = py + (20 - REORDER_GLYPH.length) / 2;
        for (int gy = 0; gy < REORDER_GLYPH.length; gy++) {
            for (int gx = 0; gx < REORDER_GLYPH[gy].length(); gx++) {
                if (REORDER_GLYPH[gy].charAt(gx) == 'X') {
                    context.fill(offX + gx, offY + gy, offX + gx + 1, offY + gy + 1, color);
                }
            }
        }
    }

    /** Sentence-form headers shown above the search list explaining what the list recolours, indexed
     *  0 block / 1 tag / 2 biome. Each takes one %s arg — the bolded key term below. */
    public static final String[] TYPE_HEADER_KEYS = {
            "firorize.config.searchHeader.block", "firorize.config.searchHeader.tag", "firorize.config.searchHeader.biome"
    };
    /** The bolded term substituted into the matching TYPE_HEADER_KEYS sentence ("block" / "block tag" / "biome"). */
    public static final String[] TYPE_HEADER_TERM_KEYS = {
            "firorize.config.searchHeader.termBlock", "firorize.config.searchHeader.termTag", "firorize.config.searchHeader.termBiome"
    };
    /** Accent RGB for each type's bolded header term + divider rule (block green / tag blue / biome orange). */
    public static final int[] TYPE_HEADER_ACCENTS = { 0x8FD08F, 0x8FAFE0, 0xE0B070 };

    private String hexCode = "#ffffff";
    public Color[] baseColor = new Color[]{new Color(Main.CONFIG_MANAGER.getCurrentBlockFireColors().getRight()[0]), new Color(Main.CONFIG_MANAGER.getCurrentBlockFireColors().getRight()[1])};
    public static Color[] pickedColor = {new Color(Color.decode("#ffffff").getRGB(), true), new Color(Color.decode("#ffffff").getRGB(), true)};
    public static Color[] lastPickedColor = null;
    private double hue = 0;
    private double saturation = 0.0;
    private double lightness = 1.0;
    public final int wheelRadius = 100;
    private final int cursorDimensions = 8;
    public final int[] wheelCoords = {42, 42};
    public final int[] sliderDimensions = {20, wheelRadius*2};
    public final int[] sliderCoords = {wheelCoords[0] + wheelRadius*2 + 20, wheelCoords[1]};
    public final double sliderPadding = (double) sliderDimensions[0] / 2;
    public double sliderClickedY = sliderCoords[1] + sliderPadding;
    private final double sliderClickedX = sliderCoords[0] + sliderPadding;
    public final int[] hexBoxCoords = {wheelCoords[0], wheelCoords[1] + wheelRadius*2 + 20};
    public boolean isOverlay = false;

    public static KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>, int[]> deepClone(
            KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>, int[]> originalPair) {

        int[] originalArray = originalPair.getRight();
        int[] clonedArray = originalArray.clone();

        ArrayList<ListOrderedMap<String, int[]>> originalList = originalPair.getLeft();
        ArrayList<ListOrderedMap<String, int[]>> clonedList = new ArrayList<>();

        for (ListOrderedMap<String, int[]> originalMap : originalList) {
            ListOrderedMap<String, int[]> clonedMap = new ListOrderedMap<>();

            for (Map.Entry<String, int[]> entry : originalMap.entrySet()) {
                int[] originalMapArray = entry.getValue();
                int[] clonedMapArray = originalMapArray.clone();

                clonedMap.put(entry.getKey(), clonedMapArray);
            }
            clonedList.add(clonedMap);
        }
        return new KeyValuePair<>(clonedList, clonedArray);
    }

    /**
     * Positional deep-equality for the fire-colour data. The three maps in {@code getLeft()}
     * (0=blocks, 1=tags, 2=biomes) are compared index-for-index — including key order, since
     * that drives in-category resolution — plus the global base colour in {@code getRight()}.
     * <p>Unlike a set-style cross-match, this correctly reports a change when entries are
     * <em>deleted</em> (e.g. a map emptied so it coincides with another empty category), so the
     * texture reload in {@link #onClose()} fires for deletions, not just edits.
     */
    private static boolean fireColorsEqual(
            KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>, int[]> a,
            KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>, int[]> b) {
        if (!Arrays.equals(a.getRight(), b.getRight())) return false;
        ArrayList<ListOrderedMap<String, int[]>> la = a.getLeft();
        ArrayList<ListOrderedMap<String, int[]>> lb = b.getLeft();
        if (la.size() != lb.size()) return false;
        for (int i = 0; i < la.size(); i++) {
            ListOrderedMap<String, int[]> ma = la.get(i);
            ListOrderedMap<String, int[]> mb = lb.get(i);
            if (!ma.keyList().equals(mb.keyList())) return false;
            for (String key : ma.keyList()) {
                if (!Arrays.equals(ma.get(key), mb.get(key))) return false;
            }
        }
        return true;
    }

    // Snapshot of the persistent profile state taken when the screen opens: each profile's colour data
    // keyed by name, the active set, and the profile order. The close-time texture reload fires only
    // when one of these actually changed — NOT when the user merely selected a different profile row
    // (selecting overwrites the live editing buffer but changes nothing persistent).
    private final java.util.LinkedHashMap<String, KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>, int[]>> comparedProfileData;
    private final java.util.LinkedHashSet<String> comparedActiveProfiles;
    private final ArrayList<String> comparedProfileOrder;
    protected ChangeFireColorScreen(Screen parent) {
        super(Text.translatable("options.videoTitle"));
        this.comparedProfileData = snapshotProfileData();
        this.comparedActiveProfiles = new java.util.LinkedHashSet<>(Main.CONFIG_MANAGER.getActiveProfiles());
        this.comparedProfileOrder = new ArrayList<>(Main.CONFIG_MANAGER.getFireColorPresets().keyList());
        this.parent = parent;
    }

    /** True when {@link PresetListWidget} has a real selected profile to edit/commit. */
    public boolean hasProfile() {
        return presetListWidget != null && presetListWidget.curPresetID != null
                && Main.CONFIG_MANAGER.getFireColorPresets().containsKey(presetListWidget.curPresetID);
    }

    /** Deep snapshot of every profile's colour data (the 3 maps + base), keyed by name, taken when the
     *  screen opens. Independent of the live editing buffer, so switching the selected profile doesn't
     *  register as a change in {@link #profileDataChanged()}. */
    private static java.util.LinkedHashMap<String, KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>, int[]>> snapshotProfileData() {
        java.util.LinkedHashMap<String, KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>, int[]>> out = new java.util.LinkedHashMap<>();
        ListOrderedMap<String, KeyValuePair<KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>, int[]>, ArrayList<Integer>>> presets = Main.CONFIG_MANAGER.getFireColorPresets();
        for (String name : presets.keyList()) out.put(name, deepClone(presets.get(name).getLeft()));
        return out;
    }

    /** True when any profile's colours/base differ from the open-time snapshot, or a profile was
     *  added/removed. Invariant to which profile is selected, so it only reports real edits. */
    private boolean profileDataChanged() {
        ListOrderedMap<String, KeyValuePair<KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>, int[]>, ArrayList<Integer>>> presets = Main.CONFIG_MANAGER.getFireColorPresets();
        if (!comparedProfileData.keySet().equals(presets.keySet())) return true;
        for (java.util.Map.Entry<String, KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>, int[]>> e : comparedProfileData.entrySet()) {
            KeyValuePair<KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>, int[]>, ArrayList<Integer>> cur = presets.get(e.getKey());
            if (cur == null || !fireColorsEqual(e.getValue(), cur.getLeft())) return true;
        }
        return false;
    }
    public void onClose() {
        Main.inConfig = false;
        // Flush the live editing buffer back into the selected preset *before* comparing, so the
        // selected profile's latest edits count as a real profile-data change (skipped when no
        // profile exists). Reload only when profile colours, the active set, or the profile order
        // actually changed — not when the user merely switched which profile is selected.
        commitToPreset();
        if (!isPresetAdd && (profileDataChanged()
                || !comparedActiveProfiles.equals(Main.CONFIG_MANAGER.getActiveProfiles())
                || !comparedProfileOrder.equals(Main.CONFIG_MANAGER.getFireColorPresets().keyList()))) {
            MinecraftClient.getInstance().reloadResources();  }

        Main.CONFIG_MANAGER.save();

        int i = this.client.getWindow().calculateScaleFactor(this.client.options.getGuiScale().getValue(), this.client.forcesUnicodeFont());
        this.client.getWindow().setScaleFactor((double)i);

        client.setScreen(parent);
    }
    private boolean onBaseColor = true;
    public TextFieldWidget textFieldWidget;
    public TextFieldWidget blockUnderField;
    public ColoredCycleButton cyclicalPresets;
    private Block blockUnder = Blocks.NETHERRACK;
    private List<Block> allBlockUnders = new ArrayList<>();
    public String input = "";
    public ChangeFireColorScreen.SearchScreenListWidget searchScreenListWidget;
    public PresetListWidget presetListWidget;
    private List<Block> blockUnderList = Registries.BLOCK.stream().filter(block -> {
        BlockState state = block.getDefaultState();
        for (Direction direction : Direction.values()) {
            if (state.isSideSolidFullSquare(EmptyBlockView.INSTANCE, BlockPos.ORIGIN, direction)) {
                return true;
            }
        }
        return ((FireBlockInvoker)Blocks.FIRE).getBurnChances().containsKey(block);
    }).toList();
    private final int[] blockSearchCoords = {0, 18};
    private final int[] blockSearchDimensions = {300, 320};
    private ButtonWidget[] overlayToggles = new ButtonWidget[2];
    public ButtonWidget undoButton;
    public ButtonWidget redoButton;
    private boolean colorRedo = false;
    private ButtonWidget saveButton;

    /** Maximum number of undo steps retained. Edit this to change history depth. */
    public static final int MAX_HISTORY = 50;
    private final Deque<HistoryEntry> undoStack = new ArrayDeque<>();
    private final Deque<HistoryEntry> redoStack = new ArrayDeque<>();
    /** Guards against history pushes/clears while we are applying an undo or redo. */
    private boolean isUndoRedoing = false;
    /** pickedColor pair captured at the start of a colour-wheel/slider gesture. */
    private Color[] gestureStartColor = null;
    /** before-snapshot captured by {@link #historyBefore()} for the next config action. */
    private KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>, int[]> pendingBefore = null;
    private ArrayList<Integer> pendingPrioBefore = null;
    private ListOrderedMap<String, int[]> pendingPresetBefore = null;

    private static ListOrderedMap<String, int[]> clonePresets(ListOrderedMap<String, int[]> src) {
        ListOrderedMap<String, int[]> out = new ListOrderedMap<>();
        for (String k : src.keyList()) out.put(k, src.get(k).clone());
        return out;
    }

    private void clearPending() {
        pendingBefore = null;
        pendingPrioBefore = null;
        pendingPresetBefore = null;
    }

    private void pushHistory(HistoryEntry e) {
        if (isUndoRedoing) return;
        redoStack.clear();
        undoStack.push(e);
        while (undoStack.size() > MAX_HISTORY) undoStack.removeLast();
        refreshHistoryButtons();
    }

    /** Clears all undo/redo history (e.g. when switching profiles). */
    public void clearHistory() {
        undoStack.clear();
        redoStack.clear();
        refreshHistoryButtons();
    }

    private void refreshHistoryButtons() {
        if (undoButton != null) undoButton.active = !undoStack.isEmpty();
        if (redoButton != null) redoButton.active = !redoStack.isEmpty();
    }

    /** Capture the config state before a breaking action; pair with {@link #historyAfter}. */
    public void historyBefore() {
        if (isUndoRedoing) return;
        pendingBefore = deepClone(Main.CONFIG_MANAGER.getCurrentBlockFireColors());
        pendingPrioBefore = new ArrayList<>(Main.CONFIG_MANAGER.getPriorityOrder());
        pendingPresetBefore = clonePresets(Main.CONFIG_MANAGER.getCustomColorPresets());
    }

    /** Push a CONFIG history entry using the snapshot from {@link #historyBefore} as "before". */
    public void historyAfter(int tab, String target, boolean overlay) {
        if (isUndoRedoing || pendingBefore == null) { clearPending(); return; }
        pushHistory(HistoryEntry.config(tab, target, overlay, false,
                pendingBefore, pendingPrioBefore, pendingPresetBefore,
                deepClone(Main.CONFIG_MANAGER.getCurrentBlockFireColors()),
                new ArrayList<>(Main.CONFIG_MANAGER.getPriorityOrder()),
                clonePresets(Main.CONFIG_MANAGER.getCustomColorPresets())));
        clearPending();
    }

    /** Convenience for reset: navigates back to the base-colour entry on the current tab. */
    public void historyAfterReset() {
        historyAfter(currentSearchButton, baseEntryName(), false);
    }

    /** Push a CONFIG entry for a tab-priority reorder (refreshes tab buttons, keeps no entry target). */
    public void historyAfterTabs() {
        historyAfter(currentSearchButton, null, isOverlay);
    }

    /** Push a CONFIG entry for a custom colour-preset add/delete (does not disturb list/tab selection). */
    public void historyAfterPreset() {
        if (isUndoRedoing || pendingBefore == null) { clearPending(); return; }
        pushHistory(HistoryEntry.config(currentSearchButton, null, isOverlay, true,
                pendingBefore, pendingPrioBefore, pendingPresetBefore,
                deepClone(Main.CONFIG_MANAGER.getCurrentBlockFireColors()),
                new ArrayList<>(Main.CONFIG_MANAGER.getPriorityOrder()),
                clonePresets(Main.CONFIG_MANAGER.getCustomColorPresets())));
        clearPending();
    }

    public String baseEntryName() {
        return Text.translatable("firorize.config.baseFire").getString();
    }

    // ---- In-screen confirmation dialog (drawn over this screen, not a separate Screen) ----
    private boolean confirmActive = false;
    private Text confirmTitle;
    private Text confirmMessage;
    private Runnable confirmOnYes;
    private final int confirmBoxW = 280;
    private final int confirmBoxH = 110;

    /** Shows a modal confirm box over the current screen; runs onYes only if the user confirms. */
    public void showConfirm(Text title, Text message, Runnable onYes) {
        this.confirmTitle = title;
        this.confirmMessage = message;
        this.confirmOnYes = onYes;
        this.confirmActive = true;
        this.setFocused(null);
    }

    private void closeConfirm() {
        confirmActive = false;
        confirmOnYes = null;
    }

    private int confirmBoxX() { return (width - confirmBoxW) / 2; }
    private int confirmBoxY() { return (height - confirmBoxH) / 2; }
    private int[] confirmYesRect() {
        int w = (confirmBoxW - 45) / 2;
        return new int[]{confirmBoxX() + 15, confirmBoxY() + confirmBoxH - 30, w, 20};
    }
    private int[] confirmNoRect() {
        int w = (confirmBoxW - 45) / 2;
        return new int[]{confirmBoxX() + confirmBoxW - 15 - w, confirmBoxY() + confirmBoxH - 30, w, 20};
    }
    private static boolean inRect(int[] r, double mx, double my) {
        return mx >= r[0] && mx <= r[0] + r[2] && my >= r[1] && my <= r[1] + r[3];
    }

    private void renderConfirm(DrawContext context, int mouseX, int mouseY) {
        // The centre block/fire preview is drawn as depth-tested 3D, so push the overlay to a high Z
        // (like vanilla tooltips) to guarantee it sits in front of everything.
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 1000);
        context.fill(0, 0, width, height, 0xB0000000); // dim everything behind the box
        int bx = confirmBoxX(), by = confirmBoxY();
        context.fill(bx - 1, by - 1, bx + confirmBoxW + 1, by + confirmBoxH + 1, 0xFF000000);
        context.fill(bx, by, bx + confirmBoxW, by + confirmBoxH, 0xFF1A1A1A);
        context.drawBorder(bx, by, confirmBoxW, confirmBoxH, 0xFF8B8B8B);
        context.drawCenteredTextWithShadow(textRenderer, confirmTitle, width / 2, by + 10, 0xFFFFFF);
        int ty = by + 30;
        for (OrderedText line : textRenderer.wrapLines(confirmMessage, confirmBoxW - 24)) {
            context.drawCenteredTextWithShadow(textRenderer, line, width / 2, ty, 0xFFC0C0C0);
            ty += 11;
        }
        drawConfirmButton(context, confirmYesRect(), ScreenTexts.YES, mouseX, mouseY);
        drawConfirmButton(context, confirmNoRect(), ScreenTexts.NO, mouseX, mouseY);
        context.getMatrices().pop();
    }

    private void drawConfirmButton(DrawContext context, int[] r, Text label, int mouseX, int mouseY) {
        boolean hover = inRect(r, mouseX, mouseY);
        context.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], hover ? 0xFF505050 : 0xFF383838);
        context.drawBorder(r[0], r[1], r[2], r[3], hover ? 0xFFFFFFFF : 0xFF8B8B8B);
        context.drawCenteredTextWithShadow(textRenderer, label, r[0] + r[2] / 2, r[1] + (r[3] - 8) / 2, 0xFFFFFF);
    }

    private void undo() {
        if (undoStack.isEmpty()) return;
        HistoryEntry e = undoStack.pop();
        isUndoRedoing = true;
        applyEntry(e, false);
        redoStack.push(e);
        isUndoRedoing = false;
        refreshHistoryButtons();
    }

    private void redo() {
        if (redoStack.isEmpty()) return;
        HistoryEntry e = redoStack.pop();
        isUndoRedoing = true;
        applyEntry(e, true);
        undoStack.push(e);
        isUndoRedoing = false;
        refreshHistoryButtons();
    }

    private void applyEntry(HistoryEntry e, boolean redo) {
        if (e.type == HistoryEntry.Type.COLOR) {
            navigateTo(e.tab, e.target, e.overlay, true);
            Color[] c = redo ? e.colorAfter : e.colorBefore;
            pickedColor[0] = c[0];
            pickedColor[1] = c[1];
            int RGB = pickedColor[e.overlay ? 1 : 0].getRGB();
            textFieldWidget.setText("#" + Integer.toHexString(RGB).substring(2));
            updateCursor("#" + Integer.toHexString(RGB).substring(2));
        } else {
            restoreConfig(redo ? e.cfgAfter : e.cfgBefore, redo ? e.prioAfter : e.prioBefore,
                    redo ? e.presetAfter : e.presetBefore);
            if (!e.presetOnly) {
                navigateTo(e.tab, e.target, e.overlay, false);
            }
        }
    }

    /** Overwrites the live currentBlockFireColors + priority order + custom presets from a snapshot and commits it. */
    private void restoreConfig(KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>, int[]> snap, ArrayList<Integer> prio,
                               ListOrderedMap<String, int[]> presets) {
        KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>, int[]> live = Main.CONFIG_MANAGER.getCurrentBlockFireColors();
        System.arraycopy(snap.getRight(), 0, live.getRight(), 0, live.getRight().length);
        for (int t = 0; t < live.getLeft().size(); t++) {
            ListOrderedMap<String, int[]> lm = live.getLeft().get(t);
            ListOrderedMap<String, int[]> sm = snap.getLeft().get(t);
            lm.clear();
            for (String k : sm.keyList()) lm.put(k, sm.get(k).clone());
        }
        Main.CONFIG_MANAGER.getPriorityOrder().clear();
        Main.CONFIG_MANAGER.getPriorityOrder().addAll(prio);
        // Restore the custom colour presets and rebuild the cycle button from them.
        ListOrderedMap<String, int[]> livePresets = Main.CONFIG_MANAGER.getCustomColorPresets();
        livePresets.clear();
        for (String k : presets.keyList()) livePresets.put(k, presets.get(k).clone());
        cyclicalPresets.rebuildValues();
        baseColor = new Color[]{new Color(live.getRight()[0]), new Color(live.getRight()[1])};
        commitToPreset();
        Main.CONFIG_MANAGER.save();
    }

    /** Copies the live currentBlockFireColors + priority order into the active preset (no disk write). */
    public void commitToPreset() {
        if (!hasProfile()) return;
        int[] list = Main.CONFIG_MANAGER.getCurrentBlockFireColors().getRight();
        System.arraycopy(list, 0, Main.CONFIG_MANAGER.getFireColorPresets().get(presetListWidget.curPresetID).getLeft().getRight(), 0, list.length);
        Collections.copy(Main.CONFIG_MANAGER.getFireColorPresets().get(presetListWidget.curPresetID).getLeft().getLeft(), Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft());
        Collections.copy(Main.CONFIG_MANAGER.getFireColorPresets().get(presetListWidget.curPresetID).getRight(), Main.CONFIG_MANAGER.getPriorityOrder());
    }

    /** Switches tab/selection/side to the context an undone/redone action belonged to. */
    private void navigateTo(int tab, String target, boolean overlay, boolean colorOnly) {
        if (colorOnly) {
            // Colour edits don't change the list; only switch tab if it actually differs.
            if (client.world != null && tab != currentSearchButton) {
                changeSearchOption(tab);
            }
        } else if (client.world != null) {
            // Config restore: rebuild the list and refresh the tab buttons from the restored priority order.
            changeSearchOption(tab);
        } else {
            searchScreenListWidget.test();
        }
        if (target != null) {
            searchScreenListWidget.selectByName(target);
        }
        applyOverlay(overlay);
    }

    private void applyOverlay(boolean overlay) {
        isOverlay = overlay;
        overlayToggles[isOverlay ? 1 : 0].active = false;
        overlayToggles[!isOverlay ? 1 : 0].active = true;
        int RGB = pickedColor[isOverlay ? 1 : 0].getRGB();
        textFieldWidget.setText("#" + Integer.toHexString(RGB).substring(2));
        updateCursor("#" + Integer.toHexString(RGB).substring(2));
    }
    public ButtonWidget[] searchOptions = new ButtonWidget[3];
    private List<TagKey<Block>> blockTags = new ArrayList<>();
    private List<RegistryKey<Biome>> biomeKeys = new ArrayList<>();
    public void handlePickedColor(Color[] input) {
        // Colour-wheel history is now recorded per gesture in mouseReleased; this hook no longer
        // couples colour setting (or list selection) to the undo button.
        buffer = false;
    }
    public void setPickedColors(Color[] pickedColor) {
        handlePickedColor(pickedColor);
        ChangeFireColorScreen.pickedColor = pickedColor;
    }
    public void setPickedColor(Color pickedColor, int index) {
        handlePickedColor(ChangeFireColorScreen.pickedColor);
        ChangeFireColorScreen.pickedColor[index] = pickedColor;
    }
    public ButtonWidget addButton;
    public Color[] tempColor;
    public ButtonWidget addColorButton;
    public InvisibleTextFieldWidget invisibleTextFieldWidget;
    public ButtonWidget browseOnlineButton;
    public ButtonWidget shareBottomButton;
    public ButtonWidget inboxButton;
    public ButtonWidget resetProfileButton;
    /** Toggles {@link PresetListWidget#reorderMode} so profiles can be drag-reordered (their order is
     *  the inter-profile priority — top active wins). */
    public ButtonWidget reorderProfilesButton;
    /** Set by {@link PresetListWidget} while hovering an imported profile's globe; drawn once then cleared. */
    public net.minecraft.text.Text globeTooltip = null;
    public ButtonWidget[] movableArrowButtons = new ButtonWidget[6];
    public int profileButtonY = wheelCoords[0] + wheelRadius*2 + 80 + PresetListWidget.TOP_GAP;

    public int profileButtonXInitial = (wheelRadius*2 + sliderDimensions[0] + 20) + wheelCoords[0] - 20;
    public boolean isCycling = false;
    public int[] profileButtonXs = new int[]{profileButtonXInitial-40, profileButtonXInitial-20, profileButtonXInitial};
    @Override
    protected void init() {
        Main.inConfig = true;

        invisibleTextFieldWidget = new InvisibleTextFieldWidget(this, this.textRenderer, wheelCoords[0] + 50 + 20, hexBoxCoords[1],wheelRadius*2  + sliderDimensions[0] - 50 - 20, 20, ScreenTexts.DONE);

        invisibleTextFieldWidget.visible = false;

        invisibleTextFieldWidget.setPlaceholder(Text.translatable("firorize.config.placeholder.newColorPresetField"));

        addColorButton = new ButtonWidget.Builder(Text.literal("+"), button -> cyclicalPresets.addColor()).dimensions((wheelRadius*2 + sliderDimensions[0] + 20) + wheelCoords[0] - 20, hexBoxCoords[1], 20, 20).build();

        this.cyclicalPresets = ColoredCycleButton.builder()
                .build(this, wheelCoords[0] + 50 + 20, hexBoxCoords[1], wheelRadius*2  + sliderDimensions[0] - 50 - 20, 20, textRenderer);

        blockSearchCoords[0] = width - 300 - 20;
        undoButton = new UndoButton(hexBoxCoords[0] - 22, hexBoxCoords[1], 20, 20, button -> undo());
        redoButton = new RedoButton(hexBoxCoords[0], hexBoxCoords[1], 20, 20, button -> redo());
        saveButton = new ButtonWidget.Builder(Text.translatable("firorize.config.button.applyButton"), button -> save()).dimensions(width - 300 - 20, 20 + blockSearchDimensions[1], 150, 20).build();
        this.addDrawableChild(saveButton);

        saveButton.active = false;
        this.addDrawableChild(new ButtonWidget.Builder(ScreenTexts.DONE, button -> onClose()).dimensions(width - 150 - 20, 20 + blockSearchDimensions[1], 150, 20).build());
        this.searchScreenListWidget = new ChangeFireColorScreen.SearchScreenListWidget(this.client, blockSearchDimensions[0], blockSearchDimensions[1] - 40, blockSearchCoords[1] + 40, 15);
        this.addDrawableChild(searchScreenListWidget);
        textFieldWidget = new CustomTextFieldWidget(this.textRenderer, hexBoxCoords[0] + 20+1, hexBoxCoords[1]+1, 48, 18, ScreenTexts.DONE, this, true);
        blockUnderField = new CustomTextFieldWidget(this.textRenderer, blockSearchCoords[0]+1, blockSearchCoords[1]+20+1, blockSearchDimensions[0]-2, 18, ScreenTexts.DONE, this, false);this.addDrawableChild(textFieldWidget);
        this.addDrawableChild(blockUnderField);

        this.presetListWidget = new PresetListWidget(client,  wheelRadius*2 + sliderDimensions[0] + 20, height-hexBoxCoords[1] -60-20 - 30 - 48 - PresetListWidget.DESC_GAP - PresetListWidget.TOP_GAP, wheelCoords[0], 15, this, textRenderer);

        // Two button rows stack directly under the profile list (the list height above was shrunk by
        // 48 to leave room): "Community Profiles" full width, then a wide Share button with a square
        // "Inbox" text button to its right (together spanning the list width).
        this.browseOnlineButton = new ButtonWidget.Builder(Text.translatable("firorize.config.button.communityProfiles"), button -> client.setScreen(new OnlinePresetsScreen(this, OnlinePresetsScreen.View.BROWSE)))
                .dimensions(presetListWidget.getX(), presetListWidget.getY() + presetListWidget.getHeight() + 4, presetListWidget.getWidth(), 20).build();

        int row2Y = presetListWidget.getY() + presetListWidget.getHeight() + 28;
        int row2Gap = 2;
        // Inbox button wraps narrowly to its label and stays right-aligned at the end of the row;
        // Share fills the remaining width to its left.
        Text inboxLabel = Text.translatable("firorize.config.button.inbox");
        int inboxSize = textRenderer.getWidth(inboxLabel) + 12;
        int shareW = presetListWidget.getWidth() - inboxSize - row2Gap;
        this.shareBottomButton = new ButtonWidget.Builder(Text.translatable("firorize.config.button.share"), button -> client.setScreen(new ChooseProfileScreen(this, null)))
                .dimensions(presetListWidget.getX(), row2Y, shareW, 20).build();
        this.inboxButton = new ButtonWidget.Builder(inboxLabel, button -> client.setScreen(new OnlinePresetsScreen(this, OnlinePresetsScreen.View.INBOX)))
                .dimensions(presetListWidget.getX() + shareW + row2Gap, row2Y, inboxSize, 20).build();
        // Pull the inbox count so the notification badge is up to date when this screen opens.
        OnlinePresetsClient.refreshInboxCount();

        // Right-aligned: Add flush against the panel's right edge, Reset directly to its left.
        this.resetProfileButton = new ButtonWidget.Builder(Text.literal(""), button -> this.presetListWidget.resetProfile()).dimensions(profileButtonXs[1], profileButtonY, 20, 20).build();
        this.addButton = new ButtonWidget.Builder(Text.literal("+"), button -> presetListWidget.addPreset()).dimensions(profileButtonXs[2], profileButtonY, 20, 20).build();
        this.addDrawableChild(addButton);
//        textFieldWidget.setChangedListener(this::updateCursor);
        updateCursor(this.hexCode);

        overlayToggles[0] = new ButtonWidget.Builder(Text.translatable("firorize.config.button.baseButton"), button -> toggle(false)).dimensions(hexBoxCoords[0], hexBoxCoords[1] + 30, (wheelRadius*2 + 20 + sliderDimensions[0])/2, 20).build();
        overlayToggles[1]  = new ButtonWidget.Builder(Text.translatable("firorize.config.button.overlayButton"), button -> toggle(false)).dimensions(hexBoxCoords[0] + (wheelRadius*2 + 20 + sliderDimensions[0])/2, hexBoxCoords[1] + 30, (wheelRadius*2 + 20 + sliderDimensions[0])/2, 20).build();

        searchOptions[0] = new MoveableButton(this, this.textRenderer, blockSearchCoords[0], blockSearchCoords[1], blockSearchDimensions[0]/3, 20, Text.translatable("firorize.config.title.blocks"),  0);
        searchOptions[1]  = new MoveableButton(this, this.textRenderer, blockSearchCoords[0]+blockSearchDimensions[0]/3, blockSearchCoords[1], blockSearchDimensions[0]/3, 20, Text.translatable("firorize.config.title.tags"), 1);
        searchOptions[2]  = new MoveableButton(this, this.textRenderer, blockSearchCoords[0]+blockSearchDimensions[0]/3*2, blockSearchCoords[1], blockSearchDimensions[0]/3, 20, Text.translatable("firorize.config.title.biomes"), 2);

        // Single-type profiles: the block/tag/biome tabs and their priority arrows are gone — each
        // profile is one category and the editor is locked to it (see PresetListWidget#setSelected →
        // changeSearchOption). The searchOptions[] buttons are still constructed (changeSearchOption
        // flips their .active), but they're not added to the screen, so the category can't be switched.

        // Reorder toggle sits left of Reset/Add on the profile button row.
        this.reorderProfilesButton = new ButtonWidget.Builder(Text.literal(""), button -> {
            presetListWidget.reorderMode = !presetListWidget.reorderMode;
            button.setFocused(false);
        }).dimensions(profileButtonXs[0], profileButtonY, 20, 20).build();
        this.addDrawableChild(reorderProfilesButton);
        reorderProfilesButton.setTooltip(Tooltip.of(Text.translatable("firorize.config.tooltip.reorderProfilesButton")));
        reorderProfilesButton.setTooltipDelay(Duration.ofMillis(750L));

        this.addDrawableChild(presetListWidget);
        this.addDrawableChild(browseOnlineButton);
        this.addDrawableChild(inboxButton);
        this.addDrawableChild(shareBottomButton);
        this.addDrawableChild(overlayToggles[0]);
        this.addDrawableChild(overlayToggles[1]);
        this.addDrawableChild(undoButton);
        this.addDrawableChild(redoButton);
        this.addDrawableChild(cyclicalPresets);
        this.addDrawableChild(addColorButton);
        this.addDrawableChild(invisibleTextFieldWidget);
        this.addDrawableChild(resetProfileButton);

        // Lock the editor to the selected profile's single category (block/tag/biome). A biome/tag
        // profile in the main menu lists nothing until a world provides the registry — the search list
        // shows an explanatory message in that case (see SearchScreenListWidget).
        if (presetListWidget.curPresetID != null) {
            this.changeSearchOption(Main.CONFIG_MANAGER.getProfileType(presetListWidget.curPresetID));
        }

        shareBottomButton.setTooltip(Tooltip.of(Text.translatable("firorize.config.tooltip.shareProfileButton")));
        shareBottomButton.setTooltipDelay(Duration.ofMillis(750L));
        browseOnlineButton.setTooltip(Tooltip.of(Text.translatable("firorize.config.tooltip.onlinePresets")));
        browseOnlineButton.setTooltipDelay(Duration.ofMillis(750L));
        inboxButton.setTooltip(Tooltip.of(Text.translatable("firorize.config.tooltip.inboxButton")));
        inboxButton.setTooltipDelay(Duration.ofMillis(750L));
        addButton.setTooltip(Tooltip.of(Text.translatable("firorize.config.tooltip.addProfileButton")));
        addButton.setTooltipDelay(Duration.ofMillis(750L));
        resetProfileButton.setTooltip(Tooltip.of(Text.translatable("firorize.config.tooltip.resetProfileButton")));
        resetProfileButton.setTooltipDelay(Duration.ofMillis(750L));
        overlayToggles[0].setTooltip(Tooltip.of(Text.translatable("firorize.config.tooltip.baseToggle")));
        overlayToggles[0].setTooltipDelay(Duration.ofMillis(750L));
        overlayToggles[1].setTooltip(Tooltip.of(Text.translatable("firorize.config.tooltip.overlayToggle")));
        overlayToggles[1].setTooltipDelay(Duration.ofMillis(750L));
        saveButton.setTooltip(Tooltip.of(Text.translatable("firorize.config.tooltip.applyButton")));
        saveButton.setTooltipDelay(Duration.ofMillis(750L));
        undoButton.setTooltip(Tooltip.of(Text.translatable("firorize.config.tooltip.undoButton")));
        undoButton.setTooltipDelay(Duration.ofMillis(750L));
        redoButton.setTooltip(Tooltip.of(Text.translatable("firorize.config.tooltip.redoButton")));
        redoButton.setTooltipDelay(Duration.ofMillis(750L));

        toggle(true);

        searchScreenListWidget.setSelected(searchScreenListWidget.children().get(0));

        refreshHistoryButtons();

        super.init();
    }


    private int tooltipTimer = 0;

    public int cycleTooltipTimer = 0;



    @Override
    public void tick() {
        super.tick();


        if (tooltipTimer > 0) {
            tooltipTimer--;
        }
        if (cycleTooltipTimer > 0) {
            cycleTooltipTimer--;
        }
    }

    public boolean isPresetAdd = false;

    public static String serializeToString(KeyValuePair<KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>, int[]>, ArrayList<Integer>> pair) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(pair);
            oos.flush();
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        }
    }


    /** A tiny pixel-art "WWW" globe (9×9), drawn top-left at (px,py). Marks online-imported profiles. */
    public void drawGlobe(DrawContext context, int px, int py) {
        final int size = 9, cx = 4, cy = 4;
        final double r = 4.3;
        final int ocean = 0xFF3A78C2;
        final int line = 0xFFDDEEFF;
        for (int gy = 0; gy < size; gy++) {
            for (int gx = 0; gx < size; gx++) {
                int dx = gx - cx, dy = gy - cy;
                if (dx * dx + dy * dy > r * r) continue;
                boolean grid = gx == cx || gy == cy
                        || ((gx == 2 || gx == 6) && Math.abs(dy) <= 3)
                        || ((gy == 2 || gy == 6) && Math.abs(dx) <= 3);
                context.fill(px + gx, py + gy, px + gx + 1, py + gy + 1, grid ? line : ocean);
            }
        }
    }

    /** A tiny pixel-art head-and-shoulders silhouette (9×9), drawn top-left at (px,py). Marks
     *  profiles sent by another player via the inbox. */
    public void drawPerson(DrawContext context, int px, int py) {
        final int body = 0xFFDDE3EC;
        context.fill(px + 3, py + 1, px + 6, py + 4, body);
        context.fill(px + 2, py + 5, px + 7, py + 6, body);
        context.fill(px + 1, py + 6, px + 8, py + 9, body);
    }

    /** Gold star marker for profiles imported from the curated built-in gallery. */
    public void drawBuiltin(DrawContext context, int px, int py) {
        final int gold = 0xFFFFC83C;
        String[] star = {
                "....X....",
                "....X....",
                "...XXX...",
                "XXXXXXXXX",
                ".XXXXXXX.",
                "..XXXXX..",
                "..XXXXX..",
                ".XX...XX.",
                "XX.....XX",
        };
        for (int gy = 0; gy < star.length; gy++) {
            for (int gx = 0; gx < star[gy].length(); gx++) {
                if (star[gy].charAt(gx) == 'X') context.fill(px + gx, py + gy, px + gx + 1, py + gy + 1, gold);
            }
        }
    }

    /** Small tick drawn inside the active-profile checkbox. */
    public void drawCheckmark(DrawContext context, int x, int y, int color) {
        int[][] pts = {{2, 4}, {3, 5}, {4, 6}, {5, 5}, {6, 4}, {7, 3}, {8, 2}};
        for (int[] p : pts) context.fill(x + p[0], y + p[1], x + p[0] + 2, y + p[1] + 2, color);
    }

    /** Draws the reset.png sprite centred in the 20×20 reset-profile button at (px,py). Uses the
     *  block atlas the same way {@link UndoButton} draws its icon. */
    private static final int RESET_ICON_SIZE = 14;
    @SuppressWarnings("deprecation") // BLOCK_ATLAS_TEXTURE is deprecated but still the supported atlas id in 1.21
    public void drawResetIcon(DrawContext context, int px, int py) {
        Sprite reset = new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, Identifier.of("firorize:block/reset")).getSprite();
        int off = (20 - RESET_ICON_SIZE) / 2; // centred in the 20×20 button
        context.drawSprite(px + off, py + off, 10, RESET_ICON_SIZE, RESET_ICON_SIZE, reset);
    }

    /** True while this screen is being drawn purely as a static backdrop behind a modal dialog. In
     *  that mode the live colour wheel and the immediate-mode 3D block/fire previews are suppressed so
     *  they don't paint over (or depth-fight with) the overlaying dialog. */
    private boolean renderingAsBackdrop = false;

    /** Renders this config screen as a static modal backdrop (previews/wheel suppressed). */
    void renderAsBackdrop(DrawContext context, float delta) {
        boolean prev = renderingAsBackdrop;
        renderingAsBackdrop = true;
        try {
            this.render(context, -1, -1, delta);
        } finally {
            renderingAsBackdrop = prev;
        }
    }

    /** Shared by the config dialogs: renders {@code behind} as a modal backdrop so the dialog overlays
     *  the existing screen rather than cutting through to the blurred game. Unlike 1.21.11's 2D GUI,
     *  1.21.1 is z-layered, so the backdrop is pushed back in z (compounding with each nested dialog)
     *  to keep it — and its text/widgets — strictly behind the overlaying dialog and its dim. */
    public static void renderModalBackdrop(DrawContext context, @Nullable Screen behind, float delta) {
        if (behind == null) return;
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, -200);
        if (behind instanceof ChangeFireColorScreen cfc) {
            cfc.renderAsBackdrop(context, delta);
        } else {
            behind.render(context, -1, -1, delta);
        }
        context.getMatrices().pop();
    }

    /** Red notification badge on the Inbox button showing how many items are waiting (capped "9+"). */
    private void drawInboxBadge(DrawContext context) {
        int count = OnlinePresetsClient.inboxCount();
        if (count <= 0 || inboxButton == null) return;
        int cx = inboxButton.getX() + inboxButton.getWidth() - 5;
        int cy = inboxButton.getY() + 3;
        drawDisc(context, cx, cy, 5.5, 0xFF101010); // dark outline for contrast
        drawDisc(context, cx, cy, 4.5, 0xFFCC2222);
        String label = count > 9 ? "9+" : Integer.toString(count);
        context.drawText(this.textRenderer, label, cx - this.textRenderer.getWidth(label) / 2, cy - 3, 0xFFFFFFFF, false);
    }

    /** Filled circle of radius {@code r} centred at (cx,cy). */
    private void drawDisc(DrawContext context, int cx, int cy, double r, int color) {
        int rr = (int) Math.ceil(r);
        for (int gy = -rr; gy <= rr; gy++) {
            for (int gx = -rr; gx <= rr; gx++) {
                if (gx * gx + gy * gy <= r * r) {
                    context.fill(cx + gx, cy + gy, cx + gx + 1, cy + gy + 1, color);
                }
            }
        }
    }

    @Override
    public void removed() {
        Main.inConfig = false;
        super.removed();
    }

    @Override
    public void close() {
        onClose();
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        Main.setScale(width, height, client);

        super.resize(client, client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight());
    }
    private int currentSearchButton = 0;

    public void changeSearchOption(int buttonNum) {
        for (int i = 0; i < 3; i++) {
            if (i != Main.CONFIG_MANAGER.getPriorityOrder().indexOf(buttonNum)) {
                searchOptions[i].active = true;
            } else {
                searchOptions[i].active = false;
            }
        }
        currentSearchButton = buttonNum;
        searchScreenListWidget.test(false);
        searchScreenListWidget.setSelected(searchScreenListWidget.children().get(0));
    }
    private boolean buffer = false;
    private void toggle(boolean start) {
        isOverlay = start ? false : !isOverlay;
        int RGB = pickedColor[isOverlay ? 1:0].getRGB();
        textFieldWidget.setText("#"+Integer.toHexString(RGB).substring(2));
        updateCursor("#"+Integer.toHexString(RGB).substring(2));
        overlayToggles[isOverlay?1:0].active = false;
        overlayToggles[!isOverlay?1:0].active = true;
    }
    private void save() {
        // Apply is a breaking change: snapshot config before, push history after.
        historyBefore();
        int histTab = currentSearchButton;
        String histTarget = onBaseColor ? baseEntryName() : searchScreenListWidget.selectedName();
        boolean histOverlay = isOverlay;
        int num = 0;

        if (onBaseColor && !isOnAdd) {
            System.arraycopy(new int[]{pickedColor[0].getRGB(), pickedColor[1].getRGB()}, 0, Main.CONFIG_MANAGER.getCurrentBlockFireColors().getRight(), 0, 2);
            baseColor = new Color[]{new Color(Main.CONFIG_MANAGER.getCurrentBlockFireColors().getRight()[0]), new Color(Main.CONFIG_MANAGER.getCurrentBlockFireColors().getRight()[1])};
        } else {
            if (currentSearchButton == 0) {
                allBlockUnders.forEach(block -> {
                    Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft().get(0).put(Registries.BLOCK.getId(block).toString(), new int[]{pickedColor[0].getRGB(), pickedColor[1].getRGB()});
                });
                num = allBlockUnders.size();
            } else if (currentSearchButton == 1) {
                blockTags.forEach(tag -> {
                    Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft().get(1).put(tag.id().toString(), new int[]{pickedColor[0].getRGB(), pickedColor[1].getRGB()});
                });

                num = blockTags.size();
            } else if (currentSearchButton == 2) {
                biomeKeys.forEach(key -> {
                    Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft().get(2).put(key.getValue().toString(), new int[]{pickedColor[0].getRGB(), pickedColor[1].getRGB()});
                });

                num = biomeKeys.size();
            }
        }
        this.searchScreenListWidget.num = num;
        this.searchScreenListWidget.test();
        this.saveButton.active = false;
        this.saveButton.setFocused(false);

        int[] list = Main.CONFIG_MANAGER.getCurrentBlockFireColors().getRight();
        System.arraycopy(list, 0, Main.CONFIG_MANAGER.getFireColorPresets().get(presetListWidget.curPresetID).getLeft().getRight(), 0, list.length);
        Collections.copy(Main.CONFIG_MANAGER.getFireColorPresets().get(presetListWidget.curPresetID).getLeft().getLeft(), Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft());
        Collections.copy(Main.CONFIG_MANAGER.getFireColorPresets().get(presetListWidget.curPresetID).getRight(), Main.CONFIG_MANAGER.getPriorityOrder());

        Main.CONFIG_MANAGER.save();
        historyAfter(histTab, histTarget, histOverlay);
    }
    public void updateBlockUnder(String blockUnderTag) {
        blockUnder = (currentSearchButton == 0 || currentSearchButton == 1) && !onBaseColor ?  allBlockUnders.get(0) : Blocks.NETHERRACK;
        String string = Registries.BLOCK.getId(blockUnder).toString();
        buffer = false;
        if (onBaseColor || Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft().get(currentSearchButton).containsKey(blockUnderTag)) {


            int[] colorInts = onBaseColor ? Main.CONFIG_MANAGER.getCurrentBlockFireColors().getRight() : Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft().get(currentSearchButton).get(blockUnderTag);
            int RGB = colorInts[isOverlay ? 1:0];
            colorRedo = false;
            setPickedColors(new Color[]{new Color(colorInts[0]), new Color(colorInts[1])});
            textFieldWidget.setText("#"+Integer.toHexString(RGB).substring(2));
            updateCursor("#"+Integer.toHexString(RGB).substring(2));
        } else {
            int RGB = baseColor[isOverlay ? 1:0].getRGB();
            colorRedo = false;
            setPickedColors(new Color[]{baseColor[0], baseColor[1]});
            textFieldWidget.setText("#"+Integer.toHexString(RGB).substring(2));
            updateCursor("#"+Integer.toHexString(RGB).substring(2));
        }
    }
    public void updateCursor(String hexCode) {
        if (!clicked && !sliderClicked) {
            Pattern pattern = Pattern.compile("^#([A-Fa-f0-9]{6})$");
            if (pattern.matcher(hexCode).matches()) {
                Color acc = new Color(Color.decode(hexCode).getRGB());
                float[] HSB = Color.RGBtoHSB(acc.getRed(), acc.getGreen(), acc.getBlue(), null);
                hue = HSB[0];
                saturation = HSB[1];
                lightness = HSB[2];
                int RGB = Color.HSBtoRGB((float) hue, (float) saturation, (float) ((float) lightness == 0 ? lightness+0.01 : lightness));

                pickedColor[isOverlay ? 1:0] = new Color(RGB, true);
                double theta = Math.toRadians(90+HSB[0]*360);
                double radius = HSB[1] * wheelRadius;
                int x = (int) (wheelRadius+wheelCoords[0] + radius * Math.cos(theta));
                int y = (int) (wheelRadius+wheelCoords[0] + radius * Math.sin(theta));

                sliderClickedY = ((1 - HSB[2]) * (sliderDimensions[1] - sliderPadding*2)) + sliderCoords[1] + sliderPadding;
                clickedX = x;
                clickedY = y;
                if (!isCycling) cyclicalPresets.setIndex(0);

                if (onBaseColor) {
                    int[] colorInts = Main.CONFIG_MANAGER.getCurrentBlockFireColors().getRight();
                    saveButton.active = !(colorInts[0] == pickedColor[0].getRGB() && colorInts[1] == pickedColor[1].getRGB());
                } else {
                    String string = currentSearchButton == 0 ? Registries.BLOCK.getId(blockUnder).toString() : currentSearchButton == 1 ? blockTags.get(0).id().toString() : biomeKeys.get(0).getValue().toString();
                    if (Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft().get(currentSearchButton).containsKey(string)) {
                        int[] colorInts = Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft().get(currentSearchButton).get(string);
                        saveButton.active = !(colorInts[0] == pickedColor[0].getRGB() && colorInts[1] == pickedColor[1].getRGB());
                    } else {

                        saveButton.active = true;
                    }
                }

            }
        }
    }
    private void updateColorPicker(double mouseX, double mouseY, boolean click) {
        double dx = wheelRadius+wheelCoords[0] - mouseX;
        double dy = wheelRadius+wheelCoords[0] - mouseY;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist <= wheelRadius) {
            clicked = true;
            clickedX = mouseX;
            clickedY = mouseY;
        } else if (!click) {
            clickedX = wheelRadius+wheelCoords[0] + wheelRadius * -Math.cos(Math.atan2(dy, dx));
            clickedY = wheelRadius+wheelCoords[0] + wheelRadius * -Math.sin(Math.atan2(dy, dx));
        }
        if (clicked) {
            dx = wheelRadius+wheelCoords[0] - clickedX;
            dy = wheelRadius+wheelCoords[0] - clickedY;
            saturation = Math.sqrt(dx * dx + dy * dy) / wheelRadius;
            hue = (Math.atan2(dy, dx) / (2 * Math.PI) + 0.25);

            int RGB = Color.HSBtoRGB((float) hue, (float) saturation, (float) ((float) lightness == 0 ? lightness+0.01 : lightness));
            if (!isCycling) cyclicalPresets.setIndex(0);
            textFieldWidget.setText("#"+Integer.toHexString(RGB).substring(2));
            if (click) {
                buffer = false;
                colorRedo = true;
                setPickedColor(new Color(RGB, true), isOverlay ? 1:0);
            } else {
                pickedColor[isOverlay ? 1:0] = new Color(RGB, true);
            }
            if (pickedColor[0].getRGB() == baseColor[0].getRGB() && pickedColor[1].getRGB() == baseColor[1].getRGB()) {
                saveButton.active = false;
            } else {
                saveButton.active = true;
            }
        }
    }
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        addDragging = false;
        dragAdded.clear();
        clicked = false;
        sliderClicked = false;
        // End of a colour-wheel/slider gesture: record one undo step if the colour actually changed.
        if (gestureStartColor != null) {
            boolean changed = gestureStartColor[0].getRGB() != pickedColor[0].getRGB()
                    || gestureStartColor[1].getRGB() != pickedColor[1].getRGB();
            if (changed) {
                pushHistory(HistoryEntry.color(currentSearchButton, searchScreenListWidget.selectedName(),
                        isOverlay, gestureStartColor, new Color[]{pickedColor[0], pickedColor[1]}));
            }
            gestureStartColor = null;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
    private boolean isClick = false;

    private boolean isOnAdd = false;

    // Drag-to-add across the search list's left + boxes, so a run of entries can be selected by
    // dragging over their + icons instead of clicking each one. Tracked at the screen level because
    // the screen always receives mouseDragged (the entries' mouseClicked returns false, so the list
    // widget never becomes the drag target).
    private boolean addDragging = false;
    private final java.util.Set<String> dragAdded = new java.util.HashSet<>();

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (confirmActive) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                closeConfirm();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                Runnable a = confirmOnYes;
                closeConfirm();
                if (a != null) a.run();
                return true;
            }
            return true; // modal: swallow other keys
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (confirmActive) {
            if (button == 0) {
                if (inRect(confirmYesRect(), mouseX, mouseY)) {
                    Runnable a = confirmOnYes;
                    closeConfirm();
                    if (a != null) a.run();
                } else if (inRect(confirmNoRect(), mouseX, mouseY)) {
                    closeConfirm();
                }
            }
            return true; // modal: swallow clicks to the screen behind
        }
        this.setFocused(null); // clear previous focus/outline; a genuinely-clicked widget re-acquires it via super
        // Capture the colour at the start of a potential wheel/slider gesture (before it changes).
        Color[] before = new Color[]{pickedColor[0], pickedColor[1]};
        double selectSpace = (double) cursorDimensions / 2;
        if (mouseX >= sliderCoords[0] && mouseX <= sliderCoords[0] + sliderDimensions[0] && mouseY >= sliderCoords[1] + sliderPadding && mouseY <= sliderCoords[1] + sliderDimensions[1] - sliderPadding) {
            if (!isCycling) cyclicalPresets.setIndex(0);
            sliderClicked = true;
            isClick = true;
            mouseDragged(mouseX, mouseY, button, 0, 0);
        } else if (mouseX >= clickedX - selectSpace && mouseY >= clickedY - selectSpace && mouseX <= clickedX + selectSpace && mouseY <= clickedY + selectSpace) {
            clicked = true;
        } else {
            updateColorPicker(mouseX, mouseY, true);
        }
        if ((clicked || sliderClicked) && gestureStartColor == null) {
            gestureStartColor = before;
        }
        boolean result = super.mouseClicked(mouseX, mouseY, button);
        // Begin a drag-to-add gesture if the press landed on a search entry's left + box. The press
        // itself already handled the first entry, so seed the de-dup set with it.
        SearchScreenListWidget.BlockEntry addBoxEntry = searchScreenListWidget == null ? null : searchScreenListWidget.entryAtAddBox(mouseX, mouseY);
        if (addBoxEntry != null) {
            addDragging = true;
            dragAdded.clear();
            dragAdded.add(addBoxEntry.languageDefinition);
        }
        return result;
    }
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (addDragging) {
            SearchScreenListWidget.BlockEntry e = searchScreenListWidget.entryAtAddBox(mouseX, mouseY);
            if (e != null && dragAdded.add(e.languageDefinition)) {
                e.addViaDrag();
            }
            return true;
        }
        if (clicked) {
            updateColorPicker(mouseX, mouseY, false);
        }
        if (sliderClicked) {
            if (mouseY < sliderCoords[1] + sliderPadding) {
                sliderClickedY = sliderCoords[1] + sliderPadding;
            } else if (mouseY > sliderCoords[1]+sliderDimensions[1] - sliderPadding) {
                sliderClickedY = sliderCoords[1]+sliderDimensions[1] - sliderPadding;
            } else {
                sliderClickedY = mouseY;
            }
            lightness = 1 - (sliderClickedY - sliderCoords[1] - sliderPadding) / (sliderDimensions[1]-sliderPadding*2);
            float[] HSB = Color.RGBtoHSB(pickedColor[isOverlay ? 1:0].getRed(), pickedColor[isOverlay ? 1:0].getGreen(), pickedColor[isOverlay ? 1:0].getBlue(), null);

            int RGB = Color.HSBtoRGB((float) hue, (float) saturation, (float) ((float) lightness == 0 ? lightness+0.01 : lightness));

            textFieldWidget.setText("#"+Integer.toHexString(RGB).substring(2));
            if (isClick) {
                colorRedo = true;
                setPickedColor(new Color(RGB, true), isOverlay ? 1:0);
                isClick = false;
            } else {
                pickedColor[isOverlay ? 1:0] = new Color(RGB, true);

            }
            if (pickedColor[0].getRGB() == baseColor[0].getRGB() && pickedColor[1].getRGB() == baseColor[1].getRGB()) {
                saveButton.active = false;
            } else {
                saveButton.active = true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }
    private int counter = 0;
    private float dist = 0f;
    private boolean forwards = true;

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderPanoramaBackground(context, delta);

        this.applyBlur(delta);
        this.renderDarkening(context);
    }

    @Override
    @SuppressWarnings("deprecation") // SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE is deprecated but still the supported atlas id in 1.21
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        DonationTracker.onConfigFrame();
        context.getMatrices().push();

        super.render(context, mouseX, mouseY, delta);

        // Reorder + reset profile button icons, centred in their 20×20 buttons.
        drawReorderIcon(context, profileButtonXs[0], profileButtonY);
        drawResetIcon(context, profileButtonXs[1], profileButtonY);

        // Section title above the search list saying what the selected profile's single category
        // recolours (replaces the old block/tag/biome tabs). Styled as a divider-rule heading — a
        // centred caption flanked by thin lines — with the category term bolded in its type accent
        // colour (block green / tag blue / biome orange).
        if (hasProfile()) {
            int hx = blockSearchCoords[0];
            int hy = blockSearchCoords[1];
            int hw = blockSearchDimensions[0];
            int accent = TYPE_HEADER_ACCENTS[currentSearchButton];
            context.fill(hx, hy + 20, hx + hw, hy + 21, 0xFF3A3A3A);
            Text searchHeader = Text.translatable(
                    TYPE_HEADER_KEYS[currentSearchButton],
                    Text.translatable(TYPE_HEADER_TERM_KEYS[currentSearchButton])
                            .setStyle(net.minecraft.text.Style.EMPTY.withBold(true).withColor(net.minecraft.text.TextColor.fromRgb(accent))));
            int cx = hx + hw / 2;
            int midY = hy + 10;
            int tw = textRenderer.getWidth(searchHeader);
            int ruleColor = (accent & 0x00FFFFFF) | 0x55000000;
            int leftTextEdge = cx - tw / 2 - 6;
            int rightTextEdge = cx + tw / 2 + 6;
            if (leftTextEdge - hx >= 12) {
                context.fill(hx, midY, leftTextEdge, midY + 1, ruleColor);
                context.fill(rightTextEdge, midY, hx + hw, midY + 1, ruleColor);
            }
            context.drawCenteredTextWithShadow(textRenderer, searchHeader, cx, hy + 5, 0xFFD8D8D8);

            // Magnifying-glass icon pinned to the right of the search field so it reads as a search input.
            if (blockUnderField != null) {
                int iconX = blockUnderField.getX() + blockUnderField.getWidth() - 6 - 5;
                int iconY = blockUnderField.getY() + (blockUnderField.getHeight() - 7) / 2;
                drawSearchIcon(context, iconX, iconY, 0xFFAAAAAA);
            }
        }

        // Live colour wheel + 3D block/fire previews. Suppressed when this screen is drawn as a modal
        // backdrop so they don't paint over (or depth-fight with) the dialog overlaying it.
        if (!renderingAsBackdrop) {
        context.getMatrices().push();


        RenderSystem.setShader(GameRendererSetting::getRenderTypeColorWheel);
        RenderSystem.depthFunc(519);
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();

        Matrix4f matrix4f = context.getMatrices().peek().getPositionMatrix();

        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        bufferBuilder.vertex(matrix4f, wheelCoords[0], wheelCoords[1], 0f).color(1f, 1f, 1f, 1f).texture(0f, 1f);
        bufferBuilder.vertex(matrix4f, wheelCoords[0], (wheelCoords[1] + wheelRadius * 2), 0f).color(1f, 1f, 1f, 1f).texture(0f, 0f);
        bufferBuilder.vertex(matrix4f, (wheelCoords[0] + wheelRadius * 2), (wheelCoords[1] + wheelRadius * 2), 0f).color(1f, 1f, 1f, 1f).texture(1f, 0f);
        bufferBuilder.vertex(matrix4f, (wheelCoords[0] + wheelRadius * 2), wheelCoords[1], 0f).color(1f, 1f, 1f, 1f).texture(1f, 1f);
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());

        RenderSystem.setShader(GameRenderer::getPositionTexProgram);

        context.drawBorder((int) clickedX - cursorDimensions/4, (int) clickedY - cursorDimensions/4, cursorDimensions/4*3, cursorDimensions/4*3, Color.gray.getRGB());
        context.drawBorder((int) clickedX - cursorDimensions/2, (int)  clickedY - cursorDimensions/2, cursorDimensions, cursorDimensions, Color.gray.getRGB());
        context.fill((int) clickedX - cursorDimensions/4, (int) clickedY - cursorDimensions/4, (int) clickedX + cursorDimensions/4, (int) clickedY + cursorDimensions/4, Color.BLACK.getRGB());

        context.fill(sliderCoords[0], sliderCoords[1], sliderCoords[0]+sliderDimensions[0], sliderCoords[1]+sliderDimensions[1]/2, Color.HSBtoRGB((float) hue, (float) saturation, 1.0f));
        context.fill(sliderCoords[0], sliderCoords[1]+sliderDimensions[1]/2, sliderCoords[0]+sliderDimensions[0], sliderCoords[1]+sliderDimensions[1], Color.BLACK.getRGB());
        context.fillGradient(sliderCoords[0], sliderCoords[1]+11, sliderCoords[0]+sliderDimensions[0], sliderCoords[1]+sliderDimensions[1]-11, Color.HSBtoRGB((float) hue, (float) saturation, 1.0f), Color.BLACK.getRGB());

        context.drawBorder((int) sliderClickedX - cursorDimensions/4, (int) sliderClickedY - cursorDimensions/4, cursorDimensions/4*3, cursorDimensions/4*3, Color.gray.getRGB());
        context.drawBorder((int) sliderClickedX - cursorDimensions/2, (int)  sliderClickedY - cursorDimensions/2, cursorDimensions, cursorDimensions, 0x7f222222);
        context.fill((int) sliderClickedX - cursorDimensions/4, (int) sliderClickedY - cursorDimensions/4, (int) sliderClickedX + cursorDimensions/4, (int) sliderClickedY + cursorDimensions/4, Color.BLACK.getRGB());



        RenderSystem.depthMask(true);
        BlockRenderManager brm = MinecraftClient.getInstance().getBlockRenderManager();
        context.getMatrices().push();

        context.enableScissor(blockSearchCoords[0], blockSearchDimensions[1]+40+10, width-20, height-10);

        context.getMatrices().translate(width - 300 - 20, 20 + blockSearchDimensions[1] + 20 + 10, 1);

        Quaternionf q = new Quaternionf();
        q.rotateZ((float) Math.toRadians(180));
        q.rotateX((float) Math.toRadians(45));
        q.rotateY((float) Math.toRadians(45));

        int scale = 15;

        // Preview grid: 11 columns, ~7 rows visible before it scrolls (tighter row spacing than before,
        // matching the newer versions). The threshold/range track the 7-row visible window.
        if (Math.ceil(allBlockUnders.size()/11f) > 7) {
            double amount = .5 * ((Math.ceil(allBlockUnders.size()/11f)-7)/2);
            dist = (float) (dist + (forwards ? amount : -amount));
            if (dist > (10 * (Math.ceil(allBlockUnders.size()/11f)-4))) {
                counter++;
                forwards = false;
            } else if (dist < 1) {
                counter++;
                forwards = true;
            }
        } else {
            dist = 0;
        }

        context.getMatrices().translate(0, -dist, 0);

        for (int i = 0; i < allBlockUnders.size(); i++) {
            Block block = allBlockUnders.get(i);
            VertexConsumer c = context.getVertexConsumers().getBuffer(RenderLayers.getBlockLayer(block.getDefaultState()));

            context.getMatrices().push();

            context.getMatrices().translate((blockSearchDimensions[0]-21)/10f*(i%11), (height-blockSearchDimensions[1]-40-20)/7f*((double) (i / 11)), 0);

            context.getMatrices().multiply(q);
            context.getMatrices().scale(-1, 1, 1);
            context.getMatrices().scale(scale, scale, scale);
            context.getMatrices().translate(1, -0.36, 0);

            if (block instanceof BlockWithEntity) {
                BlockEntity blockEntity = ((BlockWithEntity) block).createBlockEntity(BlockPos.ORIGIN, block.getDefaultState());
                BlockEntityRenderer<BlockEntity> blockEntityRenderer = MinecraftClient.getInstance().getBlockEntityRenderDispatcher().get(blockEntity);

                boolean blockModel = blockEntityRenderer == null;
                if (!blockModel) blockModel = blockEntityRenderer.rendersOutsideBoundingBox(blockEntity);
                if (blockModel || block.getDefaultState().getRenderType() == BlockRenderType.MODEL) {
                    brm.getModelRenderer().render(context.getMatrices().peek(), c, block.getDefaultState(), brm.getModel(block.getDefaultState()), 0.0f, 0.0f, 0.0f, 15728880, OverlayTexture.DEFAULT_UV);
                } else {
                    assert blockEntity != null;
                    blockEntity.setWorld(MinecraftClient.getInstance().world);
                    MinecraftClient.getInstance().getBlockEntityRenderDispatcher().renderEntity(blockEntity, context.getMatrices(), context.getVertexConsumers(), 15728880, OverlayTexture.DEFAULT_UV);
                }
            } else {
                brm.getModelRenderer().render(context.getMatrices().peek(), c, block.getDefaultState(), brm.getModel(block.getDefaultState()), 0.0f, 0.0f, 0.0f, 15728880, OverlayTexture.DEFAULT_UV);
            }

            context.getMatrices().pop();
        }


        context.getMatrices().pop();

        context.getVertexConsumers().draw();

        context.disableScissor();

        context.getMatrices().push();

        context.getMatrices().translate(0, 0, -375);

        context.enableScissor(0, 0, blockSearchCoords[0], height);


        context.getMatrices().translate(width/3f + 10, height - 15, 10);

        context.getMatrices().multiply(q);
        context.getMatrices().scale(-1, 1, 1);
        context.getMatrices().scale(190, 190, 190);
        float left = 1.75f;
        context.getMatrices().translate(1, 2.142, 0);

        BlockRenderManager blockRenderManager = MinecraftClient.getInstance().getBlockRenderManager();
        VertexConsumer consumer = context.getVertexConsumers().getBuffer(RenderLayers.getBlockLayer(blockUnder.getDefaultState()));

        if (blockUnder instanceof BlockWithEntity) {
            BlockEntity blockEntity = ((BlockWithEntity) blockUnder).createBlockEntity(BlockPos.ORIGIN, blockUnder.getDefaultState());
            BlockEntityRenderer<BlockEntity> blockEntityRenderer = MinecraftClient.getInstance().getBlockEntityRenderDispatcher().get(blockEntity);

            boolean blockModel = blockEntityRenderer == null;
            if (!blockModel) blockModel = blockEntityRenderer.rendersOutsideBoundingBox(blockEntity);
            if (blockModel || blockUnder.getDefaultState().getRenderType() == BlockRenderType.MODEL) {
                blockRenderManager.getModelRenderer().render(context.getMatrices().peek(), consumer, blockUnder.getDefaultState(), blockRenderManager.getModel(blockUnder.getDefaultState()), 1f, 1f, 1f, 15728880, OverlayTexture.DEFAULT_UV);
            } else {
                assert blockEntity != null;
                blockEntity.setWorld(MinecraftClient.getInstance().world);
                MinecraftClient.getInstance().getBlockEntityRenderDispatcher().renderEntity(blockEntity, context.getMatrices(), context.getVertexConsumers(), 15728880, OverlayTexture.DEFAULT_UV);
            }
        } else {
            blockRenderManager.getModelRenderer().render(context.getMatrices().peek(), consumer, blockUnder.getDefaultState(), blockRenderManager.getModel(blockUnder.getDefaultState()), 1f, 1f, 1f, 15728880, OverlayTexture.DEFAULT_UV);
        }

        context.getMatrices().scale(-1, 1, 1);

        context.getMatrices().translate(-1, 1, 0);
        Block block = Blocks.FIRE;
        Block block2 = Blocks.SOUL_FIRE;
        consumer = context.getVertexConsumers().getBuffer(CustomRenderLayer.getCustomTint());

        blockRenderManager.getModelRenderer().render(context.getMatrices().peek(), consumer, block.getDefaultState(), blockRenderManager.getModel(block.getDefaultState()), pickedColor[0].getRed()/255f, pickedColor[0].getGreen()/255f, pickedColor[0].getBlue()/255f, 1, 1);
        blockRenderManager.getModelRenderer().render(context.getMatrices().peek(), consumer, block2.getDefaultState(), blockRenderManager.getModel(block2.getDefaultState()), pickedColor[1].getRed()/255f, pickedColor[1].getGreen()/255f, pickedColor[1].getBlue()/255f, 1, 1);

        context.getVertexConsumers().draw();

        context.disableScissor();

        context.getMatrices().pop();

        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.depthFunc(515);

        context.getMatrices().pop();
        }

        if (globeTooltip != null) {
            context.drawTooltip(this.textRenderer, globeTooltip, mouseX, mouseY);
            globeTooltip = null;
        }
        context.getMatrices().pop();

        drawInboxBadge(context);
        if (confirmActive && !renderingAsBackdrop) renderConfirm(context, mouseX, mouseY);
    }

    @Environment(value= EnvType.CLIENT)
    class SearchScreenListWidget
            extends AlwaysSelectedEntryListWidget<ChangeFireColorScreen.SearchScreenListWidget.BlockEntry> {
        private void generateEntries() {
            List<ChangeFireColorScreen.SearchScreenListWidget.BlockEntry> first = new ArrayList<>();
            List<ChangeFireColorScreen.SearchScreenListWidget.BlockEntry> second = new ArrayList<>();

            BlockEntry base = new BlockEntry(Text.translatable("firorize.config.baseFire").getString());
            base.isCustomized = true;
            first.add(base);

            if (currentSearchButton == 0) {
                blockUnderList.forEach((block) -> {
                    String string = Registries.BLOCK.getId(block).toString();
                    if (string.contains(input)) {
                        ChangeFireColorScreen.SearchScreenListWidget.BlockEntry blockEntry = new ChangeFireColorScreen.SearchScreenListWidget.BlockEntry(string);
                        if(!Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft().get(0).containsKey(Registries.BLOCK.getId(block).toString())) {
                            second.add(blockEntry);
                        }
                    }
                });
                Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft().get(0).keyList().forEach(string -> {
                    if (blockUnderList.stream().map(block -> Registries.BLOCK.getId(block).toString()).toList().contains(string)) {
                        ChangeFireColorScreen.SearchScreenListWidget.BlockEntry blockEntry = new ChangeFireColorScreen.SearchScreenListWidget.BlockEntry(string);
                        first.add(blockEntry);
                        blockEntry.isCustomized = true;
                    }
                });
                if (num > 0) {
                    selected.clear();
                    for (int i = first.size()-1; i >= 0 && num > 0; i--, num--) {
                        selected.add(i);
                    }
                }
                Stream.concat(first.stream(), second.stream()).toList().forEach(this::addEntry);
            } else if (currentSearchButton == 1) {
                Main.blockTagList.forEach((key) -> {
                    String string = key.id().toString();
                    if (string.contains(input)) {
                        ChangeFireColorScreen.SearchScreenListWidget.BlockEntry blockEntry = new ChangeFireColorScreen.SearchScreenListWidget.BlockEntry(string);
                        if(!Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft().get(1).containsKey(string)) {
                            second.add(blockEntry);
                        }
                    }
                });
                Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft().get(1).keyList().forEach(string -> {
                    // Cross-version profiles may carry tags that don't exist in this version; skip
                    // them instead of resolving (findFirst().get() would throw) so they're ignored.
                    if (Main.blockTagList.stream().anyMatch(tagg -> tagg.id().toString().equals(string))) {
                        ChangeFireColorScreen.SearchScreenListWidget.BlockEntry blockEntry = new ChangeFireColorScreen.SearchScreenListWidget.BlockEntry(string);
                        first.add(blockEntry);
                        blockEntry.isCustomized = true;
                    }
                });
                if (num > 0) {
                    selected.clear();
                    for (int i = first.size()-1; i >= 0 && num > 0; i--, num--) {
                        selected.add(i);
                    }
                }
                Stream.concat(first.stream(), second.stream()).toList().forEach(this::addEntry);
            } else {
                Main.biomeKeyList.forEach((key) -> {
                    String string = key.getValue().toString();
                    if (string.contains(input)) {
                        ChangeFireColorScreen.SearchScreenListWidget.BlockEntry blockEntry = new ChangeFireColorScreen.SearchScreenListWidget.BlockEntry(string);
                        if(!Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft().get(2).containsKey(string)) {
                            second.add(blockEntry);
                        }
                    }
                });
                Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft().get(2).keyList().forEach(string -> {
                    if (Main.biomeKeyList.contains(RegistryKey.of(RegistryKeys.BIOME, Identifier.tryParse(string)))) {
                        ChangeFireColorScreen.SearchScreenListWidget.BlockEntry blockEntry = new ChangeFireColorScreen.SearchScreenListWidget.BlockEntry(string);
                        first.add(blockEntry);
                        blockEntry.isCustomized = true;
                    }
                });
                if (num > 0) {
                    selected.clear();
                    for (int i = first.size()-1; i >= 0 && num > 0; i--, num--) {
                        selected.add(i);
                    }
                }
                Stream.concat(first.stream(), second.stream()).toList().forEach(this::addEntry);
            }

            if (this.getSelectedOrNull() != null) {
                this.centerScrollOn(this.getSelectedOrNull());
            }
        }
        public SearchScreenListWidget(MinecraftClient client, int width, int height, int x, int y) {
            super(client, width, height, x, y);
            generateEntries();
        }

        @Override
        public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            super.renderWidget(context, mouseX, mouseY, delta);
            // Tag and biome lists are populated from the server/datapack registries, which only exist
            // once a world is loaded. In the main menu they're null/empty, so the list would otherwise
            // be blank with no explanation — spell out why instead.
            boolean needsWorld = (currentSearchButton == 1 && (Main.blockTagList == null || Main.blockTagList.isEmpty()))
                    || (currentSearchButton == 2 && (Main.biomeKeyList == null || Main.biomeKeyList.isEmpty()));
            if (needsWorld) {
                Text msg = Text.translatable(currentSearchButton == 1
                        ? "firorize.config.status.noTagsNoWorld" : "firorize.config.status.noBiomesNoWorld");
                int cx = getX() + getWidth() / 2;
                int cy = getY() + getHeight() / 2 - ChangeFireColorScreen.this.textRenderer.fontHeight;
                for (net.minecraft.text.OrderedText line : ChangeFireColorScreen.this.textRenderer.wrapLines(msg, getWidth() - 24)) {
                    context.drawCenteredTextWithShadow(ChangeFireColorScreen.this.textRenderer, line, cx, cy, 0xFF9A9A9A);
                    cy += ChangeFireColorScreen.this.textRenderer.fontHeight + 1;
                }
            }
        }
        public int num = 0;
        public void test() {
            test(true);
        }
        public void test(boolean keepScroll) {
            double scroll = getScrollAmount();
            this.clearEntries();
            generateEntries();
            // setScrollAmount clamps to [0, getMaxScroll()], so restoring the prior
            // amount keeps the user's place and snaps to the end if the list shrank.
            setScrollAmount(keepScroll ? scroll : 0.0);
            num = 0;
        }
        @Override
        public int getRowWidth() {
            return this.getWidth();
        }

        public List<Integer> selected = new ArrayList<>();

        /** languageDefinition of the currently selected entry, or null if nothing is selected. */
        public String selectedName() {
            if (selected.isEmpty()) return null;
            int i = selected.get(0);
            if (i < 0 || i >= children().size()) return null;
            return children().get(i).languageDefinition;
        }

        /** Selects the entry with the given languageDefinition (falls back to index 0). */
        public void selectByName(String name) {
            if (name != null) {
                for (BlockEntry e : children()) {
                    if (e.languageDefinition.equals(name)) {
                        setSelected(e);
                        centerScrollOn(e);
                        return;
                    }
                }
            }
            if (!children().isEmpty()) setSelected(children().get(0));
        }

        @Override
        protected boolean isSelectedEntry(int index) {
            if (selected.contains(index)) {
                this.getEntry(index).isSelected = true;
                return true;
            }
            return super.isSelectedEntry(index);
        }

        @Override
        protected void drawSelectionHighlight(DrawContext context, int y, int entryWidth, int entryHeight, int borderColor, int fillColor) {
            // No-op: selection is drawn per-entry in renderEntry via drawSelectionBorder, which merges
            // the borders of adjacent selected entries. The vanilla per-entry highlight would re-draw a
            // full divider between them, so it's suppressed here.
        }

        /**
         * Draws the selection outline for one selected entry. When the entry above/below is also
         * selected the border between them is filled in (no separating line) so a run of selected
         * entries reads as a single block; an isolated selected entry gets a full 1px border.
         */
        private void drawSelectionBorder(DrawContext context, int entryWidth, int y, int entryHeight, boolean prevSelected, boolean nextSelected) {
            int color = this.isFocused() ? -1 : -8355712;
            int left = this.getX() + (this.width - entryWidth) / 2;
            int right = getScrollbarX() - 1; // keep the right edge clear of the scrollbar
            // Extend the coloured edge and black interior through the gap to a selected neighbour.
            // The entries sit 4px apart (itemHeight 15 − entryHeight 11), so a selected entry must
            // reach +1 further into the gap than the 1px-spacing reference to close the vertical seam.
            int outerBottom = nextSelected ? y + entryHeight + 3 : y + entryHeight + 1;
            int innerTop = prevSelected ? y - 4 : y - 1;
            int innerBottom = nextSelected ? y + entryHeight + 4 : y + entryHeight;
            context.fill(left, y - 1, right, outerBottom, color);
            context.fill(left + 1, innerTop + 1, right - 1, innerBottom, 0xFF000000);
        }

        @Override
        protected int getScrollbarX() {
            return super.getScrollbarX() - 16;
        }
        @Override
        public int getX() {
            return super.getX() + blockSearchCoords[0];
        }
        @Override
        public void setSelected(@Nullable ChangeFireColorScreen.SearchScreenListWidget.BlockEntry entry) {
            if (entry.realSelect) {
                lastPickedColor = pickedColor.clone();
            } else {
                entry.realSelect = true;
            }

            lastSelected = selected.stream().map(SerializationUtils::clone).collect(Collectors.toList());

            selected.forEach(index -> this.getEntry(index).isSelected = false);

            selected.clear();
            selected.add(this.children().indexOf(entry));

            cyclicalPresets.setIndex(0);

            if (children().indexOf(entry) == 0) {
                onBaseColor = true;
                allBlockUnders.clear();
                allBlockUnders.add(Blocks.NETHERRACK);
                blockUnderField.setText(entry.languageDefinition);
                updateBlockUnder(entry.languageDefinition);
            } else {
                onBaseColor = false;
                if (currentSearchButton == 0) {
                    allBlockUnders = new ArrayList<>();
                    allBlockUnders.add(Registries.BLOCK.get(Identifier.tryParse(entry.languageDefinition)));
                    blockUnderField.setText(entry.languageDefinition);
                    updateBlockUnder(entry.languageDefinition);
                } else if (currentSearchButton == 1) {
                    TagKey<Block> tag = Main.blockTagList.stream().filter(tagg -> tagg.id().toString().equals(entry.languageDefinition)).findFirst().get();
                    blockTags = new ArrayList<>();
                    blockTags.add(tag);
                    allBlockUnders = Registries.BLOCK.getEntryList(tag).get().stream().map(entry2 -> entry2.value()).filter(block -> blockUnderList.contains(block)).collect(Collectors.toList());;
                    blockUnderField.setText(entry.languageDefinition);
                    updateBlockUnder(entry.languageDefinition);
                } else if (currentSearchButton == 2) {
                    RegistryKey<Biome> key = RegistryKey.of(RegistryKeys.BIOME, Identifier.tryParse(entry.languageDefinition));
                    biomeKeys = new ArrayList<>();
                    biomeKeys.add(key);
                    blockUnderField.setText(entry.languageDefinition);
                    updateBlockUnder(entry.languageDefinition);
                }
            }
            ChangeFireColorScreen.this.dist = 0;
            ChangeFireColorScreen.this.counter = 0;
        }

        public void moveEntryUp(BlockEntry entry) {
            historyBefore();
            int index = this.children().indexOf(entry);
            if (index > 0) {
                this.children().set(index, this.children().get(index-1));
                this.children().set(index-1, entry);
                selected.clear();
                selected.add(index-1);

                index--;

                ListOrderedMap<String, int[]> temp = Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft().get(currentSearchButton);
                Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft().get(currentSearchButton).put(index, temp.get(index-1), temp.getValue(index-1));
                Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft().get(currentSearchButton).put(index-1, temp.get(index), temp.getValue(index));
            }
            historyAfter(currentSearchButton, entry.languageDefinition, isOverlay);
        }

        public void moveEntryDown(BlockEntry entry) {
            historyBefore();
            int index = this.children().indexOf(entry);
            if (index < Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft().get(currentSearchButton).size()) {
                this.children().set(index, this.children().get(index+1));
                this.children().set(index+1, entry);
                selected.clear();
                selected.add(index+1);

                index--;

                ListOrderedMap<String, int[]> temp = Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft().get(currentSearchButton);
                Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft().get(currentSearchButton).put(index, temp.get(index+1), temp.getValue(index+1));
                Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft().get(currentSearchButton).put(index+1, temp.get(index), temp.getValue(index));
            }
            historyAfter(currentSearchButton, entry.languageDefinition, isOverlay);
        }

        @Override
        protected void renderEntry(DrawContext context, int mouseX, int mouseY, float delta, int index, int x, int y, int entryWidth, int entryHeight) {
            BlockEntry entry = this.getEntry(index);
            entry.x = x;
            entry.entryHeight = entryHeight;
            entry.y = y;
            // Draw the selection outline ourselves (before the entry content) so adjacent selected
            // entries merge into one block instead of each showing a full divider.
            if (selected.contains(index)) {
                drawSelectionBorder(context, entryWidth, y, entryHeight, selected.contains(index - 1), selected.contains(index + 1));
            }
            super.renderEntry(context, mouseX, mouseY, delta, index, x, y, entryWidth, entryHeight);
        }

        /** The entry whose left + (add) box contains (mx,my), or null. Used for click-drag multi-add.
         *  The hitbox is the full entry-height left square (the visual + glyph is drawn inset). */
        BlockEntry entryAtAddBox(double mx, double my) {
            BlockEntry e = getEntryAtPosition(mx, my);
            if (e == null) return null;
            // Hitbox is the full entry-height left square, extended 4px to the right (matching the
            // click/hover hitbox) so a drag over the + boxes doesn't fall into a mis-select edge.
            return (mx >= e.x && mx <= e.x + e.entryHeight + 4 && my >= e.y && my <= e.y + e.entryHeight) ? e : null;
        }

        @Environment(value=EnvType.CLIENT)
        public class BlockEntry
                extends AlwaysSelectedEntryListWidget.Entry<ChangeFireColorScreen.SearchScreenListWidget.BlockEntry> {
            private final String languageDefinition;
            private boolean realSelect = true;
            public BlockEntry(String languageDefinition) {
                this.languageDefinition = languageDefinition;
            }
            private boolean isCustomized = false;
            private float alpha;
            private int x;
            private boolean isSelected = false;
            private int y;
            private int entryHeight;

            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                context.drawCenteredTextWithShadow(ChangeFireColorScreen.this.textRenderer, Text.literal(languageDefinition), (entryWidth-6) / 2  + blockSearchCoords[0], y+1, 0xFFFFFF);
                boolean shiftPressed = InputUtil.isKeyPressed(client.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_SHIFT);
                if ((shiftPressed && index < Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft().get(currentSearchButton).size() - 1) || (!shiftPressed && index > 0)) {
                    // +4 so the add hitbox extends a little past the full-size + box: aiming at the +
                    // no longer mis-selects the row on the right edge.
                    if (mouseX >= x && mouseX <= x + entryHeight + 4 && mouseY >= y && mouseY <= y + entryHeight) {
                        alpha = 1f;
                    } else {
                        alpha = 0.5f;
                    }
                } else {
                    alpha = 0.5f;
                }
                int colorInt = new Color(1f/255*150, 1f/255*150, 1f/255*150, 1f).getRGB();

                if (!ChangeFireColorScreen.this.searchScreenListWidget.selected.contains(ChangeFireColorScreen.this.searchScreenListWidget.children().indexOf(this)) && !isCustomized) {
                    context.fill(x, y, x+entryHeight, y+entryHeight, new Color(1f/255*44, 1f/255*44, 1f/255*44, alpha).getRGB());
                    context.fill(x+entryHeight/2, y+3, x+entryHeight/2+1, y+entryHeight-3, colorInt);
                    context.fill(x+3, y+entryHeight/2, x+entryHeight-3, y+entryHeight/2+1, colorInt);
                    context.drawBorder(x, y, entryHeight, entryHeight, new Color(1f/255*99, 1f/255*99, 1f/255*99, 0.8f).getRGB());
                }
                if (isCustomized && currentSearchButton == 1  && children().indexOf(this) != 0) {
                    context.fill(x, y, x+entryHeight, y+entryHeight, new Color(1f/255*44, 1f/255*44, 1f/255*44, alpha).getRGB());
                    context.drawBorder(x, y, entryHeight, entryHeight, new Color(1f/255*99, 1f/255*99, 1f/255*99, 0.8f).getRGB());
                    if (shiftPressed) {
                        if (index < Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft().get(currentSearchButton).size()) {
                            context.fill(x+entryHeight/2, y+3, x+entryHeight/2+1, y+entryHeight-3, colorInt);
                            context.fill(x+entryHeight/2 - 1, y+entryHeight-4, x+entryHeight/2, y+entryHeight-5, colorInt);
                            context.fill(x+entryHeight/2 - 2, y+entryHeight-5, x+entryHeight/2 - 1, y+entryHeight-6, colorInt);
                            context.fill(x+entryHeight/2 + 1, y+entryHeight-4, x+entryHeight/2 + 2, y+entryHeight-5, colorInt);
                            context.fill(x+entryHeight/2 + 2, y+entryHeight-5, x+entryHeight/2 + 3, y+entryHeight-6, colorInt);
                        }
                    } else {
                        if (index > 1) {
                            context.fill(x + entryHeight / 2, y + 3, x + entryHeight / 2 + 1, y + entryHeight - 3, colorInt);
                            context.fill(x + entryHeight / 2 - 1, y + 4, x + entryHeight / 2, y + 5, colorInt);
                            context.fill(x + entryHeight / 2 - 2, y + 5, x + entryHeight / 2 - 1, y + 6, colorInt);
                            context.fill(x + entryHeight / 2 + 1, y + 4, x + entryHeight / 2 + 2, y + 5, colorInt);
                            context.fill(x + entryHeight / 2 + 2, y + 5, x + entryHeight / 2 + 3, y + 6, colorInt);
                        }
                    }
                }
                if (isCustomized) {
                    if (mouseX >= x+entryWidth-entryHeight-10 && mouseX <= x+entryWidth-10 && mouseY >= y && mouseY <= y+entryHeight && children().indexOf(this) != 0) {
                        context.fill(x+entryWidth-entryHeight-10, y, x+entryWidth-10, y + entryHeight, new Color(1f/255*44, 1f/255*44, 1f/255*44, alpha).getRGB());
                        drawX(context, entryWidth, entryHeight, y, x);
                    } else {
                        int[] test = this.languageDefinition.equals(Text.translatable("firorize.config.baseFire").getString()) ? Main.CONFIG_MANAGER.getCurrentBlockFireColors().getRight(): Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft().get(currentSearchButton).get(this.languageDefinition);
                        context.fill(x + entryWidth - entryHeight - 10, y, x + entryWidth - 10, y + entryHeight, test[0]);
                        context.fill(x + entryWidth - entryHeight - 7, y + 3, x + entryWidth - 13, y + entryHeight - 3, test[1]);
                    }
                    context.drawBorder(x+entryWidth-entryHeight-10, y, entryHeight, entryHeight, new Color(1f/255*99, 1f/255*99, 1f/255*99, 0.8f).getRGB());
                }

            }
            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (mouseX >= x+getWidth()-entryHeight-10 && mouseX <= x+getWidth()-10 && mouseY >= y && mouseY <= y+entryHeight && children().indexOf(this) != 0) {
                    if (isCustomized) {
                        // Deleting a saved block/tag/biome colour is undoable.
                        historyBefore();
                        Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft().get(currentSearchButton).remove(this.languageDefinition);
                        commitToPreset();
                        Main.CONFIG_MANAGER.save();
                        test();
                        historyAfter(currentSearchButton, this.languageDefinition, isOverlay);
                        return false;
                    }
                }
                if (mouseX >= x && mouseX <= x+entryHeight+4 && mouseY >= y && mouseY <= y+entryHeight) {
                    if (isCustomized && currentSearchButton == 1) {
                        int index = ChangeFireColorScreen.this.searchScreenListWidget.children().indexOf(this);
                        boolean shiftPressed = InputUtil.isKeyPressed(client.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_SHIFT);
                        if ((shiftPressed && index < Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft().get(currentSearchButton).size() + 1)) {
                            ChangeFireColorScreen.this.searchScreenListWidget.moveEntryDown(this);
                        } if ((!shiftPressed && index > 1)) {
                            ChangeFireColorScreen.this.searchScreenListWidget.moveEntryUp(this);
                        }
                    }
                    else if ((!isFocused() && !isSelected && !selected.isEmpty())) {
                        this.onAddButton();
                    } else {
                        ChangeFireColorScreen.this.searchScreenListWidget.setSelected(this);
                        return false;
                    }
                } else {
                    ChangeFireColorScreen.this.searchScreenListWidget.setSelected(this);
                    return false;
                }
                return false;
            }
            void onAddButton() {
                isOnAdd = true;
                boolean clear = onBaseColor || ChangeFireColorScreen.this.searchScreenListWidget.children().get(ChangeFireColorScreen.this.searchScreenListWidget.selected.get(0)).isCustomized;

                ChangeFireColorScreen.this.dist = 0;
                ChangeFireColorScreen.this.counter = 0;
                if (currentSearchButton == 0) {
                    if (clear) allBlockUnders.clear();
                    allBlockUnders.add(Registries.BLOCK.get(Identifier.tryParse(this.languageDefinition)));
                } else if (currentSearchButton == 1) {
                    if (clear) blockTags.clear();
                    TagKey<Block> tag = Main.blockTagList.stream().filter(tagg -> tagg.id().toString().equals(this.languageDefinition)).findFirst().get();

                    blockTags.add(tag);
                    List<Block> newBlocks = Registries.BLOCK.getEntryList(tag).get().stream()
                            .map(entry2 -> entry2.value())
                            .filter(block -> blockUnderList.contains(block) && !allBlockUnders.contains(block))
                            .toList();

                    allBlockUnders = Stream.concat(allBlockUnders.stream(), newBlocks.stream()).collect(Collectors.toList());;
                } else if (currentSearchButton == 2) {
                    if (clear) biomeKeys.clear();
                    RegistryKey<Biome> key = RegistryKey.of(RegistryKeys.BIOME, Identifier.tryParse(this.languageDefinition));
                    biomeKeys.add(key);
                }
                if (clear) {
                    ChangeFireColorScreen.this.save();
                    BlockEntry entry = ChangeFireColorScreen.this.searchScreenListWidget.children().stream().filter(child -> child.languageDefinition.equals(this.languageDefinition)).findFirst().get();
                    ChangeFireColorScreen.this.searchScreenListWidget.setSelected(entry);
                    ChangeFireColorScreen.this.searchScreenListWidget.centerScrollOn(entry);
                    isOnAdd = false;
                } else {
                    ChangeFireColorScreen.this.searchScreenListWidget.selected.add(ChangeFireColorScreen.this.searchScreenListWidget.children().indexOf(this));
                }
            }
            /** Adds this entry during a + box click-drag, mirroring a + click in multi-add mode. */
            void addViaDrag() {
                if (isCustomized) return; // already coloured; don't reorder/replace on a drag
                int index = ChangeFireColorScreen.this.searchScreenListWidget.children().indexOf(this);
                if (selected.contains(index)) return;
                if (selected.isEmpty()) {
                    ChangeFireColorScreen.this.searchScreenListWidget.setSelected(this);
                } else {
                    onAddButton();
                }
            }
            @Override
            public Text getNarration() {
                return Text.translatable("narrator.select", this.languageDefinition);
            }
        }
    }
}