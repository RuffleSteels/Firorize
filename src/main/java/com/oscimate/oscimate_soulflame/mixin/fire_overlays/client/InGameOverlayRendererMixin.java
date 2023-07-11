package com.oscimate.oscimate_soulflame.mixin.fire_overlays.client;


import com.oscimate.oscimate_soulflame.Main;
import com.oscimate.oscimate_soulflame.OnSoulFireAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.BufferBuilder;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@SuppressWarnings("ConstantConditions")
@Environment(EnvType.CLIENT)
@Mixin(InGameOverlayRenderer.class)
public class InGameOverlayRendererMixin {
    private static final SpriteIdentifier SOUL_FIRE_1 = new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, new Identifier("block/soul_fire_1"));



    @Inject(method = "renderFireOverlay",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V"))
    private static void onRenderFireOverlay(MinecraftClient client, MatrixStack matrices, CallbackInfo ci) {
        matrices.translate(0.0, Main.currentFireHeight, 0.0);
    }

    @Redirect(method = "renderFireOverlay", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/SpriteIdentifier;getSprite()Lnet/minecraft/client/texture/Sprite;"))
    private static Sprite getSprite(SpriteIdentifier obj, MinecraftClient client) {
        if (((OnSoulFireAccessor) client.player).isRenderSoulFire()) {
            return SOUL_FIRE_1.getSprite();
        }
        return obj.getSprite();
    }

//    @Inject(method = "renderFireOverlay", at = @At(value = "INVOKE_ASSIGN", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderTexture(ILnet/minecraft/util/Identifier;)V"), locals = LocalCapture.CAPTURE_FAILEXCEPTION)
//    private static void changeSprite(MinecraftClient client, MatrixStack matrices, CallbackInfo ci, BufferBuilder bufferBuilder, Sprite sprite) {
//        if (((OnSoulFireAccessor) client.player).isRenderSoulFire()) {
//            sprite = SOUL_FIRE_1.getSprite();
//        }
//    }
}