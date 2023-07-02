package com.oscimate.oscimate_soulflame;

import net.fabricmc.fabric.api.client.model.ModelProviderContext;
import net.fabricmc.fabric.api.client.model.ModelProviderException;
import net.fabricmc.fabric.api.client.model.ModelResourceProvider;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.util.Identifier;

public class TutorialModelProvider implements ModelResourceProvider {
    public static final Identifier FOUR_SIDED_FURNACE_MODEL = new Identifier("block/bamboo_block");

    @Override
    public UnbakedModel loadModelResource(Identifier identifier, ModelProviderContext modelProviderContext) throws ModelProviderException {
        if (identifier.equals(FOUR_SIDED_FURNACE_MODEL)) {
            return new FourSidedFurnaceModel();
        } else {
            return null;
        }
    }
}
