package com.oscimate.firorize;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AtlasManager;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.util.Identifier;

/**
 * Resolves recoloured fire sprites from the block atlas. In 1.21.11 {@code SpriteIdentifier.getSprite()}
 * was removed, so sprites must be looked up through the {@link AtlasManager}. This centralises the
 * fire-sprite selection logic shared by the entity-fire ({@code FireCommandRenderer}) and first-person
 * overlay paths.
 */
@Environment(EnvType.CLIENT)
public final class FireSprites {

    @SuppressWarnings("deprecation") // BLOCK_ATLAS_TEXTURE is still the supported atlas id in 1.21
    public static final Identifier ATLAS = SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;

    public static AtlasManager atlasManager() {
        return MinecraftClient.getInstance().getAtlasManager();
    }

    public static Sprite block(AtlasManager atlas, String path) {
        return atlas.getSprite(new SpriteIdentifier(ATLAS, Identifier.of(path)));
    }

    /**
     * Picks the recoloured fire sprite for a fire colour.
     *
     * @param atlas    the block atlas manager
     * @param color    per-entity fire colour ({@code null} → use {@code vanilla})
     * @param soulPath sprite path used for the soul/lightning case ({@code color[0] == 2})
     * @param vanilla  the original sprite to fall back to
     */
    public static Sprite resolve(AtlasManager atlas, int[] color, String soulPath, Sprite vanilla) {
        if (color == null) {
            return vanilla;
        }
        int fireColor = color[0];
        if (fireColor < 1) {
            // A real recolour carries {base, overlay}; the lava/soul sentinel ({2}) is length 1 but never
            // reaches this branch (its colour[0] is 2). A malformed length-1 recolour can't name a sprite,
            // so fall back to vanilla rather than indexing colour[1] out of bounds.
            if (color.length < 2) return vanilla;
            Sprite sprite = block(atlas, "block/fire_1_" + Math.abs(color[0]) + "_" + Math.abs(color[1]));
            if (sprite.getContents().getId().equals(MissingSprite.getMissingSpriteId())) {
                int[] base = Main.CONFIG_MANAGER.getCurrentBlockFireColors().getRight();
                if (base == null || base.length < 2) base = new int[]{-7456000, -6456034};
                return block(atlas, "block/fire_1_" + Math.abs(base[0]) + "_" + Math.abs(base[1]));
            }
            return sprite;
        } else if (fireColor == 2) {
            return block(atlas, soulPath);
        }
        return vanilla;
    }

    private FireSprites() {
    }
}
