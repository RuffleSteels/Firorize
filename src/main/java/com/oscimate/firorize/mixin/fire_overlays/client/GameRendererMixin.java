package com.oscimate.firorize.mixin.fire_overlays.client;

import net.minecraft.client.renderer.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRenderer.class)
public interface GameRendererMixin {
    @Invoker float callGetFov(Camera camera, float tickDelta, boolean changingFov);
}
