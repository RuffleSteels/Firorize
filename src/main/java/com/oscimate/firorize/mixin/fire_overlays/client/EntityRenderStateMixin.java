package com.oscimate.firorize.mixin.fire_overlays.client;

import com.oscimate.firorize.RenderFireColorAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Carries the per-entity fire colour on the render state. Entity fire is now drawn from
 * {@code FireCommandRenderer}, which only receives an {@link EntityRenderState} (not the entity),
 * so the colour computed at render-state build time is stashed here.
 */
@Environment(EnvType.CLIENT)
@Mixin(EntityRenderState.class)
public class EntityRenderStateMixin implements RenderFireColorAccessor {
    @Unique
    private int[] firorize$renderFireColor;

    @Override
    public int[] firorize$getRenderFireColor() {
        return firorize$renderFireColor;
    }

    @Override
    public void firorize$setRenderFireColor(int[] renderFireColor) {
        this.firorize$renderFireColor = renderFireColor;
    }
}
