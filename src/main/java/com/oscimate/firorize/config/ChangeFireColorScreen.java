package com.oscimate.firorize.config;

import com.mojang.blaze3d.systems.RenderSystem;
import com.oscimate.firorize.CustomRenderLayer;
import com.oscimate.firorize.GameRendererSetting;
import com.oscimate.firorize.Main;
import com.oscimate.firorize.mixin.fire_overlays.client.FireBlockInvoker;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.Blocks;
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
    private final ArrayList<Integer> comparedPriorityOrder;
    protected ChangeFireColorScreen(Screen parent) {
        super(Text.translatable("options.videoTitle"));
        this.comparedCurrentFire = deepClone(Main.CONFIG_MANAGER.getCurrentBlockFireColors());
        this.comparedPriorityOrder = new ArrayList<>(Main.CONFIG_MANAGER.getPriorityOrder());
        this.parent = parent;
    }
    public void onClose() {
        Main.inConfig = false;
        if (!isPresetAdd && (!comparedPriorityOrder.equals(Main.CONFIG_MANAGER.getPriorityOrder()) || !(Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft().size() == comparedCurrentFire.getLeft().size() &&
                Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft().stream().allMatch(map1 ->
                        comparedCurrentFire.getLeft().stream().anyMatch(map2 ->
                                map1.size() == map2.size() &&
                                        map1.keySet().equals(map2.keySet()) &&
                                        map1.keySet().stream().allMatch(key ->
                                                Arrays.equals(map1.get(key), map2.get(key))
                                        )
                        )
                ) && Arrays.equals(comparedCurrentFire.getRight(), Main.CONFIG_MANAGER.getCurrentBlockFireColors().getRight())))) {
            MinecraftClient.getInstance().reloadResources();  }

        int[] list = Main.CONFIG_MANAGER.getCurrentBlockFireColors().getRight();
        System.arraycopy(list, 0, Main.CONFIG_MANAGER.getFireColorPresets().get(presetListWidget.curPresetID).getLeft().getRight(), 0, list.length);
        Collections.copy(Main.CONFIG_MANAGER.getFireColorPresets().get(presetListWidget.curPresetID).getLeft().getLeft(), Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft());
        Collections.copy(Main.CONFIG_MANAGER.getFireColorPresets().get(presetListWidget.curPresetID).getRight(), Main.CONFIG_MANAGER.getPriorityOrder());

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
    boolean isReset = false;
    private List<Block> blockUnderList = Registries.BLOCK.stream().filter(block -> block.getDefaultState().isSideSolidFullSquare(EmptyBlockView.INSTANCE, BlockPos.ORIGIN, Direction.UP) || ((FireBlockInvoker)Blocks.FIRE).getBurnChances().containsKey(block)).toList();
    private final int[] blockSearchCoords = {0, 18};
    private final int[] blockSearchDimensions = {300, 320};
    private ButtonWidget[] overlayToggles = new ButtonWidget[2];
    public ButtonWidget redoButton;
    public boolean hasRedo = false;
    private boolean colorRedo = false;
    private ButtonWidget saveButton;
    public ButtonWidget[] searchOptions = new ButtonWidget[3];
    private List<TagKey<Block>> blockTags = new ArrayList<>();
    private List<RegistryKey<Biome>> biomeKeys = new ArrayList<>();
    public void handlePickedColor(Color[] input) {
        if (!buffer) {
            setRedo(true, !resetBuffer);
            if (colorRedo) {
                lastPickedColor = input.clone();
            }
        }
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
        redoButton = new UndoButton(hexBoxCoords[0], hexBoxCoords[1], 20, 20, button -> redo());
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
        redoButton.setTooltip(Tooltip.of(Text.translatable("firorize.config.tooltip.undoButton")));
        redoButton.setTooltipDelay(Duration.ofMillis(750L));

        toggle(true);

        searchScreenListWidget.setSelected(searchScreenListWidget.children().get(0));

        setRedo(false);

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
    public void resize(MinecraftClient client, int width, int height) {
        Main.setScale(width, height, client);

        super.resize(client, client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight());
    }
    private int currentSearchButton = 0;

    public boolean resetBuffer = false;

    public void changeSearchOption(int buttonNum) {
        for (int i = 0; i < 3; i++) {
            if (i != Main.CONFIG_MANAGER.getPriorityOrder().indexOf(buttonNum)) {
                searchOptions[i].active = true;
            } else {
                searchOptions[i].active = false;
            }
        }
        currentSearchButton = buttonNum;
        searchScreenListWidget.test();
        searchScreenListWidget.setSelected(searchScreenListWidget.children().get(0));
        setRedo(false, !resetBuffer);
    }
    private boolean buffer = false;
    private void redo() {
        if (hasRedo) {
            if (isReset) {
                KeyValuePair<KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>,  int[]>, ArrayList<Integer>> temp = Main.CONFIG_MANAGER.getFireColorPresets().get(presetListWidget.curPresetID);
                int[] list = temp.getLeft().getRight();
                System.arraycopy(list, 0, Main.CONFIG_MANAGER.getCurrentBlockFireColors().getRight(), 0, list.length);
                Collections.copy(Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft(), temp.getLeft().getLeft());
                Collections.copy(Main.CONFIG_MANAGER.getPriorityOrder(), temp.getRight());

                Main.CONFIG_MANAGER.save();

                presetListWidget.setSelected(presetListWidget.children().stream().filter(thing -> thing.languageDefinition.equalsIgnoreCase(presetListWidget.curPresetID)).findFirst().get());

                isReset = false;
            } else {
                if (colorRedo) {
                    pickedColor = lastPickedColor;
                    int RGB = pickedColor[isOverlay ? 1 : 0].getRGB();
                    textFieldWidget.setText("#" + Integer.toHexString(RGB).substring(2));
                    updateCursor("#" + Integer.toHexString(RGB).substring(2));
                } else {
                    buffer = true;
                    this.dist = 0;
                    this.counter = 0;
                    allBlockUnders = new ArrayList<>();
                    biomeKeys = new ArrayList<>();
                    lastSelected.forEach(index -> {
                        SearchScreenListWidget.BlockEntry entry = this.searchScreenListWidget.children().get(index);
                        if (currentSearchButton == 0) {
                            allBlockUnders.add(Registries.BLOCK.get(Identifier.tryParse(entry.languageDefinition)));
                        } else if (currentSearchButton == 1) {
                            TagKey<Block> tag = Main.blockTagList.stream().filter(tagg -> tagg.id().toString().equals(entry.languageDefinition)).findFirst().get();

                            blockTags.add(tag);
                            List<Block> newBlocks = Registries.BLOCK.getEntryList(tag).get().stream()
                                    .map(entry2 -> entry2.value())
                                    .filter(block -> blockUnderList.contains(block) && !allBlockUnders.contains(block))
                                    .toList();

                            allBlockUnders = Stream.concat(allBlockUnders.stream(), newBlocks.stream()).collect(Collectors.toList());;
                        } else if (currentSearchButton == 2) {
                            RegistryKey<Biome> key = RegistryKey.of(RegistryKeys.BIOME, Identifier.tryParse(entry.languageDefinition));
                            biomeKeys.add(key);
                        }
                    });

                    searchScreenListWidget.selected = lastSelected.stream().map(SerializationUtils::clone).collect(Collectors.toList());
                    pickedColor = lastPickedColor.clone();
                    int RGB = lastPickedColor[isOverlay ? 1 : 0].getRGB();
                    textFieldWidget.setText("#" + Integer.toHexString(RGB).substring(2));
                    updateCursor("#" + Integer.toHexString(RGB).substring(2));
                }
            }
            if (onBaseColor) {
                allBlockUnders.clear();
                allBlockUnders.add(Blocks.NETHERRACK);
            }
            setRedo(false);
        }
    }
    private void toggle(boolean start) {
        setRedo(false);
        isOverlay = start ? false : !isOverlay;
        int RGB = pickedColor[isOverlay ? 1:0].getRGB();
        textFieldWidget.setText("#"+Integer.toHexString(RGB).substring(2));
        updateCursor("#"+Integer.toHexString(RGB).substring(2));
        overlayToggles[isOverlay?1:0].active = false;
        overlayToggles[!isOverlay?1:0].active = true;
    }
    private void save() {
        hasRedo = false;
        setRedo(false);
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
        clicked = false;
        sliderClicked = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }
    private boolean isClick = false;

    private boolean isOnAdd = false;

    public void setRedo(boolean bool) {
        setRedo(bool, false);
    }

    public void setRedo(boolean bool, boolean useRedo) {
        if (isReset && resetBuffer) {
            isReset = false;
            int[] list = Main.CONFIG_MANAGER.getCurrentBlockFireColors().getRight();
            System.arraycopy(list, 0, Main.CONFIG_MANAGER.getFireColorPresets().get(presetListWidget.curPresetID).getLeft().getRight(), 0, list.length);
            Collections.copy(Main.CONFIG_MANAGER.getFireColorPresets().get(presetListWidget.curPresetID).getLeft().getLeft(), Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft());
            Collections.copy(Main.CONFIG_MANAGER.getFireColorPresets().get(presetListWidget.curPresetID).getRight(), Main.CONFIG_MANAGER.getPriorityOrder());

            Main.CONFIG_MANAGER.save();

            presetListWidget.setSelected(presetListWidget.children().stream().filter(thing -> thing.languageDefinition.equalsIgnoreCase(presetListWidget.curPresetID)).findFirst().get());
            resetBuffer = false;
        }
        hasRedo = bool;
        redoButton.active = bool;
        this.saveButton.setFocused(false);
    }
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
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
        return super.mouseClicked(mouseX, mouseY, button);
    }
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
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
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.getMatrices().push();

        super.render(context, mouseX, mouseY, delta);

        Sprite RESET = new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, Identifier.of("firorize:block/reset")).getSprite();
        context.drawSprite(profileButtonXs[0] + (20 - RESET.getContents().getWidth())/2, profileButtonY + (20 - RESET.getContents().getHeight())/2, 10, RESET.getContents().getWidth(), RESET.getContents().getHeight(), RESET);

        Sprite SHARE = new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, Identifier.of("firorize:block/share")).getSprite();
        context.drawSprite(profileButtonXs[1] + (20 - SHARE.getContents().getWidth())/2, profileButtonY + (20 - SHARE.getContents().getHeight())/2, 10, SHARE.getContents().getWidth(), SHARE.getContents().getHeight(), SHARE);



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

        if (Math.ceil(allBlockUnders.size()/11f) > 4) {
            double amount = 0.15 * ((Math.ceil(allBlockUnders.size()/11f)-4)/2);
            dist += forwards ? amount : -amount;
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

        context.getMatrices().translate(0, -dist, 0);

        for (int i = 0; i < allBlockUnders.size(); i++) {
            Block block = allBlockUnders.get(i);
            VertexConsumer c = context.getVertexConsumers().getBuffer(RenderLayers.getBlockLayer(block.getDefaultState()));

            context.getMatrices().push();

            context.getMatrices().translate((blockSearchDimensions[0]-21)/10f*(i%11), (height-blockSearchDimensions[1]-40-20)/4f*((double) (i / 11)), 0);

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

        if (tooltipTimer > 0) {
            context.drawTooltip(this.textRenderer, Text.translatable("firorize.config.tooltip.copied"), shareProfileButton.getX() + 50, shareProfileButton.getY() - 10);
        }
        context.getMatrices().pop();
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
            this.clearEntries();
            generateEntries();
            setScrollAmount(0.0);
            num = 0;
        }
        @Override
        public int getRowWidth() {
            return this.getWidth();
        }

        public List<Integer> selected = new ArrayList<>();

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
            setRedo(false);
        }

        public void moveEntryDown(BlockEntry entry) {
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
            setRedo(false);
        }

        @Override
        protected void renderEntry(DrawContext context, int mouseX, int mouseY, float delta, int index, int x, int y, int entryWidth, int entryHeight) {
            BlockEntry entry = this.getEntry(index);
            entry.x = x;
            entry.entryHeight = entryHeight;
            entry.y = y;
            super.renderEntry(context, mouseX, mouseY, delta, index, x, y, entryWidth, entryHeight);
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
                    if (mouseX >= x && mouseX <= x + entryHeight && mouseY >= y && mouseY <= y + entryHeight) {
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

                        Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft().get(currentSearchButton).remove(this.languageDefinition);

                        test();
                        return false;
                    }
                }
                if (mouseX >= x && mouseX <= x+entryHeight && mouseY >= y && mouseY <= y+entryHeight) {
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
                    setRedo(false);
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