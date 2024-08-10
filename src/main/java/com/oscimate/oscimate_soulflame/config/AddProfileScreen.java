package com.oscimate.oscimate_soulflame.config;

import com.oscimate.oscimate_soulflame.Main;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.collections4.map.ListOrderedMap;

import java.io.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Map;

public class AddProfileScreen extends Screen {
    private final ChangeFireColorScreen parent;
    protected AddProfileScreen(ChangeFireColorScreen parent) {
        super(Text.translatable("options.videoTitle"));
        this.parent = parent;
        System.out.println("EEKA");
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
        this.fromExistingButton = new ButtonWidget.Builder(Text.literal("From Existing"), button -> addFromExisting()).dimensions(width/2 - (3*100 + 3*15)/2, height/2-10, 100, 20).build();
        this.fromNewButton = new ButtonWidget.Builder(Text.literal("From New"), button ->  addFromNew()).dimensions(width/2 - (3*100 + 3*15)/2 + 115, height/2-10, 100, 20).build();
        this.fromCodeButton = new ButtonWidget.Builder(Text.literal("From Code"), button ->  fromCode()).dimensions(width/2 - (3*100 + 3*15)/2 + 230, height/2-10, 100, 20).build();

        presetNameField.setMaxLength(Integer.MAX_VALUE);
        this.addDrawableChild(presetNameField);
        this.addDrawableChild(fromExistingButton);
        this.addDrawableChild(fromNewButton);
        this.addDrawableChild(fromCodeButton);
        super.init();
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
            fromCodeButton.setMessage(Text.literal("Click again to paste"));
            pasteTime = true;
        } else {
            pasteTime = false;
            KeyValuePair<KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>, int[]>, ArrayList<Integer>> newProfile = deserializeFromString(MinecraftClient.getInstance().keyboard.getClipboard());
            fromCodeButton.setMessage(Text.literal("From Code"));
            if (newProfile == null) {
                tooltipTime = 30;
            } else {
                addProfile(newProfile);
            }
        }
    }

    public void addFromExisting() {
        addProfile(KeyValuePair.of(Main.CONFIG_MANAGER.getCurrentBlockFireColors(), Main.CONFIG_MANAGER.getPriorityOrder()));
    }

    public void addFromNew() {
        ArrayList<ListOrderedMap<String, int[]>> temp = new ArrayList<ListOrderedMap<String, int[]>>();
        ListOrderedMap<String, int[]> soulStuff = new ListOrderedMap<>();
        soulStuff.put("minecraft:soul_sand", new int[]{-15372685,-13404045});
        soulStuff.put("minecraft:soul_soil", new int[]{-15372685,-13404045});
        temp.add(soulStuff);
        temp.add(new ListOrderedMap<String, int[]>());
        temp.add(new ListOrderedMap<String, int[]>());
        ArrayList<Integer> temp2 = new ArrayList<>();
        temp2.add(0);
        temp2.add(1);
        temp2.add(2);

        KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>, int[]> mapp = KeyValuePair.of(temp, new int[]{-6267112,-4682209});

        addProfile(KeyValuePair.of(mapp, temp2));
    }



    @Override
    public void close() {
        client.setScreen(parent);
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        int e = client.getWindow().calculateScaleFactor(4, client.forcesUnicodeFont());
        client.getWindow().setScaleFactor((double)e);
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
                int e = client.getWindow().calculateScaleFactor(4, client.forcesUnicodeFont());
                client.getWindow().setScaleFactor((double)e);
                client.setScreen(parent);
            }
        }
    }

    public static KeyValuePair<KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>, int[]>, ArrayList<Integer>> deserializeFromString(String str) {
        try {
            // Decode Base64
            byte[] data = Base64.getDecoder().decode(str);

            // Deserialize object
            try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
                 ObjectInputStream ois = new ObjectInputStream(bais)) {
                Object obj = ois.readObject();

                // Check type
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
            // Log the error if needed
            e.printStackTrace();
        }
        return null; // Return null if Base64 decoding fails or object type does not match
    }
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderPanoramaBackground(context, delta);
        this.applyBlur(delta);
        this.renderDarkening(context);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        if (tooltipTime > 0) {
            context.drawTooltip(textRenderer, Text.literal("Invalid code"), fromCodeButton.getX() + 10, fromCodeButton.getY() - 5);
        }
        if (profileNameTooltipTime > 0) {
            context.drawTooltip(textRenderer, Text.literal(profileNameTooltip), presetNameField.getX() + 10, presetNameField.getY() + presetNameField.getHeight() + 5);
        }
    }
}
