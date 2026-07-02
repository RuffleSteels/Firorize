package com.oscimate.firorize.test;

import com.oscimate.firorize.FireSprites;
import com.oscimate.firorize.Main;
import net.fabricmc.fabric.api.blockview.v2.FabricBlockView;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.ModelHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FireBlock;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.BlockModelPart;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.texture.AtlasManager;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.biome.Biome;

import java.util.function.Predicate;

/**
 * Wraps the vanilla FIRE / SOUL_FIRE {@link BlockStateModel} and recolours its quads per-position:
 * for each fire block it resolves the configured colour (block-under-fire → priority order over
 * block/tag/biome) and re-textures the fire quads onto the generated {@code block/fire_<n>_<R>_<B>}
 * sprite produced by {@code SpriteLoaderMixin}.
 *
 * <p>Ported from the pre-1.21.4 {@code FabricBakedModel} implementation to the new
 * {@link WrapperBlockStateModel} / {@code FabricBlockStateModel} emit API.
 */
public class TestModel extends WrapperBlockStateModel {
    /**
     * ARGB colour the config preview fire should be tinted to. Set by the config renderer
     * immediately before {@code FabricBlockModelRenderer.render} (single-threaded render thread)
     * because the {@code r,g,b} tint args of that call are dropped for untinted quads (fire has no
     * tint index), so the colour must be written onto the vertices here instead. Read by the
     * {@code custom_tint} shader as the HSV target. See docs/RENDERING-1.21.11.md §4.
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
    public void emitQuads(QuadEmitter emitter, BlockRenderView blockView, BlockPos pos, BlockState state,
                          Random random, Predicate<Direction> cullTest) {
        AtlasManager atlas = FireSprites.atlasManager();

        Sprite configSprite = null;
        int[] ints = null;
        if (Main.inConfig) {
            configSprite = atlas.getSprite(new SpriteIdentifier(FireSprites.ATLAS,
                    Identifier.of("firorize", soulFire ? "block/blank_fire_overlay_1_config" : "block/blank_fire_1_config")));
        } else {
            ints = computeColor(blockView, pos, state);
        }

        for (BlockModelPart part : getParts(random)) {
            for (int faceIndex = 0; faceIndex <= ModelHelper.NULL_FACE_ID; faceIndex++) {
                Direction d = ModelHelper.faceFromIndex(faceIndex);
                if (d != null && cullTest.test(d)) {
                    continue;
                }
                for (BakedQuad q : part.getQuads(d)) {
                    Sprite src = q.sprite();
                    Sprite target;
                    if (Main.inConfig) {
                        target = configSprite;
                    } else {
                        int n = src.getContents().getId().getPath().endsWith("1") ? 1 : 0;
                        target = FireSprites.block(atlas, "block/fire_" + n + "_" + Math.abs(ints[0]) + "_" + Math.abs(ints[1]));
                    }

                    emitter.fromBakedQuad(q);
                    // Normalize the imported (source-sprite) UVs, then re-bake onto the recoloured sprite.
                    float uMin = src.getMinU(), uMax = src.getMaxU(), vMin = src.getMinV(), vMax = src.getMaxV();
                    for (int i = 0; i < 4; i++) {
                        float nu = (emitter.u(i) - uMin) / (uMax - uMin);
                        float nv = (emitter.v(i) - vMin) / (vMax - vMin);
                        emitter.uv(i, nu, nv);
                    }
                    emitter.spriteBake(target, MutableQuadView.BAKE_NORMALIZED);
                    if (Main.inConfig) {
                        // fromBakedQuad copied the fire quad's white vertex colour; overwrite it with
                        // the preview target so the custom_tint shader recolours toward it (the tint
                        // args of FabricBlockModelRenderer.render never reach untinted fire quads).
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
    private int[] computeColor(BlockRenderView blockView, BlockPos pos, BlockState state) {
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

        RegistryEntry<Biome> biome = ((FabricBlockView) blockView).getBiomeFabric(pos);
        String biomeKey = biome == null ? null : biome.getKey().map(bk -> bk.getValue().toString()).orElse(null);

        int[] resolved = blockUnder == null ? null : Main.resolveActiveFireColor(blockUnder, biomeKey);
        return resolved != null ? resolved : Main.topActiveBase().clone();
    }
}
