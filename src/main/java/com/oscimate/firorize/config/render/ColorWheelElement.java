package com.oscimate.firorize.config.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

/**
 * Draws the HSV colour wheel through Firorize's custom {@code COLOR_WHEEL} {@link RenderPipeline}.
 * Replaces the pre-1.21.5 {@code RenderSystem.setShader} + {@code Tessellator}/{@code BufferRenderer}
 * immediate draw. The shader reads the lightness {@code Value} from the quad's vertex-colour alpha.
 */
public record ColorWheelElement(
        RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2f pose,
        int x1, int y1, int x2, int y2, float value,
        @Nullable ScreenRect scissorArea, @Nullable ScreenRect bounds
) implements SimpleGuiElementRenderState {

    public ColorWheelElement(RenderPipeline pipeline, Matrix3x2f pose, int x1, int y1, int x2, int y2, float value, @Nullable ScreenRect scissorArea) {
        this(pipeline, TextureSetup.empty(), pose, x1, y1, x2, y2, value, scissorArea,
                new ScreenRect(x1, y1, x2 - x1, y2 - y1).transformEachVertex(pose));
    }

    @Override
    public void setupVertices(VertexConsumer v) {
        int color = (Math.round(value * 255f) << 24) | 0xFFFFFF; // Value (lightness) carried in alpha
        v.vertex(pose, x1, y1).texture(0f, 1f).color(color);
        v.vertex(pose, x1, y2).texture(0f, 0f).color(color);
        v.vertex(pose, x2, y2).texture(1f, 0f).color(color);
        v.vertex(pose, x2, y1).texture(1f, 1f).color(color);
    }
}
