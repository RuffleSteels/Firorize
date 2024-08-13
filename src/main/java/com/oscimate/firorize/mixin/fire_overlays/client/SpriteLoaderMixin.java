package com.oscimate.firorize.mixin.fire_overlays.client;

import com.google.common.collect.ImmutableList;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.oscimate.firorize.ColorizeMath;
import com.oscimate.firorize.Main;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.SpriteContents;
import net.minecraft.client.texture.SpriteDimensions;
import net.minecraft.client.texture.SpriteLoader;
import net.minecraft.screen.PlayerScreenHandler;
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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private void addSprites(List<SpriteContents> sp, int mipLevel, Executor executor, CallbackInfoReturnable<SpriteLoader.StitchResult> cir, @Local LocalRef<List<SpriteContents>> sprites) {
        if (id.equals(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE)) {
            List<int[]> ints = Stream.concat(
                    Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft().stream()
                            .flatMap(map -> map.values().stream())
                            .distinct()
                            .collect(Collectors.toMap(
                                    arr -> arr[0] + "-" + arr[1],
                                    arr -> arr,
                                    (existing, replacement) -> existing
                            ))
                            .values()
                            .stream(),
                    Stream.of(Main.CONFIG_MANAGER.getCurrentBlockFireColors().getRight())
                            .filter(newArr -> Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft().stream()
                                    .flatMap(map -> map.values().stream())
                                    .distinct()
                                    .noneMatch(arr -> Arrays.equals(arr, newArr)))
            ).toList();

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
                                    all.add(new SpriteContents((Identifier.of("block/fire_" + num + "_" + Math.abs(ints.get(i)[0]) + "_" + Math.abs(ints.get(i)[1]))), new SpriteDimensions(16, 16), NativeImageInvoker.invokeInit(NativeImage.Format.RGBA, 16, 16 * 32, false, pointer), spriteContents.getMetadata()));
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
