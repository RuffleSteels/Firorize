package com.oscimate.firorize.mixin.fire_overlays.client;

import com.oscimate.firorize.FireSprites;
import com.oscimate.firorize.RenderFireColorAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.render.command.FireCommandRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.Material;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Recolours the two entity-fire atlas sprites (FIRE_0 / FIRE_1) using the colour carried on the
 * render state. Replaces the old {@code EntityRenderDispatcher#renderFire} redirects.
 */
@Environment(EnvType.CLIENT)
@Mixin(FireCommandRenderer.class)
public class FireCommandRendererMixin {

    @Redirect(
            method = "render(Lnet/minecraft/client/util/math/PoseStack$Entry;Lnet/minecraft/client/render/MultiBufferSource;Lnet/minecraft/client/render/entity/state/EntityRenderState;Lorg/joml/Quaternionf;Lnet/minecraft/client/texture/AtlasManager;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/texture/AtlasManager;getSprite(Lnet/minecraft/client/util/Material;)Lnet/minecraft/client/texture/TextureAtlasSprite;", ordinal = 0))
    private TextureAtlasSprite firorize$fire0(AtlasManager atlas, Material id,
                                  PoseStack.Entry entry, MultiBufferSource vertexConsumers,
                                  EntityRenderState state, Quaternionf rotation, AtlasManager am) {
        int[] color = ((RenderFireColorAccessor) (Object) state).firorize$getRenderFireColor();
        return FireSprites.resolve(atlas, color, "block/fire_0", atlas.getSprite(id));
    }

    @Redirect(
            method = "render(Lnet/minecraft/client/util/math/PoseStack$Entry;Lnet/minecraft/client/render/MultiBufferSource;Lnet/minecraft/client/render/entity/state/EntityRenderState;Lorg/joml/Quaternionf;Lnet/minecraft/client/texture/AtlasManager;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/texture/AtlasManager;getSprite(Lnet/minecraft/client/util/Material;)Lnet/minecraft/client/texture/TextureAtlasSprite;", ordinal = 1))
    private TextureAtlasSprite firorize$fire1(AtlasManager atlas, Material id,
                                  PoseStack.Entry entry, MultiBufferSource vertexConsumers,
                                  EntityRenderState state, Quaternionf rotation, AtlasManager am) {
        int[] color = ((RenderFireColorAccessor) (Object) state).firorize$getRenderFireColor();
        return FireSprites.resolve(atlas, color, "block/fire_1", atlas.getSprite(id));
    }
}
