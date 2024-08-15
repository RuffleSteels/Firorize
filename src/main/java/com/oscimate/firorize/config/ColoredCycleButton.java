package com.oscimate.firorize.config;

import com.mojang.blaze3d.systems.RenderSystem;
import com.oscimate.firorize.Colors;
import com.oscimate.firorize.Main;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.awt.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;

public class ColoredCycleButton extends PressableWidget {
    private int index;
    private final ArrayList<Colors> values;
    private final ChangeFireColorScreen instance;
    public boolean isAdding = false;
    private final int x;
    private final int y;
    private final TextRenderer textRenderer;


    ColoredCycleButton(ChangeFireColorScreen instance, int x, int y, int width, int height, TextRenderer textRenderer) {
        super(x, y, width, height, Text.literal(""));
        this.textRenderer = textRenderer;
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

        this.setMessage(Text.translatable("firorize.config.title.color").append(": " + values.get(index).getName()));

    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        context.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();

        context.fill(instance.wheelCoords[0] + 50 + 20, instance.hexBoxCoords[1], instance.wheelCoords[0] + instance.wheelRadius*2  + instance.sliderDimensions[0], instance.hexBoxCoords[1] + 20, ChangeFireColorScreen.pickedColor[instance.isOverlay ? 1:0].getRGB());

        context.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int i = this.active ? 16777215 : 10526880;
        if (mouseX >= this.getX() && mouseY >= this.getY() && mouseX <= this.getX()+this.getWidth() && mouseY <= this.getY() + this.getHeight()) {

            double dx = instance.wheelRadius+instance.wheelCoords[0] - instance.clickedX;
            double dy = instance.wheelRadius+instance.wheelCoords[0] - instance.clickedY;
            double saturation = Math.sqrt(dx * dx + dy * dy) / instance.wheelRadius;
            double lightness = (instance.sliderClickedY - instance.sliderCoords[1] - instance.sliderPadding) / (instance.sliderDimensions[1] - instance.sliderPadding*2);
            context.drawBorder(this.getX(),this.getY(), this.getWidth(), this.getHeight(), saturation < 0.25 && lightness < 0.25 ? Color.BLACK.getRGB() : Color.white.getRGB());
            i = 10526880;
        }
        if (!isAdding) {
            this.drawMessage(context, minecraftClient.textRenderer, i | MathHelper.ceil(this.alpha * 255.0F) << 24);
        }

        if (instance.cycleTooltipTimer > 0) {
            context.drawTooltip(textRenderer, Text.translatable(tooltip), instance.invisibleTextFieldWidget.getX() + 10, instance.invisibleTextFieldWidget.getY() + instance.invisibleTextFieldWidget.getHeight() + 5);
        }
    }

    private boolean isWhite = false;
    private boolean removing = false;

    private final String[] tooltips = new String[]{"firorize.config.tooltip.empty", "firorize.config.tooltip.exists"};
    private String tooltip = "";

    public void addColor() {
        if (index > 0) {
            removing = true;
            cycle(-1);
            Main.CONFIG_MANAGER.getCustomColorPresets().remove(values.get(index+1).getName());
            values.remove(index+1);
            Main.CONFIG_MANAGER.save();
        } else {
            if (isAdding) {
                if (this.values.stream().noneMatch(colors -> colors.getName().equalsIgnoreCase(instance.invisibleTextFieldWidget.getText())) && !instance.invisibleTextFieldWidget.getText().isEmpty()) {
                    instance.invisibleTextFieldWidget.visible = false;
                    this.setPosition(x, y);
                    isAdding = false;
                    String string = instance.invisibleTextFieldWidget.getText();
                    int[] ints = new int[]{ChangeFireColorScreen.pickedColor[0].getRGB(), ChangeFireColorScreen.pickedColor[1].getRGB()};

                    Main.CONFIG_MANAGER.getCustomColorPresets().put(string, ints);
                    values.add(new Colors(string, ints));
                    this.setIndex(values.size() - 1);
                    Main.CONFIG_MANAGER.save();
                    isWhite = true;
                    instance.invisibleTextFieldWidget.setText("");
                } else if (instance.invisibleTextFieldWidget.getText().isEmpty()) {
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
    public void onClick(double mouseX, double mouseY) {
        super.onClick(mouseX, mouseY);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {

    }

    @Override
    public void onPress() {
        if (Screen.hasShiftDown()) {
            this.cycle(-1);
        } else {
            this.cycle(1);
        }
        instance.setRedo(false);
    }

    private void cycle(int amount) {
        if (!isAdding && values.size() > 1) {
            instance.isCycling = true;
            this.setIndex(MathHelper.floorMod(this.index + amount, this.values.size()));

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
            instance.textFieldWidget.setText("#"+Integer.toHexString(RGB).substring(2));
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
            instance.addColorButton.setTooltip(Tooltip.of(Text.translatable("firorize.config.tooltip.addColorPresetButton")));
            instance.addColorButton.setTooltipDelay(Duration.ofMillis(750L));
            instance.addColorButton.setMessage(Text.literal("+"));
        } else {
            instance.addColorButton.setTooltip(Tooltip.of(Text.translatable("firorize.config.tooltip.removeColorPresetButton")));
            instance.addColorButton.setTooltipDelay(Duration.ofMillis(750L));
            instance.addColorButton.setMessage(Text.literal("x"));
        }
        this.setMessage(Text.translatable("firorize.config.title.color").append(": " + this.values.get(index).getName()));
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


        public ColoredCycleButton build(ChangeFireColorScreen instance, int x, int y, int width, int height, TextRenderer textRenderer) {
                return new ColoredCycleButton(
                        instance,
                        x,
                        y,
                        width,
                        height,
                        textRenderer
                );
            }
    }
}
