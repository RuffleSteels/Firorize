package com.oscimate.firorize.config;

import com.oscimate.firorize.FireSprites;
import com.oscimate.firorize.Main;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.Collections;

public class MoveableButton extends Button {
    private final WidgetSprites TEXTURES = new WidgetSprites(Identifier.parse("widget/button"), Identifier.parse("widget/button_disabled"), Identifier.parse("widget/button_highlighted"));
    private final int index;
    private final String[] headers = new String[]{"Blocks", "Tags", "Biomes"};
    private final ChangeFireColorScreen instance;
    public final int[] x;
    private final int height = 13;
    public final int y;


    public int[] getXX() {
        return x;
    }

    public int getYY() {
        return y;
    }

    private final Font font;
    @SuppressWarnings("this-escape") // updateMessage/getY are called after super(), values are set deterministically
    protected MoveableButton(ChangeFireColorScreen instance, Font font, int x, int y, int width, int height, net.minecraft.network.chat.Component message, int index) {
        super(x, y, width, height, message, null, DEFAULT_NARRATION);
        this.index = index;
        this.instance = instance;

        this.y = getY() - this.height;
        this.x = new int[]{getX(), getX()+getWidth()-getHeight()};
        this.font = font;
    }

    public void move(boolean right) {
        // Reordering the block/tag/biome priority tabs is undoable.
        instance.historyBefore();
        int temp = Main.CONFIG_MANAGER.getPriorityOrder().get(index);
        Main.CONFIG_MANAGER.getPriorityOrder().set(index, Main.CONFIG_MANAGER.getPriorityOrder().get(right ? index+1 : index-1));
        Main.CONFIG_MANAGER.getPriorityOrder().set(right ? index+1 : index-1, temp);
        boolean tempB = instance.searchOptions[index].active;
        instance.searchOptions[index].setFocused(false);
        instance.searchOptions[index].active = instance.searchOptions[right ? index+1 : index-1].active;
        instance.searchOptions[right ? index+1 : index-1].active = tempB;

        Collections.copy(Main.CONFIG_MANAGER.getFireColorPresets().get(instance.presetListWidget.curPresetID).getRight(), Main.CONFIG_MANAGER.getPriorityOrder());
        instance.historyAfterTabs();
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        // getMessage() can no longer be overridden, so keep the header in sync here (1-frame lag on reorder).
        setMessage(net.minecraft.network.chat.Component.literal(headers[Main.CONFIG_MANAGER.getPriorityOrder().get(index)]));
        context.centeredText(this.font, getMessage(), getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2, 0xFFFFFFFF);

        TextureAtlasSprite ARROW_RIGHT = FireSprites.block(FireSprites.atlasManager(), "firorize:block/arrow_right");
        TextureAtlasSprite ARROW_LEFT = FireSprites.block(FireSprites.atlasManager(), "firorize:block/arrow_left");

        if (index!=2) context.blitSprite(RenderPipelines.GUI_TEXTURED, ARROW_RIGHT, x[1] + ((getHeight()-ARROW_RIGHT.contents().width())/2), y+((height-ARROW_RIGHT.contents().height())/2), ARROW_RIGHT.contents().width(), ARROW_RIGHT.contents().height());
        if (index!=0) context.blitSprite(RenderPipelines.GUI_TEXTURED, ARROW_LEFT, x[0] + ((getHeight()-ARROW_LEFT.contents().width())/2), y+((height-ARROW_LEFT.contents().height())/2), ARROW_LEFT.contents().width(), ARROW_LEFT.contents().height());
    }

    @Override
    public void onPress(net.minecraft.client.input.InputWithModifiers input) {
        instance.blockUnderField.setValue("");
        instance.input = instance.blockUnderField.getValue();
        instance.searchScreenListWidget.selected.clear();
        instance.searchScreenListWidget.test();
        instance.changeSearchOption(Main.CONFIG_MANAGER.getPriorityOrder().get(index));
    }
}
