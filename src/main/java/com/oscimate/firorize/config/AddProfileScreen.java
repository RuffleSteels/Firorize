package com.oscimate.firorize.config;

import com.oscimate.firorize.Main;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import org.apache.commons.collections4.map.ListOrderedMap;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.time.Duration;
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
    private final String[] profileNameTooltips = new String[]{"firorize.config.tooltip.empty", "firorize.config.tooltip.exists"};
    private String profileNameTooltip = "";
    private int profileNameTooltipTime = 0;

    @Override
    protected void init() {
        parent.isPresetAdd = false;
        this.presetNameField = new PlaceholderField(this.textRenderer, width/2 - 65, height/2  - 25, 130, 20, ScreenTexts.DONE);
        this.fromExistingButton = new ButtonWidget.Builder(Text.translatable("firorize.config.button.profileFromCurrentButton"), button -> addFromExisting()).dimensions(width/2 - 60 - 15 - 120, height/2+5, 120, 20).build();
        this.fromNewButton = new ButtonWidget.Builder(Text.translatable("firorize.config.button.profileFromNewButton"), button ->  addFromNew()).dimensions(width/2 - 60, height/2+5, 120, 20).build();
        this.fromCodeButton = new ButtonWidget.Builder(Text.translatable("firorize.config.button.profileFromCodeButton1"), button ->  fromCode()).dimensions(width/2 + 60 + 15, height/2+5, 120, 20).build();

        presetNameField.setMaxLength(Integer.MAX_VALUE);
        this.addDrawableChild(new ButtonWidget.Builder(Text.literal("x"), button -> close()).dimensions(width/2 + 60 + 15 + 100, height/2  - 25, 20, 20).build());
        this.addDrawableChild(presetNameField);
        this.addDrawableChild(fromExistingButton);
        this.addDrawableChild(fromNewButton);
        this.addDrawableChild(fromCodeButton);
        super.init();
        Main.inConfig = true;

        fromExistingButton.setTooltip(Tooltip.of(Text.translatable("firorize.config.tooltip.profileFromCurrentButton")));
        fromExistingButton.setTooltipDelay(Duration.ofMillis(750L));
        fromNewButton.setTooltip(Tooltip.of(Text.translatable("firorize.config.tooltip.profileFromNewButton")));
        fromNewButton.setTooltipDelay(Duration.ofMillis(750L));
        fromCodeButton.setTooltip(Tooltip.of(Text.translatable("firorize.config.tooltip.profileFromCodeButton")));
        fromCodeButton.setTooltipDelay(Duration.ofMillis(750L));
        presetNameField.setPlaceholder(Text.translatable("firorize.config.placeholder.newProfileNameField"));
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
        Main.setScale(width, height, client);
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
                Main.setScale(width, height, client);
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


        this.renderDarkening(context);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, -500);
        parent.render(context, 0, 0, delta);
        context.getMatrices().pop();
        context.getMatrices().push();

        context.getMatrices().translate(0, 0, -490);


        super.render(context, mouseX, mouseY, delta);

        if (tooltipTime > 0) {
            context.drawTooltip(textRenderer, Text.translatable("firorize.config.tooltip.invalidCode"), fromCodeButton.getX() + 10, fromCodeButton.getY() - 5);
        }
        if (profileNameTooltipTime > 0) {
            context.drawTooltip(textRenderer, Text.translatable(profileNameTooltip), presetNameField.getX() + 10, presetNameField.getY() + presetNameField.getHeight() + 5);
        }

        context.getMatrices().pop();
    }
}
