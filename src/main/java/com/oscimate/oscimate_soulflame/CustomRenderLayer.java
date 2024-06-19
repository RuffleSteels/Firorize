package com.oscimate.oscimate_soulflame;

import com.oscimate.oscimate_soulflame.mixin.fire_overlays.client.RenderLayerAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

//import net.minecraft.client.render.RenderLayer;

import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.*;

import static net.minecraft.client.render.VertexFormats.*;


@Environment(value= EnvType.CLIENT)
public class CustomRenderLayer extends RenderLayer {

    protected static final ShaderProgram CUSTOM_TINT_SHADER = new ShaderProgram(GameRendererSetting::getRenderTypeCustomTint);

    //    private static final RenderLayer CUSTOM_TINT = RenderLayerAccessor.callOf("custom_tint", VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS, 131072, true, false, MultiPhaseParameters.builder().lightmap(ENABLE_LIGHTMAP).program(CUSTOM_TINT_SHADER).texture(BLOCK_ATLAS_TEXTURE).build(true));

//    public static final VertexFormatElement CUSTOM_TEXTURE_ELEMENT = new VertexFormatElement(1, VertexFormatElement.ComponentType.FLOAT, VertexFormatElement.Type.UV, 2);
//
//    public static final VertexFormat CUSTOM_POSITION_COLOR_TEXTURE_LIGHT_NORMAL = new VertexFormat(ImmutableMap.builder().put("UV1", CUSTOM_TEXTURE_ELEMENT).put("Position", POSITION_ELEMENT).put("Color", COLOR_ELEMENT).put("UV0", TEXTURE_ELEMENT).put("UV2", LIGHT_ELEMENT).put("Normal", NORMAL_ELEMENT).put("Padding", PADDING_ELEMENT).build());
//
    private static final RenderLayer CUSTOM_TINT = RenderLayerAccessor.callOf("custom_tint", POSITION_COLOR_TEXTURE_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS, 262144, true, false, MultiPhaseParameters.builder().lightmap(ENABLE_LIGHTMAP).program(CUSTOM_TINT_SHADER).transparency(TRANSLUCENT_TRANSPARENCY).texture(BLOCK_ATLAS_TEXTURE).build(true));

    public CustomRenderLayer(String name, VertexFormat vertexFormat, VertexFormat.DrawMode drawMode, int expectedBufferSize, boolean hasCrumbling, boolean translucent, Runnable startAction, Runnable endAction) {
        super(name, vertexFormat, drawMode, expectedBufferSize, hasCrumbling, translucent, startAction, endAction);
    }

    public static RenderLayer getCustomTint() {
        return CUSTOM_TINT;
    }
}
