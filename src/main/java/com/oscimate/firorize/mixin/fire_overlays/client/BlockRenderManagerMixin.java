package com.oscimate.firorize.mixin.fire_overlays.client;

import net.minecraft.client.render.block.BlockRenderManager;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockRenderManager.class)
public class BlockRenderManagerMixin {

//    @WrapOperation(method = "renderBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/block/BlockModelRenderer;render(Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/client/render/model/BakedModel;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;ZLnet/minecraft/util/math/random/Random;JI)V"))
//    private void changeOverlay(BlockModelRenderer instance, BlockRenderView world, BakedModel model, BlockState state, BlockPos pos, MatrixStack matrices, VertexConsumer vertexConsumer, boolean cull, Random random, long seed, int overlay, Operation<Void> original) {
//        if (MinecraftClient.getInstance().world != null) {
//            RegistryKey<Biome> biome = MinecraftClient.getInstance().world.getBiome(pos).getLeft().orElseThrow();
//            if (state.getBlock() == Blocks.FIRE && (biome.equals(BiomeKeys.WARPED_FOREST) || biome.equals(BiomeKeys.BASALT_DELTAS))) {
//                overlay = OverlayTexture.packUv(Main.BLANK_FIRE_0_OVERLAY.get().getX(), Main.BLANK_FIRE_0_OVERLAY.get().getY());
//            }
//        }
//        original.call(instance, world, model, state, pos, matrices, vertexConsumer, cull, random, seed, overlay);
//    }
}
