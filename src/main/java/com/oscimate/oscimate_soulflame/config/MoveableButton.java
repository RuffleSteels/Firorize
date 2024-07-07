package com.oscimate.oscimate_soulflame.config;

import com.oscimate.oscimate_soulflame.Main;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;

public class MoveableButton extends ButtonWidget {
    private final ButtonTextures TEXTURES = new ButtonTextures(new Identifier("widget/button"), new Identifier("widget/button_disabled"), new Identifier("widget/button_highlighted"));
    private final int index;
    private final String[] headers = new String[]{"Blocks", "Tags", "Biomes"};
    private final ChangeFireColorScreen instance;

    private final int[] x;
    private final int height = 13;
    private final int y;
    protected MoveableButton(ChangeFireColorScreen instance, int x, int y, int width, int height, Text message, int index) {
        super(x, y, width, height, message, null, DEFAULT_NARRATION_SUPPLIER);
        this.index = index;
        this.instance = instance;

        this.y = getY() - this.height;
        this.x = new int[]{getX(), getX()+getWidth()-getHeight()};
    }

    private void move(boolean right) {
        int temp = Main.CONFIG_MANAGER.getPriorityOrder().get(index);
        Main.CONFIG_MANAGER.getPriorityOrder().set(index, Main.CONFIG_MANAGER.getPriorityOrder().get(right ? index+1 : index-1));
        Main.CONFIG_MANAGER.getPriorityOrder().set(right ? index+1 : index-1, temp);
        boolean tempB = instance.searchOptions[index].active;
        instance.searchOptions[index].setFocused(false);
        instance.searchOptions[index].active = instance.searchOptions[right ? index+1 : index-1].active;
        instance.searchOptions[right ? index+1 : index-1].active = tempB;

        Collections.copy(Main.CONFIG_MANAGER.getFireColorPresets().get(instance.presetListWidget.curPresetID).getSecond(), Main.CONFIG_MANAGER.getPriorityOrder());
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderWidget(context, mouseX, mouseY, delta);

        Identifier backTexture = TEXTURES.get(!(index == 0), false);
        Identifier frontTexture = TEXTURES.get(!(index == 2), false);

        if (mouseX >= x[0] && mouseX <= x[0]+getHeight() && mouseY >= y && mouseY <= y+height && index != 0) {
            backTexture = TEXTURES.get(true, true);
        }

        if (mouseX >= x[1] && mouseX <= x[1]+getHeight() && mouseY >= y && mouseY <= y+height && index != 2) {
            frontTexture = TEXTURES.get(true, true);
        }

        context.drawGuiTexture(backTexture, x[0], y, getHeight(), height);
        context.drawGuiTexture(frontTexture, x[1], y, getHeight(), height);
        context.drawSprite(x[1] + ((getHeight()-Main.ARROW_RIGHT.get().getContents().getWidth())/2), y+((height-Main.ARROW_RIGHT.get().getContents().getHeight())/2), 10,Main.ARROW_RIGHT.get().getContents().getWidth(), Main.ARROW_RIGHT.get().getContents().getHeight(), Main.ARROW_RIGHT.get());
        context.drawSprite(x[0] + ((getHeight()-Main.ARROW_LEFT.get().getContents().getWidth())/2), y+((height-Main.ARROW_LEFT.get().getContents().getHeight())/2), 10,Main.ARROW_LEFT.get().getContents().getWidth(), Main.ARROW_LEFT.get().getContents().getHeight(), Main.ARROW_LEFT.get());
    }

    @Override
    public void onPress() {
        instance.changeSearchOption(Main.CONFIG_MANAGER.getPriorityOrder().get(index));
    }


    @Override
    public Text getMessage() {
        return Text.literal(headers[Main.CONFIG_MANAGER.getPriorityOrder().get(index)]);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX >= x[0] && mouseX <= x[0]+getHeight() && mouseY >= y && mouseY <= y+height && index != 0) {
            move(false);
            return false;
        } else if (mouseX >= x[1] && mouseX <= x[1]+getHeight() && mouseY >= y && mouseY <= y+height && index != 2) {
            move(true);
            return false;
        } else {
            return super.mouseClicked(mouseX, mouseY, button);
        }
    }
}
