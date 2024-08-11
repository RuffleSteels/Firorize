package com.oscimate.firorize.mixin.fire_overlays.client;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.mojang.datafixers.util.Pair;
import com.oscimate.firorize.GameRendererSetting;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.resource.ResourceFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

@Mixin(GameRenderer.class)
public abstract class GamRenMixin {
    @ModifyReceiver(method = "loadPrograms", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
    private List<Pair<ShaderProgram, Consumer<ShaderProgram>>> addShaderPrograms(List<Pair<ShaderProgram, Consumer<ShaderProgram>>> instance, Object e, ResourceFactory factory) throws IOException {
        instance.add(Pair.of(new ShaderProgram(factory, "firorize/rendertype_custom_tint", VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL), program -> {
            GameRendererSetting.renderTypeCustomTint = program;
        }));
        instance.add(Pair.of(new ShaderProgram(factory, "firorize/rendertype_color_wheel", VertexFormats.POSITION_TEXTURE_COLOR), program -> {
            GameRendererSetting.renderTypeColorWheel = program;
        }));
        return instance;
    }

}
