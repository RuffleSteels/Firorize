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
    private final Screen parent;
    protected static final int windowWidth = 176;
    protected static final int windowHeight = 182;
    private final Identifier WINDOW = Identifier.of("oscimate_soulflame", "textures/gui/info_box.png");
    protected int guiTop, guiLeft;

    protected ConfigScreen(Screen parent) {
        super(Text.translatable("ImprovedFireOverlay"));
        this.parent = parent;
    }



    @Override
    protected void init() {
        this.addDrawableChild(new ButtonWidget.Builder(Text.literal("Change Fire Height"), button -> doStuff(new ChangeFireHeightScreen(this))).dimensions(width / 2 + buttonWidth/2 - 40, height/2 - 15 - 20, buttonWidth, 20).build());
        this.addDrawableChild(new ButtonWidget.Builder(Text.literal("Change Fire Color"), button -> doStuff(new ChangeFireColorScreen(this))).dimensions(width / 2 - buttonWidth - buttonWidth/2 + 40, height/2 - 15 - 20, buttonWidth, 20).build());

        this.addDrawableChild(new ButtonWidget.Builder(ScreenTexts.DONE, button -> onClose()).dimensions(width / 2 - 100, height/2 + 15, 200, 20).build());
        super.init();
    }

    private void doStuff(Object object) {
        int e = client.getWindow().calculateScaleFactor(4, client.forcesUnicodeFont());
        client.getWindow().setScaleFactor((double)e);

        this.client.setScreen((Screen) object);

    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawText(this.textRenderer, "Improved Fire Overlay", this.width / 2 - textRenderer.getWidth("Improved Fire Overlay") / 2, height/2 - windowHeight/2 - 20*3 - 5, 0xFFFFFF, false);
    }

    @Override
    public void close() {
        onClose();
    }

    public void onClose() {
        client.setScreen(parent);
    }

    @Override
    public void removed() {
        Main.CONFIG_MANAGER.save();
    }
}