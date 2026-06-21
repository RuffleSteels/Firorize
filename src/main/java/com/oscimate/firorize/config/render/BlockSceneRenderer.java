package com.oscimate.firorize.config.render;

import com.oscimate.firorize.FirorizePipelines;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.render.SpecialGuiElementRenderer;
import net.minecraft.client.render.BlockRenderLayers;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Renders a {@link BlockSceneRenderState} (a list of transformed, optionally custom-tinted block
 * models) inside the config screen. Registered via Fabric's {@code SpecialGuiElementRegistry} in
 * {@code Main}. The base class supplies a real 3D {@link MatrixStack} (origin at the element box's
 * centre-bottom, pre-scaled by the window scale × {@code state.scale()}) and flushes the buffers.
 */
public class BlockSceneRenderer extends SpecialGuiElementRenderer<BlockSceneRenderState> {

    public BlockSceneRenderer(VertexConsumerProvider.Immediate vertexConsumers) {
        super(vertexConsumers);
    }

    @Override
    public Class<BlockSceneRenderState> getElementClass() {
        return BlockSceneRenderState.class;
    }

    @Override
    protected String getName() {
        return "firorize block scene";
    }

    @Override
    protected void render(BlockSceneRenderState scene, MatrixStack matrices) {
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.gameRenderer.getDiffuseLighting().setShaderLights(DiffuseLighting.Type.ENTITY_IN_UI);
        BlockRenderManager brm = mc.getBlockRenderManager();

        matrices.push();
        for (BlockSceneRenderState.BlockDrawOp op : scene.ops()) {
            if (op.push()) {
                matrices.push();
            }
            matrices.translate(op.tx(), op.ty(), op.tz());
            matrices.multiply(op.rotation());
            if (op.mirror()) {
                matrices.scale(-1f, 1f, 1f);
            }
            matrices.scale(op.blockScale(), op.blockScale(), op.blockScale());
            matrices.translate(op.ptx(), op.pty(), op.ptz());

            VertexConsumer vc = this.vertexConsumers.getBuffer(
                    op.customTint() ? FirorizePipelines.getCustomTint() : BlockRenderLayers.getEntityBlockLayer(op.state()));
            BlockModelRenderer.render(matrices.peek(), vc, brm.getModel(op.state()),
                    op.r(), op.g(), op.b(), 0xF000F0, OverlayTexture.DEFAULT_UV);
            if (op.pop()) {
                matrices.pop();
            }
        }
        matrices.pop();
    }
}
