package com.santapexie.santapexie_soulflame;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

public interface OnSoulFireAccessor {

    void renderSoully(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Entity entity);

}
