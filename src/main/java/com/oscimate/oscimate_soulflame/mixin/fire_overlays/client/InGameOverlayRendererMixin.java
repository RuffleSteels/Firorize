package com.oscimate.oscimate_soulflame.mixin.fire_overlays.client;


import com.google.common.base.Suppliers;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import com.oscimate.oscimate_soulflame.GameRendererSetting;
import com.oscimate.oscimate_soulflame.Main;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@SuppressWarnings("ConstantConditions")
@Environment(EnvType.CLIENT)
@Mixin(InGameOverlayRenderer.class)
public class InGameOverlayRendererMixin {


    @Inject(method = "renderFireOverlay",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V"))
    private static void onRenderFireOverlay(MinecraftClient client, MatrixStack matrices, CallbackInfo ci) {
        RenderSystem.setShader(GameRendererSetting::getRenderTypeCustomTint);
        matrices.translate(0.0, Main.currentFireHeight, 0.0);
    }

    @WrapOperation(method = "renderFireOverlay", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShader(Ljava/util/function/Supplier;)V"))
    private static void changeShader(Supplier<ShaderProgram> program, Operation<Void> original) {
        original.call((Supplier<ShaderProgram>) GameRendererSetting::getRenderTypeCustomTint);
    }

    @WrapOperation(method = "renderFireOverlay", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/VertexConsumer;color(FFFF)Lnet/minecraft/client/render/VertexConsumer;"))
    private static VertexConsumer changeColor(VertexConsumer instance, float red, float green, float blue, float alpha, Operation<VertexConsumer> original) {
        return original.call(instance, 0.0f, 1.0f, 200/255f, 1f).light(0);
    }


    @Inject(method = "renderFireOverlay",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;disableBlend()V"))
    private static void renderOverlay(MinecraftClient client, MatrixStack matrices, CallbackInfo ci, @Local BufferBuilder bufferBuilder) {
        Sprite sprite = Main.BLANK_FIRE_1_OVERLAY.get();
        RenderSystem.setShader(GameRendererSetting::getRenderTypeCustomTint);
        float f = sprite.getMinU();
        float g = sprite.getMaxU();
        float h = (f + g) / 2.0f;
        float i = sprite.getMinV();
        float j = sprite.getMaxV();
        float k = (i + j) / 2.0f;
        float l = sprite.getAnimationFrameDelta();
        float m = MathHelper.lerp(l, f, h);
        float n = MathHelper.lerp(l, g, h);
        float o = MathHelper.lerp(l, i, k);
        float p = MathHelper.lerp(l, j, k);
        float q = 1.0f;
        for (int r = 0; r < 2; ++r) {
            matrices.push();
            float s = -0.5f;
            float t = 0.5f;
            float u = -0.5f;
            float v = 0.5f;
            float w = -0.5f;
            matrices.translate((float) (-(r * 2 - 1)) * 0.24f, -0.3f, 0.0f);
            matrices.translate(0.0, Main.currentFireHeight, 0.0);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) (r * 2 - 1) * 10.0f));
            Matrix4f matrix4f = matrices.peek().getPositionMatrix();
            bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE);
            bufferBuilder.vertex(matrix4f, -0.5f, -0.5f, -0.5f).color(195/255f, 1.0f, 0f, 1f).texture(n, p).light(0).next();
            bufferBuilder.vertex(matrix4f, 0.5f, -0.5f, -0.5f).color(195/255f, 1.0f, 0f, 1f).texture(m, p).light(0).next();
            bufferBuilder.vertex(matrix4f, 0.5f, 0.5f, -0.5f).color(195/255f, 1.0f, 0f, 1f).texture(m, o).light(0).next();
            bufferBuilder.vertex(matrix4f, -0.5f, 0.5f, -0.5f).color(195/255f, 1.0f, 0f, 1f).texture(n, o).light(0).next();
            BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
            matrices.pop();
        }
    }

    @Redirect(method = "renderFireOverlay", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/SpriteIdentifier;getSprite()Lnet/minecraft/client/texture/Sprite;"))
    private static Sprite getSprite(SpriteIdentifier obj, MinecraftClient client) {
        return Main.BLANK_FIRE_1.get();
//        if (((OnSoulFireAccessor) client.player).isRenderSoulFire()) {
//            return SOUL_FIRE_1.get();
//        }
//        return obj.getSprite();
    }

//    @Inject(method = "renderFireOverlay", at = @At(value = "INVOKE_ASSIGN", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderTexture(ILnet/minecraft/util/Identifier;)V"), locals = LocalCapture.CAPTURE_FAILEXCEPTION)
//    private static void changeSprite(MinecraftClient client, MatrixStack matrices, CallbackInfo ci, BufferBuilder bufferBuilder, Sprite sprite) {
//        if (((OnSoulFireAccessor) client.player).isRenderSoulFire()) {
//            sprite = SOUL_FIRE_1.getSprite();
//        }
//    }
}