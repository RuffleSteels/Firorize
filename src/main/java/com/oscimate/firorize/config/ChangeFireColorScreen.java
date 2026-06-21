package com.oscimate.firorize.config;

import com.oscimate.firorize.FireSprites;
import com.oscimate.firorize.FirorizePipelines;
import com.oscimate.firorize.Main;
import com.oscimate.firorize.config.render.BlockSceneRenderState;
import com.oscimate.firorize.config.render.BlockSceneRenderState.BlockDrawOp;
import com.oscimate.firorize.config.render.ColorWheelElement;
import com.oscimate.firorize.mixin.fire_overlays.client.FireBlockInvoker;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
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
import org.joml.Matrix3x2f;
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
    public void drawX(DrawContext context, int y, int x) {
        int colorInt = new Color(150f / 255f, 150f / 255f, 150f / 255f, 1f).getRGB();

        int b = x - 1;
        int cy = y;

        context.fill(b + 1, cy + 1, b, cy, colorInt);
        context.fill(b + 2, cy, b + 1, cy - 1, colorInt);
        context.fill(b, cy, b - 1, cy - 1, colorInt);
        context.fill(b + 2, cy + 2, b + 1, cy + 1, colorInt);
        context.fill(b, cy + 2, b - 1, cy + 1, colorInt);
        context.fill(b + 3, cy - 1, b + 2, cy - 2, colorInt);
        context.fill(b - 1, cy - 1, b - 2, cy - 2, colorInt);
        context.fill(b + 3, cy + 3, b + 2, cy + 2, colorInt);
        context.fill(b - 1, cy + 3, b - 2, cy + 2, colorInt);
    }
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

    public float testVal = 0f;
    public float testtVal = 0f;

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

    private final ArrayList<Integer> comparedPriorityOrder;
    protected ChangeFireColorScreen(Screen parent) {
        super(Text.translatable("options.videoTitle"));
        this.comparedCurrentFire = deepClone(Main.CONFIG_MANAGER.getCurrentBlockFireColors());
        this.comparedPriorityOrder = new ArrayList<>(Main.CONFIG_MANAGER.getPriorityOrder());
        this.parent = parent;
    }
    public void onClose() {
        Main.inConfig = false;
        if (!isPresetAdd && (!comparedPriorityOrder.equals(Main.CONFIG_MANAGER.getPriorityOrder())
                || !fireColorsEqual(comparedCurrentFire, Main.CONFIG_MANAGER.getCurrentBlockFireColors()))) {
            MinecraftClient.getInstance().reloadResources();  }

        int[] list = Main.CONFIG_MANAGER.getCurrentBlockFireColors().getRight();
        System.arraycopy(list, 0, Main.CONFIG_MANAGER.getFireColorPresets().get(presetListWidget.curPresetID).getLeft().getRight(), 0, list.length);
        Collections.copy(Main.CONFIG_MANAGER.getFireColorPresets().get(presetListWidget.curPresetID).getLeft().getLeft(), Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft());
        Collections.copy(Main.CONFIG_MANAGER.getFireColorPresets().get(presetListWidget.curPresetID).getRight(), Main.CONFIG_MANAGER.getPriorityOrder());

        Main.CONFIG_MANAGER.save();

        int i = this.client.getWindow().calculateScaleFactor(this.client.options.getGuiScale().getValue(), this.client.forcesUnicodeFont());
        this.client.getWindow().setScaleFactor(i);

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
        // Drawn last in render(), so in the 2D GUI (draw order = call order) it sits in front of the
        // 3D previews, which are composited as 2D quads earlier in the queue.
        context.getMatrices().pushMatrix();
        context.fill(0, 0, width, height, 0xB0000000); // dim everything behind the box
        int bx = confirmBoxX(), by = confirmBoxY();
        context.fill(bx - 1, by - 1, bx + confirmBoxW + 1, by + confirmBoxH + 1, 0xFF000000);
        context.fill(bx, by, bx + confirmBoxW, by + confirmBoxH, 0xFF1A1A1A);
        context.drawStrokedRectangle(bx, by, confirmBoxW, confirmBoxH, 0xFF8B8B8B);
        context.drawCenteredTextWithShadow(textRenderer, confirmTitle, width / 2, by + 10, 0xFFFFFFFF);
        int ty = by + 30;
        for (OrderedText line : textRenderer.wrapLines(confirmMessage, confirmBoxW - 24)) {
            context.drawCenteredTextWithShadow(textRenderer, line, width / 2, ty, 0xFFC0C0C0);
            ty += 11;
        }
        drawConfirmButton(context, confirmYesRect(), ScreenTexts.YES, mouseX, mouseY);
        drawConfirmButton(context, confirmNoRect(), ScreenTexts.NO, mouseX, mouseY);
        context.getMatrices().popMatrix();
    }

    private void drawConfirmButton(DrawContext context, int[] r, Text label, int mouseX, int mouseY) {
        boolean hover = inRect(r, mouseX, mouseY);
        context.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], hover ? 0xFF505050 : 0xFF383838);
        context.drawStrokedRectangle(r[0], r[1], r[2], r[3], hover ? 0xFFFFFFFF : 0xFF8B8B8B);
        context.drawCenteredTextWithShadow(textRenderer, label, r[0] + r[2] / 2, r[1] + (r[3] - 8) / 2, 0xFFFFFFFF);
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
    public ButtonWidget shareProfileButton;
    public ButtonWidget resetProfileButton;
    private final KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>, int[]> comparedCurrentFire;
    public ButtonWidget[] movableArrowButtons = new ButtonWidget[6];
    public int profileButtonY = wheelCoords[0] + wheelRadius*2 + 80;

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

        this.presetListWidget = new PresetListWidget(client,  wheelRadius*2 + sliderDimensions[0] + 20, height-hexBoxCoords[1] -60-20 - 30, wheelCoords[0], 15, this, textRenderer);

        this.resetProfileButton = new ButtonWidget.Builder(Text.literal(""), button -> this.presetListWidget.resetProfile()).dimensions(profileButtonXs[0], profileButtonY, 20, 20).build();

        this.shareProfileButton = new ButtonWidget.Builder(Text.literal(""), button -> saveProfile()).dimensions(profileButtonXs[1], profileButtonY, 20, 20).build();
        this.addButton = new ButtonWidget.Builder(Text.literal("+"), button -> presetListWidget.addPreset()).dimensions(profileButtonXs[2], profileButtonY, 20, 20).build();
        this.addDrawableChild(addButton);
//        textFieldWidget.setChangedListener(this::updateCursor);
        updateCursor(this.hexCode);

        overlayToggles[0] = new ButtonWidget.Builder(Text.translatable("firorize.config.button.baseButton"), button -> toggle(false)).dimensions(hexBoxCoords[0], hexBoxCoords[1] + 30, (wheelRadius*2 + 20 + sliderDimensions[0])/2, 20).build();
        overlayToggles[1]  = new ButtonWidget.Builder(Text.translatable("firorize.config.button.overlayButton"), button -> toggle(false)).dimensions(hexBoxCoords[0] + (wheelRadius*2 + 20 + sliderDimensions[0])/2, hexBoxCoords[1] + 30, (wheelRadius*2 + 20 + sliderDimensions[0])/2, 20).build();

        searchOptions[0] = new MoveableButton(this, this.textRenderer, blockSearchCoords[0], blockSearchCoords[1], blockSearchDimensions[0]/3, 20, Text.translatable("firorize.config.title.blocks"),  0);
        searchOptions[1]  = new MoveableButton(this, this.textRenderer, blockSearchCoords[0]+blockSearchDimensions[0]/3, blockSearchCoords[1], blockSearchDimensions[0]/3, 20, Text.translatable("firorize.config.title.tags"), 1);
        searchOptions[2]  = new MoveableButton(this, this.textRenderer, blockSearchCoords[0]+blockSearchDimensions[0]/3*2, blockSearchCoords[1], blockSearchDimensions[0]/3, 20, Text.translatable("firorize.config.title.biomes"), 2);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 2; j++) {
                if (2*i+j != 0 && 2*i+j != 5) {
                    int finalJ = j;
                    MoveableButton button = ((MoveableButton) searchOptions[i]);
                    movableArrowButtons[2 * i + j] = new ButtonWidget.Builder(Text.literal(""), buttonn -> button.move(finalJ != 0)).dimensions(button.getXX()[j], button.getYY(), button.getHeight(), 13).build();
                    this.addDrawableChild(movableArrowButtons[2 * i + j]);

                    movableArrowButtons[2 * i + j].setTooltip(Tooltip.of(Text.translatable("firorize.config.tooltip.priorityArrow")));
                    movableArrowButtons[2 * i + j].setTooltipDelay(Duration.ofMillis(750L));
                }
            }
        }



        this.addDrawableChild(presetListWidget);
        this.addDrawableChild(shareProfileButton);
        this.addDrawableChild(searchOptions[0]);
        this.addDrawableChild(searchOptions[1]);
        this.addDrawableChild(searchOptions[2]);
        this.addDrawableChild(overlayToggles[0]);
        this.addDrawableChild(overlayToggles[1]);
        this.addDrawableChild(undoButton);
        this.addDrawableChild(redoButton);
        this.addDrawableChild(cyclicalPresets);
        this.addDrawableChild(addColorButton);
        this.addDrawableChild(invisibleTextFieldWidget);
        this.addDrawableChild(resetProfileButton);

        if (client.world == null) {
            searchOptions[1].setTooltip(Tooltip.of(Text.translatable("firorize.config.tooltip.movableButton")));
            searchOptions[1].active = false;
            searchOptions[2].setTooltip(Tooltip.of(Text.translatable("firorize.config.tooltip.movableButton")));
            searchOptions[2].active = false;
            searchOptions[0].active = false;
        } else {
            this.changeSearchOption(Main.CONFIG_MANAGER.getPriorityOrder().get(0));
        }

        shareProfileButton.setTooltip(Tooltip.of(Text.translatable("firorize.config.tooltip.shareProfileButton")));
        shareProfileButton.setTooltipDelay(Duration.ofMillis(750L));
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


    private void saveProfile() {
        try {
            MinecraftClient.getInstance().keyboard.setClipboard(serializeToString(KeyValuePair.of(Main.CONFIG_MANAGER.getCurrentBlockFireColors(), Main.CONFIG_MANAGER.getPriorityOrder())));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        tooltipTimer = 40;
        shareProfileButton.setFocused(false);
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
    public void resize(int width, int height) {
        MinecraftClient client = MinecraftClient.getInstance();
        Main.setScale(width, height, client);

        super.resize(client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight());
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
    public boolean mouseReleased(net.minecraft.client.gui.Click click) {
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
        return super.mouseReleased(click);
    }
    private boolean isClick = false;

    private boolean isOnAdd = false;

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyInput input) {
        int keyCode = input.key();
        if (keyCode == GLFW.GLFW_KEY_D) {
            testtVal += 45f;
        }
        if (keyCode == GLFW.GLFW_KEY_A) {
            testtVal -= 45f;
        }
        if (keyCode == GLFW.GLFW_KEY_W) {
            testVal += .1f;
        }
        if (keyCode == GLFW.GLFW_KEY_S) {
            testVal -= .1f;
        }
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
        return super.keyPressed(input);
    }
    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
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
            mouseDragged(click, 0, 0);
        } else if (mouseX >= clickedX - selectSpace && mouseY >= clickedY - selectSpace && mouseX <= clickedX + selectSpace && mouseY <= clickedY + selectSpace) {
            clicked = true;
        } else {
            updateColorPicker(mouseX, mouseY, true);
        }
        if ((clicked || sliderClicked) && gestureStartColor == null) {
            gestureStartColor = before;
        }
        return super.mouseClicked(click, doubled);
    }
    @Override
    public boolean mouseDragged(net.minecraft.client.gui.Click click, double deltaX, double deltaY) {
        double mouseX = click.x();
        double mouseY = click.y();
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
        return super.mouseDragged(click, deltaX, deltaY);
    }
    private int counter = 0;
    private float dist = 0f;
    private boolean forwards = true;

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderPanoramaBackground(context, delta);

        this.applyBlur(context);
        this.renderDarkening(context);
    }

    @Override
    @SuppressWarnings("deprecation") // SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE is deprecated but still the supported atlas id in 1.21
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.getMatrices().pushMatrix();

        super.render(context, mouseX, mouseY, delta);

        Sprite RESET = FireSprites.block(FireSprites.atlasManager(), "firorize:block/reset");
        context.drawSpriteStretched(RenderPipelines.GUI_TEXTURED, RESET, profileButtonXs[0] + (20 - RESET.getContents().getWidth())/2, profileButtonY + (20 - RESET.getContents().getHeight())/2, RESET.getContents().getWidth(), RESET.getContents().getHeight());

        Sprite SHARE = FireSprites.block(FireSprites.atlasManager(), "firorize:block/share");
        context.drawSpriteStretched(RenderPipelines.GUI_TEXTURED, SHARE, profileButtonXs[1] + (20 - SHARE.getContents().getWidth())/2, profileButtonY + (20 - SHARE.getContents().getHeight())/2, SHARE.getContents().getWidth(), SHARE.getContents().getHeight());

        // Colour wheel — drawn through the custom COLOR_WHEEL pipeline (lightness Value carried in
        // the quad's vertex-colour alpha; full brightness here).
        context.state.addSimpleElement(new ColorWheelElement(
                FirorizePipelines.COLOR_WHEEL, new Matrix3x2f(context.getMatrices()),
                wheelCoords[0], wheelCoords[1], wheelCoords[0] + wheelRadius * 2, wheelCoords[1] + wheelRadius * 2,
                1.0f, null));

        context.drawStrokedRectangle((int) clickedX - cursorDimensions/4, (int) clickedY - cursorDimensions/4, cursorDimensions/4*3, cursorDimensions/4*3, Color.gray.getRGB());
        context.drawStrokedRectangle((int) clickedX - cursorDimensions/2, (int)  clickedY - cursorDimensions/2, cursorDimensions, cursorDimensions, Color.gray.getRGB());
        context.fill((int) clickedX - cursorDimensions/4, (int) clickedY - cursorDimensions/4, (int) clickedX + cursorDimensions/4, (int) clickedY + cursorDimensions/4, Color.BLACK.getRGB());

        context.fill(sliderCoords[0], sliderCoords[1], sliderCoords[0]+sliderDimensions[0], sliderCoords[1]+sliderDimensions[1]/2, Color.HSBtoRGB((float) hue, (float) saturation, 1.0f));
        context.fill(sliderCoords[0], sliderCoords[1]+sliderDimensions[1]/2, sliderCoords[0]+sliderDimensions[0], sliderCoords[1]+sliderDimensions[1], Color.BLACK.getRGB());
        context.fillGradient(sliderCoords[0], sliderCoords[1]+11, sliderCoords[0]+sliderDimensions[0], sliderCoords[1]+sliderDimensions[1]-11, Color.HSBtoRGB((float) hue, (float) saturation, 1.0f), Color.BLACK.getRGB());

        context.drawStrokedRectangle((int) sliderClickedX - cursorDimensions/4, (int) sliderClickedY - cursorDimensions/4, cursorDimensions/4*3, cursorDimensions/4*3, Color.gray.getRGB());
        context.drawStrokedRectangle((int) sliderClickedX - cursorDimensions/2, (int)  sliderClickedY - cursorDimensions/2, cursorDimensions, cursorDimensions, 0x7f222222);
        context.fill((int) sliderClickedX - cursorDimensions/4, (int) sliderClickedY - cursorDimensions/4, (int) sliderClickedX + cursorDimensions/4, (int) sliderClickedY + cursorDimensions/4, Color.BLACK.getRGB());



        // ===== 3D block grid (scrollable, scissored) — drawn via the special-element renderer =====
        Quaternionf q = new Quaternionf();
        q.rotateZ((float) Math.toRadians(0));
        q.rotateX((float) Math.toRadians(45));
        q.rotateY((float) Math.toRadians(-45));

        if (Math.ceil(allBlockUnders.size()/11f) > 4) {
            double amount = 0.15 * ((Math.ceil(allBlockUnders.size()/11f)-4)/2);
            dist = (float) (dist + (forwards ? amount : -amount));
            if (dist > (31 * (Math.ceil(allBlockUnders.size()/11f)-4))) {
                counter++;
                forwards = false;
            } else if (dist < 1) {
                counter++;
                forwards = true;
            }
        } else {
            dist = 0;
        }

        // The special-element renderer puts the model origin at the box centre-bottom, pre-scaled by
        // the window scale × the state's scale(); op.tx/ty therefore map a screen-pixel offset to model
        // units by (px - boxCentre)/scale. (Exact placement/scale may want in-game tuning.)
        int gridX1 = blockSearchCoords[0];
        int gridY1 = blockSearchDimensions[1] + 40 + 10;
        int gridX2 = width - 20;
        int gridY2 = height - 10;
        float gridScale = 15f;
        float gridBoxW = gridX2 - gridX1;
        float gridBoxH = gridY2 - gridY1;
        float gridOriginX = (width - 300 - 20) - gridX1;
        float gridOriginY = (20 + blockSearchDimensions[1] + 20 + 10) - gridY1;
        java.util.List<BlockDrawOp> gridOps = new java.util.ArrayList<>();
        for (int i = 0; i < allBlockUnders.size(); i++) {
            float cellPx = gridOriginX + (blockSearchDimensions[0] - 21) / 10f * (i % 11);
            float cellPy = gridOriginY + (height - blockSearchDimensions[1] - 40 - 20) / 4f * (i / 11) - dist;
            gridOps.add(BlockDrawOp.independent(allBlockUnders.get(i).getDefaultState(),
                    (cellPx - gridBoxW / 2f) / gridScale, (cellPy - gridBoxH) / gridScale,
                    q, true, 1f, 1f, -0.36f, 0f, 1f, 1f, 1f, false));
        }
        context.state.addSpecialElement(new BlockSceneRenderState(gridX1, gridY1, gridX2, gridY2, gridScale, gridOps,
                new ScreenRect(gridX1, gridY1, gridX2 - gridX1, gridY2 - gridY1)));




        java.util.List<BlockDrawOp> pvOps = new java.util.ArrayList<>();

        pvOps.add(new BlockDrawOp(blockUnder.getDefaultState(),
                0f, (-4f/960) * context.getScaledWindowHeight(), 0f,
                q, true, -2f, -.5f, -.5f, -.5f, 1f, 1f, 1f, false, true, true));

        pvOps.add(new BlockDrawOp(Blocks.FIRE.getDefaultState(),
                0f, (-4f/960) * context.getScaledWindowHeight(), 0f,
                q, true, -2f, -.5f, .5f, -0.5f,
                pickedColor[0].getRed() / 255f, pickedColor[0].getGreen() / 255f, pickedColor[0].getBlue() / 255f, true, true, true));

//        // Soul fire (custom tint, overlay colour): same matrix as fire, then pop.
        pvOps.add(new BlockDrawOp(Blocks.SOUL_FIRE.getDefaultState(),
                0f, (-4f/960) * context.getScaledWindowHeight(), 0f,
                q, true, -2f, -.5f, .5f, -0.5f,
                pickedColor[1].getRed() / 255f, pickedColor[1].getGreen() / 255f, pickedColor[1].getBlue() / 255f, true, true, true));

        int x1 = wheelCoords[0] + (wheelRadius*2 + sliderDimensions[0] + 20);
        int x2 = context.getScaledWindowWidth() - (blockSearchCoords[1] ) -  blockSearchDimensions[0];

//        context.drawStrokedRectangle(x1, 10, x2 - x1, 1000, Color.RED.getRGB());

        context.state.addSpecialElement(new BlockSceneRenderState(x1, 0, x2, context.getScaledWindowHeight(), 100f, pvOps,
                new ScreenRect(0, 0, context.getScaledWindowWidth(), context.getScaledWindowHeight())));

        if (tooltipTimer > 0) {
            context.drawTooltip(this.textRenderer, Text.translatable("firorize.config.tooltip.copied"), shareProfileButton.getX() + 50, shareProfileButton.getY() - 10);
        }
        context.getMatrices().popMatrix();

        if (confirmActive) renderConfirm(context, mouseX, mouseY);
    }

    @Environment(value= EnvType.CLIENT)
    class SearchScreenListWidget
            extends AlwaysSelectedEntryListWidget<ChangeFireColorScreen.SearchScreenListWidget.BlockEntry> {
        public void generateEntries() {
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
                    TagKey<Block> tag = Main.blockTagList.stream().filter(tagg -> tagg.id().toString().equals(string)).findFirst().get();
                    if (Main.blockTagList.contains(tag)) {
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
        public int num = 0;
        public void test() {
            test(true);
        }
        public void test(boolean keepScroll) {
            this.clearEntries();
            generateEntries();
            // The scroll-amount getter was removed in 1.21.x, so the list resets to the top on refresh.
            setScrollY(0.0);
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

        // Selection is tracked in the `selected` index list (this widget never calls super.setSelected,
        // so vanilla's getSelectedOrNull/drawSelectionHighlight path never fires). Draw the highlight for
        // every selected entry ourselves, before the entry content so text/swatch render on top.
        @Override
        protected void renderEntry(DrawContext context, int mouseX, int mouseY, float tickDelta, BlockEntry entry) {
            int index = this.children().indexOf(entry);
            if (selected.contains(index)) {
                drawSelectionBorder(context, entry, selected.contains(index - 1), selected.contains(index + 1));
            }
            super.renderEntry(context, mouseX, mouseY, tickDelta, entry);
        }

        /**
         * Draws the selection outline for one entry. When the entry above/below is also selected the
         * border between them is filled in (no separating line) so a run of selected entries reads as a
         * single block; an isolated selected entry gets a full border on all sides.
         */
        private void drawSelectionBorder(DrawContext context, BlockEntry entry, boolean prevSelected, boolean nextSelected) {
            int color = this.isFocused() ? -1 : -8355712;
            int entryWidth = getRowWidth();
            int entryHeight = entry.getHeight();
            int y = entry.getY();
            int left = this.getX() + (this.width - entryWidth) / 2;
            int right = getScrollbarX() - 1; // keep the right edge clear of the scrollbar
            // Extend the coloured edge and black interior through the 4px gap to a selected neighbour, so a
            // run of selections reads as one block; an isolated entry keeps its 1px border on every side.
            int outerBottom = nextSelected ? y + entryHeight + 2 : y + entryHeight + 1;
            int innerTop = prevSelected ? y - 3 : y - 1;
            int innerBottom = nextSelected ? y + entryHeight + 3 : y + entryHeight;
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

            selected.forEach(index -> this.children().get(index).isSelected = false);

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
                    allBlockUnders = java.util.stream.StreamSupport.stream(Registries.BLOCK.iterateEntries(tag).spliterator(), false).map(entry2 -> entry2.value()).filter(block -> blockUnderList.contains(block)).collect(Collectors.toList());;
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
            private boolean isSelected = false;

            @Override
            public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                int x = getX();
                int y = getY();
                int entryWidth = getWidth();
                int entryHeight = getHeight();
                int index = ChangeFireColorScreen.this.searchScreenListWidget.children().indexOf(this);
                context.drawCenteredTextWithShadow(ChangeFireColorScreen.this.textRenderer, Text.literal(languageDefinition), (entryWidth-6) / 2  + blockSearchCoords[0], y+3, 0xFFFFFFFF);
                // Left action box (+ / reorder arrows): inset slightly and vertically centred so it reads better.
                int boxInset = 2;
                int boxSize = entryHeight - boxInset * 2;
                int bx = x + boxInset;
                int by = y + boxInset;
                int bcx = bx + boxSize / 2;
                int bcy = by + boxSize / 2;
                boolean shiftPressed = InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT);
                if ((shiftPressed && index < Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft().get(currentSearchButton).size() - 1) || (!shiftPressed && index > 0)) {
                    if (mouseX >= bx && mouseX <= bx + boxSize && mouseY >= by && mouseY <= by + boxSize) {
                        alpha = 1f;
                    } else {
                        alpha = 0.5f;
                    }
                } else {
                    alpha = 0.5f;
                }
                int colorInt = new Color(1f/255*150, 1f/255*150, 1f/255*150, 1f).getRGB();

                if (!ChangeFireColorScreen.this.searchScreenListWidget.selected.contains(ChangeFireColorScreen.this.searchScreenListWidget.children().indexOf(this)) && !isCustomized) {
                    context.fill(bx, by, bx+boxSize, by+boxSize, new Color(1f/255*44, 1f/255*44, 1f/255*44, alpha).getRGB());
                    context.fill(bcx, by+2, bcx+1, by+boxSize-2, colorInt);
                    context.fill(bx+2, bcy, bx+boxSize-2, bcy+1, colorInt);
                    context.drawStrokedRectangle(bx, by, boxSize, boxSize, new Color(1f/255*99, 1f/255*99, 1f/255*99, 0.8f).getRGB());
                }
                if (isCustomized && currentSearchButton == 1  && children().indexOf(this) != 0) {
                    context.fill(bx, by, bx+boxSize, by+boxSize, new Color(1f/255*44, 1f/255*44, 1f/255*44, alpha).getRGB());
                    context.drawStrokedRectangle(bx, by, boxSize, boxSize, new Color(1f/255*99, 1f/255*99, 1f/255*99, 0.8f).getRGB());
                    if (shiftPressed) {
                        if (index < Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft().get(currentSearchButton).size()) {
                            context.fill(bcx, by+2, bcx+1, by+boxSize-2, colorInt);
                            context.fill(bcx - 1, by+boxSize-3, bcx, by+boxSize-4, colorInt);
                            context.fill(bcx - 2, by+boxSize-4, bcx - 1, by+boxSize-5, colorInt);
                            context.fill(bcx + 1, by+boxSize-3, bcx + 2, by+boxSize-4, colorInt);
                            context.fill(bcx + 2, by+boxSize-4, bcx + 3, by+boxSize-5, colorInt);
                        }
                    } else {
                        if (index > 1) {
                            context.fill(bcx, by + 2, bcx + 1, by + boxSize - 2, colorInt);
                            context.fill(bcx - 1, by + 3, bcx, by + 4, colorInt);
                            context.fill(bcx - 2, by + 4, bcx - 1, by + 5, colorInt);
                            context.fill(bcx + 1, by + 3, bcx + 2, by + 4, colorInt);
                            context.fill(bcx + 2, by + 4, bcx + 3, by + 5, colorInt);
                        }
                    }
                }
                if (isCustomized) {
                    // Right colour/delete box: same inset + vertical centring as the left action box.
                    int rbRight = x + entryWidth - 10 - boxInset;
                    int rbLeft = rbRight - boxSize;
                    int rbcx = rbLeft + boxSize / 2;
                    if (mouseX >= rbLeft && mouseX <= rbRight && mouseY >= by && mouseY <= by+boxSize && children().indexOf(this) != 0) {
                        context.fill(rbLeft, by, rbRight, by+boxSize, new Color(1f/255*44, 1f/255*44, 1f/255*44, alpha).getRGB());
                        drawX(context, bcy, rbcx + 1);
                    } else {

                        int[] test = this.languageDefinition.equals(Text.translatable("firorize.config.baseFire").getString()) ? Main.CONFIG_MANAGER.getCurrentBlockFireColors().getRight(): Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft().get(currentSearchButton).get(this.languageDefinition);
                        if (test != null) {
                            context.fill(rbLeft, by, rbRight, by+boxSize, test[0]);
                            context.fill(rbLeft+2, by+2, rbRight-2, by+boxSize-2, test[1]);
                            }
                        }
                    context.drawStrokedRectangle(rbLeft, by, boxSize, boxSize, new Color(1f/255*99, 1f/255*99, 1f/255*99, 0.8f).getRGB());
                }

            }
            @Override
            public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
                double mouseX = click.x();
                double mouseY = click.y();
                int x = getX();
                int y = getY();
                int entryHeight = getHeight();
                int boxInset = 2;
                int boxSize = entryHeight - boxInset * 2;
                int bx = x + boxInset;
                int by = y + boxInset;
                int rbRight = x + getWidth() - 10 - boxInset;
                int rbLeft = rbRight - boxSize;
                if (mouseX >= rbLeft && mouseX <= rbRight && mouseY >= by && mouseY <= by+boxSize && children().indexOf(this) != 0) {
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
                if (mouseX >= bx && mouseX <= bx+boxSize && mouseY >= by && mouseY <= by+boxSize) {
                    if (isCustomized && currentSearchButton == 1) {
                        int index = ChangeFireColorScreen.this.searchScreenListWidget.children().indexOf(this);
                        boolean shiftPressed = InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT);
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
                    List<Block> newBlocks = java.util.stream.StreamSupport.stream(Registries.BLOCK.iterateEntries(tag).spliterator(), false)
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
            @Override
            public Text getNarration() {
                return Text.translatable("narrator.select", this.languageDefinition);
            }
        }
    }
}