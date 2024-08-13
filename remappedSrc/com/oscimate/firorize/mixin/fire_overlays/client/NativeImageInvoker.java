package com.oscimate.firorize.mixin.fire_overlays.client;

import net.minecraft.client.texture.NativeImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(NativeImage.class)
public interface NativeImageInvoker {
    @Invoker("<init>")
    static NativeImage invokeInit(NativeImage.Format format, int width, int height, boolean useStb, long pointer) {
        throw new AssertionError();
    }

    @Accessor long getPointer();

    @Accessor long getSizeBytes();
}
