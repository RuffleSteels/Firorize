package com.oscimate.oscimate_soulflame.config;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
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

    @Override
    protected void init() {
        parent.isPresetAdd = false;
        this.presetNameField = new TextFieldWidget(this.textRenderer, width/2 - 50, height/2  +30, 100, 20, ScreenTexts.DONE);
        this.fromExistingButton = new ButtonWidget.Builder(Text.literal("From Existing"), button -> System.out.println("A")).dimensions(width/2 - (3*100 + 3*15)/2, height/2-10, 100, 20).build();
        this.fromNewButton = new ButtonWidget.Builder(Text.literal("From New"), button ->  System.out.println("A")).dimensions(width/2 - (3*100 + 3*15)/2 + 115, height/2-10, 100, 20).build();
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
    }

    private boolean pasteTime = false;

    private void fromCode()  {
        if (!pasteTime) {
            fromCodeButton.setMessage(Text.literal("Click again to paste"));
            pasteTime = true;
        } else {
            pasteTime = false;
            try {
                Pair<Pair<ArrayList<ListOrderedMap<String, int[]>>, int[]>, ArrayList<Integer>> newProfile = deserializeFromString(MinecraftClient.getInstance().keyboard.getClipboard());
                addProfile(newProfile);
            } catch (IOException | ClassNotFoundException e) {
                tooltipTime = 30;
            }
        }
    }

    public void addProfile(Pair<Pair<ArrayList<ListOrderedMap<String, int[]>>, int[]>, ArrayList<Integer>> newProfile) {
        if (newProfile != null) {
            parent.presetListWidget.addProfile("Test", newProfile);
        }
    }

    public static Pair<Pair<ArrayList<ListOrderedMap<String, int[]>>, int[]>, ArrayList<Integer>>  deserializeFromString(String str) throws IOException, ClassNotFoundException {
        byte[] data = Base64.getDecoder().decode(str);
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            return (Pair<Pair<ArrayList<ListOrderedMap<String, int[]>>, int[]>, ArrayList<Integer>>) ois.readObject();
        }
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        this.applyBlur(delta);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        if (tooltipTime > 0) {
            context.drawTooltip(textRenderer, Text.literal("Invalid code"), fromCodeButton.getX() + 10, fromCodeButton.getY() - 5);
        }
    }
}
