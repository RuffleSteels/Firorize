package com.oscimate.firorize.config.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;

/**
 * Renders a {@link BlockSceneRenderState} (a list of transformed, optionally custom-tinted block
 * models) inside the config screen. Registered via Fabric's {@code PictureInPictureRendererRegistry}
 * in {@code Main}: special GUI elements became Picture-in-Picture in 26.1.
 *
 * <p><b>26.1.2 port TODO — preview rendering not yet reimplemented.</b> The 1.21.11 version drew the
 * block grid + fire preview with {@code BlockModelRenderer.render} / Fabric
 * {@code FabricBlockModelRenderer.render(BlockVertexConsumerProvider, ...)}, both of which were removed.
 * In 26.1.2:
 * <ul>
 *   <li>Lighting: {@code net.minecraft.client.Minecraft.getInstance().gameRenderer.getLighting()
 *       .setupFor(com.mojang.blaze3d.platform.Lighting.Entry.ITEMS_3D)} (replaces the manual
 *       GpuBuffer/Std140 UBO).</li>
 *   <li>Block model parts come from {@code BlockStateModel.collectParts(RandomSource, List&lt;
 *       BlockStateModelPart&gt;)} → {@code BlockStateModelPart.getQuads(Direction)} →
 *       {@code VertexConsumer.putBakedQuad(PoseStack.Pose, BakedQuad, QuadInstance)} into
 *       {@code bufferSource.getBuffer(net.minecraft.client.renderer.Sheets.cutoutBlockSheet())}.</li>
 *   <li>The custom-tint fire preview must run {@code TestModel.emitQuads} (so the fire texture is
 *       re-textured and recoloured), which now goes through Fabric
 *       {@code AltModelBlockRenderer.tesselateBlock(QuadEmitter, ...)} with a QuadEmitter bridged to
 *       a {@code FirorizePipelines.getCustomTint()} buffer; set {@code TestModel.configPreviewColor}
 *       first.</li>
 * </ul>
 * This needs in-game iteration to get right, so it is left as a no-op (blank preview) until then; the
 * rest of the config screen is fully functional.
 */
public class BlockSceneRenderer extends PictureInPictureRenderer<BlockSceneRenderState> {

    public BlockSceneRenderer(MultiBufferSource.BufferSource bufferSource) {
        super(bufferSource);
    }

    @Override
    public Class<BlockSceneRenderState> getRenderStateClass() {
        return BlockSceneRenderState.class;
    }

    @Override
    protected String getTextureLabel() {
        return "firorize block scene";
    }

    @Override
    protected void renderToTexture(BlockSceneRenderState scene, PoseStack poseStack) {
        // TODO(26.1.2 port): reimplement the block-grid + fire preview against the new block-model
        // render API (see class javadoc). Left as a no-op so the config screen compiles and launches.
    }
}
