package com.oscimate.firorize.config;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;
import com.oscimate.firorize.GameRendererSetting;
import com.oscimate.firorize.Main;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.RotatingCubeMapRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.*;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import org.apache.commons.collections4.map.ListOrderedMap;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import java.io.*;
import java.util.ArrayList;
import java.util.Base64;

public class AddProfileScreen extends Screen {
    private final ChangeFireColorScreen parent;
    protected AddProfileScreen(ChangeFireColorScreen parent) {
        super(Text.translatable("options.videoTitle"));
        this.parent = parent;
    }
    public  ButtonWidget fromExistingButton;
    public ButtonWidget fromNewButton;
    public  ButtonWidget fromCodeButton;
    public TextFieldWidget presetNameField;
    private String[] profileNameTooltips = new String[]{"Please enter your desired profile name", "This profile name already exists"};
    private String profileNameTooltip = "";
    private int profileNameTooltipTime = 0;

    @Override
    protected void init() {
        parent.isPresetAdd = false;
        this.presetNameField = new TextFieldWidget(this.textRenderer, width/2 - 50, height/2  +30, 100, 20, ScreenTexts.DONE);
        this.fromExistingButton = new ButtonWidget.Builder(Text.translatable("firorize.config.button.profileFromCurrentButton"), button -> addFromExisting()).dimensions(width/2 - (3*100 + 3*15)/2, height/2-10, 100, 20).build();
        this.fromNewButton = new ButtonWidget.Builder(Text.translatable("firorize.config.button.profileFromNewButton"), button ->  addFromNew()).dimensions(width/2 - (3*100 + 3*15)/2 + 115, height/2-10, 100, 20).build();
        this.fromCodeButton = new ButtonWidget.Builder(Text.translatable("firorize.config.button.profileFromCodeButton1"), button ->  fromCode()).dimensions(width/2 - (3*100 + 3*15)/2 + 230, height/2-10, 100, 20).build();

        presetNameField.setMaxLength(Integer.MAX_VALUE);
        this.addDrawableChild(presetNameField);
        this.addDrawableChild(fromExistingButton);
        this.addDrawableChild(fromNewButton);
        this.addDrawableChild(fromCodeButton);
        super.init();
        Main.inConfig = true;
    }

    private int tooltipTime = 0;

    @Override
    public void tick() {
        super.tick();
        if (tooltipTime > 0) {
            tooltipTime--;
        }
        if (profileNameTooltipTime > 0) {
            profileNameTooltipTime--;
        }
    }

    private boolean pasteTime = false;

    private void fromCode()  {
        if (!pasteTime) {
            fromCodeButton.setMessage(Text.translatable("firorize.config.button.profileFromCodeButton2"));
            pasteTime = true;
        } else {
            pasteTime = false;
            KeyValuePair<KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>, int[]>, ArrayList<Integer>> newProfile = deserializeFromString(MinecraftClient.getInstance().keyboard.getClipboard());
            fromCodeButton.setMessage(Text.translatable("firorize.config.button.profileFromCodeButton1"));
            if (newProfile == null) {
                tooltipTime = 30;
            } else {
                addProfile(newProfile);
            }
        }
    }

    public void addFromExisting() {
        ArrayList<Integer> tempPriorityOrder = new ArrayList<>(Main.CONFIG_MANAGER.getPriorityOrder());
        KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>, int[]> tempCurrentColors = Main.CONFIG_MANAGER.getCurrentBlockFireColors();
        ArrayList<ListOrderedMap<String, int[]>> tempStuff = new ArrayList<>(tempCurrentColors.getLeft());

        addProfile(KeyValuePair.of(KeyValuePair.of(tempStuff, tempCurrentColors.getRight().clone()), tempPriorityOrder));
    }

    public void addFromNew() {
        addProfile(Main.CONFIG_MANAGER.getDefaultProfile());
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        client.getWindow().setScaleFactor(2);
        super.resize(client, client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight());
    }

    public void addProfile(KeyValuePair<KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>, int[]>, ArrayList<Integer>> newProfile) {
        if (newProfile != null) {
            if (presetNameField.getText().isEmpty()) {
                profileNameTooltip = profileNameTooltips[0];
                profileNameTooltipTime = 30;
            } else if (Main.CONFIG_MANAGER.getFireColorPresets().keySet().stream().anyMatch(presetNameField.getText()::equalsIgnoreCase)) {
                profileNameTooltip = profileNameTooltips[1];
                profileNameTooltipTime = 30;
            } else {
                parent.presetListWidget.addProfile(presetNameField.getText(), newProfile);
                client.getWindow().setScaleFactor(2);
                client.setScreen(parent);
            }
        }
    }

    public static KeyValuePair<KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>, int[]>, ArrayList<Integer>> deserializeFromString(String str) {
        try {
            byte[] data = Base64.getDecoder().decode(str);

            try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
                 ObjectInputStream ois = new ObjectInputStream(bais)) {
                Object obj = ois.readObject();

                if (obj instanceof KeyValuePair<?, ?>) {
                    KeyValuePair<?, ?> pair = (KeyValuePair<?, ?>) obj;
                    if (pair.getLeft() instanceof KeyValuePair<?, ?>) {
                        KeyValuePair<?, ?> innerPair = (KeyValuePair<?, ?>) pair.getLeft();
                        if (innerPair.getLeft() instanceof ArrayList && innerPair.getRight() instanceof int[]
                                && pair.getRight() instanceof ArrayList) {
                            return (KeyValuePair<KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>, int[]>, ArrayList<Integer>>) obj;
                        }
                    }
                }
            }
        } catch (IllegalArgumentException | ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        this.applyBlur(delta);
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 1000);
        this.renderDarkening(context);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        parent.render(context, 0, 0, delta);
        super.render(context, mouseX, mouseY, delta);
        if (tooltipTime > 0) {
            context.drawTooltip(textRenderer, Text.translatable("firorize.config.tooltip.invalidCode"), fromCodeButton.getX() + 10, fromCodeButton.getY() - 5);
        }
        if (profileNameTooltipTime > 0) {
            context.drawTooltip(textRenderer, Text.literal(profileNameTooltip), presetNameField.getX() + 10, presetNameField.getY() + presetNameField.getHeight() + 5);
        }

        context.getMatrices().pop();
    }
}
