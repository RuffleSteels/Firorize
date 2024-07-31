package com.oscimate.oscimate_soulflame.mixin.fire_overlays.client;


import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.oscimate.oscimate_soulflame.Main;
import com.oscimate.oscimate_soulflame.RenderFireColorAccessor;
import com.oscimate.oscimate_soulflame.config.FireHeightSliderWidget;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
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
        matrices.translate(0.0,FireHeightSliderWidget.getFireHeight(Main.CONFIG_MANAGER.getCurrentFireHeightSlider()), 0.0);
    }
//
    @Inject(method = "renderFireOverlay", at = @At("HEAD"))
    private static void changeShader(MinecraftClient client, MatrixStack matrices, CallbackInfo ci) {
        Main.settingFireColor(client.player);
    }

//    @WrapOperation(method = "renderFireOverlay", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/VertexConsumer;color(FFFF)Lnet/minecraft/client/render/VertexConsumer;"))
//    private static VertexConsumer changeColor(VertexConsumer instance, float red, float green, float blue, float alpha, Operation<VertexConsumer> original, MinecraftClient client, MatrixStack matrices) {
//        int fireColor = ((RenderFireColorAccessor)client.player).getRenderFireColor()[0];
//        if (fireColor < 1) {
//            Color cfc = new Color(((RenderFireColorAccessor) client.player).getRenderFireColor()[0]);
//            return original.call(instance, cfc.getRed()/255f, cfc.getGreen()/255f, cfc.getBlue()/255f, 1f).light(0);
//        } else {
//            return original.call(instance, red, green, blue, alpha);
//        }
//    }


    @WrapOperation(method = "renderFireOverlay", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/SpriteIdentifier;getSprite()Lnet/minecraft/client/texture/Sprite;"))
    private static Sprite renderOverlay(SpriteIdentifier instance, Operation<Sprite> original, MinecraftClient client, MatrixStack matrices) {
        int fireColor = ((RenderFireColorAccessor)client.player).getRenderFireColor()[0];
        if (fireColor < 1) {
            return new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, Identifier.of("block/fire_1_"+Math.abs(((RenderFireColorAccessor)client.player).getRenderFireColor()[0])+"_"+Math.abs(((RenderFireColorAccessor)client.player).getRenderFireColor()[1]))).getSprite();
        } else if (fireColor == 1) {
            return new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, Identifier.of("block/soul_fire_1")).getSprite();
        }
        return original.call(instance);
    }

//    @Redirect(method = "renderFireOverlay", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/SpriteIdentifier;getSprite()Lnet/minecraft/client/texture/Sprite;"))
//    private static Sprite getSprite(SpriteIdentifier obj, MinecraftClient client) {
//        int fireColor = ((RenderFireColorAccessor)client.player).getRenderFireColor()[0];
//        System.out.println(fireColor);
//        if (fireColor < 1) {
//            return new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, Identifier.of("block/fire_"+Math.abs(((RenderFireColorAccessor)client.player).getRenderFireColor()[0])+"_"+Math.abs(((RenderFireColorAccessor)client.player).getRenderFireColor()[1]))).getSprite();
//        } else if (fireColor == 1) {
//            return Main.SOUL_FIRE_1.get();
//        }
//        return obj.getSprite();
//    }
}