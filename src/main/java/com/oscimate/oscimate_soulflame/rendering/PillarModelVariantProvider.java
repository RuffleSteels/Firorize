package com.oscimate.oscimate_soulflame.rendering;

import net.fabricmc.fabric.api.client.model.ModelProviderContext;
import net.fabricmc.fabric.api.client.model.ModelVariantProvider;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.util.Identifier;

import javax.annotation.Nullable;

public class PillarModelVariantProvider implements ModelVariantProvider {

    public final Identifier oakPlanksId = new Identifier("fire");
    @Override
    @Nullable
    public UnbakedModel loadModelVariant(ModelIdentifier modelId, ModelProviderContext context) {
        System.out.println(modelId);
        if (oakPlanksId.equals(modelId)) {
            return new PillarUnbakedModel();
        } else {
            return null;
        }
    }
}