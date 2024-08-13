package com.oscimate.firorize.mixin.fire_overlays.client;

import com.oscimate.firorize.SpriteContentsDuck;
import net.minecraft.client.resource.metadata.AnimationResourceMetadata;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.SpriteContents;
import net.minecraft.client.texture.SpriteDimensions;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpriteContents.class)
public class SpriteContentsMixin implements SpriteContentsDuck {

    @Mutable
    @Unique @Final
    AnimationResourceMetadata metadataa;
    @Override
    public AnimationResourceMetadata firorize$getMetadata() {
        return metadataa;
    }
    @Inject(method = "<init>", at = @At("RETURN"))
    private void test(Identifier id, SpriteDimensions dimensions, NativeImage image, AnimationResourceMetadata metadata, CallbackInfo ci) {
        metadataa = metadata;
    }
}

