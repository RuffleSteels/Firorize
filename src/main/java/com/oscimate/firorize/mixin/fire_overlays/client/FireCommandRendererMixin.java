package com.oscimate.firorize.mixin.fire_overlays.client;

import com.oscimate.firorize.FireSprites;
import com.oscimate.firorize.RenderFireColorAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.FlameFeatureRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.client.resources.model.sprite.SpriteId;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Recolours the two entity-fire atlas sprites (FIRE_0 / FIRE_1) using the colour carried on the
 * render state. In 26.1.2 entity fire is drawn by {@link FlameFeatureRenderer#renderFlame}, which
 * looks the sprites up via {@code AtlasManager.get(SpriteId)}.
 */
@Environment(EnvType.CLIENT)
@Mixin(FlameFeatureRenderer.class)
public class FireCommandRendererMixin {

    @Redirect(
            method = "renderFlame(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lorg/joml/Quaternionf;Lnet/minecraft/client/resources/model/sprite/AtlasManager;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/model/sprite/AtlasManager;get(Lnet/minecraft/client/resources/model/sprite/SpriteId;)Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;", ordinal = 0))
    private TextureAtlasSprite firorize$fire0(AtlasManager atlas, SpriteId id,
                                  PoseStack.Pose pose, MultiBufferSource bufferSource,
                                  EntityRenderState state, Quaternionf rotation, AtlasManager am) {
        int[] color = ((RenderFireColorAccessor) (Object) state).firorize$getRenderFireColor();
        return FireSprites.resolve(atlas, color, "block/fire_0", atlas.get(id));
    }

    @Redirect(
            method = "renderFlame(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lorg/joml/Quaternionf;Lnet/minecraft/client/resources/model/sprite/AtlasManager;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/model/sprite/AtlasManager;get(Lnet/minecraft/client/resources/model/sprite/SpriteId;)Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;", ordinal = 1))
    private TextureAtlasSprite firorize$fire1(AtlasManager atlas, SpriteId id,
                                  PoseStack.Pose pose, MultiBufferSource bufferSource,
                                  EntityRenderState state, Quaternionf rotation, AtlasManager am) {
        int[] color = ((RenderFireColorAccessor) (Object) state).firorize$getRenderFireColor();
        return FireSprites.resolve(atlas, color, "block/fire_1", atlas.get(id));
    }
}
