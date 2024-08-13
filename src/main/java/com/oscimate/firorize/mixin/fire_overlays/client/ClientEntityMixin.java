package com.oscimate.firorize.mixin.fire_overlays.client;


import com.oscimate.firorize.RenderFireColorAccessor;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Entity.class)
public class ClientEntityMixin implements RenderFireColorAccessor {
    @Unique
    private int[] renderFireColor;

    @Override
    public int[] firorize$getRenderFireColor() {
        return renderFireColor;
    }

    @Override
    public void firorize$setRenderFireColor(int[] renderFireColor) {
        this.renderFireColor = renderFireColor;
    }

}
