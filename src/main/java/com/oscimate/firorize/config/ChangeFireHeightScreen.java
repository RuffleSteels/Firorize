package com.oscimate.firorize.config;

import com.mojang.blaze3d.systems.RenderSystem;
import com.oscimate.firorize.Main;
import com.oscimate.firorize.mixin.fire_overlays.client.GameRendererMixin;
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
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

public class ChangeFireHeightScreen extends Screen {
    private Screen parent;

    private int counter = 16;
    private int ticks = 0;

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

        // 2D preview of the (animated) fire overlay sprite; the height slider shifts it vertically so
        // the user can see the effect. (The old immediate-mode 3D overlay quads can't be drawn in the
        // 1.21.5+ 2D GUI without a special-element renderer; this conveys the same adjustment.)
        Sprite sprite = MinecraftClient.getInstance().getAtlasManager().getSprite(ModelBaker.FIRE_1);
        double fireHeight = FireHeightSliderWidget.getFireHeight(Main.CONFIG_MANAGER.getCurrentFireHeightSlider());
        int size = 120;
        int cx = this.width / 2;
        int baseY = this.height / 2 + 60;
        int offsetY = (int) Math.round(fireHeight * size);
        context.drawSpriteStretched(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, sprite,
                cx - size / 2, baseY - size + offsetY, size, size);

        if (ticks % 4 == 0) counter++;
        ticks++;
        if (counter > 32) {
            counter = 0;
            ticks = 0;
        }
    }

}