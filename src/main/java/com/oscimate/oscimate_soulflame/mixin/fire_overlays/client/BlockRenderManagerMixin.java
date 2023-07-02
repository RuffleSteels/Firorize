package com.oscimate.oscimate_soulflame.mixin.fire_overlays.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.model.BakedModel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Environment(EnvType.CLIENT)
@Mixin(BlockRenderManager.class)
public abstract class BlockRenderManagerMixin {

    @Shadow @Final private BlockModelRenderer blockModelRenderer;


    @Shadow public abstract BakedModel getModel(BlockState state);





//    @ModifyArg(method = "renderBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/block/BlockRenderManager;renderBlock(Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;ZLnet/minecraft/util/math/random/Random;)V"), index = 1)
//    private void renderBlankFire(BlockState state, BlockPos pos, BlockRenderView world, MatrixStack matrices, VertexConsumer vertexConsumer, boolean cull, Random random, CallbackInfo ci) {
//        if (MinecraftClient.getInstance().world.getBiome(pos).getKey().orElseThrow().equals(BiomeKeys.WARPED_FOREST) && state.getBlock() instanceof FireBlock) {
//            MinecraftClient.getInstance().world.get
//            blockModelRenderer.render(world, getModel(Blocks.BONE_BLOCK.getDefaultState()), state, pos, matrices, vertexConsumer, cull, random, state.getRenderingSeed(pos), OverlayTexture.DEFAULT_UV);
//        }
//    }

//    @ModifyArg(method = "renderBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/block/BlockRenderManager;renderBlock(Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;ZLnet/minecraft/util/math/random/Random;)V"), index = 1)
//    private void renderBlankFire(BlockState state, BlockPos pos, BlockRenderView world, MatrixStack matrices, VertexConsumer vertexConsumer, boolean cull, Random random, CallbackInfo ci) {
//        if (MinecraftClient.getInstance().world.getBiome(pos).getKey().orElseThrow().equals(BiomeKeys.WARPED_FOREST) && state.getBlock() instanceof FireBlock) {
//            MinecraftClient.getInstance().world.get
//            blockModelRenderer.render(world, getModel(Blocks.BONE_BLOCK.getDefaultState()), state, pos, matrices, vertexConsumer, cull, random, state.getRenderingSeed(pos), OverlayTexture.DEFAULT_UV);
//        }
//    }

//    @Shadow @Final private BlockModels models;

//    @Inject(method = "getModel", at = @At(value = "TAIL"), cancellable = true)
//    public void getModelss(BlockState state, CallbackInfoReturnable<BakedModel> cir){
//        cir.setReturnValue(models.getModel(Blocks.NETHER_GOLD_ORE.getDefaultState()));
//    }
}
