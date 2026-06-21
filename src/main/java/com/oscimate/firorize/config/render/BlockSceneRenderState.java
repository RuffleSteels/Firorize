package com.oscimate.firorize.config.render;

import net.minecraft.block.BlockState;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.special.SpecialGuiElementRenderState;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * A 3D "scene" of block models drawn inside the config screen via {@link BlockSceneRenderer}. Since
 * 1.21.5 the GUI matrix stack is 2D, so block models must be rendered through a registered
 * {@code SpecialGuiElementRenderer} into an offscreen texture and composited back. Each {@link BlockDrawOp}
 * carries its own transform (matching the old immediate-mode {@code translate→rotate→scale→translate}
 * sequence) and tint; {@code customTint} ops are drawn through {@code FirorizePipelines.getCustomTint()}.
 */
public record BlockSceneRenderState(
        int x1, int y1, int x2, int y2, float scale,
        List<BlockDrawOp> ops,
        @Nullable ScreenRect scissorArea,
        @Nullable ScreenRect bounds
) implements SpecialGuiElementRenderState {

    public BlockSceneRenderState(int x1, int y1, int x2, int y2, float scale, List<BlockDrawOp> ops, @Nullable ScreenRect scissorArea) {
        this(x1, y1, x2, y2, scale, ops, scissorArea, SpecialGuiElementRenderState.createBounds(x1, y1, x2, y2, scissorArea));
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
