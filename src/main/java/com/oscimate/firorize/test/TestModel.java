package com.oscimate.firorize.test;

import com.oscimate.firorize.FireSprites;
import com.oscimate.firorize.Main;
import net.fabricmc.fabric.api.blockgetter.v2.FabricBlockGetter;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.model.ModelHelper;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Wraps the vanilla FIRE / SOUL_FIRE {@link BlockStateModel} and recolours its quads per-position:
 * for each fire block it resolves the configured colour (block-under-fire → priority order over
 * block/tag/biome) and re-textures the fire quads onto the generated {@code block/fire_<n>_<R>_<B>}
 * sprite produced by {@code SpriteLoaderMixin}.
 */
public class TestModel extends WrapperBlockStateModel {
    /**
     * ARGB colour the config preview fire should be tinted to. Set by the config renderer immediately
     * before the block model is rendered, because the {@code r,g,b} tint args of the render call are
     * dropped for untinted fire quads, so the colour must be written onto the vertices here instead.
     * Read by the {@code custom_tint} shader as the HSV target.
     */
    public static int configPreviewColor = 0xFFFFFFFF;

    private final boolean soulFire;
    /** Caches the last resolved block-under-fire, so animation frames where the source block reads as air keep their colour. */
    private Block unique = null;

    public TestModel(BlockStateModel wrapped, boolean soulFire) {
        super(wrapped);
        this.soulFire = soulFire;
    }

    @Override
    public void emitQuads(QuadEmitter emitter, BlockAndTintGetter blockView, BlockPos pos, BlockState state,
                          RandomSource random, Predicate<Direction> cullTest) {
        AtlasManager atlas = FireSprites.atlasManager();

        TextureAtlasSprite configSprite = null;
        int[] ints = null;
        if (Main.inConfig) {
            configSprite = atlas.get(new SpriteId(FireSprites.ATLAS,
                    Identifier.fromNamespaceAndPath("firorize", soulFire ? "block/blank_fire_overlay_1_config" : "block/blank_fire_1_config")));
        } else {
            ints = computeColor(blockView, pos, state);
        }

        List<BlockStateModelPart> parts = new ArrayList<>();
        this.collectParts(random, parts);
        for (BlockStateModelPart part : parts) {
            for (int faceIndex = 0; faceIndex <= ModelHelper.NULL_FACE_ID; faceIndex++) {
                Direction d = ModelHelper.faceFromIndex(faceIndex);
                if (d != null && cullTest.test(d)) {
                    continue;
                }
                for (BakedQuad q : part.getQuads(d)) {
                    TextureAtlasSprite src = q.materialInfo().sprite();
                    TextureAtlasSprite target;
                    if (Main.inConfig) {
                        target = configSprite;
                    } else {
                        int n = src.contents().name().getPath().endsWith("1") ? 1 : 0;
                        target = FireSprites.block(atlas, "block/fire_" + n + "_" + Math.abs(ints[0]) + "_" + Math.abs(ints[1]));
                    }

                    emitter.fromBakedQuad(q);
                    // Normalize the imported (source-sprite) UVs, then re-bake onto the recoloured sprite.
                    float uMin = src.getU0(), uMax = src.getU1(), vMin = src.getV0(), vMax = src.getV1();
                    for (int i = 0; i < 4; i++) {
                        float nu = (emitter.u(i) - uMin) / (uMax - uMin);
                        float nv = (emitter.v(i) - vMin) / (vMax - vMin);
                        emitter.uv(i, nu, nv);
                    }
                    emitter.materialBake(new Material.Baked(target, false), MutableQuadView.BAKE_NORMALIZED);
                    if (Main.inConfig) {
                        // fromBakedQuad copied the fire quad's white vertex colour; overwrite it with the
                        // preview target so the custom_tint shader recolours toward it.
                        emitter.color(configPreviewColor, configPreviewColor, configPreviewColor, configPreviewColor);
                    }
                    emitter.cullFace(d);
                    emitter.emit();
                }
            }
        }
    }

    /**
     * Resolves the fire colour {@code int[]{baseRGB, overlayRGB}} for the fire at {@code pos} by the
     * same active-profile list order as {@link Main#resolveActiveFireColor} (top of the profile list
     * wins), so the in-world fire block agrees with the burning-entity / first-person overlay instead
     * of tracking whichever profile is open in the editor.
     */
    private int[] computeColor(BlockAndTintGetter blockView, BlockPos pos, BlockState state) {
        Block blockUnder;
        if (!soulFire) {
            if (state.getValue(FireBlock.NORTH)) {
                blockUnder = blockView.getBlockState(pos.north()).getBlock();
            } else if (state.getValue(FireBlock.EAST)) {
                blockUnder = blockView.getBlockState(pos.east()).getBlock();
            } else if (state.getValue(FireBlock.SOUTH)) {
                blockUnder = blockView.getBlockState(pos.south()).getBlock();
            } else if (state.getValue(FireBlock.WEST)) {
                blockUnder = blockView.getBlockState(pos.west()).getBlock();
            } else if (state.getValue(FireBlock.UP)) {
                blockUnder = blockView.getBlockState(pos.above()).getBlock();
            } else {
                blockUnder = blockView.getBlockState(pos.below()).getBlock();
            }
        } else {
            blockUnder = blockView.getBlockState(pos.below()).getBlock();
        }

        // Animation frames occasionally read the source block as air; reuse the last non-air block
        // under this fire so its colour doesn't flicker. Soul fire only burns on soul soil/sand, so
        // fall back to soul_sand when even the cache is empty.
        if (blockUnder == null || blockUnder.equals(Blocks.AIR)) {
            blockUnder = unique != null ? unique : (soulFire ? Blocks.SOUL_SAND : null);
        } else {
            unique = blockUnder;
        }

        Holder<Biome> biome = ((FabricBlockGetter) blockView).getBiomeFabric(pos);
        String biomeKey = biome == null ? null : biome.unwrapKey().get().identifier().toString();

        int[] resolved = blockUnder == null ? null : Main.resolveActiveFireColor(blockUnder, biomeKey);
        return resolved != null ? resolved : Main.topActiveBase().clone();
    }
}
