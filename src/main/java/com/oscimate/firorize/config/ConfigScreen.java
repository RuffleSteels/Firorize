package com.oscimate.firorize.config;

import com.oscimate.firorize.Main;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ConfigScreen extends Screen {
    protected static final int buttonWidth = 130;
    private final Screen parent;
    protected static final int windowWidth = 176;
    protected static final int windowHeight = 182;
    private final Identifier WINDOW = Identifier.of("firorize", "textures/gui/info_box.png");
    protected int guiTop, guiLeft;

    protected ConfigScreen(Screen parent) {
        super(Text.literal(""));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.addDrawableChild(new ButtonWidget.Builder(Text.translatable("firorize.config.button.changeFireHeightScreen"), button -> this.client.setScreen(new ChangeFireHeightScreen(this))).dimensions(width / 2 + buttonWidth/2 - 40, height/2 - 15 - 20, buttonWidth, 20).build());
        this.addDrawableChild(new ButtonWidget.Builder(Text.translatable("firorize.config.button.changeFireColorScreen"), button -> doStuff(new ChangeFireColorScreen(this))).dimensions(width / 2 - buttonWidth - buttonWidth/2 + 40, height/2 - 15 - 20, buttonWidth, 20).build());

        this.addDrawableChild(new ButtonWidget.Builder(ScreenTexts.DONE, button -> onClose()).dimensions(width / 2 - 100, height/2 + 15, 200, 20).build());
        super.init();
    }

    private void doStuff(Object object) {
        Main.setScale(width, height, client);

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