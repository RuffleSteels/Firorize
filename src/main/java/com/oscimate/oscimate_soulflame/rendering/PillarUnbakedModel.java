package com.oscimate.oscimate_soulflame.rendering;

import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.Baker;
import net.minecraft.client.render.model.ModelBakeSettings;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public class PillarUnbakedModel implements UnbakedModel {
    private static final SpriteIdentifier SPRITES = new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, new Identifier("block/netherite_block"));

    @Override
    public Collection<Identifier> getModelDependencies() {
        return List.of();
    }

    @Override
    public void setParents(Function<Identifier, UnbakedModel> modelLoader) {
    }

    @Nullable
    @Override
    public BakedModel bake(Baker baker, Function<SpriteIdentifier, Sprite> textureGetter, ModelBakeSettings rotationContainer, Identifier modelId) {
        Sprite sprites;

        sprites = textureGetter.apply(SPRITES);

        return new PillarBakedModel(sprites);
//        return ((BlockRenderManagerAccessor)MinecraftClient.getInstance().getBlockRenderManager()).getModels().getModel(Blocks.NETHERITE_BLOCK.getDefaultState());
    }
}