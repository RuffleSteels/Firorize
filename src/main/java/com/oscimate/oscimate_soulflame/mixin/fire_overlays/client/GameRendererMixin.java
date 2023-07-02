package com.oscimate.oscimate_soulflame.mixin.fire_overlays.client;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.mojang.datafixers.util.Pair;
import com.oscimate.oscimate_soulflame.GameRendererSetting;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.resource.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

@Mixin(RenderPhase.class)
public class GameRendererMixin {

    @ModifyReceiver(method = "loadShader", at = @At(value = "INVOKE", target = "java/util/List.add(Ljava/lang/Object;)Z", ordinal = 0))
    private List<Pair<ShaderProgram, Consumer<ShaderProgram>>> addCustomShader(List<Pair<ShaderProgram, Consumer<ShaderProgram>>> reciever, Object newX, ResourceManager manager) throws IOException {
        reciever.add(Pair.of(new ShaderProgram(manager, "oscimate_soulflame/rendertype_custom_tint", VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL), shader -> {
            GameRendererSetting.renderTypeCustomTint = shader;
        }));
        return reciever;
    }
}
