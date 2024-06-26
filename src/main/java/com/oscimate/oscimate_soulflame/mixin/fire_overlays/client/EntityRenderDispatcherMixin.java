package com.oscimate.oscimate_soulflame.mixin.fire_overlays.client;


import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.oscimate.oscimate_soulflame.CustomRenderLayer;
import com.oscimate.oscimate_soulflame.Main;
import com.oscimate.oscimate_soulflame.RenderFireColorAccessor;
import com.sun.tools.jconsole.JConsoleContext;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.FireBlock;
import net.minecraft.block.SoulFireBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;

@Environment(EnvType.CLIENT)
@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
    @Unique
    private static Entity currentEntity;

    @Redirect(method = "renderFire", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/SpriteIdentifier;getSprite()Lnet/minecraft/client/texture/Sprite;", ordinal = 0))
    private Sprite getSprite0(SpriteIdentifier obj, MatrixStack matrices, VertexConsumerProvider vertexConsumers, Entity entity) {
        Main.settingFireColor(entity);
        int fireColor = ((RenderFireColorAccessor) entity).getRenderFireColor()[0];
        if (fireColor < 1) {
            return Main.BLANK_FIRE_0.get();
        } else if (fireColor == 1) {
            return Main.SOUL_FIRE_0.get();
        }
        return obj.getSprite();
    }

    @Redirect(method = "renderFire", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/SpriteIdentifier;getSprite()Lnet/minecraft/client/texture/Sprite;", ordinal = 1))
    private Sprite getSprite1(SpriteIdentifier obj, MatrixStack matrices, VertexConsumerProvider vertexConsumers, Entity entity) {
        currentEntity = entity;
        int fireColor = ((RenderFireColorAccessor)entity).getRenderFireColor()[0];
        if (fireColor < 1) {
            return Main.BLANK_FIRE_1.get();
        } else if (fireColor == 1) {
            return Main.SOUL_FIRE_1.get();
        }
        return obj.getSprite();
    }

    @WrapOperation(method = "renderFire", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/VertexConsumerProvider;getBuffer(Lnet/minecraft/client/render/RenderLayer;)Lnet/minecraft/client/render/VertexConsumer;"))
    private VertexConsumer setShader(VertexConsumerProvider instance, RenderLayer renderLayer, Operation<VertexConsumer> original, MatrixStack matrices, VertexConsumerProvider vertexConsumers, Entity entity, Quaternionf rotation) {
        if (((RenderFireColorAccessor)entity).getRenderFireColor()[0] < 1) {
            return original.call(instance, CustomRenderLayer.getCustomTint());
        }
        return original.call(instance, renderLayer);
    }


    @WrapOperation(method = "drawFireVertex", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/VertexConsumer;color(IIII)Lnet/minecraft/client/render/VertexConsumer;"))
    private static VertexConsumer changeColor(VertexConsumer instance, int r, int g, int b, int a, Operation<VertexConsumer> original) {
        if (((RenderFireColorAccessor) currentEntity).getRenderFireColor()[0] < 1) {
            Color cfc = new Color(((RenderFireColorAccessor) currentEntity).getRenderFireColor()[0]);
            return original.call(instance, cfc.getRed(), cfc.getGreen(), cfc.getBlue(), 255);
        }
        return original.call(instance, r, g, b, a);
    }

    @Inject(method = "renderFire", at = @At(value = "INVOKE",  target = "Lnet/minecraft/client/util/math/MatrixStack;pop()V"))
    private void addOverlay(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Entity entity, Quaternionf rotation, CallbackInfo ci) {
        if (((RenderFireColorAccessor)entity).getRenderFireColor()[0] < 1) {
            Color cfc = new Color(((RenderFireColorAccessor) entity).getRenderFireColor()[1]);
            float f = entity.getWidth() * 1.4f;
            float g = 0.5f;
            float i = entity.getHeight() / f;
            float j = 0.0f;
            float k = 0.0f;
            int l = 0;
            VertexConsumer vertexConsumer = vertexConsumers.getBuffer(CustomRenderLayer.getCustomTint());
            MatrixStack.Entry entry = matrices.peek();
            while (i > 0.0f) {
                Sprite sprite3 = l % 2 == 0 ? Main.BLANK_FIRE_0_OVERLAY.get() : Main.BLANK_FIRE_1_OVERLAY.get();
                float m = sprite3.getMinU();
                float n = sprite3.getMinV();
                float o = sprite3.getMaxU();
                float p = sprite3.getMaxV();
                if (l / 2 % 2 == 0) {
                    float q = o;
                    o = m;
                    m = q;
                }
                vertexConsumer.vertex(entry.getPositionMatrix(), g - 0.0f, 0.0f - j, k).color(cfc.getRed(), cfc.getGreen(), cfc.getBlue(), 255).texture(o, p).overlay(0, 10).light(LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE).normal(entry.getNormalMatrix(), 0.0f, 1.0f, 0.0f).next();
                vertexConsumer.vertex(entry.getPositionMatrix(), -g - 0.0f, 0.0f - j, k).color(cfc.getRed(), cfc.getGreen(), cfc.getBlue(), 255).texture(m, p).overlay(0, 10).light(LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE).normal(entry.getNormalMatrix(), 0.0f, 1.0f, 0.0f).next();
                vertexConsumer.vertex(entry.getPositionMatrix(), -g - 0.0f, 1.4f - j, k).color(cfc.getRed(), cfc.getGreen(), cfc.getBlue(), 255).texture(m, n).overlay(0, 10).light(LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE).normal(entry.getNormalMatrix(), 0.0f, 1.0f, 0.0f).next();
                vertexConsumer.vertex(entry.getPositionMatrix(), g - 0.0f, 1.4f - j, k).color(cfc.getRed(), cfc.getGreen(), cfc.getBlue(), 255).texture(o, n).overlay(0, 10).light(LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE).normal(entry.getNormalMatrix(), 0.0f, 1.0f, 0.0f).next();
                i -= 0.45f;
                j -= 0.45f;
                g *= 0.9f;
                k += 0.03f;
                ++l;
            }
        }
    }

}