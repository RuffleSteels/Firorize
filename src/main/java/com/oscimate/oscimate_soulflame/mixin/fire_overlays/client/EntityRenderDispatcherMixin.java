package com.oscimate.oscimate_soulflame.mixin.fire_overlays.client;


import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.systems.RenderSystem;
import com.oscimate.oscimate_soulflame.GameRendererSetting;
import com.oscimate.oscimate_soulflame.Main;
import com.oscimate.oscimate_soulflame.OnSoulFireAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
    @Redirect(method = "renderFire", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/SpriteIdentifier;getSprite()Lnet/minecraft/client/texture/Sprite;", ordinal = 0))
    private Sprite getSprite0(SpriteIdentifier obj, MatrixStack matrices, VertexConsumerProvider vertexConsumers, Entity entity) {
        if (((OnSoulFireAccessor)entity).isRenderSoulFire()) {
//            return SOUL_FIRE_0.getSprite();
            return Main.BLANK_FIRE_0.get();
        }
        return obj.getSprite();
    }

    @Redirect(method = "renderFire", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/SpriteIdentifier;getSprite()Lnet/minecraft/client/texture/Sprite;", ordinal = 1))
    private Sprite getSprite1(SpriteIdentifier obj, MatrixStack matrices, VertexConsumerProvider vertexConsumers, Entity entity) {
        if (((OnSoulFireAccessor)entity).isRenderSoulFire()) {
//            return SOUL_FIRE_1.getSprite();
            return Main.BLANK_FIRE_1.get();
        }
        return obj.getSprite();
    }

    @Shadow
    public Camera camera;

    @Inject(method = "renderFire", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;push()V"))
    private void setShader(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Entity entity, CallbackInfo ci) {
        RenderSystem.setShader(GameRendererSetting::getRenderTypeCustomTint);

    }

    @WrapOperation(method = "drawFireVertex", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/VertexConsumer;color(IIII)Lnet/minecraft/client/render/VertexConsumer;"))
    private static VertexConsumer changeColor(VertexConsumer instance, int r, int g, int b, int a, Operation<VertexConsumer> original) {
        return original.call(instance, 0, 255, 200, 255);
    }

//    @Inject(method = "renderFire", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;pop()V"))
//    private void addOverlay(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Entity entity, CallbackInfo ci) {
//        matrices.push();
//        float f = entity.getWidth() * 1.4f;
//        matrices.scale(f, f, f);
//        float g = 0.5f;
//        float h = 0.0f;
//        float i = entity.getHeight() / f;
//        float j = 0.0f;
//        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-this.camera.getYaw()));
//        matrices.translate(0.0f, 0.0f, -0.3f + (float)((int)i) * 0.02f);
//        float k = 0.0f;
//        int l = 0;
//        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(TexturedRenderLayers.getEntityCutout());
//        MatrixStack.Entry entry = matrices.peek();
//        while (i > 0.0f) {
//            Sprite sprite3 = l % 2 == 0 ? Main.BLANK_FIRE_0_OVERLAY.get() : Main.BLANK_FIRE_1_OVERLAY.get();
//            float m = sprite3.getMinU();
//            float n = sprite3.getMinV();
//            float o = sprite3.getMaxU();
//            float p = sprite3.getMaxV();
//            if (l / 2 % 2 == 0) {
//                float q = o;
//                o = m;
//                m = q;
//            }
//            vertexConsumer.vertex(entry.getPositionMatrix(), g - 0.0f, 0.0f - j, k).color(195, 255, 0, 255).texture(o, p).overlay(0, 10).light(LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE).normal(entry.getNormalMatrix(), 0.0f, 1.0f, 0.0f).next();
//            vertexConsumer.vertex(entry.getPositionMatrix(), -g - 0.0f, 0.0f - j, k).color(195, 255, 0, 255).texture(m, p).overlay(0, 10).light(LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE).normal(entry.getNormalMatrix(), 0.0f, 1.0f, 0.0f).next();
//            vertexConsumer.vertex(entry.getPositionMatrix(), -g - 0.0f, 1.4f - j, k).color(195, 255, 0, 255).texture(m, n).overlay(0, 10).light(LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE).normal(entry.getNormalMatrix(), 0.0f, 1.0f, 0.0f).next();
//            vertexConsumer.vertex(entry.getPositionMatrix(), g - 0.0f, 1.4f - j, k).color(195, 255, 0, 255).texture(o, n).overlay(0, 10).light(LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE).normal(entry.getNormalMatrix(), 0.0f, 1.0f, 0.0f).next();
//            i -= 0.45f;
//            j -= 0.45f;
//            g *= 0.9f;
//            k += 0.03f;
//            ++l;
//        }
//        matrices.pop();
//
//        RenderSystem.setShader(GameRenderer::getPositionProgram);
//    }

}