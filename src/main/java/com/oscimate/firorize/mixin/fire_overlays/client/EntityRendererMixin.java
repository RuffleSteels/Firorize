package com.oscimate.firorize.mixin.fire_overlays.client;

import com.oscimate.firorize.Main;
import com.oscimate.firorize.RenderFireColorAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Computes the fire colour for an entity when its render state is built, and copies it onto the
 * render state so {@code FireCommandRenderer} can recolour the entity-fire sprites.
 */
@Environment(EnvType.CLIENT)
@Mixin(EntityRenderer.class)
public class EntityRendererMixin {
    @Inject(method = "updateRenderState", at = @At("TAIL"))
    private void firorize$captureFireColor(Entity entity, EntityRenderState state, float tickProgress, CallbackInfo ci) {
        Main.settingFireColor(entity);
        int[] color = ((RenderFireColorAccessor) entity).firorize$getRenderFireColor();
        ((RenderFireColorAccessor) (Object) state).firorize$setRenderFireColor(color);
    }
}
