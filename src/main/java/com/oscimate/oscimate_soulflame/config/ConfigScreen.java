package com.oscimate.oscimate_soulflame.config;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.systems.RenderSystem;
import com.oscimate.oscimate_soulflame.ColorizeMath;
import com.oscimate.oscimate_soulflame.FireLogic;
import com.oscimate.oscimate_soulflame.Main;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.option.VideoOptionsScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.function.Supplier;

public class ConfigScreen extends Screen {
    protected static final int buttonWidth = 130;
    private Screen parent;
    protected static final int windowWidth = 176;
    protected static final int windowHeight = 182;
    private final Identifier WINDOW = Identifier.of("oscimate_soulflame", "textures/gui/info_box.png");
    protected int guiTop, guiLeft;


    protected ConfigScreen(Text title) {
        super(title);
    }



    @Override
    protected void init() {
//        CyclingButtonWidget<FireLogic> enabledButton = CyclingButtonWidget.builder(FireLogic::getTranslatableName)
//                .values(FireLogic.values())
//                .initially(Main.CONFIG_MANAGER.getCurrentFireLogic())
//                .tooltip(value -> Tooltip.of(Text.literal("Changes the soul fire logic")))
//                .build(this.width / 2 - (buttonWidth/2), height/2 - windowHeight/2 - 20*2, buttonWidth, 20, Text.literal("Fire Logic"), (button, fireLogic) -> {
//                    Main.CONFIG_MANAGER.setCurrentFireLogic((FireLogic) fireLogic);
//                });
//        this.addDrawableChild(enabledButton);
//        renderWindow();
        this.addDrawableChild(new ButtonWidget.Builder(Text.literal("Change Fire Height"), button -> this.client.setScreen(new ChangeFireHeightScreen(this))).dimensions(width / 2 + buttonWidth/2 - 40, height/2 - 15 - 20, buttonWidth, 20).build());
        this.addDrawableChild(new ButtonWidget.Builder(Text.literal("Change Fire Color"), button -> this.client.setScreen(new ChangeFireColorScreen(this))).dimensions(width / 2 - buttonWidth - buttonWidth/2 + 40, height/2 - 15 - 20, buttonWidth, 20).build());

        this.addDrawableChild(new ButtonWidget.Builder(ScreenTexts.DONE, button -> onClose()).dimensions(width / 2 - 100, height/2 + 15, 200, 20).build());
        super.init();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
//        this.renderBackgroundTexture(context);
        super.render(context, mouseX, mouseY, delta);
        context.drawText(this.textRenderer, "Improved Fire Overlay", this.width / 2 - textRenderer.getWidth("Improved Fire Overlay") / 2, height/2 - windowHeight/2 - 20*3 - 5, 0xFFFFFF, false);
    }


    public void onClose() {
        client.setScreen(parent);
    }

    @Override
    public void removed() {
        Main.CONFIG_MANAGER.save();
    }
}