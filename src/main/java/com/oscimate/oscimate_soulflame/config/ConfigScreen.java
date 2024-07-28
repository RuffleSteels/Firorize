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
    private final Identifier WINDOW = new Identifier("oscimate_soulflame", "textures/gui/info_box.png");
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

    private void renderWindow() {
        this.addDrawable((matrices, mouseX, mouseY, delta) -> {
            RenderSystem.enableBlend();
            RenderSystem.setShaderTexture(0, WINDOW);
            matrices.drawTexture(WINDOW, width/2 - (windowWidth/2), height/2 - (windowHeight/2), 0, 0, windowWidth, windowHeight);
            Text title = Text.literal("ImprovedFireOverlay");
            matrices.drawText(this.textRenderer, title.getString(), width / 2, guiTop - 15, 0xFFFFFF, false);
            this.renderOriginContent(matrices, mouseX, mouseY);
            RenderSystem.disableBlend();
        });
    }


    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackgroundTexture(context);
        context.drawText(this.textRenderer, "Improved Fire Overlay", this.width / 2 - textRenderer.getWidth("Improved Fire Overlay") / 2, height/2 - windowHeight/2 - 20*3 - 5, 0xFFFFFF, false);
        super.render(context, mouseX, mouseY, delta);
    }



    public List<OrderedText> getTranslatableTooltip(MinecraftClient minecraftClient) {
        return minecraftClient.textRenderer.wrapLines(Text.translatable("oscimate_soulflame.config." + Main.CONFIG_MANAGER.getCurrentFireLogic().toString() + ".tooltip"), 200);
    }

    public void onClose() {
        client.setScreen(parent);
    }

    @Override
    public void removed() {
        long sizeBytes = (long) 16 * 16 * NativeImage.Format.RGBA.getChannelCount();

        long pointer = MemoryUtil.nmemAlloc(sizeBytes);

        GL11.glGetTexImage(MinecraftClient.getInstance().getTextureManager().getTexture(new Identifier("textures/block/fire_0.png")).getGlId(), 0, NativeImage.Format.RGBA.toGl(), GlConst.GL_UNSIGNED_BYTE, pointer);

        ByteBuffer baseBuffer = MemoryUtil.memByteBuffer(pointer, (int) sizeBytes);

//        long overlayPointer = MemoryUtil.nmemAlloc(sizeBytes);
//
//        GL11.glGetTexImage(MinecraftClient.getInstance().getTextureManager().getTexture(new Identifier("block/fire_0_overlay")).getGlId(), 0, NativeImage.Format.RGBA.toGl(), GlConst.GL_UNSIGNED_BYTE, overlayPointer);
//
//        ByteBuffer overlayBuffer = MemoryUtil.memByteBuffer(overlayPointer, (int) sizeBytes);

        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int index = (y * 16 + x) * 4;

//                int overlayR = overlayBuffer.get(index);
//                int overlayG = overlayBuffer.get(index + 1);
//                int overlayB = overlayBuffer.get(index + 2);
//                int overlayA = overlayBuffer.get(index + 3);

//                float[] colorizedOverlay = ColorizeMath.applyColorization(new float[]{overlayR/255f, overlayG/255f, overlayB/255f, overlayA/255f}, new float[]{255/255f, 0/255f, 0/255f, 1f});
//
//                int[] co = ColorizeMath.convert(colorizedOverlay);

                int baseR = baseBuffer.get(index);
                int baseG = baseBuffer.get(index + 1);
                int baseB = baseBuffer.get(index + 2);
                int baseA = baseBuffer.get(index + 3);

                float[] colorizedBase = ColorizeMath.applyColorization(new float[]{baseR/255f, baseG/255f, baseB/255f, baseA/255f}, new float[]{255/255f, 0/255f, 0/255f, 1f});

                int[] cb = ColorizeMath.convert(colorizedBase);

//                int outR = (co[0] * co[3] + cb[0] * (255 - co[3])) / 255;
//                int outG = (co[1] * co[3] + cb[1] * (255 - co[3])) / 255;
//                int outB = (co[2] * co[3] + cb[2] * (255 - co[3])) / 255;

                baseBuffer.put(index, (byte) cb[0]);
                baseBuffer.put(index + 1, (byte) cb[1]);
                baseBuffer.put(index + 2, (byte) cb[2]);
                baseBuffer.put(index + 3, (byte) baseA);
            }
        }

        GL11.glTexSubImage2D(MinecraftClient.getInstance().getTextureManager().getTexture(new Identifier("textures/block/fire_0.png")).getGlId(), 0, 0, 0, 16, 16, NativeImage.Format.RGBA.toGl(), GlConst.GL_UNSIGNED_BYTE, baseBuffer);

        MemoryUtil.nmemFree(pointer);

        Main.CONFIG_MANAGER.save();
    }

    private void renderOriginContent(DrawContext drawContext, int mouseX, int mouseY) {
        int textWidth = windowWidth - 30;
        int titleHeight = (height/2 - (windowHeight/2)) + 15;
        int infoHeight = (height/2 - (windowHeight/2)) + 15;
        int x = guiLeft + 18;
        int y = guiTop + 50;
        int startY = y;
        int endY = y - 72 + windowHeight;

        Text info = Text.translatable("oscimate_soulflame.config." + Main.CONFIG_MANAGER.currentFireLogic.toString() + ".info");

        List<OrderedText> descLines = textRenderer.wrapLines(info, textWidth);
        for(OrderedText line : descLines) {
            if(y >= startY - 18 && y <= endY + 12) {
                int infoWidth = textRenderer.getWidth(line);
                drawContext.drawText(this.textRenderer, line, width/2 - (windowWidth/2) + 15, (((infoHeight + 25) - windowHeight/2) + 30) + (y +2), 0xCCCCCC, false);
            }
            y += 12;
        }

        int titleWidth = textRenderer.getWidth(Text.translatable("oscimate_soulflame.config." + Main.CONFIG_MANAGER.currentFireLogic.toString() + ".title"));

        drawContext.drawText(this.textRenderer, Text.translatable("oscimate_soulflame.config." + Main.CONFIG_MANAGER.currentFireLogic.toString() + ".title").formatted(Formatting.UNDERLINE), width/2 - (titleWidth/2), titleHeight, 0xFFFFFF, false);
    }
}