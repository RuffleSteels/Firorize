package com.oscimate.firorize.config;


import com.oscimate.firorize.Main;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class ChangeFireHeightScreen extends Screen {
    private Screen parent;

    protected ChangeFireHeightScreen(Screen parent) {
        super(Component.translatable("options.videoTitle"));
        this.parent = parent;
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent click, boolean doubled) {
        this.setFocused(null); // clear previous focus/outline; a genuinely-clicked widget re-acquires it via super
        return super.mouseClicked(click, doubled);
    }

    public void onClose() {
        Main.CONFIG_MANAGER.save();

        client.setScreen(parent);
    }
    @Override
    protected void init() {
        FireHeightSliderWidget customTimeSliderWidget = new FireHeightSliderWidget(this.width / 2 - 75, 10, 150, 20, Component.translatable("firorize.config.title.height"), (double) Main.CONFIG_MANAGER.getCurrentFireHeightSlider() /100);
        this.addDrawableChild(customTimeSliderWidget);
        this.addDrawableChild(new Button.Builder(CommonComponents.DONE, button -> onClose()).dimensions(width / 2 - 100, 50, 200, 20).build());
        super.init();
    }
    @Override
    public void close() {
        onClose();
    }
    @Override
    public void resize(int width, int height) {
        Minecraft client = Minecraft.getInstance();
//        Main.setScale(width, height, client);
        super.resize(client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight());
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        DonationTracker.onConfigFrame();
        super.render(context, mouseX, mouseY, delta);
        // TODO(26.1.2 port): the live first-person fire-height preview used immediate-mode 3D vertex
        // rendering inside render(), which is incompatible with the new GUI render-extraction model.
        // Re-implement it as a Picture-in-Picture renderer (like BlockSceneRenderer) if the preview is
        // wanted. The slider itself still applies the configured height in-world.
    }

}