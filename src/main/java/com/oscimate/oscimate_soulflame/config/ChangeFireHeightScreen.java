package com.oscimate.oscimate_soulflame.config;

import com.mojang.blaze3d.systems.RenderSystem;
import com.oscimate.oscimate_soulflame.Main;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.*;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector2f;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.w3c.dom.css.RGBColor;

import java.awt.*;

import static com.oscimate.oscimate_soulflame.config.ConfigScreen.windowHeight;

public class ChangeFireHeightScreen extends Screen {

    private Screen parent;

    private int counter = 16;
    private int ticks = 0;

    protected ChangeFireHeightScreen(Screen parent) {
        super(Text.translatable("options.videoTitle"));
        this.parent = parent;
    }
    public void onClose() {
        client.setScreen(parent);
    }
    @Override
    protected void init() {
        FireHeightSliderWidget customTimeSliderWidget = new FireHeightSliderWidget(this.width / 2 - 75, 10, 150, 20, Text.literal("Height"), (double) Main.CONFIG_MANAGER.getCurrentFireHeightSlider() /100);
        this.addDrawableChild(customTimeSliderWidget);
        this.addDrawableChild(new ButtonWidget.Builder(ScreenTexts.DONE, button -> onClose()).dimensions(width / 2 - 100, height/2 + windowHeight/2 + 20, 200, 20).build());
        super.init();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackgroundTexture(context);
        MatrixStack matrixStack = context.getMatrices();
        Matrix4f matrix = matrixStack.peek().getPositionMatrix();
        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorTexProgram);
        RenderSystem.setShaderTexture(0, new Identifier("textures/block/fire_1.png"));
        float f = 0.0F;
        float a = 1.0F;
        float i = 1/32F * (counter-1);
        float j = 1/32F * counter;
        for (int r = 0; r < 2; ++r) {
            matrixStack.push();
            matrixStack.translate(1000.0F, 0.0F, 0.0f);
            bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE);
            bufferBuilder.vertex(matrix, (float)10, (float)500, 0.0F).color(1.0f, 1.0f, 1.0f, 0.9f).texture(a, j).next();
            bufferBuilder.vertex(matrix, (float)500, (float)500, 0.0F).color(1.0f, 1.0f, 1.0f, 0.9f).texture(f, j).next();
            bufferBuilder.vertex(matrix, (float)500, (float)10, 0.0F).color(1.0f, 1.0f, 1.0f, 0.9f).texture(f, i).next();
            bufferBuilder.vertex(matrix, (float)10, (float)10, 0.0F).color(1.0f, 1.0f, 1.0f, 0.9f).texture(a, i).next();
            BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
            RenderSystem.disableBlend();
            matrixStack.pop();
        }

        super.render(context, mouseX, mouseY, delta);

        if (ticks % 4 == 0) counter++;
        ticks++;
        if (counter > 32) {
            counter = 0;
            ticks = 0;
        }
    }

}