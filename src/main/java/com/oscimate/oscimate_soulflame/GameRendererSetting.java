package com.oscimate.oscimate_soulflame;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Optional;


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
}
