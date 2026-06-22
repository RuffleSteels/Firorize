package com.oscimate.firorize.config.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.oscimate.firorize.FirorizePipelines;
import com.oscimate.firorize.test.TestModel;
import net.fabricmc.fabric.api.renderer.v1.render.BlockVertexConsumerProvider;
import net.fabricmc.fabric.api.renderer.v1.render.FabricBlockModelRenderer;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EmptyBlockRenderView;
import org.joml.Vector3f;

import java.nio.ByteBuffer;

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
        setFrontLighting();
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

            if (op.customTint()) {
                // Custom-tint (fire) preview: must go through the Fabric path so TestModel.emitQuads
                // runs (re-textures onto the grayscale blank_fire_*_config sprite) and buffers into
                // our custom_tint layer. The tint colour can't ride the r,g,b args (dropped for
                // untinted fire quads), so stamp it for emitQuads to write. See docs §4.
                TestModel.configPreviewColor = 0xFF000000
                        | ((int) (op.r() * 255) << 16) | ((int) (op.g() * 255) << 8) | (int) (op.b() * 255);
                BlockVertexConsumerProvider tintProvider =
                        layer -> this.vertexConsumers.getBuffer(FirorizePipelines.getCustomTint());
                FabricBlockModelRenderer.render(matrices.peek(), tintProvider, brm.getModel(op.state()),
                        1f, 1f, 1f, 0xF000F0, OverlayTexture.DEFAULT_UV,
                        EmptyBlockRenderView.INSTANCE, BlockPos.ORIGIN, op.state());
            } else {
                VertexConsumer vc = this.vertexConsumers.getBuffer(BlockRenderLayers.getEntityBlockLayer(op.state()));
                BlockModelRenderer.render(matrices.peek(), vc, brm.getModel(op.state()),
                        op.r(), op.g(), op.b(), 0xF000F0, OverlayTexture.DEFAULT_UV);
            }
            if (op.pop()) {
                matrices.pop();
            }
        }
        matrices.pop();
    }

    /** Lazily-built UBO holding our two diffuse light directions (see {@link #setFrontLighting}). */
    private static GpuBuffer lightingBuffer;

    /**
     * Light the block from above-front so the camera-facing faces are lit, not shadowed. The vanilla
     * {@code ENTITY_IN_UI} preset shadows them here because the special-element renderer pre-applies a
     * {@code scale(f, f, -f)} that negates view-space Z (flipping front/back diffuse). We compensate by
     * pushing the light's Z toward the camera. Directions are tunable: bump Y for a brighter top, swap
     * X sign to favour the other side face.
     */
    private static void setFrontLighting() {
        if (lightingBuffer == null) {
            GpuDevice device = RenderSystem.getDevice();
            Vector3f light0 = new Vector3f(1f, -1.0f, 0.7f).normalize();
            Vector3f light1 = new Vector3f(-1f, -1.0f, -0.7f).normalize();
            try (org.lwjgl.system.MemoryStack stack = org.lwjgl.system.MemoryStack.stackPush()) {
                ByteBuffer data = Std140Builder.onStack(stack, DiffuseLighting.UBO_SIZE)
                        .putVec3(light0).putVec3(light1).get();
                lightingBuffer = device.createBuffer(() -> "Firorize UI block lighting",
                        GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, data);
            }
        }
        RenderSystem.setShaderLights(lightingBuffer.slice());
    }
}
