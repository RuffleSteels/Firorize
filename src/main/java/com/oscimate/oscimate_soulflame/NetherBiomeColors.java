package com.oscimate.oscimate_soulflame;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.biome.ColorResolver;

public class NetherBiomeColors {

    public static final ColorResolver NETHER_COLOR = (biome, x, z) -> biome.getWaterColor();

    public NetherBiomeColors() {
    }

    private static int getColor(BlockRenderView world, BlockPos pos, ColorResolver resolver) {
        return world.getColor(pos, resolver);
    }

    public static int getNetherColor(BlockRenderView world, BlockPos pos) {
        return getColor(world, pos, NETHER_COLOR);
    }
}
