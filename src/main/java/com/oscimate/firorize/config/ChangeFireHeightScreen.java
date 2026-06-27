package com.oscimate.firorize.config;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.oscimate.firorize.Main;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

public class ChangeFireHeightScreen extends Screen {
    private Screen parent;

    protected ChangeFireHeightScreen(Screen parent) {
        super(Component.translatable("options.videoTitle"));
        this.parent = parent;
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent click, boolean doubled) {
        this.setFocused(null); // clear previous focus/outline; a genuinely-clicked widget re-acquires it via super
        return super.mouseClicked(click, doubled);
    }

    public void onClose() {
        Main.CONFIG_MANAGER.save();

        minecraft.setScreen(parent);
    }
    @Override
    protected void init() {
        FireHeightSliderWidget customTimeSliderWidget = new FireHeightSliderWidget(this.width / 2 - 75, 10, 150, 20, Component.translatable("firorize.config.title.height"), (double) Main.CONFIG_MANAGER.getCurrentFireHeightSlider() /100);
        this.addRenderableWidget(customTimeSliderWidget);
        this.addRenderableWidget(new Button.Builder(CommonComponents.GUI_DONE, button -> onClose()).bounds(width / 2 - 100, 50, 200, 20).build());
        super.init();
    }
    @Override
    public void resize(int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
//        Main.setScale(width, height, minecraft);
        super.resize(minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight());
    }




    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        DonationTracker.onConfigFrame();
        super.extractRenderState(context, mouseX, mouseY, delta);

        PoseStack poseStack = new PoseStack();
        TextureAtlasSprite sprite = context.getSprite(ModelBakery.FIRE_1);
        VertexConsumer builder = minecraft.renderBuffers().bufferSource().getBuffer(RenderTypes.fireScreenEffect(sprite.atlasLocation()));
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();

        // In-world, this geometry is added to the world buffer source and flushed with the camera's
        // view-rotation matrix (conjugate of the camera rotation) applied. That locks the fire to
        // world-north, so it only appears when the player faces north. Bake the camera's forward
        // rotation into the pose so the view rotation cancels out, leaving the fire screen-locked
        // directly in front of the player (vanilla's screen-effect behaviour). Out of a world there
        // is no live camera/view rotation, so we leave the pose at identity (already correct).
        if (minecraft.player != null) {
            Camera camera = minecraft.gameRenderer.getMainCamera();
            poseStack.mulPose(camera.rotation());
        }

        poseStack.translate(0.0, FireHeightSliderWidget.getFireHeight(Main.CONFIG_MANAGER.getCurrentFireHeightSlider() - (minecraft.player == null ? 2 : 0)), 0.0);

        for (int i = 0; i < 2; i++) {
            poseStack.pushPose();
            float x0 = -0.5F;
            float x1 = 0.5F;
            float y0 = -0.5F;
            float y1 = 0.5F;
            float z0 = -0.5F;
            poseStack.translate(-(i * 2 - 1) * 0.24F, -0.3F, 0.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees((i * 2 - 1) * 10.0F));
            Matrix4f pose = poseStack.last().pose();
            builder.addVertex(pose, -0.5F, -0.5F, -0.5F).setUv(u1, v1).setColor(1.0F, 1.0F, 1.0F, 0.9F);
            builder.addVertex(pose, 0.5F, -0.5F, -0.5F).setUv(u0, v1).setColor(1.0F, 1.0F, 1.0F, 0.9F);
            builder.addVertex(pose, 0.5F, 0.5F, -0.5F).setUv(u0, v0).setColor(1.0F, 1.0F, 1.0F, 0.9F);
            builder.addVertex(pose, -0.5F, 0.5F, -0.5F).setUv(u1, v0).setColor(1.0F, 1.0F, 1.0F, 0.9F);
            poseStack.popPose();
        }
    }
}