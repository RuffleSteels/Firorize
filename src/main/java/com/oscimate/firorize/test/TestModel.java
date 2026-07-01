package com.oscimate.firorize.test;

import com.oscimate.firorize.Main;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FireBlock;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.BlockView;
import net.minecraft.world.EmptyBlockView;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class TestModel implements FabricBakedModel, BakedModel {
    BakedModel model;
    int fireNum;
    boolean soulFire;
    String endBit;
    public TestModel(BakedModel model, int fireNum, boolean soulFire, String endBit) {
        this.model = model;
        this.soulFire = soulFire;
        this.fireNum = fireNum;
        this.endBit = endBit;
    }
    Block unique = null;
    @Override
    public boolean isVanillaAdapter() {
        return Main.inConfig;
    }
    @SuppressWarnings("deprecation") // SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE is deprecated but still the supported atlas id in 1.21
    private BakedModel editModel(BlockView blockView, BlockPos pos) {
        return new BakedModel() {
            @Override
            public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, Random random) {
                    List<BakedQuad> beforeTempList = model.getQuads(state, face, random);
                    List<BakedQuad> tempList = new ArrayList<>();
                    for(int g = 0; g < beforeTempList.size(); g++) {
                        tempList.add(g, beforeTempList.get(g));
                    }

                    Sprite sprite = soulFire ? new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, Identifier.of("firorize:block/blank_fire_overlay_1_config")).getSprite() : new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, Identifier.of("firorize:block/blank_fire_1_config")).getSprite();

                    if (!Main.inConfig) {
                        int[] ints = computeColor(blockView, pos, state);
                        sprite = new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, Identifier.of("block/fire_" + fireNum + "_" + Math.abs(ints[0]) + "_" + Math.abs(ints[1]))).getSprite();
                    }



                    for (int n = 0; n < tempList.size(); n++) {
                        int[] verticesOriginal = tempList.get(n).getVertexData();
                        int[] verticesNew = new int[32];

                        for (int cornerIndex = 0; cornerIndex < 4; ++cornerIndex) {
                            int i = cornerIndex * 8;
                            float min1U = tempList.get(n).getSprite().getMinU();
                            float max1U = tempList.get(n).getSprite().getMaxU();
                            float min2U = sprite.getMinU();
                            float max2U = sprite.getMaxU();
                            float min1V = tempList.get(n).getSprite().getMinV();
                            float max1V = tempList.get(n).getSprite().getMaxV();
                            float min2V = sprite.getMinV();
                            float max2V = sprite.getMaxV();

                            verticesNew[i] = verticesOriginal[i];
                            verticesNew[i + 1] = verticesOriginal[i + 1];
                            verticesNew[i + 2] = verticesOriginal[i + 2];
                            verticesNew[i + 3] = verticesOriginal[i + 3];
                            verticesNew[i + 4] = Float.floatToRawIntBits((Float.intBitsToFloat(verticesOriginal[i + 4]) - min1U) * (max2U - min2U) / (max1U - min1U) + min2U);
                            verticesNew[i + 4 + 1] = Float.floatToRawIntBits((Float.intBitsToFloat(verticesOriginal[i + 4 + 1]) - min1V) * (max2V - min2V) / (max1V - min1V) + min2V);
                        }
                        BakedQuad bakedQuad = new BakedQuad(verticesNew, 0, tempList.get(n).getFace(), sprite, tempList.get(n).hasShade());
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

    /**
     * Resolves the fire colour {@code int[]{baseRGB, overlayRGB}} for the fire at {@code pos} by the
     * same active-profile list order as {@link Main#resolveActiveFireColor} (top of the profile list
     * wins), so the in-world fire block agrees with the burning-entity / first-person overlay instead
     * of tracking whichever profile is open in the editor.
     */
    private int[] computeColor(BlockView blockView, BlockPos pos, BlockState state) {
        Block blockUnder;
        if (!soulFire) {
            if (state.get(FireBlock.NORTH)) {
                blockUnder = blockView.getBlockState(pos.north()).getBlock();
            } else if (state.get(FireBlock.EAST)) {
                blockUnder = blockView.getBlockState(pos.east()).getBlock();
            } else if (state.get(FireBlock.SOUTH)) {
                blockUnder = blockView.getBlockState(pos.south()).getBlock();
            } else if (state.get(FireBlock.WEST)) {
                blockUnder = blockView.getBlockState(pos.west()).getBlock();
            } else if (state.get(FireBlock.UP)) {
                blockUnder = blockView.getBlockState(pos.up()).getBlock();
            } else {
                blockUnder = blockView.getBlockState(pos.down()).getBlock();
            }
        } else {
            blockUnder = blockView.getBlockState(pos.down()).getBlock();
        }

        // Animation frames occasionally read the source block as air; reuse the last non-air block
        // under this fire so its colour doesn't flicker. Soul fire only burns on soul soil/sand, so
        // fall back to soul_sand when even the cache is empty.
        if (blockUnder == null || blockUnder.equals(Blocks.AIR)) {
            blockUnder = unique != null ? unique : (soulFire ? Blocks.SOUL_SAND : null);
        } else {
            unique = blockUnder;
        }

        var biome = blockView.getBiomeFabric(pos);
        String biomeKey = biome == null ? null : biome.getKey().get().getValue().toString();

        int[] resolved = blockUnder == null ? null : Main.resolveActiveFireColor(blockUnder, biomeKey);
        return resolved != null ? resolved : Main.topActiveBase().clone();
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
        if (blockView != null) {
            editModel(blockView, pos).emitBlockQuads(blockView, state, pos, randomSupplier, context);
        } else {
            model.emitBlockQuads(blockView, state, pos, randomSupplier, context);
        }
    }
}
