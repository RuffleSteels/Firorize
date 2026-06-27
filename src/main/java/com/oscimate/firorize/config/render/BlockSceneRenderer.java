package com.oscimate.firorize.config.render;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.oscimate.firorize.test.TestModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders a {@link BlockSceneRenderState} (a list of transformed, optionally tinted block models)
 * inside the config screen as a Picture-in-Picture element (registered in {@code Main}). Block models
 * are rendered to the offscreen buffer via {@code VertexConsumer.putBakedQuad}, following vanilla's
 * {@code BlockFeatureRenderer} pattern; the PiP base composites the result and flushes the buffer.
 *
 * <p>Note: {@code customTint} (fire) ops are flat-tinted toward the target colour here. The full HSV
 * shader recolour ({@code TestModel.emitQuads} + the custom_tint pipeline) only runs through Fabric's
 * block emit path, which isn't driven from this PiP renderer — so the in-config fire preview is an
 * approximation of the in-world result.
 */
public class BlockSceneRenderer extends PictureInPictureRenderer<BlockSceneRenderState> {
    private static final Direction[] DIRECTIONS = Direction.values();
    private final QuadInstance quadInstance = new QuadInstance();
    private final RandomSource random = RandomSource.create();
    private final List<BlockStateModelPart> parts = new ArrayList<>();

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
        Minecraft mc = Minecraft.getInstance();
        mc.gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);
        BlockStateModelSet models = mc.getModelManager().getBlockStateModelSet();
        VertexConsumer buffer = this.bufferSource.getBuffer(Sheets.cutoutBlockSheet());

        this.quadInstance.setLightCoords(0xF000F0);
        this.quadInstance.setOverlayCoords(OverlayTexture.NO_OVERLAY);

        poseStack.pushPose();
        for (BlockSceneRenderState.BlockDrawOp op : scene.ops()) {
            if (op.push()) poseStack.pushPose();
            poseStack.translate(op.tx(), op.ty(), op.tz());
            poseStack.mulPose(op.rotation());
            if (op.mirror()) poseStack.scale(-1f, 1f, 1f);
            poseStack.scale(op.blockScale(), op.blockScale(), op.blockScale());
            poseStack.translate(op.ptx(), op.pty(), op.ptz());

            int color = 0xFF000000
                    | ((int) (op.r() * 255) << 16) | ((int) (op.g() * 255) << 8) | (int) (op.b() * 255);
            if (op.customTint()) {
                TestModel.configPreviewColor = color;
            }

            BlockStateModel model = models.get(op.state());
            // Deterministic per-block seed: otherwise random-variant blocks (e.g. netherrack's rotated
            // variants) re-roll every frame and flicker/spin.
            this.random.setSeed(op.state().getSeed(net.minecraft.core.BlockPos.ZERO));
            model.collectParts(this.random, this.parts);
            for (BlockStateModelPart part : this.parts) {
                for (Direction d : DIRECTIONS) {
                    for (BakedQuad quad : part.getQuads(d)) {
                        putQuad(buffer, poseStack, quad, op.customTint(), color);
                    }
                }
                for (BakedQuad quad : part.getQuads(null)) {
                    putQuad(buffer, poseStack, quad, op.customTint(), color);
                }
            }
            this.parts.clear();

            if (op.pop()) poseStack.popPose();
        }
        poseStack.popPose();
    }

    private void putQuad(VertexConsumer buffer, PoseStack poseStack, BakedQuad quad, boolean customTint, int color) {
        // Custom-tint (fire) quads are flat-tinted; normal blocks only tint their tint-indexed quads
        // (e.g. foliage) exactly like vanilla, leaving everything else white.
        boolean tinted = customTint || quad.materialInfo().tintIndex() != -1;
        this.quadInstance.setColor(tinted ? color : -1);
        buffer.putBakedQuad(poseStack.last(), quad, this.quadInstance);
    }
}
