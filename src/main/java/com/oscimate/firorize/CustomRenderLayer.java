package com.oscimate.firorize;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;

import static net.minecraft.client.render.VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL;


@Environment(value= EnvType.CLIENT)
public class CustomRenderLayer extends RenderLayer {

    protected static final ShaderProgram CUSTOM_TINT_SHADER = new ShaderProgram(GameRendererSetting::getRenderTypeCustomTint);
    private static final RenderLayer CUSTOM_TINT = RenderLayer.of("custom_tint", POSITION_COLOR_TEXTURE_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS, 262144, true, false, MultiPhaseParameters.builder().lightmap(ENABLE_LIGHTMAP).program(CUSTOM_TINT_SHADER).transparency(TRANSLUCENT_TRANSPARENCY).depthTest(RenderPhase.LEQUAL_DEPTH_TEST).texture(BLOCK_ATLAS_TEXTURE).build(true));

    public CustomRenderLayer(String name, VertexFormat vertexFormat, VertexFormat.DrawMode drawMode, int expectedBufferSize, boolean hasCrumbling, boolean translucent, Runnable startAction, Runnable endAction) {
        super(name, vertexFormat, drawMode, expectedBufferSize, hasCrumbling, translucent, startAction, endAction);
    }
    public static RenderLayer getCustomTint() {
        return CUSTOM_TINT;
    }
}
