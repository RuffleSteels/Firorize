package com.oscimate.firorize;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.resource.ResourceManager;
import org.jetbrains.annotations.Nullable;


public class GameRendererSetting extends GameRenderer {

    public GameRendererSetting(MinecraftClient client, HeldItemRenderer heldItemRenderer, ResourceManager resourceManager, BufferBuilderStorage buffers) {
        super(client, heldItemRenderer, resourceManager, buffers);
    }

    @Nullable
    public static ShaderProgram getRenderTypeCustomTint() {
        return renderTypeCustomTint;
    }


    @Nullable
    public static ShaderProgram renderTypeCustomTint;

    @Nullable
    public static ShaderProgram getRenderTypeColorWheel() {
        return renderTypeColorWheel;
    }


    @Nullable
    public static ShaderProgram renderTypeColorWheel;
}
