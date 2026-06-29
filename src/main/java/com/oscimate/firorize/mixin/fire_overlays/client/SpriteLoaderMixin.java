package com.oscimate.firorize.mixin.fire_overlays.client;

import com.google.common.collect.ImmutableList;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.oscimate.firorize.ColorizeMath;
import com.oscimate.firorize.Main;
import net.minecraft.client.resource.metadata.AnimationFrameResourceMetadata;
import net.minecraft.client.resource.metadata.AnimationResourceMetadata;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.SpriteContents;
import net.minecraft.client.texture.SpriteDimensions;
import net.minecraft.client.texture.SpriteLoader;
import net.minecraft.util.Identifier;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.awt.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

@Mixin(SpriteLoader.class)
public class SpriteLoaderMixin {
    @Shadow @Final private Identifier id;
    @Unique
    public ByteBuffer deepCopy(ByteBuffer source, ByteBuffer target) {
        int sourceP = source.position();
        int sourceL = source.limit();

        if (null == target) {
            target = ByteBuffer.allocate(source.remaining());
        }
        target.put(source);
        target.flip();

        source.position(sourceP);
        source.limit(sourceL);
        return target;
    }

    @Unique
    ArrayList<Identifier> validIds = new ArrayList<>(List.of(
            Identifier.of("firorize:block/blank_fire_overlay_0"),
            Identifier.of("firorize:block/blank_fire_overlay_1"),
            Identifier.of("firorize:block/blank_fire_0"),
            Identifier.of("firorize:block/blank_fire_1")
    ));

    @Inject(method = "stitch", at = @At("HEAD"))
    @SuppressWarnings("deprecation") // SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE is deprecated but still the supported atlas id in 1.21
    private void addSprites(List<SpriteContents> sp, int mipLevel, Executor executor, CallbackInfoReturnable<SpriteLoader.StitchResult> cir, @Local LocalRef<List<SpriteContents>> sprites) {
        if (id.equals(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)) {
            // Animation for the generated fire sprites, matching blank_fire_{0,1}.png.mcmeta
            // (32 frames of 16x16, reordered 16..31 then 0..15). getMetadata() was removed in 1.21.11.
            List<AnimationFrameResourceMetadata> fireFrames = new ArrayList<>();
            for (int f = 16; f < 32; f++) fireFrames.add(new AnimationFrameResourceMetadata(f));
            for (int f = 0; f < 16; f++) fireFrames.add(new AnimationFrameResourceMetadata(f));
            AnimationResourceMetadata fireAnimation =
                    new AnimationResourceMetadata(Optional.of(fireFrames), Optional.of(16), Optional.of(16), 1, false);

            // Generate a recoloured sprite for every distinct colour used by ANY profile (plus their
            // base colours) — not just the one being edited — because multiple profiles can be active
            // at once and each contributes colours to the in-world fire. Also include the live editing
            // buffer so uncommitted edits preview correctly.
            java.util.LinkedHashMap<String, int[]> distinct = new java.util.LinkedHashMap<>();
            for (var profile : Main.CONFIG_MANAGER.getFireColorPresets().values()) {
                for (var map : profile.getLeft().getLeft()) {
                    for (int[] c : map.values()) distinct.putIfAbsent(c[0] + "-" + c[1], c);
                }
                int[] base = profile.getLeft().getRight();
                distinct.putIfAbsent(base[0] + "-" + base[1], base);
            }
            for (var map : Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft()) {
                for (int[] c : map.values()) distinct.putIfAbsent(c[0] + "-" + c[1], c);
            }
            int[] liveBase = Main.CONFIG_MANAGER.getCurrentBlockFireColors().getRight();
            distinct.putIfAbsent(liveBase[0] + "-" + liveBase[1], liveBase);
            List<int[]> ints = new ArrayList<>(distinct.values());

            ArrayList<Long> pointers = new ArrayList<>();

            ArrayList<SpriteContents> all = new ArrayList<>();

            for (int z = 0; z < 4; z++) {
                for (SpriteContents spriteContents : sp) {
                    if (validIds.contains(spriteContents.getId())) {
                        if (spriteContents.getId().equals(validIds.get(z))) {
                            boolean isOverlay = validIds.subList(0, 2).contains(spriteContents.getId());

                            ByteBuffer original = MemoryUtil.memByteBuffer((((NativeImageInvoker) (Object) ((SpriteContentsInvoker) spriteContents).getImage())).getPointer(), (int) (((NativeImageInvoker) (Object) ((SpriteContentsInvoker) spriteContents).getImage())).getSizeBytes());

                            for (int i = 0; i < ints.size(); i++) {
                                long pointer = MemoryUtil.nmemAlloc(original.capacity());

                                pointers.add(pointer);

                                ByteBuffer baseBuffer = MemoryUtil.memByteBuffer(pointer, original.capacity());

                                deepCopy(original, baseBuffer);

                                ByteBuffer overlayBuffer = null;

                                if (!isOverlay) {
                                    overlayBuffer = MemoryUtil.memByteBuffer(pointers.get(i), original.capacity());
                                }

                                Color c = new Color(ints.get(i)[0]);

                                if (isOverlay) {
                                    c = new Color(ints.get(i)[1]);
                                }

                                float[] vertexColor = new float[]{c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, 1f};

                                for (int y = 0; y < 16 * 32; y++) {
                                    for (int x = 0; x < 16; x++) {
                                        int index = (y * 16 + x) * 4;

                                        int baseR = baseBuffer.get(index) & 0xFF;
                                        int baseG = baseBuffer.get(index + 1) & 0xFF;
                                        int baseB = baseBuffer.get(index + 2) & 0xFF;
                                        int baseA = baseBuffer.get(index + 3) & 0xFF;

                                        float[] cb = ColorizeMath.applyColorization(new float[]{baseR / 255f, baseG / 255f, baseB / 255f, 1f}, vertexColor);

                                        int finalR = (Math.round(cb[0] * 255) & 0xFF);
                                        int finalG = (Math.round(cb[1] * 255) & 0xFF);
                                        int finalB = (Math.round(cb[2] * 255) & 0xFF);

                                        if (!isOverlay) {
                                            int overlayR = overlayBuffer.get(index) & 0xFF;
                                            int overlayG = overlayBuffer.get(index + 1) & 0xFF;
                                            int overlayB = overlayBuffer.get(index + 2) & 0xFF;
                                            int overlayA = overlayBuffer.get(index + 3) & 0xFF;

                                            finalR = (overlayR * overlayA + finalR * (255 - overlayA)) / 255;
                                            finalG = (overlayG * overlayA + finalG * (255 - overlayA)) / 255;
                                            finalB = (overlayB * overlayA + finalB * (255 - overlayA)) / 255;
                                        }

                                        baseBuffer.put(index, (byte) finalR);
                                        baseBuffer.put(index + 1, (byte) finalG);
                                        baseBuffer.put(index + 2, (byte) finalB);
                                        baseBuffer.put(index + 3, (byte) (baseA & 0xFF));
                                    }
                                }

                                int num = spriteContents.getId().toString().contains("1") ? 1 : 0;

                                if (!isOverlay) {
                                    all.add(new SpriteContents(
                                            Identifier.of("block/fire_" + num + "_" + Math.abs(ints.get(i)[0]) + "_" + Math.abs(ints.get(i)[1])),
                                            new SpriteDimensions(16, 16),
                                            NativeImageInvoker.invokeInit(NativeImage.Format.RGBA, 16, 16 * 32, false, pointer),
                                            Optional.of(fireAnimation),
                                            List.of(),
                                            Optional.empty()));
                                }
                            }
                        }
                    }
                }
            }
            sprites.set(new ImmutableList.Builder<SpriteContents>()
                    .addAll(sp)
                    .addAll(all)
                    .build());
        }
    }
}
