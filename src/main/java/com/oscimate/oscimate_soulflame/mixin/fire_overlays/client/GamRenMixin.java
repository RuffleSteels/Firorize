package com.oscimate.oscimate_soulflame.mixin.fire_overlays.client;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GamRenMixin {
    @Shadow public abstract Matrix4f getBasicProjectionMatrix(double fov);

    @Shadow protected abstract double getFov(Camera camera, float tickDelta, boolean changingFov);

    @Inject(method = "renderHand", at = @At("HEAD"))
    public void test(MatrixStack matrices, Camera camera, float tickDelta, CallbackInfo ci) {
//        System.out.println(this.getBasicProjectionMatrix(this.getFov(camera, tickDelta, false)));

    }
}
