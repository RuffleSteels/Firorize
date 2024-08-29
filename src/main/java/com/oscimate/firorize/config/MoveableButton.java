package com.oscimate.firorize.config;

import com.oscimate.firorize.Main;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Collections;

public class MoveableButton extends ButtonWidget {
    private final ButtonTextures TEXTURES = new ButtonTextures(Identifier.of("widget/button"), Identifier.of("widget/button_disabled"), Identifier.of("widget/button_highlighted"));
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

    private final TextRenderer textRenderer;
    protected MoveableButton(ChangeFireColorScreen instance, TextRenderer textRenderer, int x, int y, int width, int height, Text message, int index) {
        super(x, y, width, height, message, null, DEFAULT_NARRATION_SUPPLIER);
        this.index = index;
        this.instance = instance;

        this.y = getY() - this.height;
        this.x = new int[]{getX(), getX()+getWidth()-getHeight()};
        this.textRenderer = textRenderer;
    }

    public void move(boolean right) {
        int temp = Main.CONFIG_MANAGER.getPriorityOrder().get(index);
        Main.CONFIG_MANAGER.getPriorityOrder().set(index, Main.CONFIG_MANAGER.getPriorityOrder().get(right ? index+1 : index-1));
        Main.CONFIG_MANAGER.getPriorityOrder().set(right ? index+1 : index-1, temp);
        boolean tempB = instance.searchOptions[index].active;
        instance.searchOptions[index].setFocused(false);
        instance.searchOptions[index].active = instance.searchOptions[right ? index+1 : index-1].active;
        instance.searchOptions[right ? index+1 : index-1].active = tempB;

        Collections.copy(Main.CONFIG_MANAGER.getFireColorPresets().get(instance.presetListWidget.curPresetID).getRight(), Main.CONFIG_MANAGER.getPriorityOrder());
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderWidget(context, mouseX, mouseY, delta);

        Sprite ARROW_RIGHT = new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, Identifier.of("firorize:block/arrow_right")).getSprite();
        Sprite ARROW_LEFT = new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, Identifier.of("firorize:block/arrow_left")).getSprite();

        if (index!=2) context.drawSprite(x[1] + ((getHeight()-ARROW_RIGHT.getContents().getWidth())/2), y+((height-ARROW_RIGHT.getContents().getHeight())/2), 10,ARROW_RIGHT.getContents().getWidth(), ARROW_RIGHT.getContents().getHeight(), ARROW_RIGHT);
        if (index!=0) context.drawSprite(x[0] + ((getHeight()-ARROW_LEFT.getContents().getWidth())/2), y+((height-ARROW_LEFT.getContents().getHeight())/2), 10,ARROW_LEFT.getContents().getWidth(), ARROW_LEFT.getContents().getHeight(), ARROW_LEFT);
    }

    @Override
    public void onPress() {
        instance.blockUnderField.setText("");
        instance.input = instance.blockUnderField.getText();
        instance.searchScreenListWidget.selected.clear();
        instance.searchScreenListWidget.test();
        instance.changeSearchOption(Main.CONFIG_MANAGER.getPriorityOrder().get(index));
    }

    @Override
    public Text getMessage() {
        return Text.literal(headers[Main.CONFIG_MANAGER.getPriorityOrder().get(index)]);
    }
}
