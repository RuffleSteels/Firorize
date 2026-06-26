package com.oscimate.firorize.mixin.fire_overlays.client;

import com.oscimate.firorize.FireSprites;
import com.oscimate.firorize.Main;
import com.oscimate.firorize.RenderFireColorAccessor;
import com.oscimate.firorize.config.FireHeightSliderWidget;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Recolours the first-person fire overlay and applies the fire-height slider. In 1.21.6 the fire
 * sprite is chosen by the caller and passed into the now-static
 * {@code renderFireOverlay(PoseStack, MultiBufferSource, TextureAtlasSprite)}, so the sprite is swapped
 * via {@link ModifyVariable} rather than by wrapping a {@code getSprite()} call.
 */
@Environment(EnvType.CLIENT)
@Mixin(ScreenEffectRenderer.class)
public class InGameOverlayRendererMixin {

    @ModifyVariable(method = "renderFireOverlay", at = @At("HEAD"), argsOnly = true)
    private static TextureAtlasSprite firorize$recolourFireSprite(TextureAtlasSprite sprite) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return sprite;
        }
        Main.settingFireColor(client.player);
        int[] color = ((RenderFireColorAccessor) client.player).firorize$getRenderFireColor();
        return FireSprites.resolve(FireSprites.atlasManager(), color, "block/fire_1", sprite);
    }

    @Inject(method = "renderFireOverlay",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/PoseStack;translate(FFF)V"))
    private static void firorize$applyFireHeight(PoseStack matrices, MultiBufferSource vertexConsumers, TextureAtlasSprite sprite, CallbackInfo ci) {
        matrices.translate(0.0, FireHeightSliderWidget.getFireHeight(Main.CONFIG_MANAGER.getCurrentFireHeightSlider()), 0.0);
    }
}
