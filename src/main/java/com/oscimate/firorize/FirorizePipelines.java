package com.oscimate.firorize;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;

/**
 * Holds Firorize's two custom {@link RenderPipeline}s and the render type that draws the in-screen
 * fire preview. Pipelines are compiled lazily on first use, so they only need to be built here. The
 * shader sources live under {@code assets/minecraft/shaders/core/firorize/}.
 */
@Environment(EnvType.CLIENT)
public final class FirorizePipelines {

    /** Colorises the fire texture toward a target colour (carried in the vertex colour). GUI preview only. */
    public static final RenderPipeline CUSTOM_TINT = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("firorize", "pipeline/custom_tint"))
            .withVertexShader("core/firorize/rendertype_custom_tint")
            .withFragmentShader("core/firorize/rendertype_custom_tint")
            .withSampler("Sampler0")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(DepthStencilState.DEFAULT)
            .withVertexFormat(VertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
            .build();

    /** Draws the HSV colour-wheel in the config screen. Lightness (Value) is carried in vertex-colour alpha. */
    public static final RenderPipeline COLOR_WHEEL = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("firorize", "pipeline/color_wheel"))
            .withVertexShader("core/firorize/rendertype_color_wheel")
            .withFragmentShader("core/firorize/rendertype_color_wheel")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withVertexFormat(VertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
            .build();

    private static final RenderType CUSTOM_TINT_LAYER = RenderType.create(
            "firorize_custom_tint",
            RenderSetup.builder(CUSTOM_TINT)
                    .withTexture("Sampler0", TextureAtlas.LOCATION_BLOCKS)
                    .bufferSize(262144)
                    .createRenderSetup());

    public static RenderType getCustomTint() {
        return CUSTOM_TINT_LAYER;
    }

    private FirorizePipelines() {
    }
}
