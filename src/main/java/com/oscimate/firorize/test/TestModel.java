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

import static com.oscimate.firorize.Main.CONFIG_MANAGER;

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
                        ArrayList<ListOrderedMap<String, int[]>> list = CONFIG_MANAGER.getCurrentBlockFireColors().getLeft();
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

                        int[] ints;
                        if ((blockUnder.equals(Blocks.AIR) && unique != null)|| (blockUnder.getDefaultState().streamTags().anyMatch(tag -> Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft().get(1).containsKey(tag.id().toString())) ||
                                (blockView.getBiomeFabric(pos) != null && Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft().get(2).containsKey(blockView.getBiomeFabric(pos).getKey().get().getValue().toString())) ||
                                list.get(0).containsKey(Registries.BLOCK.getId(blockUnder).toString()))) {
                            for (int i = 0; i < 3; i++) {
                                int order = Main.CONFIG_MANAGER.getPriorityOrder().get(i);
                                if (order == 0) {
                                    if (blockUnder == null || blockUnder.equals(Blocks.AIR)) {
                                        blockUnder = unique;
                                    }
                                    if (blockUnder != null && list.get(0).containsKey(Registries.BLOCK.getId(blockUnder).toString())) {
                                        unique = blockUnder;
                                        ints = list.get(0).get(Registries.BLOCK.getId(blockUnder).toString());
                                        sprite = new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, Identifier.of("block/fire_" + fireNum + "_" + Math.abs(ints[0]) + "_" + Math.abs(ints[1]))).getSprite();
                                        break;
                                    }
                                } else if (order == 1) {
                                    if (blockUnder == null || blockUnder.equals(Blocks.AIR)) {
                                        blockUnder = unique;
                                    }
                                    if (blockUnder != null && blockUnder.getDefaultState().streamTags().anyMatch(tag -> Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft().get(1).containsKey(tag.id().toString()))) {
                                        unique = blockUnder;
                                        ListOrderedMap<String, int[]> map = Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft().get(1);
                                        Block finalBlockUnder = blockUnder;
                                        List<TagKey<Block>> tags = map.keyList().stream().filter(tag -> finalBlockUnder.getDefaultState().streamTags().map(tagg -> tagg.id().toString()).toList().contains(tag)).map(tag -> Main.blockTagList.stream().filter(tagg -> tagg.id().toString().equals(tag)).findFirst().get()).toList();
                                        ints = list.get(1).get(tags.get(0).id().toString());
                                        sprite = new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, Identifier.of("block/fire_" + fireNum + "_" + Math.abs(ints[0]) + "_" + Math.abs(ints[1]))).getSprite();
                                        break;
                                    }
                                } else if (order == 2) {
                                    if (blockUnder != null && Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft().get(2).containsKey(blockView.getBiomeFabric(pos).getKey().get().getValue().toString())) {
                                        ints = list.get(2).get(String.valueOf(blockView.getBiomeFabric(pos).getKey().get().getValue().toString()));
                                        sprite = new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, Identifier.of("block/fire_" + fireNum + "_" + Math.abs(ints[0]) + "_" + Math.abs(ints[1]))).getSprite();
                                        break;
                                    }
                                }
                            }
                        } else {
                            ints = CONFIG_MANAGER.getCurrentBlockFireColors().getRight();
                            sprite = new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, Identifier.of("block/fire_" + fireNum + "_" + Math.abs(ints[0]) + "_" + Math.abs(ints[1]))).getSprite();
                        }
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
            editModel(blockView, pos).emitBlockQuads(blockView, state, pos, randomSupplier, context);
    }
}
