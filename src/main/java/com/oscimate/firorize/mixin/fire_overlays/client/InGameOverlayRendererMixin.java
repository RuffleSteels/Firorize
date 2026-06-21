package com.oscimate.firorize.mixin.fire_overlays.client;

import com.oscimate.firorize.FireSprites;
import com.oscimate.firorize.Main;
import com.oscimate.firorize.RenderFireColorAccessor;
import com.oscimate.firorize.config.FireHeightSliderWidget;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Recolours the first-person fire overlay and applies the fire-height slider. In 1.21.6 the fire
 * sprite is chosen by the caller and passed into the now-static
 * {@code renderFireOverlay(MatrixStack, VertexConsumerProvider, Sprite)}, so the sprite is swapped
 * via {@link ModifyVariable} rather than by wrapping a {@code getSprite()} call.
 */
@Environment(EnvType.CLIENT)
@Mixin(InGameOverlayRenderer.class)
public class InGameOverlayRendererMixin {

    @ModifyVariable(method = "renderFireOverlay", at = @At("HEAD"), argsOnly = true)
    private static Sprite firorize$recolourFireSprite(Sprite sprite) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return sprite;
        }
        Main.settingFireColor(client.player);
        int[] color = ((RenderFireColorAccessor) client.player).firorize$getRenderFireColor();
        return FireSprites.resolve(FireSprites.atlasManager(), color, "block/fire_1", sprite);
    }

    @Inject(method = "renderFireOverlay",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V"))
    private static void firorize$applyFireHeight(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Sprite sprite, CallbackInfo ci) {
        matrices.translate(0.0, FireHeightSliderWidget.getFireHeight(Main.CONFIG_MANAGER.getCurrentFireHeightSlider()), 0.0);
    }
}
