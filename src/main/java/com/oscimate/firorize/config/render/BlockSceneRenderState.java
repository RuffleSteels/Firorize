package com.oscimate.firorize.config.render;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * A 3D "scene" of block models drawn inside the config screen via {@link BlockSceneRenderer}. The GUI
 * matrix stack is 2D, so block models are rendered through a registered Picture-in-Picture renderer
 * into an offscreen texture and composited back. Each {@link BlockDrawOp} carries its own transform
 * (matching the old immediate-mode {@code translate→rotate→scale→translate} sequence) and tint;
 * {@code customTint} ops are drawn through {@code FirorizePipelines.getCustomTint()}.
 *
 * <p>Coordinate components follow the {@link PictureInPictureRenderState} convention: {@code x0/y0}
 * is the top-left and {@code x1/y1} the bottom-right.
 */
public record BlockSceneRenderState(
        int x0, int y0, int x1, int y1, float scale,
        List<BlockDrawOp> ops,
        @Nullable ScreenRectangle scissorArea,
        @Nullable ScreenRectangle bounds
) implements PictureInPictureRenderState {

    public BlockSceneRenderState(int x0, int y0, int x1, int y1, float scale, List<BlockDrawOp> ops, @Nullable ScreenRectangle scissorArea) {
        this(x0, y0, x1, y1, scale, ops, scissorArea, PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea));
    }

    /**
     * One transformed block draw. {@code push}/{@code pop} control the matrix stack so a group of ops
     * can share a cumulative transform (e.g. the block-under + fire + soul-fire preview), exactly as the
     * old immediate-mode code did between its {@code push()}/{@code pop()} pairs.
     */
    public record BlockDrawOp(
            BlockState state,
            float tx, float ty, float tz,
            Quaternionf rotation,
            boolean mirror,
            float blockScale,
            float ptx, float pty, float ptz,
            float r, float g, float b,
            boolean customTint,
            boolean push,
            boolean pop
    ) {
        /** Independent op (own push/pop) — used for the grid cells. */
        public static BlockDrawOp independent(BlockState state, float tx, float ty, Quaternionf rotation, boolean mirror,
                                              float blockScale, float ptx, float pty, float ptz, float r, float g, float b, boolean customTint) {
            return new BlockDrawOp(state, tx, ty, 0f, rotation, mirror, blockScale, ptx, pty, ptz, r, g, b, customTint, true, true);
        }
    }
}
