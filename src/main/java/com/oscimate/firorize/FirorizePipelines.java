package com.oscimate.firorize;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.SpriteAtlasTexture;

/**
 * Holds Firorize's two custom {@link RenderPipeline}s and the render layer that draws the in-screen
 * fire preview. These replace the pre-1.21.5 core-shader injection: pipelines are compiled lazily
 * on first use ({@code GlBackend.compilePipelineCached}), so they only need to be built here, not
 * registered into vanilla's pipeline map. The shader sources live under
 * {@code assets/minecraft/shaders/core/firorize/}.
 */
@Environment(EnvType.CLIENT)
public final class FirorizePipelines {

    /** Colorises the fire texture toward a target colour (carried in the vertex colour). GUI preview only. */
    public static final RenderPipeline CUSTOM_TINT = RenderPipeline.builder(RenderPipelines.TRANSFORMS_PROJECTION_FOG_SNIPPET)
            .withLocation(net.minecraft.util.Identifier.of("firorize", "pipeline/custom_tint"))
            .withVertexShader("core/firorize/rendertype_custom_tint")
            .withFragmentShader("core/firorize/rendertype_custom_tint")
            .withSampler("Sampler0")
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withVertexFormat(VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS)
            .build();

    /** Draws the HSV colour-wheel in the config screen. Lightness (Value) is carried in vertex-colour alpha. */
    public static final RenderPipeline COLOR_WHEEL = RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
            .withLocation(net.minecraft.util.Identifier.of("firorize", "pipeline/color_wheel"))
            .withVertexShader("core/firorize/rendertype_color_wheel")
            .withFragmentShader("core/firorize/rendertype_color_wheel")
            .withBlend(BlendFunction.TRANSLUCENT)
            .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS)
            .build();

    @SuppressWarnings("deprecation") // BLOCK_ATLAS_TEXTURE is still the supported atlas id in 1.21
    private static final RenderLayer CUSTOM_TINT_LAYER = RenderLayer.of(
            "firorize_custom_tint",
            RenderSetup.builder(CUSTOM_TINT)
                    .texture("Sampler0", SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)
                    .expectedBufferSize(262144)
                    .build());

    public static RenderLayer getCustomTint() {
        return CUSTOM_TINT_LAYER;
    }

    private FirorizePipelines() {
    }
}
