package com.oscimate.firorize.config;

import com.oscimate.firorize.Main;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.apache.commons.collections4.map.ListOrderedMap;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;

/**
 * "New profile" dialog. Lets the player create a profile from the current colours or from defaults,
 * giving it a name. Styled as a centred dark box over a dimmed config screen, matching the
 * delete-profile confirm box ({@link ChangeFireColorScreen#renderConfirm}) and {@link UploadPresetScreen}.
 *
 * <p>{@link #deserializeFromString} remains here (used by {@link OnlinePresetListWidget} to import
 * online presets), but the old clipboard "paste a code" entry point has been removed in favour of
 * the username-based sharing flow.
 */
public class AddProfileScreen extends Screen {
    private final ChangeFireColorScreen parent;
    protected AddProfileScreen(ChangeFireColorScreen parent) {
        super(Component.translatable("firorize.config.title.newProfile"));
        this.parent = parent;
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
        this.setFocused(null); // clear previous focus/outline; a genuinely-clicked widget re-acquires it via super
        return super.mouseClicked(click, doubled);
    }

    public Button fromExistingButton;
    public Button fromNewButton;
    public EditBox presetNameField;
    private Component nameError = null;

    private int boxX, boxY, boxW, boxH;

    @Override
    protected void init() {
        parent.isPresetAdd = false;
        // Box sized snugly to its content: title, name field, the two buttons, and a reserved line
        // for the validation message — no dead space at the bottom.
        int pad = 10;
        boxW = Math.min(300, width - 40);
        boxH = 96;
        boxX = (width - boxW) / 2;
        boxY = (height - boxH) / 2;

        this.presetNameField = new PlaceholderField(this.font, boxX + pad, boxY + 28, boxW - pad * 2, 20, CommonComponents.DONE);
        presetNameField.setMaxLength(Integer.MAX_VALUE);

        int btnW = (boxW - pad * 2 - 6) / 2;
        this.fromExistingButton = new Button.Builder(Component.translatable("firorize.config.button.profileFromCurrentButton"), button -> addFromExisting())
                .dimensions(boxX + pad, boxY + 54, btnW, 20).build();
        this.fromNewButton = new Button.Builder(Component.translatable("firorize.config.button.profileFromNewButton"), button -> addFromNew())
                .dimensions(boxX + pad + btnW + 6, boxY + 54, btnW, 20).build();

        this.addDrawableChild(new Button.Builder(Component.literal("x"), button -> close())
                .dimensions(boxX + boxW - 22, boxY + 6, 16, 16).build());
        this.addDrawableChild(presetNameField);
        this.addDrawableChild(fromExistingButton);
        this.addDrawableChild(fromNewButton);
        super.init();
        Main.inConfig = true;

        fromExistingButton.setTooltip(Tooltip.of(Component.translatable("firorize.config.tooltip.profileFromCurrentButton")));
        fromExistingButton.setTooltipDelay(Duration.ofMillis(750L));
        fromNewButton.setTooltip(Tooltip.of(Component.translatable("firorize.config.tooltip.profileFromNewButton")));
        fromNewButton.setTooltipDelay(Duration.ofMillis(750L));
        presetNameField.setPlaceholder(Component.translatable("firorize.config.placeholder.newProfileNameField"));
    }

    public void addFromExisting() {
        ArrayList<Integer> tempPriorityOrder = new ArrayList<>(Main.CONFIG_MANAGER.getPriorityOrder());
        KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>, int[]> tempCurrentColors = Main.CONFIG_MANAGER.getCurrentBlockFireColors();
        ArrayList<ListOrderedMap<String, int[]>> tempStuff = new ArrayList<>(tempCurrentColors.getLeft());

        addProfile(KeyValuePair.of(KeyValuePair.of(tempStuff, tempCurrentColors.getRight().clone()), tempPriorityOrder));
    }

    public void addFromNew() {
        addProfile(Main.CONFIG_MANAGER.getDefaultProfile());
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    @Override
    public void resize(int width, int height) {
        Minecraft client = Minecraft.getInstance();
        Main.setScale(width, height, client);
        super.resize(client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight());
    }

    public void addProfile(KeyValuePair<KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>, int[]>, ArrayList<Integer>> newProfile) {
        if (newProfile != null) {
            if (presetNameField.getText().isEmpty()) {
                nameError = Component.translatable("firorize.config.tooltip.empty");
            } else if (Main.CONFIG_MANAGER.getFireColorPresets().keySet().stream().anyMatch(presetNameField.getText()::equalsIgnoreCase)) {
                nameError = Component.translatable("firorize.config.tooltip.exists");
            } else {
                parent.presetListWidget.addProfile(presetNameField.getText(), newProfile);
                Main.setScale(width, height, client);
                client.setScreen(parent);
            }
        }
    }

    @SuppressWarnings("unchecked") // shape is validated by the instanceof checks above the cast
    public static KeyValuePair<KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>, int[]>, ArrayList<Integer>> deserializeFromString(String str) {
        try {
            byte[] data = Base64.getDecoder().decode(str);

            try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
                 ObjectInputStream ois = new ObjectInputStream(bais)) {
                Object obj = ois.readObject();

                if (obj instanceof KeyValuePair<?, ?>) {
                    KeyValuePair<?, ?> pair = (KeyValuePair<?, ?>) obj;
                    if (pair.getLeft() instanceof KeyValuePair<?, ?>) {
                        KeyValuePair<?, ?> innerPair = (KeyValuePair<?, ?>) pair.getLeft();
                        if (innerPair.getLeft() instanceof ArrayList && innerPair.getRight() instanceof int[]
                                && pair.getRight() instanceof ArrayList) {
                            return (KeyValuePair<KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>, int[]>, ArrayList<Integer>>) obj;
                        }
                    }
                }
            }
        } catch (IllegalArgumentException | ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void renderBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        // Draw the config screen behind (with its deferred 3D/colour-wheel elements suppressed so they
        // don't composite over this dialog), then a dim overlay and the dialog box (matching the
        // profile-delete confirm box), rather than blurring through to the game.
        ChangeFireColorScreen.renderModalBackdrop(context, parent, delta);
        context.fill(0, 0, this.width, this.height, 0xB0000000);
        context.fill(boxX - 1, boxY - 1, boxX + boxW + 1, boxY + boxH + 1, 0xFF000000);
        context.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xFF1A1A1A);
        context.drawStrokedRectangle(boxX, boxY, boxW, boxH, 0xFF8B8B8B);
        context.drawTextWithShadow(font, getTitle(), boxX + 10, boxY + 9, 0xFFFFFFFF);
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta); // renderBackground (parent + dim + box) then the dialog widgets

        // Validation feedback as red text in the dialog (matching the other dialogs), not a tooltip.
        if (nameError != null) {
            context.drawCenteredTextWithShadow(font, nameError, width / 2, boxY + boxH - 14, 0xFFE08080);
        }
    }
}
