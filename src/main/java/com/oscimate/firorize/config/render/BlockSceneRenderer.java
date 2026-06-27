package com.oscimate.firorize.config.render;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.oscimate.firorize.FirorizePipelines;
import com.oscimate.firorize.test.TestModel;
import net.fabricmc.fabric.api.client.renderer.v1.Renderer;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableMesh;
import net.fabricmc.fabric.api.client.renderer.v1.render.AltModelBlockRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders a {@link BlockSceneRenderState} (a list of transformed, optionally custom-tinted block
 * models) inside the config screen as a Picture-in-Picture element (registered in {@code Main}).
 *
 * <p>Normal blocks are rendered straight to the offscreen buffer via {@code collectParts} +
 * {@code VertexConsumer.putBakedQuad} (vanilla {@code BlockFeatureRenderer} pattern). Fire
 * ({@code customTint}) ops instead go through Fabric's block-emit path so {@link TestModel#emitQuads}
 * runs (re-texturing the fire onto the grayscale config sprite and stamping the target colour onto the
 * vertices); the resulting quads are pushed into Firorize's {@code custom_tint} render type, whose
 * shader does the HSV colorisation — matching the in-world look. The PiP base composites + flushes.
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
            BlockStateModel model = models.get(op.state());

            if (op.customTint() && mc.level != null && renderFireTinted(mc, poseStack, op, model, color)) {
                // handled by the custom_tint shader path
            } else {
                renderPlain(buffer, poseStack, model, op, color);
            }

            if (op.pop()) poseStack.popPose();
        }
        poseStack.popPose();
    }

    /** Normal block models: emit baked quads directly, tinting only tint-indexed (or all, for fire fallback) quads. */
    private void renderPlain(VertexConsumer buffer, PoseStack poseStack, BlockStateModel model,
                             BlockSceneRenderState.BlockDrawOp op, int color) {
        this.random.setSeed(op.state().getSeed(BlockPos.ZERO));
        model.collectParts(this.random, this.parts);
        for (BlockStateModelPart part : this.parts) {
            for (Direction d : DIRECTIONS) {
                for (BakedQuad quad : part.getQuads(d)) putQuad(buffer, poseStack, quad, op.customTint(), color);
            }
            for (BakedQuad quad : part.getQuads(null)) putQuad(buffer, poseStack, quad, op.customTint(), color);
        }
        this.parts.clear();
    }

    private void putQuad(VertexConsumer buffer, PoseStack poseStack, BakedQuad quad, boolean customTint, int color) {
        boolean tinted = customTint || quad.materialInfo().tintIndex() != -1;
        this.quadInstance.setColor(tinted ? color : -1);
        buffer.putBakedQuad(poseStack.last(), quad, this.quadInstance);
    }

    /**
     * Fire preview through the custom_tint shader: run {@link TestModel#emitQuads} (config branch, since
     * {@code Main.inConfig} is true) into a Fabric mesh, then copy the quads into the custom_tint buffer
     * (POSITION_TEX_COLOR). Returns false (falling back to plain tint) if the renderer path is
     * unavailable or throws.
     */
    private boolean renderFireTinted(Minecraft mc, PoseStack poseStack, BlockSceneRenderState.BlockDrawOp op,
                                     BlockStateModel model, int color) {
        try {
            TestModel.configPreviewColor = color;
            MutableMesh mesh = Renderer.get().mutableMesh();
            AltModelBlockRenderer renderer = Renderer.get().altModelBlockRenderer(false, false, mc.getBlockColors());
            long seed = op.state().getSeed(BlockPos.ZERO);
            renderer.tesselateBlock(mesh.emitter(), 1f, 1f, 1f, mc.level, BlockPos.ZERO, op.state(), model, seed);

            VertexConsumer tint = this.bufferSource.getBuffer(FirorizePipelines.getCustomTint());
            PoseStack.Pose pose = poseStack.last();
            mesh.forEach(quad -> {
                for (int i = 0; i < 4; i++) {
                    tint.addVertex(pose, quad.x(i), quad.y(i), quad.z(i)).setUv(quad.u(i), quad.v(i)).setColor(quad.color(i));
                }
            });
            mesh.clear();
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
