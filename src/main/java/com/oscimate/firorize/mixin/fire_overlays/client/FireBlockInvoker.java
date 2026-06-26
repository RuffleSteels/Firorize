package com.oscimate.firorize.mixin.fire_overlays.client;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FireBlock.class)
public interface FireBlockInvoker {
    // Yarn's burnChances is named burnOdds in 26.1.2 official mappings.
    @Accessor("burnOdds")
    Object2IntMap<Block> getBurnChances();
}
