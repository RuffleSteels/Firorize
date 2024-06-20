package com.oscimate.oscimate_soulflame.config;

import com.mojang.blaze3d.systems.RenderSystem;
import com.oscimate.oscimate_soulflame.GameRendererSetting;
import com.oscimate.oscimate_soulflame.Main;
import com.oscimate.oscimate_soulflame.mixin.fire_overlays.client.GameRendererMixin;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.FloatSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import dev.isxander.yacl3.gui.controllers.ColorController;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.passive.ParrotEntity;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static com.oscimate.oscimate_soulflame.config.ConfigScreen.windowHeight;

public class ChangeFireColorScreen extends Screen {
    private Screen parent;
    private boolean clicked = false;
    private boolean sliderClicked = false;

    private double clickedX = 95.0;

    private double clickedY = 95.0;


    private String hexCode = "#ffffff";
    private Color pickedColor = new Color(Color.decode(hexCode).getRGB(), true);
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

    protected ChangeFireColorScreen(Screen parent) {
        super(Text.translatable("options.videoTitle"));
        this.parent = parent;
    }
    public void onClose() {
        client.setScreen(parent);
    }

    private TextFieldWidget textFieldWidget;

    @Override
    protected void init() {
        this.addDrawableChild(new ButtonWidget.Builder(ScreenTexts.DONE, button -> onClose()).dimensions(width / 2 - 100, height/2 + windowHeight/2 + 20, 200, 20).build());
        textFieldWidget = new TextFieldWidget(this.textRenderer, hexBoxCoords[0], hexBoxCoords[1], wheelRadius, 20, ScreenTexts.DONE);
        this.addDrawableChild(this.textFieldWidget);
        textFieldWidget.setChangedListener(this::updateCursor);
        updateCursor(this.hexCode);
        super.init();
    }

    private void updateCursor(String hexCode) {
        if (!clicked && !sliderClicked) {
            Pattern pattern = Pattern.compile("^#([A-Fa-f0-9]{6})$");
            if (pattern.matcher(hexCode).matches()) {
                pickedColor = new Color(Color.decode(hexCode).getRGB(), true);

                float[] HSB = Color.RGBtoHSB(pickedColor.getRed(), pickedColor.getGreen(), pickedColor.getBlue(), null);
                hue = HSB[0];
                saturation = HSB[1];
                double theta = Math.toRadians(90+HSB[0]*360);
                double radius = HSB[1] * wheelRadius;
                int x = (int) (120 + radius * Math.cos(theta));
                int y = (int) (120 + radius * Math.sin(theta));

                sliderClickedY = ((1 - HSB[2]) * (sliderDimensions[1] - sliderPadding*2)) + sliderCoords[1] + sliderPadding;
                clickedX = x;
                clickedY = y;
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

            int RGB = Color.HSBtoRGB((float) hue, (float) saturation, (float) lightness);

            textFieldWidget.setText("#"+Integer.toHexString(RGB).substring(2));
            pickedColor = new Color(RGB, true);
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        clicked = false;
        sliderClicked = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double selectSpace = (double) cursorDimensions / 2;
//        if (mouseX >= sliderClickedX - selectSpace && mouseY >= sliderClickedY - selectSpace && mouseX <= sliderClickedX+selectSpace && mouseY <= sliderClickedY+selectSpace) {
        if (mouseX >= sliderCoords[0] && mouseX <= sliderCoords[0]+sliderDimensions[0] && mouseY >= sliderCoords[1] + sliderPadding && mouseY <= sliderCoords[1] + sliderDimensions[1] - sliderPadding) {
            sliderClicked = true;
            mouseDragged(mouseX, mouseY, button, 0, 0);
        } else if (mouseX >= clickedX-selectSpace && mouseY >= clickedY-selectSpace && mouseX <= clickedX+selectSpace && mouseY <= clickedY+selectSpace) {
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
            float[] HSB = Color.RGBtoHSB(pickedColor.getRed(), pickedColor.getGreen(), pickedColor.getBlue(), null);

            int RGB = Color.HSBtoRGB((float) hue, (float) saturation, (float) lightness);

            textFieldWidget.setText("#"+Integer.toHexString(RGB).substring(2));
            pickedColor = new Color(RGB, true);
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

        context.fill(wheelCoords[0] + wheelRadius, hexBoxCoords[1], wheelCoords[0] + wheelRadius*2 + 20 + sliderDimensions[0], hexBoxCoords[1] + 20, pickedColor.getRGB());

        Sprite sprite = Main.BLANK_FIRE_1.get();
        RenderSystem.setShaderTexture(0, sprite.getAtlasId());
        RenderSystem.setShader(GameRendererSetting::getRenderTypeCustomTint);
        Matrix4f matrix4f2 = context.getMatrices().peek().getPositionMatrix();
        BufferBuilder bufferBuilder2 = Tessellator.getInstance().getBuffer();
        bufferBuilder2.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE);
        bufferBuilder2.vertex(matrix4f2, (float)200, (float)100, (float)100).color(pickedColor.getRed(), pickedColor.getGreen(), pickedColor.getBlue(), 255).texture(sprite.getMinU(), sprite.getMinV()).next();
        bufferBuilder2.vertex(matrix4f2, (float)200, (float)260, (float)100).color(pickedColor.getRed(), pickedColor.getGreen(), pickedColor.getBlue(), 255).texture(sprite.getMinU(), sprite.getMaxV()).next();
        bufferBuilder2.vertex(matrix4f2, (float)360, (float)260, (float)100).color(pickedColor.getRed(), pickedColor.getGreen(), pickedColor.getBlue(), 255).texture(sprite.getMaxU(), sprite.getMaxV()).next();
        bufferBuilder2.vertex(matrix4f2, (float)360, (float)100, (float)100).color(pickedColor.getRed(), pickedColor.getGreen(), pickedColor.getBlue(), 255).texture(sprite.getMaxU(), sprite.getMinV()).next();
        BufferRenderer.drawWithGlobalProgram(bufferBuilder2.end());

//        context.drawSprite(200, 30, 100, 160, 160, Main.BLANK_FIRE_1.get());

        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.depthFunc(515);
    }
}