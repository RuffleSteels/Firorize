package com.oscimate.firorize.mixin.fire_overlays.client;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.block.Block;
import net.minecraft.block.FireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FireBlock.class)
public interface FireBlockInvoker {
    @Accessor
    Object2IntMap<Block> getBurnChances();
}
