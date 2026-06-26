package com.oscimate.firorize.mixin.fire_overlays.client;

import com.mojang.blaze3d.platform.NativeImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(NativeImage.class)
public interface NativeImageInvoker {
    // 26.1.2 NativeImage fields: pixels (native pointer) and size.
    @Accessor("pixels") long getPointer();

    @Accessor("size") long getSizeBytes();
}
