package com.oscimate.firorize.config;

import com.oscimate.firorize.Colors;
import com.oscimate.firorize.Main;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.awt.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;

public class ColoredCycleButton extends AbstractButton {
    private int index;
    private final ArrayList<Colors> values;
    private final ChangeFireColorScreen instance;
    public boolean isAdding = false;
    private final int x;
    private final int y;
    private final Font font;


    ColoredCycleButton(ChangeFireColorScreen instance, int x, int y, int width, int height, Font font) {
        super(x, y, width, height, Component.literal(""));
        this.font = font;
        this.x = x;
        this.y = y;
        this.instance = instance;
        this.index = 0;
        values = new ArrayList<Colors>();
        values.add(new Colors("CUSTOM", Main.CONFIG_MANAGER.getCurrentBlockFireColors().getRight()));
        for (Map.Entry<String, int[]> entry : Main.CONFIG_MANAGER.getCustomColorPresets().entrySet()) {
            String key = entry.getKey();
            int[] value = entry.getValue();

            values.add(new Colors(key, value));
        }

        this.setMessage(Component.translatable("firorize.config.title.color").append(": " + values.get(index).getName()));

    }

    @Override
    protected void extractContents(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        Minecraft minecraftClient = Minecraft.getInstance();
        // Fade the swatch via the colour's alpha channel (setShaderColor was removed in 1.21.5).
        int swatch = (ChangeFireColorScreen.pickedColor[instance.isOverlay ? 1 : 0].getRGB() & 0xFFFFFF)
                | (Mth.ceil(this.alpha * 255.0F) << 24);
        context.fill(instance.wheelCoords[0] + 50 + 20, instance.hexBoxCoords[1], instance.wheelCoords[0] + instance.wheelRadius*2  + instance.sliderDimensions[0], instance.hexBoxCoords[1] + 20, swatch);

        int i = this.active ? 16777215 : 10526880;
        if (mouseX >= this.getX() && mouseY >= this.getY() && mouseX <= this.getX()+this.getWidth() && mouseY <= this.getY() + this.getHeight()) {

            double dx = instance.wheelRadius+instance.wheelCoords[0] - instance.clickedX;
            double dy = instance.wheelRadius+instance.wheelCoords[0] - instance.clickedY;
            double saturation = Math.sqrt(dx * dx + dy * dy) / instance.wheelRadius;
            double lightness = (instance.sliderClickedY - instance.sliderCoords[1] - instance.sliderPadding) / (instance.sliderDimensions[1] - instance.sliderPadding*2);
            context.outline(this.getX(),this.getY(), this.getWidth(), this.getHeight(), saturation < 0.25 && lightness < 0.25 ? Color.BLACK.getRGB() : Color.white.getRGB());
            i = 10526880;
        }
        if (!isAdding) {
            context.centeredText(minecraftClient.font, getMessage(), getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2, i | Mth.ceil(this.alpha * 255.0F) << 24);
        }

        if (instance.cycleTooltipTimer > 0) {
            context.setTooltipForNextFrame(font, Component.translatable(tooltip), instance.invisibleTextFieldWidget.getX() + 10, instance.invisibleTextFieldWidget.getY() + instance.invisibleTextFieldWidget.getHeight() + 5);
        }
    }

    private boolean isWhite = false;
    private boolean removing = false;

    private final String[] tooltips = new String[]{"firorize.config.tooltip.empty", "firorize.config.tooltip.exists"};
    private String tooltip = "";

    /** Rebuilds the cycle list from the persisted custom colour presets (used after undo/redo). */
    public void rebuildValues() {
        values.clear();
        values.add(new Colors("CUSTOM", Main.CONFIG_MANAGER.getCurrentBlockFireColors().getRight()));
        for (Map.Entry<String, int[]> entry : Main.CONFIG_MANAGER.getCustomColorPresets().entrySet()) {
            values.add(new Colors(entry.getKey(), entry.getValue()));
        }
        isAdding = false;
        instance.invisibleTextFieldWidget.visible = false;
        this.setPosition(x, y);
        setIndex(0);
    }

    public void addColor() {
        if (index > 0) {
            // Deleting a custom colour preset is undoable.
            instance.historyBefore();
            removing = true;
            cycle(-1);
            Main.CONFIG_MANAGER.getCustomColorPresets().remove(values.get(index+1).getName());
            values.remove(index+1);
            Main.CONFIG_MANAGER.save();
            instance.historyAfterPreset();
        } else {
            if (isAdding) {
                if (this.values.stream().noneMatch(colors -> colors.getName().equalsIgnoreCase(instance.invisibleTextFieldWidget.getValue())) && !instance.invisibleTextFieldWidget.getValue().isEmpty()) {
                    instance.invisibleTextFieldWidget.visible = false;
                    this.setPosition(x, y);
                    isAdding = false;
                    String string = instance.invisibleTextFieldWidget.getValue();
                    int[] ints = new int[]{ChangeFireColorScreen.pickedColor[0].getRGB(), ChangeFireColorScreen.pickedColor[1].getRGB()};

                    // Saving a new custom colour preset is undoable.
                    instance.historyBefore();
                    Main.CONFIG_MANAGER.getCustomColorPresets().put(string, ints);
                    values.add(new Colors(string, ints));
                    this.setIndex(values.size() - 1);
                    Main.CONFIG_MANAGER.save();
                    instance.historyAfterPreset();
                    isWhite = true;
                    instance.invisibleTextFieldWidget.setValue("");
                } else if (instance.invisibleTextFieldWidget.getValue().isEmpty()) {
                    tooltip = tooltips[0];
                    instance.cycleTooltipTimer = 30;
                } else {
                    tooltip = tooltips[1];
                    instance.cycleTooltipTimer = 30;
                }
            } else {
                instance.invisibleTextFieldWidget.visible = true;
                this.setPosition(-100, -100);
                isAdding = true;
            }
        }
    }

    @Override
    public void onClick(net.minecraft.client.input.MouseButtonEvent click, boolean doubled) {
        super.onClick(click, doubled);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {

    }

    @Override
    public void onPress(net.minecraft.client.input.InputWithModifiers input) {
        if (input.hasShiftDown()) {
            this.cycle(-1);
        } else {
            this.cycle(1);
        }
    }

    private void cycle(int amount) {
        if (!isAdding && values.size() > 1) {
            instance.isCycling = true;
            this.setIndex(Math.floorMod(this.index + amount, this.values.size()));

            if (this.index == 1 && !removing) {
                instance.tempColor = ChangeFireColorScreen.pickedColor.clone();
            }
            removing = false;
            if (this.index == 0 && !isWhite) {
                ChangeFireColorScreen.pickedColor = instance.tempColor == null ? new Color[]{instance.baseColor[0], instance.baseColor[1]} : instance.tempColor;
            } else {
                isWhite = false;
                ChangeFireColorScreen.pickedColor = new Color[]{new Color(this.values.get(index).getColors()[0]), new Color(this.values.get(index).getColors()[1])};
            }
            int RGB = ChangeFireColorScreen.pickedColor[instance.isOverlay ? 1:0].getRGB();
            instance.textFieldWidget.setValue("#"+Integer.toHexString(RGB).substring(2));
            instance.updateCursor("#"+Integer.toHexString(RGB).substring(2));
            instance.isCycling = false;
        }
    }
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount > 0.0) {
            this.cycle(-1);
        } else if (verticalAmount < 0.0) {
            this.cycle(1);
        }

        return true;
    }

    public void setIndex(int index) {
        if (index == 0) {
            instance.addColorButton.setTooltip(Tooltip.create(Component.translatable("firorize.config.tooltip.addColorPresetButton")));
            instance.addColorButton.setTooltipDelay(Duration.ofMillis(750L));
            instance.addColorButton.setMessage(Component.literal("+"));
        } else {
            instance.addColorButton.setTooltip(Tooltip.create(Component.translatable("firorize.config.tooltip.removeColorPresetButton")));
            instance.addColorButton.setTooltipDelay(Duration.ofMillis(750L));
            instance.addColorButton.setMessage(Component.literal("x"));
        }
        this.setMessage(Component.translatable("firorize.config.title.color").append(": " + this.values.get(index).getName()));
        this.index = index;
    }

    public static ColoredCycleButton.Builder builder() {
        return new ColoredCycleButton.Builder();
    }


    @Environment(EnvType.CLIENT)
    public static class Builder {
        private ArrayList<Colors> values = new ArrayList<>();


        public Builder() {
        }

        public ColoredCycleButton.Builder values(ArrayList<Colors> values) {
            this.values = values;
            return this;
        }


        public ColoredCycleButton build(ChangeFireColorScreen instance, int x, int y, int width, int height, Font font) {
                return new ColoredCycleButton(
                        instance,
                        x,
                        y,
                        width,
                        height,
                        font
                );
            }
    }
}
