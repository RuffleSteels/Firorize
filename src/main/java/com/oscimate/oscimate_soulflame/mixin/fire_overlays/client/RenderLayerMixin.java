package com.oscimate.oscimate_soulflame.mixin.fire_overlays.client;

import com.google.common.collect.ImmutableList;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.oscimate.oscimate_soulflame.CustomRenderLayer;
import net.minecraft.client.render.RenderLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(RenderLayer.class)
public class RenderLayerMixin {

    @ModifyReturnValue(method = "getBlockLayers", at = @At("RETURN"))
    private static List<RenderLayer> addBlockLayer(List<RenderLayer> original) {
        ImmutableList<RenderLayer> immutableList = ImmutableList.<RenderLayer>builder().add(CustomRenderLayer.getCustomTint()).add(CustomRenderLayer.getTriangle()).addAll(original).build();
        return immutableList;
    }

}
