package com.oscimate.firorize.mixin.fire_overlays.client;


import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.oscimate.firorize.Main;
import com.oscimate.firorize.RenderFireColorAccessor;
import com.oscimate.firorize.config.FireHeightSliderWidget;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;

@SuppressWarnings("ConstantConditions")
@Environment(EnvType.CLIENT)
@Mixin(InGameOverlayRenderer.class)
public class InGameOverlayRendererMixin {
    @Inject(method = "renderFireOverlay",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V"))
    private static void onRenderFireOverlay(MinecraftClient client, MatrixStack matrices, CallbackInfo ci) {
        matrices.translate(0.0,FireHeightSliderWidget.getFireHeight(Main.CONFIG_MANAGER.getCurrentFireHeightSlider()), 0.0);
    }

    @Inject(method = "renderFireOverlay", at = @At("HEAD"))
    private static void changeShader(MinecraftClient client, MatrixStack matrices, CallbackInfo ci) {
        Main.settingFireColor(client.player);
    }

    @WrapOperation(method = "renderFireOverlay", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/SpriteIdentifier;getSprite()Lnet/minecraft/client/texture/Sprite;"))
    private static Sprite renderOverlay(SpriteIdentifier instance, Operation<Sprite> original, MinecraftClient client, MatrixStack matrices) {
        int fireColor = ((RenderFireColorAccessor)client.player).firorize$getRenderFireColor()[0];
        if (fireColor < 1) {
            Sprite sprite = new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, Identifier.of("block/fire_1_"+Math.abs(((RenderFireColorAccessor)client.player).firorize$getRenderFireColor()[0])+"_"+Math.abs(((RenderFireColorAccessor)client.player).firorize$getRenderFireColor()[1]))).getSprite();
            return sprite.getContents().getId().equals(MissingSprite.getMissingSpriteId()) ? new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, Identifier.of("block/fire_1_"+Math.abs(Main.CONFIG_MANAGER.getCurrentBlockFireColors().getRight()[0])+"_"+Math.abs(Main.CONFIG_MANAGER.getCurrentBlockFireColors().getRight()[1]))).getSprite() : sprite;
        } else if (fireColor == 2) {
            return new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, Identifier.of("block/fire_1")).getSprite();
        }
        return original.call(instance);
    }
}