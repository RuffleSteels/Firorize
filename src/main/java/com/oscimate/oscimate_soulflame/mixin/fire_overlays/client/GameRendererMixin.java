package com.oscimate.oscimate_soulflame.mixin.fire_overlays.client;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.mojang.datafixers.util.Pair;
import com.oscimate.oscimate_soulflame.GameRendererSetting;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.resource.ResourceManager;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

@Mixin(GameRenderer.class)
public interface GameRendererMixin {
    @Invoker double callGetFov(Camera camera, float tickDelta, boolean changingFov);
}
