package com.oscimate.firorize.config;


import com.oscimate.firorize.Main;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.*;
import net.minecraft.client.render.model.ModelBaker;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

public class ChangeFireHeightScreen extends Screen {
    private Screen parent;

    protected ChangeFireHeightScreen(Screen parent) {
        super(Text.translatable("options.videoTitle"));
        this.parent = parent;
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
        this.setFocused(null); // clear previous focus/outline; a genuinely-clicked widget re-acquires it via super
        return super.mouseClicked(click, doubled);
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
    public void resize(int width, int height) {
        MinecraftClient client = MinecraftClient.getInstance();
//        Main.setScale(width, height, client);
        super.resize(client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        MatrixStack matrices = new MatrixStack();

        Sprite sprite = client.getAtlasManager().getSprite(ModelBaker.FIRE_1);


        VertexConsumer vertexConsumer = client.getBufferBuilders().getEntityVertexConsumers().getBuffer(RenderLayers.fireScreenEffect(sprite.getAtlasId()));
        float f = sprite.getMinU();
        float g = sprite.getMaxU();
        float h = sprite.getMinV();
        float i = sprite.getMaxV();
        float j = 1.0F;
        matrices.translate(0.0, FireHeightSliderWidget.getFireHeight(Main.CONFIG_MANAGER.getCurrentFireHeightSlider() - (client.world == null ? 2 : 0)), 0.0);

        for (int k = 0; k < 2; k++) {
            matrices.push();
            float l = -0.5F;
            float m = 0.5F;
            float n = -0.5F;
            float o = 0.5F;
            float p = -0.5F;
            matrices.translate(-(k * 2 - 1) * 0.24F, -0.3F, 0.0F);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((k * 2 - 1) * 10.0F));
            Matrix4f matrix4f = matrices.peek().getPositionMatrix();
            vertexConsumer.vertex(matrix4f, -0.5F, -0.5F, -0.5F).texture(g, i).color(1.0F, 1.0F, 1.0F, 0.9F);
            vertexConsumer.vertex(matrix4f, 0.5F, -0.5F, -0.5F).texture(f, i).color(1.0F, 1.0F, 1.0F, 0.9F);
            vertexConsumer.vertex(matrix4f, 0.5F, 0.5F, -0.5F).texture(f, h).color(1.0F, 1.0F, 1.0F, 0.9F);
            vertexConsumer.vertex(matrix4f, -0.5F, 0.5F, -0.5F).texture(g, h).color(1.0F, 1.0F, 1.0F, 0.9F);
            matrices.pop();
        }
    }

}