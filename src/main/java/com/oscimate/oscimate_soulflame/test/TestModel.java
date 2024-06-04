package com.oscimate.oscimate_soulflame.test;

import com.oscimate.oscimate_soulflame.Main;
import net.fabricmc.fabric.api.blockview.v2.FabricBlockView;
import net.fabricmc.fabric.api.client.model.loading.v1.FabricBakedModelManager;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.biome.BiomeKeys;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class TestModel implements FabricBakedModel, BakedModel {

    BakedModel model;

    public TestModel(BakedModel model) {
        this.model = model;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, Random random) {
        return null;
    }

    @Override
    public boolean useAmbientOcclusion() {
        return false;
    }

    @Override
    public boolean hasDepth() {
        return false;
    }

    @Override
    public boolean isSideLit() {
        return false;
    }

    @Override
    public boolean isBuiltin() {
        return false;
    }

    @Override
    public Sprite getParticleSprite() {
        return null;
    }

    @Override
    public ModelTransformation getTransformation() {
        return null;
    }

    @Override
    public ModelOverrideList getOverrides() {
        return null;
    }

    @Override
    public void emitBlockQuads(BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, RenderContext context) {
        if (((FabricBlockView)blockView).getBiomeFabric(pos).getKey().orElseThrow().equals(BiomeKeys.WARPED_FOREST)) {
            BakedModel bakedModel = new BakedModel() {
                @Override
                public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, Random random) {
                    List<BakedQuad> beforeTempList = model.getQuads(state, face, random); // get blockstate here
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
                    return model.useAmbientOcclusion();
                }

                @Override
                public boolean hasDepth() {
                    return model.hasDepth();
                }

                @Override
                public boolean isSideLit() {
                    return model.isSideLit();
                }

                @Override
                public boolean isBuiltin() {
                    return model.isBuiltin();
                }

                @Override
                public Sprite getParticleSprite() {
                    return model.getParticleSprite();
                }

                @Override
                public ModelTransformation getTransformation() {
                    return model.getTransformation();
                }

                @Override
                public ModelOverrideList getOverrides() {
                    return model.getOverrides();
                }
            };
            bakedModel.emitBlockQuads(blockView, state, pos, randomSupplier, context);
        } else {
            model.emitBlockQuads(blockView, state, pos, randomSupplier, context);
        }
    }
}
