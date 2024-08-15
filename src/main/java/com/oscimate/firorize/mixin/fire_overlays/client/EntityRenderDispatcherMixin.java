package com.oscimate.firorize.mixin.fire_overlays.client;


import com.oscimate.firorize.Main;
import com.oscimate.firorize.RenderFireColorAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Environment(EnvType.CLIENT)
@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
    @Redirect(method = "renderFire", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/SpriteIdentifier;getSprite()Lnet/minecraft/client/texture/Sprite;", ordinal = 0))
    private Sprite getSprite0(SpriteIdentifier obj, MatrixStack matrices, VertexConsumerProvider vertexConsumers, Entity entity) {
        Main.settingFireColor(entity);
        int fireColor = ((RenderFireColorAccessor) entity).firorize$getRenderFireColor()[0];
        if (fireColor < 1) {
            Sprite sprite = new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, Identifier.of("block/fire_1_"+Math.abs(((RenderFireColorAccessor)entity).firorize$getRenderFireColor()[0])+"_"+Math.abs(((RenderFireColorAccessor)entity).firorize$getRenderFireColor()[1]))).getSprite();
            return sprite.getContents().getId().equals(MissingSprite.getMissingSpriteId()) ? new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, Identifier.of("block/fire_1_"+Math.abs(Main.CONFIG_MANAGER.getCurrentBlockFireColors().getRight()[0])+"_"+Math.abs(Main.CONFIG_MANAGER.getCurrentBlockFireColors().getRight()[1]))).getSprite() : sprite;
        } else if (fireColor == 2) {
            return new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, Identifier.of("block/fire_0")).getSprite();
        }
        return obj.getSprite();
    }

    @Redirect(method = "renderFire", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/SpriteIdentifier;getSprite()Lnet/minecraft/client/texture/Sprite;", ordinal = 1))
    private Sprite getSprite1(SpriteIdentifier obj, MatrixStack matrices, VertexConsumerProvider vertexConsumers, Entity entity) {
        Main.settingFireColor(entity);
        int fireColor = ((RenderFireColorAccessor)entity).firorize$getRenderFireColor()[0];
        if (fireColor < 1) {
            Sprite sprite = new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, Identifier.of("block/fire_1_"+Math.abs(((RenderFireColorAccessor)entity).firorize$getRenderFireColor()[0])+"_"+Math.abs(((RenderFireColorAccessor)entity).firorize$getRenderFireColor()[1]))).getSprite();
            return sprite.getContents().getId().equals(MissingSprite.getMissingSpriteId()) ? new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, Identifier.of("block/fire_1_"+Math.abs(Main.CONFIG_MANAGER.getCurrentBlockFireColors().getRight()[0])+"_"+Math.abs(Main.CONFIG_MANAGER.getCurrentBlockFireColors().getRight()[1]))).getSprite() : sprite;
        } else if (fireColor == 2) {
            return new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, Identifier.of("block/fire_1")).getSprite();
        }
        return obj.getSprite();
    }
}