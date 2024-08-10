package com.oscimate.firorize.mixin.fire_overlays.client;

import net.minecraft.client.render.block.BlockModelRenderer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockModelRenderer.class)
public class BlockModelRendererMixin {
//    @WrapOperation(method = "renderQuads", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/util/math/MathHelper;clamp(FFF)F"))
//    private static float addTintIndex(float value, float min, float max, Operation<Float> original, @Local BakedQuad bakedQuad) {
//        if (bakedQuad.getSprite() == Main.BLANK_FIRE_0_OVERLAY.get()) {
//            return original.call(ChangeFireColorScreen.pickedColor[1].getRed()/255f, min, max);
//        } if (bakedQuad.getSprite() == Main.BLANK_FIRE_0.get()) {
//            return original.call(ChangeFireColorScreen.pickedColor[0].getRed()/255f, min, max);
//        }
//        return original.call(value, min, max);
//    }
//    @WrapOperation(method = "renderQuads", at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/util/math/MathHelper;clamp(FFF)F"))
//    private static float addTintIndex2(float value, float min, float max, Operation<Float> original, @Local BakedQuad bakedQuad) {
//        if (bakedQuad.getSprite() == Main.BLANK_FIRE_0_OVERLAY.get()) {
//            return original.call(ChangeFireColorScreen.pickedColor[1].getGreen()/255f, min, max);
//        } if (bakedQuad.getSprite() == Main.BLANK_FIRE_0.get()) {
//            return original.call(ChangeFireColorScreen.pickedColor[0].getGreen()/255f, min, max);
//        }
//        return original.call(value, min, max);
//    }
//    @WrapOperation(method = "renderQuads", at = @At(value = "INVOKE", ordinal = 2, target = "Lnet/minecraft/util/math/MathHelper;clamp(FFF)F"))
//    private static float addTintIndex3(float value, float min, float max, Operation<Float> original, @Local BakedQuad bakedQuad) {
//        if (bakedQuad.getSprite() == Main.BLANK_FIRE_0_OVERLAY.get()) {
//            return original.call(ChangeFireColorScreen.pickedColor[1].getBlue()/255f, min, max);
//        } if (bakedQuad.getSprite() == Main.BLANK_FIRE_0.get()) {
//            return original.call(ChangeFireColorScreen.pickedColor[0].getBlue()/255f, min, max);
//        }
//        return original.call(value, min, max);
//    }

}
