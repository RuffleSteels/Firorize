package com.oscimate.oscimate_soulflame.mixin.fire_overlays.client;

import com.oscimate.oscimate_soulflame.Main;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.BlockRenderContext;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.TerrainRenderContext;
import net.fabricmc.fabric.mixin.client.indigo.renderer.ChunkBuilderBuiltChunkRebuildTaskMixin;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChunkBuilderBuiltChunkRebuildTaskMixin.class)
public class BlockRenderContextMixin {

    @ModifyVariable(method = "hookChunkBuild", at = @At(value = "HEAD"), argsOnly = true)
    private TerrainRenderContext changeOverlay(TerrainRenderContext value) {
        return value;
    }
}
