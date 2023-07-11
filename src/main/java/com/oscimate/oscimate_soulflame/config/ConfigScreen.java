package com.oscimate.oscimate_soulflame.config;

import com.mojang.blaze3d.systems.RenderSystem;
import com.oscimate.oscimate_soulflame.FireLogic;
import com.oscimate.oscimate_soulflame.Main;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.List;

public class ConfigScreen extends Screen {
    Integer buttonWidth = 130;
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
        FireHeightSliderWidget customTimeSliderWidget = new FireHeightSliderWidget(64, 68, this.buttonWidth, 20, Text.literal("Height"), (double) Main.CONFIG_MANAGER.getCurrentFireHeightSlider() /100);
        this.addDrawableChild(customTimeSliderWidget);

        CyclingButtonWidget<FireLogic> enabledButton = CyclingButtonWidget.builder(FireLogic::getTranslatableName)
                .values(FireLogic.values())
                .initially(Main.CONFIG_MANAGER.getCurrentFireLogic())
                .tooltip(value -> Tooltip.of(Text.literal("Changes the soul fire logic")))
                .build(this.width / 2 - (buttonWidth/2), height/2 - windowHeight/2 - 20*2, buttonWidth, 20, Text.literal("Fire Logic"), (button, fireLogic) -> {
                    Main.CONFIG_MANAGER.setCurrentFireLogic((FireLogic) fireLogic);
                });
        this.addDrawableChild(enabledButton);
        this.addDrawableChild(new ButtonWidget.Builder(ScreenTexts.DONE, button -> onClose()).dimensions(width / 2 - 100, height/2 + windowHeight/2 + 20, 200, 20).build());
        super.init();
    }

    private void renderWindow(DrawContext drawContext) {
        this.addDrawable((matrices, mouseX, mouseY, delta) -> {
            RenderSystem.enableBlend();
            RenderSystem.setShaderTexture(0, WINDOW);
            drawContext.drawTexture(WINDOW, width/2 - (windowWidth/2), height/2 - (windowHeight/2), 0, 0, windowWidth, windowHeight);
            Text title = Text.literal("ImprovedFireOverlay");
            drawContext.drawText(this.textRenderer, title.getString(), width / 2, guiTop - 15, 0xFFFFFF, false);
            this.renderOriginContent(drawContext, mouseX, mouseY);
            RenderSystem.disableBlend();
        });
    }


    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        this.renderWindow(context);
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