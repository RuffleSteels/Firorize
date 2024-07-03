package com.oscimate.oscimate_soulflame.config;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.oscimate.oscimate_soulflame.CustomRenderLayer;
import com.oscimate.oscimate_soulflame.GameRendererSetting;
import com.oscimate.oscimate_soulflame.Main;
import com.oscimate.oscimate_soulflame.RenderFireColorAccessor;
import com.oscimate.oscimate_soulflame.mixin.fire_overlays.client.BlockTagAccessor;
import com.sun.jna.platform.KeyboardUtils;
import com.sun.jna.platform.win32.WinUser;
import com.sun.tools.jconsole.JConsoleContext;
import dev.isxander.yacl3.gui.SearchFieldWidget;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.impl.biome.modification.BuiltInRegistryKeys;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.screen.GameModeSelectionScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.tab.Tab;
import net.minecraft.client.gui.tab.TabManager;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.network.ClientDynamicRegistryType;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.*;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.GeneratorOptionsHolder;
import net.minecraft.data.server.tag.TagProvider;
import net.minecraft.loot.entry.TagEntry;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagBuilder;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Direction;
import net.minecraft.util.path.SymlinkValidationException;
import net.minecraft.world.EmptyBlockView;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import net.minecraft.world.level.LevelInfo;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.level.storage.LevelStorageException;
import net.minecraft.world.level.storage.LevelSummary;
import org.apache.commons.collections4.map.ListOrderedMap;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.windows.INPUT;

import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.oscimate.oscimate_soulflame.config.ConfigScreen.windowHeight;

public class ChangeFireColorScreen extends Screen {
    private Screen parent;
    private boolean clicked = false;
    private boolean sliderClicked = false;
    private double clickedX = 95.0;
    private double clickedY = 95.0;
    private String hexCode = "#ffffff";
    private final Color baseColor = Color.WHITE;
    public static Color[] pickedColor = {new Color(Color.decode("#ffffff").getRGB(), true), new Color(Color.decode("#ffffff").getRGB(), true)};
    public static Color[] lastPickedColor = null;
    private double hue = 0;
    private double saturation = 0.0;
    private double lightness = 1.0;
    private final int wheelRadius = 50;
    private final int cursorDimensions = 8;
    private final int[] wheelCoords = {70, 70};
    private final int[] sliderDimensions = {12, wheelRadius*2};
    private final int[] sliderCoords = {wheelCoords[0] + wheelRadius*2 + 20, wheelCoords[1]};
    private final double sliderPadding = (double) sliderDimensions[0] / 2;
    private double sliderClickedY = sliderCoords[1] + sliderPadding;
    private final double sliderClickedX = sliderCoords[0] + sliderPadding;
    private final int[] hexBoxCoords = {wheelCoords[0], wheelCoords[1] + wheelRadius*2 + 20};
    private boolean isOverlay = false;
    protected ChangeFireColorScreen(Screen parent) {
        super(Text.translatable("options.videoTitle"));
        this.parent = parent;
    }
    public void onClose() {
        client.setScreen(parent);
    }
    private TextFieldWidget textFieldWidget;
    private TextFieldWidget blockUnderField;
    private Block blockUnder = Blocks.NETHERRACK;
    private List<Block> allBlockUnders = new ArrayList<>();
    public String input = "";
    public ChangeFireColorScreen.SearchScreenListWidget searchScreenListWidget;
    private List<Block> blockUnderList = Registries.BLOCK.stream().filter(block -> block.getDefaultState().isSideSolidFullSquare(EmptyBlockView.INSTANCE, BlockPos.ORIGIN, Direction.UP)).toList();
    private final int[] blockSearchCoords = {400, 30};
    private final int[] blockSearchDimensions = {300, 300};
    private ButtonWidget[] overlayToggles = new ButtonWidget[2];
    private Block lastBlockUnder = Blocks.NETHERRACK;
    private ButtonWidget redoButton;
    private boolean hasRedo = false;
    private boolean colorRedo = false;
    private ButtonWidget saveButton;
    private ButtonWidget[] searchOptions = new ButtonWidget[3];
    private List<TagKey<Block>> blockTags = new ArrayList<>();
    private List<RegistryKey<Biome>> biomeKeys = new ArrayList<>();
    public void handlePickedColor(Color[] input) {
        if (!buffer) {
            hasRedo = true;
            redoButton.active = true;
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

    @Override
    protected void init() {
        saveButton = new ButtonWidget.Builder(Text.literal("SAVE"), button -> save()).dimensions(width / 2 +100, height/2 + windowHeight/2 + 20, 200, 20).build();
        this.addDrawableChild(saveButton);

        saveButton.active = false;
        this.addDrawableChild(new ButtonWidget.Builder(ScreenTexts.DONE, button -> onClose()).dimensions(width / 2 - 100, height/2 + windowHeight/2 + 20, 200, 20).build());
        this.searchScreenListWidget = new ChangeFireColorScreen.SearchScreenListWidget(this.client, blockSearchDimensions[0], blockSearchDimensions[1] - 20, blockSearchCoords[1] + 20, 15);
        this.addDrawableChild(searchScreenListWidget);
        textFieldWidget = new TextFieldWidget(this.textRenderer, hexBoxCoords[0], hexBoxCoords[1], wheelRadius, 20, ScreenTexts.DONE);
        blockUnderField = new CustomTextFieldWidget(this.textRenderer, blockSearchCoords[0], blockSearchCoords[1], blockSearchDimensions[0], 20, ScreenTexts.DONE, this);
        this.addDrawableChild(textFieldWidget);
        this.addDrawableChild(blockUnderField);

        textFieldWidget.setChangedListener(this::updateCursor);
        updateCursor(this.hexCode);

        redoButton = new ButtonWidget.Builder(Text.literal("UNDO"), button -> redo()).dimensions(150, 20, 80, 20).build();
        redoButton.active = false;
        overlayToggles[0] = new ButtonWidget.Builder(Text.literal("Base"), button -> toggle()).dimensions(250, 20, 80, 20).build();
        overlayToggles[1]  = new ButtonWidget.Builder(Text.literal("Overlay"), button -> toggle()).dimensions(300, 20, 80, 20).build();

        searchOptions[0] = new ButtonWidget.Builder(Text.literal("Blocks"), button -> changeSearchOption(0)).dimensions(blockSearchCoords[0], blockSearchCoords[1]-20, blockSearchDimensions[0]/3, 20).build();
        searchOptions[1]  = new ButtonWidget.Builder(Text.literal("Tags"), button -> changeSearchOption(1)).dimensions(blockSearchCoords[0]+blockSearchDimensions[0]/3, blockSearchCoords[1]-20, blockSearchDimensions[0]/3, 20).build();
        searchOptions[2]  = new ButtonWidget.Builder(Text.literal("Biomes"), button -> changeSearchOption(2)).dimensions(blockSearchCoords[0]+blockSearchDimensions[0]/3*2, blockSearchCoords[1]-20, blockSearchDimensions[0]/3, 20).build();

        this.addDrawableChild(searchOptions[0]);
        this.addDrawableChild(searchOptions[1]);
        this.addDrawableChild(searchOptions[2]);
        this.addDrawableChild(overlayToggles[0]);
        this.addDrawableChild(overlayToggles[1]);
        this.addDrawableChild(redoButton);

        if (Main.biomeKeyList == null) {
            searchOptions[1].setTooltip(Tooltip.of(Text.literal("You must be loaded in a world to customize with this")));
            searchOptions[1].active = false;
            searchOptions[2].setTooltip(Tooltip.of(Text.literal("You must be loaded in a world to customize with this")));
            searchOptions[2].active = false;
        }

        toggle();
        searchOptions[currentSearchButton].active = false;
        super.init();
    }

    private int currentSearchButton = 0;

    private void changeSearchOption(int buttonNum) {
        searchOptions[buttonNum].active = false;
        searchOptions[currentSearchButton].active = true;
        currentSearchButton = buttonNum;
        searchScreenListWidget.test();
    }
    private boolean buffer = false;
    private void redo() {
        if (hasRedo) {
            if (colorRedo) {
                pickedColor = lastPickedColor;
                int RGB = pickedColor[isOverlay ? 1:0].getRGB();
                textFieldWidget.setText("#"+Integer.toHexString(RGB).substring(2));
                updateCursor("#"+Integer.toHexString(RGB).substring(2));
            } else {
                buffer = true;
                searchScreenListWidget.setEntry(Registries.BLOCK.getId(lastBlockUnder).toString());
                pickedColor = lastPickedColor.clone();
                int RGB = lastPickedColor[isOverlay ? 1:0].getRGB();
                textFieldWidget.setText("#"+Integer.toHexString(RGB).substring(2));
                updateCursor("#"+Integer.toHexString(RGB).substring(2));
            }
            hasRedo = false;
            redoButton.active = false;
        }
    }
    private void toggle() {
        hasRedo = false;
        redoButton.active = false;
        isOverlay = !isOverlay;
        int RGB = pickedColor[isOverlay ? 1:0].getRGB();
        textFieldWidget.setText("#"+Integer.toHexString(RGB).substring(2));
        updateCursor("#"+Integer.toHexString(RGB).substring(2));
        overlayToggles[isOverlay?1:0].active = false;
        overlayToggles[!isOverlay?1:0].active = true;
    }
    private void save() {
        if (currentSearchButton == 0) {
            allBlockUnders.forEach(block -> {
                Main.CONFIG_MANAGER.getCurrentBlockFireColors().get(0).put(block.getTranslationKey(), new int[]{pickedColor[0].getRGB(), pickedColor[1].getRGB()});
            });
        } else if (currentSearchButton == 1) {
            Main.CONFIG_MANAGER.getCurrentBlockFireColors().get(1).put(blockTags.get(0).id().toString(), new int[]{pickedColor[0].getRGB(), pickedColor[1].getRGB()});
        } else if (currentSearchButton == 2) {
            Main.CONFIG_MANAGER.getCurrentBlockFireColors().get(2).put(biomeKeys.get(0).getValue().toString(), new int[]{pickedColor[0].getRGB(), pickedColor[1].getRGB()});
        }
        this.searchScreenListWidget.test();
    }
    public void updateBlockUnder(String blockUnderTag) {
        Identifier id = Identifier.tryParse(blockUnderTag);
        if (id != null) {
            Block block = Registries.BLOCK.get(id);
            if (blockUnderList.contains(block)) {
                lastBlockUnder = blockUnder;
                blockUnder = Registries.BLOCK.get(id);
                if (Main.CONFIG_MANAGER.getCurrentBlockFireColors().get(0).containsKey(blockUnder.getTranslationKey())) { //help
                    int[] colorInts = Main.CONFIG_MANAGER.getCurrentBlockFireColors().get(0).get(blockUnder.getTranslationKey()); //help
                    int RGB = colorInts[isOverlay ? 1:0];
                    colorRedo = false;
                    setPickedColors(new Color[]{new Color(colorInts[0]), new Color(colorInts[1])});
                    textFieldWidget.setText("#"+Integer.toHexString(RGB).substring(2));
                    updateCursor("#"+Integer.toHexString(RGB).substring(2));
                } else {

                    int RGB = baseColor.getRGB();
                    colorRedo = false;
                    setPickedColors(new Color[]{baseColor, baseColor});
                    textFieldWidget.setText("#"+Integer.toHexString(RGB).substring(2));
                    updateCursor("#"+Integer.toHexString(RGB).substring(2));
                }
            }
        }
    }
    private void updateCursor(String hexCode) {
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
                int x = (int) (120 + radius * Math.cos(theta));
                int y = (int) (120 + radius * Math.sin(theta));

                sliderClickedY = ((1 - HSB[2]) * (sliderDimensions[1] - sliderPadding*2)) + sliderCoords[1] + sliderPadding;
                clickedX = x;
                clickedY = y;

                if (Main.CONFIG_MANAGER.getCurrentBlockFireColors().get(0).containsKey(blockUnder.getTranslationKey())) { //help
                    int[] colorInts = Main.CONFIG_MANAGER.getCurrentBlockFireColors().get(0).get(blockUnder.getTranslationKey()); //help
                    saveButton.active = !(colorInts[0] == pickedColor[0].getRGB() && colorInts[1] == pickedColor[1].getRGB());
                } else {
                    saveButton.active = !(pickedColor[0].getRGB() == baseColor.getRGB() && pickedColor[1].getRGB() == baseColor.getRGB());
                }
            }
        }
    }
    private void updateColorPicker(double mouseX, double mouseY, boolean click) {
        double dx = 120 - mouseX;
        double dy = 120 - mouseY;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist <= 50) {
            clicked = true;
            clickedX = mouseX;
            clickedY = mouseY;
        } else if (!click) {
            clickedX = 120 + 50 * -Math.cos(Math.atan2(dy, dx));
            clickedY = 120 + 50 * -Math.sin(Math.atan2(dy, dx));
        }
        if (clicked) {
            dx = 120 - clickedX;
            dy = 120 - clickedY;
            saturation = Math.sqrt(dx * dx + dy * dy) / 50;
            hue = (Math.atan2(dy, dx) / (2 * Math.PI) + 0.25);

            int RGB = Color.HSBtoRGB((float) hue, (float) saturation, (float) ((float) lightness == 0 ? lightness+0.01 : lightness));

            textFieldWidget.setText("#"+Integer.toHexString(RGB).substring(2));
            if (click) {
                colorRedo = true;
                setPickedColor(new Color(RGB, true), isOverlay ? 1:0);
            } else {
                pickedColor[isOverlay ? 1:0] = new Color(RGB, true);
            }
            if (pickedColor[0].getRGB() == baseColor.getRGB() && pickedColor[1].getRGB() == baseColor.getRGB()) {
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
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 2) {
            isOverlay = !isOverlay;
            int RGB = pickedColor[isOverlay ? 1:0].getRGB();
            textFieldWidget.setText("#"+Integer.toHexString(RGB).substring(2));
            updateCursor("#"+Integer.toHexString(RGB).substring(2));
        } else {
            double selectSpace = (double) cursorDimensions / 2;
            if (mouseX >= sliderCoords[0] && mouseX <= sliderCoords[0] + sliderDimensions[0] && mouseY >= sliderCoords[1] + sliderPadding && mouseY <= sliderCoords[1] + sliderDimensions[1] - sliderPadding) {
                sliderClicked = true;
                isClick = true;
                mouseDragged(mouseX, mouseY, button, 0, 0);
            } else if (mouseX >= clickedX - selectSpace && mouseY >= clickedY - selectSpace && mouseX <= clickedX + selectSpace && mouseY <= clickedY + selectSpace) {
                clicked = true;
            } else {
                updateColorPicker(mouseX, mouseY, true);
            }
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
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackgroundTexture(context);
        super.render(context, mouseX, mouseY, delta);
        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        RenderSystem.setShader(GameRendererSetting::getRenderTypeColorWheel);
        RenderSystem.depthFunc(519);
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();

        Matrix4f matrix4f = context.getMatrices().peek().getPositionMatrix();

        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE);
        bufferBuilder.vertex(matrix4f, wheelCoords[0], wheelCoords[1], 0f).color(1f, 1f, 1f, 1f).texture(0f, 1f).next();
        bufferBuilder.vertex(matrix4f, wheelCoords[0], (wheelCoords[1] + wheelRadius * 2), 0f).color(1f, 1f, 1f, 1f).texture(0f, 0f).next();
        bufferBuilder.vertex(matrix4f, (wheelCoords[0] + wheelRadius * 2), (wheelCoords[1] + wheelRadius * 2), 0f).color(1f, 1f, 1f, 1f).texture(1f, 0f).next();
        bufferBuilder.vertex(matrix4f, (wheelCoords[0] + wheelRadius * 2), wheelCoords[1], 0f).color(1f, 1f, 1f, 1f).texture(1f, 1f).next();
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

        context.fill(wheelCoords[0] + wheelRadius, hexBoxCoords[1], wheelCoords[0] + wheelRadius*2 + 20 + sliderDimensions[0], hexBoxCoords[1] + 20, pickedColor[isOverlay ? 1:0].getRGB());

        RenderSystem.depthMask(true);

        BlockRenderManager brm = MinecraftClient.getInstance().getBlockRenderManager();


        Quaternionf q = new Quaternionf();
        q.rotateZ((float) Math.toRadians(180));
        q.rotateX((float) Math.toRadians(45));
        q.rotateY((float) Math.toRadians(45));
        context.getMatrices().multiply(q);

        context.getMatrices().scale(-1, 1, 1);

        context.getMatrices().translate(-50, 50, 0);
        context.getMatrices().scale(10, 10, 10);
        context.getMatrices().translate(15, -10, -1);

        for (int i = 0; i < allBlockUnders.size(); i++) {
            Block block = allBlockUnders.get(i);
            VertexConsumer c = context.getVertexConsumers().getBuffer(RenderLayers.getBlockLayer(block.getDefaultState()));

            context.getMatrices().translate(1, 0, 0);

            if (block instanceof BlockWithEntity) {
                BlockEntity blockEntity = ((BlockWithEntity) block).createBlockEntity(BlockPos.ORIGIN, block.getDefaultState());
                BlockEntityRenderer<BlockEntity> blockEntityRenderer = MinecraftClient.getInstance().getBlockEntityRenderDispatcher().get(blockEntity);

                boolean blockModel = blockEntityRenderer == null;
                if (!blockModel) blockModel = blockEntityRenderer.rendersOutsideBoundingBox(blockEntity);
                if (blockModel || block.getDefaultState().getRenderType() == BlockRenderType.MODEL) {
                    brm.getModelRenderer().render(context.getMatrices().peek(), c, block.getDefaultState(), brm.getModel(block.getDefaultState()), 0.0f, 0.0f, 0.0f, 1, 1);
                } else {
                    assert blockEntity != null;
                    blockEntity.setWorld(MinecraftClient.getInstance().world);
                    MinecraftClient.getInstance().getBlockEntityRenderDispatcher().renderEntity(blockEntity, context.getMatrices(), context.getVertexConsumers(), 1, OverlayTexture.DEFAULT_UV);
                }
            } else {
                brm.getModelRenderer().render(context.getMatrices().peek(), c, block.getDefaultState(), brm.getModel(block.getDefaultState()), 0.0f, 0.0f, 0.0f, 1, 1);
            }
        }

        BlockRenderManager blockRenderManager = MinecraftClient.getInstance().getBlockRenderManager();
        VertexConsumer consumer = context.getVertexConsumers().getBuffer(RenderLayers.getBlockLayer(blockUnder.getDefaultState()));


        context.getMatrices().scale(5, 5, 5);
        context.getMatrices().translate(-1, -5, 0);

        if (blockUnder instanceof BlockWithEntity) {
            BlockEntity blockEntity = ((BlockWithEntity) blockUnder).createBlockEntity(BlockPos.ORIGIN, blockUnder.getDefaultState());
            BlockEntityRenderer<BlockEntity> blockEntityRenderer = MinecraftClient.getInstance().getBlockEntityRenderDispatcher().get(blockEntity);

            boolean blockModel = blockEntityRenderer == null;
            if (!blockModel) blockModel = blockEntityRenderer.rendersOutsideBoundingBox(blockEntity);
            if (blockModel || blockUnder.getDefaultState().getRenderType() == BlockRenderType.MODEL) {
                blockRenderManager.getModelRenderer().render(context.getMatrices().peek(), consumer, blockUnder.getDefaultState(), blockRenderManager.getModel(blockUnder.getDefaultState()), 0.0f, 0.0f, 0.0f, 1, 1);
            } else {
                assert blockEntity != null;
                blockEntity.setWorld(MinecraftClient.getInstance().world);
                MinecraftClient.getInstance().getBlockEntityRenderDispatcher().renderEntity(blockEntity, context.getMatrices(), context.getVertexConsumers(), 1, OverlayTexture.DEFAULT_UV);
            }
        } else {
            blockRenderManager.getModelRenderer().render(context.getMatrices().peek(), consumer, blockUnder.getDefaultState(), blockRenderManager.getModel(blockUnder.getDefaultState()), 0.0f, 0.0f, 0.0f, 1, 1);
        }

        context.getMatrices().scale(-1, 1, 1);

        context.getMatrices().translate(-1, 1, 0);
        Block block = Blocks.FIRE;
        consumer = context.getVertexConsumers().getBuffer(CustomRenderLayer.getCustomTint());
        blockRenderManager.getModelRenderer().render(context.getMatrices().peek(), consumer, block.getDefaultState(), blockRenderManager.getModel(block.getDefaultState()), pickedColor[0].getRed()/255f, pickedColor[0].getGreen()/255f, pickedColor[0].getBlue()/255f, 1, 1);

        context.getVertexConsumers().draw();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.depthFunc(515);
    }
    @Environment(value= EnvType.CLIENT)
    class SearchScreenListWidget
            extends AlwaysSelectedEntryListWidget<ChangeFireColorScreen.SearchScreenListWidget.BlockEntry> {
        private void generateEntries() {
            if (currentSearchButton == 0) {
                List<ChangeFireColorScreen.SearchScreenListWidget.BlockEntry> first = new ArrayList<>();
                List<ChangeFireColorScreen.SearchScreenListWidget.BlockEntry> second = new ArrayList<>();
                blockUnderList.forEach((block) -> {
                    String string = Registries.BLOCK.getId(block).toString();
                    if (string.contains(input)) {
                        ChangeFireColorScreen.SearchScreenListWidget.BlockEntry blockEntry = new ChangeFireColorScreen.SearchScreenListWidget.BlockEntry(string);
                        if(!Main.CONFIG_MANAGER.getCurrentBlockFireColors().get(0).containsKey(block.getTranslationKey())) {
                            second.add(blockEntry);
                        }
                    }
                });
                Main.CONFIG_MANAGER.getCurrentBlockFireColors().get(0).keyList().forEach(string -> {
                    System.out.println(Registries.BLOCK.get(Identifier.tryParse(string)));
                    if (blockUnderList.contains(Registries.BLOCK.get(Identifier.tryParse(string)))) {
                        ChangeFireColorScreen.SearchScreenListWidget.BlockEntry blockEntry = new ChangeFireColorScreen.SearchScreenListWidget.BlockEntry(string);
                        first.add(blockEntry);
                        blockEntry.isCustomized = true;
                    }
                });
                System.out.println(first);
                Stream.concat(first.stream(), second.stream()).toList().forEach(this::addEntry);
            } else if (currentSearchButton == 1) {
                List<ChangeFireColorScreen.SearchScreenListWidget.BlockEntry> first = new ArrayList<>();
                List<ChangeFireColorScreen.SearchScreenListWidget.BlockEntry> second = new ArrayList<>();
                Main.blockTagList.forEach((key) -> {
                    String string = key.id().toString();
                    if (string.contains(input)) {
                        ChangeFireColorScreen.SearchScreenListWidget.BlockEntry blockEntry = new ChangeFireColorScreen.SearchScreenListWidget.BlockEntry(string);
                        if(!Main.CONFIG_MANAGER.getCurrentBlockFireColors().get(1).containsKey(string)) {
                            second.add(blockEntry);
                        }
                    }
                });
                Main.CONFIG_MANAGER.getCurrentBlockFireColors().get(1).keyList().forEach(string -> {
                    if (Main.blockTagList.contains(BlockTagAccessor.callOf(string))) {
                        ChangeFireColorScreen.SearchScreenListWidget.BlockEntry blockEntry = new ChangeFireColorScreen.SearchScreenListWidget.BlockEntry(string);
                        first.add(blockEntry);
                        blockEntry.isCustomized = true;
                    }
                });
                Stream.concat(first.stream(), second.stream()).toList().forEach(this::addEntry);
            } else {
                List<ChangeFireColorScreen.SearchScreenListWidget.BlockEntry> first = new ArrayList<>();
                List<ChangeFireColorScreen.SearchScreenListWidget.BlockEntry> second = new ArrayList<>();
                Main.biomeKeyList.forEach((key) -> {
                    String string = key.getValue().toString();
                    if (string.contains(input)) {
                        ChangeFireColorScreen.SearchScreenListWidget.BlockEntry blockEntry = new ChangeFireColorScreen.SearchScreenListWidget.BlockEntry(string);
                        if(!Main.CONFIG_MANAGER.getCurrentBlockFireColors().get(2).containsKey(string)) {
                            second.add(blockEntry);
                        }
                    }
                });
                Main.CONFIG_MANAGER.getCurrentBlockFireColors().get(2).keyList().forEach(string -> {
                    if (Main.biomeKeyList.contains(RegistryKey.of(RegistryKeys.BIOME, Identifier.tryParse(string)))) {
                        ChangeFireColorScreen.SearchScreenListWidget.BlockEntry blockEntry = new ChangeFireColorScreen.SearchScreenListWidget.BlockEntry(string);
                        first.add(blockEntry);
                        blockEntry.isCustomized = true;
                    }
                });
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
        public void test() {
            this.clearEntries();
            generateEntries();
            setScrollAmount(0.0);
        }
        public void setEntry(String text) {
            System.out.println(text);
            input = "";
            test();
            this.children().forEach((blockEntry -> {
                if (blockEntry.languageDefinition.equals(text)) {
                    blockEntry.realSelect = false;
                    this.centerScrollOn(blockEntry);
                    setSelected(blockEntry);
                }
            }));
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
        protected int getScrollbarPositionX() {
            return super.getScrollbarPositionX() + 20 + blockSearchCoords[0];
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

            selected.forEach(index -> this.getEntry(index).isSelected = false);

            selected.clear();
            selected.add(this.children().indexOf(entry));

            if (currentSearchButton == 0) {
                blockUnderField.setText(entry.languageDefinition);
                updateBlockUnder(entry.languageDefinition);
                allBlockUnders.clear();
                allBlockUnders.add(Registries.BLOCK.get(Identifier.tryParse(entry.languageDefinition)));
            } else if (currentSearchButton == 1) {
                TagKey<Block> tag = BlockTagAccessor.callOf(entry.languageDefinition);
                blockTags.clear();
                blockTags.add(tag);
                allBlockUnders = Registries.BLOCK.getEntryList(tag).get().stream().map(entry2 -> entry2.value()).filter(block -> blockUnderList.contains(block)).toList();
            }
            else if (currentSearchButton == 2) {
                RegistryKey<Biome> key = RegistryKey.of(RegistryKeys.BIOME, Identifier.tryParse(entry.languageDefinition));
                biomeKeys.clear();
                biomeKeys.add(key);
            }
        }

        public void moveEntryUp(BlockEntry entry) {
            int index = this.children().indexOf(entry);
            if (index > 0) {
                this.children().set(index, this.children().get(index-1));
                this.children().set(index-1, entry);
                selected.clear();
                selected.add(index-1);

                ListOrderedMap<String, int[]> temp = Main.CONFIG_MANAGER.getCurrentBlockFireColors().get(currentSearchButton);
                Main.CONFIG_MANAGER.getCurrentBlockFireColors().get(currentSearchButton).put(index, temp.get(index-1), temp.getValue(index-1));
                Main.CONFIG_MANAGER.getCurrentBlockFireColors().get(currentSearchButton).put(index-1, temp.get(index), temp.getValue(index));
            }
        }

        public void moveEntryDown(BlockEntry entry) {
            int index = this.children().indexOf(entry);
            if (index < Main.CONFIG_MANAGER.getCurrentBlockFireColors().get(currentSearchButton).size()-1) {
                this.children().set(index, this.children().get(index+1));
                this.children().set(index+1, entry);
                selected.clear();
                selected.add(index+1);

                ListOrderedMap<String, int[]> temp = Main.CONFIG_MANAGER.getCurrentBlockFireColors().get(currentSearchButton);
                Main.CONFIG_MANAGER.getCurrentBlockFireColors().get(currentSearchButton).put(index, temp.get(index+1), temp.getValue(index+1));
                Main.CONFIG_MANAGER.getCurrentBlockFireColors().get(currentSearchButton).put(index+1, temp.get(index), temp.getValue(index));
            }
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
                if (mouseX >= x && mouseX <= x+entryHeight && mouseY >= y && mouseY <= y+entryHeight) {
                    alpha = 1f;
                } else {
                    alpha = 0.5f;
                }
                if (!ChangeFireColorScreen.this.searchScreenListWidget.selected.contains(ChangeFireColorScreen.this.searchScreenListWidget.children().indexOf(this)) && !isCustomized && !ChangeFireColorScreen.this.searchScreenListWidget.selected.stream().anyMatch(index2 -> ChangeFireColorScreen.this.searchScreenListWidget.children().get(index2).isCustomized)) {
                    context.fill(x, y, x+entryHeight, y+entryHeight, new Color(1f/255*44, 1f/255*44, 1f/255*44, alpha).getRGB());
                    context.fill(x+entryHeight/2, y+3, x+entryHeight/2+1, y+entryHeight-3, new Color(1f/255*150, 1f/255*150, 1f/255*150, 1f).getRGB());
                    context.fill(x+3, y+entryHeight/2, x+entryHeight-3, y+entryHeight/2+1, new Color(1f/255*150, 1f/255*150, 1f/255*150, 1f).getRGB());
                    context.drawBorder(x, y, entryHeight, entryHeight, new Color(1f/255*99, 1f/255*99, 1f/255*99, 0.8f).getRGB());
                }
                if (isCustomized && currentSearchButton != 2) {
                    context.fill(x, y, x+entryHeight, y+entryHeight, new Color(1f/255*44, 1f/255*44, 1f/255*44, alpha).getRGB());
                    context.fill(x+entryHeight/2, y+3, x+entryHeight/2+1, y+entryHeight-3, new Color(1f/255*150, 1f/255*150, 1f/255*150, 1f).getRGB());
                    context.drawBorder(x, y, entryHeight, entryHeight, new Color(1f/255*99, 1f/255*99, 1f/255*99, 0.8f).getRGB());

                    if (InputUtil.isKeyPressed(client.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_SHIFT)) {
                        context.fill(x+entryHeight, y+entryHeight, x, y+entryHeight-3, new Color(1f/255*150, 1f/255*150, 1f/255*150, 1f).getRGB());
                    } else {
                        context.fill(x+entryHeight, y, x, y+3, new Color(1f/255*150, 1f/255*150, 1f/255*150, 1f).getRGB());
                    }
                }

            }
            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (mouseX >= x && mouseX <= x+entryHeight && mouseY >= y && mouseY <= y+entryHeight) {
                    System.out.println(isSelected);
                    System.out.println(isFocused());
                    if (isCustomized && currentSearchButton != 2) {
                        if (InputUtil.isKeyPressed(client.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_SHIFT)) {
                            ChangeFireColorScreen.this.searchScreenListWidget.moveEntryDown(this);
                        } else {
                            ChangeFireColorScreen.this.searchScreenListWidget.moveEntryUp(this);
                        }
                    }
                    else if ((!isFocused() && !isSelected)) {
                        this.onAddButton();
                    } else {
                        ChangeFireColorScreen.this.searchScreenListWidget.selected.forEach(index -> ChangeFireColorScreen.this.searchScreenListWidget.getEntry(index).isSelected = false);
                        ChangeFireColorScreen.this.searchScreenListWidget.selected.clear();
                        ChangeFireColorScreen.this.searchScreenListWidget.selected.add(ChangeFireColorScreen.this.searchScreenListWidget.children().indexOf(this));
                        ChangeFireColorScreen.this.searchScreenListWidget.setSelected(this);
                        return false;
                    }
                    return false;
                } {
                    ChangeFireColorScreen.this.searchScreenListWidget.selected.forEach(index -> ChangeFireColorScreen.this.searchScreenListWidget.getEntry(index).isSelected = false);
                    ChangeFireColorScreen.this.searchScreenListWidget.selected.clear();
                    ChangeFireColorScreen.this.searchScreenListWidget.selected.add(ChangeFireColorScreen.this.searchScreenListWidget.children().indexOf(this));
                    ChangeFireColorScreen.this.searchScreenListWidget.setSelected(this);
                    return false;
                }
            }
            void onAddButton() {
                if (currentSearchButton == 0) {
                    allBlockUnders.add(Registries.BLOCK.get(Identifier.tryParse(this.languageDefinition)));
                } else if (currentSearchButton == 1) {
                    TagKey<Block> tag = BlockTagAccessor.callOf(this.languageDefinition);
                    blockTags.add(tag);
                    List<Block> newBlocks = Registries.BLOCK.getEntryList(tag).get().stream()
                            .map(entry2 -> entry2.value())
                            .filter(block -> blockUnderList.contains(block) && !allBlockUnders.contains(block))
                            .toList();

                    allBlockUnders = Stream.concat(allBlockUnders.stream(), newBlocks.stream()).toList();
                } else if (currentSearchButton == 2) {
                    RegistryKey<Biome> key = RegistryKey.of(RegistryKeys.BIOME, Identifier.tryParse(this.languageDefinition));
                    biomeKeys.add(key);
                }
                ChangeFireColorScreen.this.searchScreenListWidget.selected.add(ChangeFireColorScreen.this.searchScreenListWidget.children().indexOf(this));
            }
            @Override
            public Text getNarration() {
                return Text.translatable("narrator.select", this.languageDefinition);
            }
        }
    }
}