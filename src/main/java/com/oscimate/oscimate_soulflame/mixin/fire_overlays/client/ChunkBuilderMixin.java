package com.oscimate.oscimate_soulflame.mixin.fire_overlays.client;

import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FireBlock;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChunkBuilder.class)
public abstract class ChunkBuilderMixin {

    @Redirect(method = "render", require = 1, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/render/block/BlockRenderManager;renderBlock(Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;ZLnet/minecraft/util/math/random/Random;)V"))
    private void hookChunkBuildTessellate(BlockRenderManager renderManager, BlockState blockState, BlockPos blockPos, BlockRenderView blockView, MatrixStack matrix, VertexConsumer bufferBuilder, boolean checkSides, Random random) {
        if (blockState.getRenderType() == BlockRenderType.MODEL && blockState.getBlock() instanceof FireBlock) {
            final BakedModel model = renderManager.getModel(Blocks.NETHERITE_BLOCK.getDefaultState());
            renderManager.renderBlock(blockState, blockPos, blockView, matrix, bufferBuilder, checkSides, random);
        }

        renderManager.renderBlock(blockState, blockPos, blockView, matrix, bufferBuilder, checkSides, random);
    }}
