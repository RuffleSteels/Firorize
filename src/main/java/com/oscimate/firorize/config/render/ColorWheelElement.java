package com.oscimate.firorize.config.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

/**
 * Draws the HSV colour wheel through Firorize's custom {@code COLOR_WHEEL} {@link RenderPipeline} as a
 * GUI element. The shader reads the lightness {@code Value} from the quad's vertex-colour alpha and
 * the wheel geometry from UV0.
 */
public record ColorWheelElement(
        RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2fc pose,
        int x0, int y0, int x1, int y1, float value,
        @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {

    public ColorWheelElement(RenderPipeline pipeline, Matrix3x2f pose, int x0, int y0, int x1, int y1, float value, @Nullable ScreenRectangle scissorArea) {
        this(pipeline, TextureSetup.noTexture(), pose, x0, y0, x1, y1, value, scissorArea, computeBounds(x0, y0, x1, y1, pose, scissorArea));
    }

    private static @Nullable ScreenRectangle computeBounds(int x0, int y0, int x1, int y1, Matrix3x2fc pose, @Nullable ScreenRectangle scissorArea) {
        ScreenRectangle bounds = new ScreenRectangle(x0, y0, x1 - x0, y1 - y0).transformMaxBounds(pose);
        return scissorArea != null ? scissorArea.intersection(bounds) : bounds;
    }

    @Override
    public void buildVertices(VertexConsumer v) {
        int color = (Math.round(value * 255f) << 24) | 0xFFFFFF; // Value (lightness) carried in alpha
        v.addVertexWith2DPose(pose, x0, y0).setUv(0f, 1f).setColor(color);
        v.addVertexWith2DPose(pose, x0, y1).setUv(0f, 0f).setColor(color);
        v.addVertexWith2DPose(pose, x1, y1).setUv(1f, 0f).setColor(color);
        v.addVertexWith2DPose(pose, x1, y0).setUv(1f, 1f).setColor(color);
    }
}
