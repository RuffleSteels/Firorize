package com.oscimate.oscimate_soulflame.mixin.fire_overlays.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.platform.GlConst;
import com.oscimate.oscimate_soulflame.ColorizeMath;
import com.oscimate.oscimate_soulflame.Main;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.client.render.model.SpriteAtlasManager;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.resource.ReloadableResourceManagerImpl;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourceReload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Unit;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(ReloadableResourceManagerImpl.class)
public class MinecraftClientMixin {


//    @Shadow @Final private TextureManager textureManager;

//    activeManager
//    reload
//    @Inject(method="<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;guiAtlasManager:Lnet/minecraft/client/texture/GuiAtlasManager;", opcode = Opcodes.PUTFIELD, shift = At.Shift.BEFORE))
//    @Inject(method="<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/resource/ReloadableResourceManagerImpl;reload(Ljava/util/concurrent/Executor;Ljava/util/concurrent/Executor;Ljava/util/concurrent/CompletableFuture;Ljava/util/List;)Lnet/minecraft/resource/ResourceReload;", shift = At.Shift.AFTER))
//    @Inject(method="reload", at = @At(value = "FIELD", target = "Lnet/minecraft/resource/ReloadableResourceManagerImpl;activeManager:Lnet/minecraft/resource/LifecycledResourceManager;", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
//    private void test(Executor prepareExecutor, Executor applyExecutor, CompletableFuture<Unit> initialStage, List<ResourcePack> packs, CallbackInfoReturnable<ResourceReload> cir) {
//        System.out.println("STITCHING");
//
//        long sizeBytes = (long) 16 * 16 * NativeImage.Format.RGBA.getChannelCount();
//
//        Main.pointer = MemoryUtil.nmemAlloc(sizeBytes);
//
//
//        System.out.println(MinecraftClient.getInstance().getTextureManager().getTexture(new Identifier("textures/block/fire_0.png")));
//
//        GL11.glGetTexImage(MinecraftClient.getInstance().getTextureManager().getTexture(new Identifier("textures/block/fire_0.png")).getGlId(), 0, NativeImage.Format.RGBA.toGl(), GlConst.GL_UNSIGNED_BYTE, Main.pointer);
//
//        System.out.println("got");
//
//        ByteBuffer baseBuffer = MemoryUtil.memByteBuffer(Main.pointer, (int) sizeBytes);
//
//        System.out.println("made buffer");
////        long overlayPointer = MemoryUtil.nmemAlloc(sizeBytes);
////
////        GL11.glGetTexImage(MinecraftClient.getInstance().getTextureManager().getTexture(new Identifier("block/fire_0_overlay")).getGlId(), 0, NativeImage.Format.RGBA.toGl(), GlConst.GL_UNSIGNED_BYTE, overlayPointer);
////
////        ByteBuffer overlayBuffer = MemoryUtil.memByteBuffer(overlayPointer, (int) sizeBytes);
//
//        for (int y = 0; y < 16; y++) {
//            for (int x = 0; x < 16; x++) {
//                int index = (y * 16 + x) * 4;
//
////                int overlayR = overlayBuffer.get(index);
////                int overlayG = overlayBuffer.get(index + 1);
////                int overlayB = overlayBuffer.get(index + 2);
////                int overlayA = overlayBuffer.get(index + 3);
//
////                float[] colorizedOverlay = ColorizeMath.applyColorization(new float[]{overlayR/255f, overlayG/255f, overlayB/255f, overlayA/255f}, new float[]{255/255f, 0/255f, 0/255f, 1f});
////
////                int[] co = ColorizeMath.convert(colorizedOverlay);
//
//                int baseR = baseBuffer.get(index);
//                int baseG = baseBuffer.get(index + 1);
//                int baseB = baseBuffer.get(index + 2);
//                int baseA = baseBuffer.get(index + 3);
//
//                System.out.println("got RGBS");
//
//                float[] colorizedBase = ColorizeMath.applyColorization(new float[]{baseR/255f, baseG/255f, baseB/255f, baseA/255f}, new float[]{255/255f, 0/255f, 0/255f, 1f});
//
//                int[] cb = ColorizeMath.convert(colorizedBase);
//
//                System.out.println("colorized");
//
////                int outR = (co[0] * co[3] + cb[0] * (255 - co[3])) / 255;
////                int outG = (co[1] * co[3] + cb[1] * (255 - co[3])) / 255;
////                int outB = (co[2] * co[3] + cb[2] * (255 - co[3])) / 255;
//
//                baseBuffer.put(index, (byte) cb[0]);
//                baseBuffer.put(index + 1, (byte) cb[1]);
//                baseBuffer.put(index + 2, (byte) cb[2]);
//                baseBuffer.put(index + 3, (byte) baseA);
//
//                System.out.println("put buffer");
//            }
//        }
//
//        GL11.glTexSubImage2D(MinecraftClient.getInstance().getTextureManager().getTexture(new Identifier("textures/block/fire_0.png")).getGlId(), 0, 0, 0, 16, 16, NativeImage.Format.RGBA.toGl(), GlConst.GL_UNSIGNED_BYTE, baseBuffer);
//
//        System.out.println("set tex");
//
//        MemoryUtil.nmemFree(Main.pointer);
//
//        System.out.println("freed mem");
//    }
}
