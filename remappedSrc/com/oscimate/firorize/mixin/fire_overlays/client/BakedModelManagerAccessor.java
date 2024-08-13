package com.oscimate.firorize.mixin.fire_overlays.client;

import net.minecraft.client.render.model.BakedModelManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BakedModelManager.class)
public interface BakedModelManagerAccessor {
    @Accessor int getMipmapLevels();
}
