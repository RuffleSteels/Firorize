package com.oscimate.oscimate_soulflame.test;

import com.oscimate.oscimate_soulflame.Main;

import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.DynamicTexture;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.BlockView;
import net.minecraft.world.EmptyBlockView;
import org.apache.commons.collections4.map.ListOrderedMap;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static com.oscimate.oscimate_soulflame.Main.CONFIG_MANAGER;

public class TestierModel implements FabricBakedModel, BakedModel {
    BakedModel model;
    BakedModel newModel;
    public TestierModel(BakedModel model) {
        this.model = model;
    }

    @Override
    public boolean isVanillaAdapter() {
        return Main.inConfig;
    }

    private BakedModel editModel(BlockView blockView, BlockPos pos) {
        return new BakedModel() {
            @Override
            public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, Random random) {
                List<BakedQuad> beforeTempList = model.getQuads(state, face, random); // get blockstate here
                List<BakedQuad> tempList = new ArrayList<>();
                for(int g = 0; g < beforeTempList.size(); g++) {
                    for (int h = 0; h < 2; h++) {
                        tempList.add(g, beforeTempList.get(g));
                    }
                }
                for (int n = 0; n < tempList.size(); n++) {
                    int accN = (int) Math.floor(n/2);
                    Sprite sprite =  new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, new Identifier("oscimate_soulflame:block/blank_fire_overlay_0")).getSprite();
                    int[] verticesOriginal = beforeTempList.get(accN).getVertexData();
                    int[] verticesNew = new int[32];
                    for (int cornerIndex = 0; cornerIndex < 4; ++cornerIndex) {
                        int i = cornerIndex * 8;
                        float min1U = beforeTempList.get(accN).getSprite().getMinU();
                        float max1U = beforeTempList.get(accN).getSprite().getMaxU();
                        float min2U = sprite.getMinU();
                        float max2U = sprite.getMaxU();
                        float min1V = beforeTempList.get(accN).getSprite().getMinV();
                        float max1V = beforeTempList.get(accN).getSprite().getMaxV();
                        float min2V = sprite.getMinV();
                        float max2V = sprite.getMaxV();

                        verticesNew[i] = verticesOriginal[i];
                        verticesNew[i + 1] = verticesOriginal[i + 1];
                        verticesNew[i + 2] = verticesOriginal[i + 2];
                        verticesNew[i + 3] = verticesOriginal[i + 3];
                        verticesNew[i + 4] = Float.floatToRawIntBits((Float.intBitsToFloat(verticesOriginal[i + 4]) - min1U) * (max2U - min2U) / (max1U - min1U) + min2U);
                        verticesNew[i + 4 + 1] = Float.floatToRawIntBits((Float.intBitsToFloat(verticesOriginal[i + 4 + 1]) - min1V) * (max2V - min2V) / (max1V - min1V) + min2V);
                    }
                    BakedQuad bakedQuad = new BakedQuad(verticesNew, beforeTempList.get(accN).getColorIndex(), beforeTempList.get(accN).getFace(), sprite, beforeTempList.get(accN).hasShade());
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
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, Random random) {
        return editModel(EmptyBlockView.INSTANCE, BlockPos.ORIGIN).getQuads(state, face, random);
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
        return model.getParticleSprite();
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
        if (Main.inConfig) {
            editModel(blockView, pos).emitBlockQuads(blockView, state, pos, randomSupplier, context);
        } else {
            model.emitBlockQuads(blockView, state, pos, randomSupplier, context);
        }
    }
}
