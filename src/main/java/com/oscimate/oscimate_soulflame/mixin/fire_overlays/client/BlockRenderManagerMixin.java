package com.oscimate.oscimate_soulflame.mixin.fire_overlays.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.oscimate.oscimate_soulflame.Main;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

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
