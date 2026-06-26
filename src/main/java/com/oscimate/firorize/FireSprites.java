package com.oscimate.firorize;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;

/**
 * Resolves recoloured fire sprites from the block atlas. Sprites are looked up through the
 * {@link AtlasManager} via {@link SpriteId} (atlas + texture). This centralises the fire-sprite
 * selection logic shared by the entity-fire and first-person overlay paths.
 */
@Environment(EnvType.CLIENT)
public final class FireSprites {

    public static final Identifier ATLAS = TextureAtlas.LOCATION_BLOCKS;

    public static AtlasManager atlasManager() {
        return Minecraft.getInstance().getAtlasManager();
    }

    public static TextureAtlasSprite block(AtlasManager atlas, String path) {
        return atlas.get(new SpriteId(ATLAS, Identifier.withDefaultNamespace(path)));
    }

    /**
     * Picks the recoloured fire sprite for a fire colour.
     *
     * @param atlas    the block atlas manager
     * @param color    per-entity fire colour ({@code null} → use {@code vanilla})
     * @param soulPath sprite path used for the soul/lightning case ({@code color[0] == 2})
     * @param vanilla  the original sprite to fall back to
     */
    public static TextureAtlasSprite resolve(AtlasManager atlas, int[] color, String soulPath, TextureAtlasSprite vanilla) {
        if (color == null) {
            return vanilla;
        }
        int fireColor = color[0];
        if (fireColor < 1) {
            TextureAtlasSprite sprite = block(atlas, "block/fire_1_" + Math.abs(color[0]) + "_" + Math.abs(color[1]));
            if (sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation())) {
                int[] base = Main.CONFIG_MANAGER.getCurrentBlockFireColors().getRight();
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
