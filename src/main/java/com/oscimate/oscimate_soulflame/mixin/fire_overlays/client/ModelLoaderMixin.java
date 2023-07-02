package com.oscimate.oscimate_soulflame.mixin.fire_overlays.client;

import com.oscimate.oscimate_soulflame.Main;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.block.BlockModels;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mixin(ModelLoader.class)
public abstract class ModelLoaderMixin {


    @Shadow @Final private Map<Identifier, BakedModel> bakedModels;

    private BakedModel createFireBakedModel(BlockState blockState) {

        BakedModel testModel = this.bakedModels.get(BlockModels.getModelId(blockState));

        return  new BakedModel() {
            @Override
            public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, Random random) {
                List<BakedQuad> beforeTempList = testModel.getQuads(blockState, face, random);
                List<BakedQuad> tempList = new ArrayList<>();
                for(int g = 0; g < beforeTempList.size(); g++) {
                    tempList.add(g, beforeTempList.get(g));
                }
                for (int n = 0; n < tempList.size(); n++) {
                    int[] verticesOriginal = tempList.get(n).getVertexData();
                    int[] verticesNew = new int[32];
                    for (int cornerIndex = 0; cornerIndex < 4; ++cornerIndex) {
                        int i = cornerIndex * 8;
                        float min1U = tempList.get(n).getSprite().getMinU();
                        float max1U = tempList.get(n).getSprite().getMaxU();
                        float min2U = Main.BLANK_FIRE.get().getMinU();
                        float max2U = Main.BLANK_FIRE.get().getMaxU();
                        float min1V = tempList.get(n).getSprite().getMinV();
                        float max1V = tempList.get(n).getSprite().getMaxV();
                        float min2V = Main.BLANK_FIRE.get().getMinV();
                        float max2V = Main.BLANK_FIRE.get().getMaxV();

                        verticesNew[i] = verticesOriginal[i];
                        verticesNew[i + 1] = verticesOriginal[i + 1];
                        verticesNew[i + 2] = verticesOriginal[i + 2];
                        verticesNew[i + 3] = verticesOriginal[i + 3];
                        verticesNew[i + 4] = Float.floatToRawIntBits((Float.intBitsToFloat(verticesOriginal[i + 4]) - min1U) * (max2U - min2U) / (max1U - min1U) + min2U);
                        verticesNew[i + 4 + 1] = Float.floatToRawIntBits((Float.intBitsToFloat(verticesOriginal[i + 4 + 1]) - min1V) * (max2V - min2V) / (max1V - min1V) + min2V);
                    }
                    BakedQuad bakedQuad = new BakedQuad(verticesNew, tempList.get(n).getColorIndex(), tempList.get(n).getFace(), Main.BLANK_FIRE.get(), tempList.get(n).hasShade());
                    tempList.set(n, bakedQuad);
                }
                return tempList;
            }

            @Override
            public boolean useAmbientOcclusion() {
                return testModel.useAmbientOcclusion();
            }

            @Override
            public boolean hasDepth() {
                return testModel.hasDepth();
            }

            @Override
            public boolean isSideLit() {
                return testModel.isSideLit();
            }

            @Override
            public boolean isBuiltin() {
                return testModel.isBuiltin();
            }

            @Override
            public Sprite getParticleSprite() {
                return testModel.getParticleSprite();
            }

            @Override
            public ModelTransformation getTransformation() {
                return testModel.getTransformation();
            }

            @Override
            public ModelOverrideList getOverrides() {
                return testModel.getOverrides();
            }
        };

//        this.bakedModels.put(BlockModels.getModelId(blockState), tempBakedModel);
    }

//    @Inject(method = "upload", at = @At("TAIL"))
//    private void test(TextureManager textureManager, Profiler profiler, CallbackInfoReturnable<SpriteAtlasManager> cir) {
//        Blocks.FIRE.getStateManager().getStates().forEach(blockState -> {
//            createFireBakedModel(blockState);
//        });
//    }
//
//    @Inject(method = "method_24150(Ljava/util/HashSet;)V", at = @At("HEAD"))
//    private static void addToDefaultTextures(HashSet textures, CallbackInfo ci) {
//        textures.add(new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, new Identifier(Main.MODID, "block/blank_fire_1")));
//    }

//    @Inject(method = "bake", at = @At("TAIL"))
//    public void injectBlankFire(BiFunction<Identifier, SpriteIdentifier, Sprite> spriteLoader, CallbackInfo ci) {
//        Blocks.FIRE.getStateManager().getStates().forEach(this::createFireBakedModel);
//    }
}
