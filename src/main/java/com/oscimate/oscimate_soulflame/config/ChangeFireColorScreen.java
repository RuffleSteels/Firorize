package com.oscimate.oscimate_soulflame.config;

import com.mojang.blaze3d.systems.RenderSystem;
import com.oscimate.oscimate_soulflame.Main;
import com.oscimate.oscimate_soulflame.mixin.fire_overlays.client.GameRendererMixin;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.FloatSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import dev.isxander.yacl3.gui.controllers.ColorController;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

import java.awt.*;

import static com.oscimate.oscimate_soulflame.config.ConfigScreen.windowHeight;

public class ChangeFireColorScreen {
    public static final ConfigClassHandler<ChangeFireColorScreen> CONFIG = ConfigClassHandler.createBuilder(ChangeFireColorScreen.class)
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .setPath(FabricLoader.getInstance().getConfigDir().resolve("fireColorScreen.json"))
                    .build())
            .build();

    @SerialEntry public Color solidColor = new Color(0, 0, 0, 102);
    @SerialEntry
    public float alpha = 102;
    @SerialEntry public boolean chroma = false;
    @SerialEntry public float saturation = 1F;
    @SerialEntry public float brightness = 1F;
    @SerialEntry public float speed = 0.25F;

    @SuppressWarnings("deprecation")
    public static Screen configScreen(Screen parent) {
        return YetAnotherConfigLib.create(CONFIG, ((defaults, config, builder) -> builder
                .title(Text.literal("Simple Block Overlay"))
                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("Simple Block Overlay"))

                        // Solid Color

                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("Solid"))
                                .option(Option.<Color>createBuilder()
                                        .name(Text.literal("Solid Color"))
                                        .description(OptionDescription.of(Text.of("The solid color to use for the outline. This is ignored if chroma is enabled.")))
                                        .binding(defaults.solidColor, () -> config.solidColor, value -> config.solidColor = value)
                                        .customController(opt -> new ColorController(opt, true))
                                        .build())
                                .build())

                        // Chroma

                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("Chroma"))
                                .option(Option.createBuilder(boolean.class)
                                        .name(Text.literal("Enable Chroma"))
                                        .description(OptionDescription.of(Text.of("Enables a rainbow/chroma effect. Disregards the solid color provided above.")))
                                        .binding(defaults.chroma, () -> config.chroma, newVal -> config.chroma = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .option(Option.createBuilder(float.class)
                                        .name(Text.literal("Saturation"))
                                        .description(OptionDescription.of(Text.of("Saturation value for the chroma effect.")))
                                        .binding(1F, () -> config.saturation, newVal -> config.saturation = newVal)
                                        .controller(opt -> FloatSliderControllerBuilder.create(opt)
                                                .valueFormatter(value -> Text.of(String.format("%,.1f", value)))
                                                .range(0F, 1F)
                                                .step(0.1F))
                                        .build())
                                .option(Option.createBuilder(float.class)
                                        .name(Text.literal("Brightness"))
                                        .description(OptionDescription.of(Text.of("Brightness value for the chroma effect.")))
                                        .binding(1F, () -> config.brightness, newVal -> config.brightness = newVal)
                                        .controller(opt -> FloatSliderControllerBuilder.create(opt)
                                                .valueFormatter(value -> Text.of(String.format("%,.1f", value)))
                                                .range(0F, 1F)
                                                .step(0.1F))
                                        .build())
                                .option(Option.createBuilder(float.class)
                                        .name(Text.literal("Alpha"))
                                        .description(OptionDescription.of(Text.of("Alpha value for the chroma effect.")))
                                        .binding(255F, () -> config.alpha, newVal -> config.alpha = newVal)
                                        .controller(opt -> FloatSliderControllerBuilder.create(opt)
                                                .valueFormatter(value -> Text.of(String.format("%,.0f", value)))
                                                .range(0F, 255F)
                                                .step(1F))
                                        .build())
                                .option(Option.createBuilder(float.class)
                                        .name(Text.literal("Speed"))
                                        .description(OptionDescription.of(Text.of("Speed value for the chroma effect.")))
                                        .binding(0.25F, () -> config.speed, newVal -> config.speed = newVal)
                                        .controller(opt -> FloatSliderControllerBuilder.create(opt)
                                                .valueFormatter(value -> Text.of(String.format("%,.1f", value)))
                                                .range(0F, 1F)
                                                .step(0.1F))
                                        .build())
                                .build())
                        .build()
                ))).generateScreen(parent);
    }
}