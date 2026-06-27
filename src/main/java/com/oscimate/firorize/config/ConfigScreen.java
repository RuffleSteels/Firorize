package com.oscimate.firorize.config;

import com.oscimate.firorize.Main;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class ConfigScreen extends Screen {
    protected static final int buttonWidth = 130;
    private Screen parent = null;
    protected static final int windowWidth = 176;
    protected static final int windowHeight = 182;
    private final Identifier WINDOW = Identifier.fromNamespaceAndPath("firorize", "textures/gui/info_box.png");
    protected int guiTop, guiLeft;

    public ConfigScreen(Screen parent) {
        super(Component.literal(""));
        this.parent = parent;
    }

    public ConfigScreen() {
        super(Component.literal(""));
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent click, boolean doubled) {
        this.setFocused(null); // clear previous focus/outline; a genuinely-clicked widget re-acquires it via super
        return super.mouseClicked(click, doubled);
    }

    @Override
    protected void init() {
        this.addRenderableWidget(new Button.Builder(Component.translatable("firorize.config.button.changeFireHeightScreen"), button -> this.minecraft.setScreen(new ChangeFireHeightScreen(this))).bounds(width / 2 + buttonWidth/2 - 40, height/2 - 15 - 20, buttonWidth, 20).build());
        this.addRenderableWidget(new Button.Builder(Component.translatable("firorize.config.button.changeFireColorScreen"), button -> doStuff(new ChangeFireColorScreen(this))).bounds(width / 2 - buttonWidth - buttonWidth/2 + 40, height/2 - 15 - 20, buttonWidth, 20).build());

        this.addRenderableWidget(new Button.Builder(CommonComponents.GUI_DONE, button -> onClose()).bounds(width / 2 - 100, height/2 + 15, 200, 20).build());

        KofiBannerButton kofiBanner = new KofiBannerButton(this, width / 2 - 90, height/2 + 43, 180, 22);
        kofiBanner.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("firorize.donate.tooltip")));
        this.addRenderableWidget(kofiBanner);
        super.init();
    }

    private void doStuff(Object object) {
        Main.setScale(width, height, minecraft);

        this.minecraft.setScreen((Screen) object);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        DonationTracker.onConfigFrame();
        super.extractRenderState(context, mouseX, mouseY, delta);
        context.text(this.font, "Improved Fire Overlay", this.width / 2 - font.width("Improved Fire Overlay") / 2, height/2 - windowHeight/2 - 20*3 - 5, 0xFFFFFF, false);
    }

    public void onClose() {
        if (parent == null) {
            super.onClose();
        } else {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public void removed() {
        Main.CONFIG_MANAGER.save();
    }

}