package com.oscimate.firorize.config;

import com.mojang.blaze3d.systems.RenderSystem;
import com.oscimate.firorize.Main;
import com.oscimate.firorize.mixin.fire_overlays.client.GameRendererMixin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.*;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

public class ChangeFireHeightScreen extends Screen {
    private Screen parent;

    protected ChangeFireHeightScreen(Screen parent) {
        super(Text.translatable("options.videoTitle"));
        this.parent = parent;
    }
    public void onClose() {
        Main.CONFIG_MANAGER.save();

        client.setScreen(parent);
    }
    @Override
    protected void init() {
        FireHeightSliderWidget customTimeSliderWidget = new FireHeightSliderWidget(this.width / 2 - 75, 10, 150, 20, Text.translatable("firorize.config.title.height"), (double) Main.CONFIG_MANAGER.getCurrentFireHeightSlider() /100);
        this.addDrawableChild(customTimeSliderWidget);
        this.addDrawableChild(new ButtonWidget.Builder(ScreenTexts.DONE, button -> onClose()).dimensions(width / 2 - 100, 50, 200, 20).build());
        super.init();
    }
    @Override
    public void close() {
        onClose();
    }
    @Override
    public void resize(MinecraftClient client, int width, int height) {
//        Main.setScale(width, height, client);
        super.resize(client, client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight());
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackgroundTexture(context);
        super.render(context, mouseX, mouseY, delta);

        MatrixStack matrixStack = context.getMatrices();
        matrixStack.push();
        GameRenderer gameRenderer = MinecraftClient.getInstance().gameRenderer;
        gameRenderer.loadProjectionMatrix(gameRenderer.getBasicProjectionMatrix(((GameRendererMixin)gameRenderer).callGetFov(gameRenderer.getCamera(), MinecraftClient.getInstance().getTickDelta(), false)));
        matrixStack.loadIdentity();


        var modelView = RenderSystem.getModelViewStack();
        modelView.push();
        modelView.loadIdentity();
        RenderSystem.applyModelViewMatrix();

        matrixStack.translate(0.0, FireHeightSliderWidget.getFireHeight(Main.CONFIG_MANAGER.getCurrentFireHeightSlider()), 0.0);

        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        RenderSystem.setShader(GameRenderer::getPositionColorTexProgram);
        RenderSystem.depthFunc(519);
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        Sprite sprite = ModelLoader.FIRE_1.getSprite();
        RenderSystem.setShaderTexture(0, sprite.getAtlasId());
        float f = sprite.getMinU();
        float g = sprite.getMaxU();
        float h = (f + g) / 2.0F;
        float i = sprite.getMinV();
        float j = sprite.getMaxV();
        float k = (i + j) / 2.0F;
        float l = sprite.getAnimationFrameDelta();
        float m = MathHelper.lerp(l, f, h);
        float n = MathHelper.lerp(l, g, h);
        float o = MathHelper.lerp(l, i, k);
        float p = MathHelper.lerp(l, j, k);
        float q = 1.0F;

        for(int r = 0; r < 2; ++r) {
            matrixStack.push();
            float s = -0.5F;
            float t = 0.5F;
            float u = -0.5F;
            float v = 0.5F;
            float w = -0.5F;
            matrixStack.translate((float)(-(r * 2 - 1)) * 0.24F, -0.3F, 0.0F);
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(r * 2 - 1) * 10.0F));
            Matrix4f matrix4f = matrixStack.peek().getPositionMatrix();
            bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE);
            bufferBuilder.vertex(matrix4f, -0.5F, -0.5F, -0.5F).color(1.0F, 1.0F, 1.0F, 0.9F).texture(n, p).next();
            bufferBuilder.vertex(matrix4f, 0.5F, -0.5F, -0.5F).color(1.0F, 1.0F, 1.0F, 0.9F).texture(m, p).next();
            bufferBuilder.vertex(matrix4f, 0.5F, 0.5F, -0.5F).color(1.0F, 1.0F, 1.0F, 0.9F).texture(m, o).next();
            bufferBuilder.vertex(matrix4f, -0.5F, 0.5F, -0.5F).color(1.0F, 1.0F, 1.0F, 0.9F).texture(n, o).next();
            BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
            matrixStack.pop();
        }


        modelView.pop();
        RenderSystem.applyModelViewMatrix();

        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.depthFunc(515);

    }
}