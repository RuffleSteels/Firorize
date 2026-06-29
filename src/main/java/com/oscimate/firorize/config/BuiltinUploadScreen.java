package com.oscimate.firorize.config;

import com.oscimate.firorize.Main;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.apache.commons.collections4.map.ListOrderedMap;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Hidden developer screen for publishing a profile into the curated <b>built-in</b> table, reached by
 * Shift+Alt-clicking Upload in {@link UploadPresetScreen}. Unlike a community upload this is not
 * attributed to a Minecraft account — it's gated by a shared password that the Worker verifies
 * server-side ({@code POST /builtin}), so a request can't be spoofed without it. The title/description
 * are carried over from the upload screen but stay editable, and a sort-order controls list position.
 */
public class BuiltinUploadScreen extends Screen {
    private final Screen back;     // where onClose() returns (the upload screen)
    private final Screen origin;   // where we land on success
    private final String profileName;
    private final String initialTitle;
    private final String initialDescription;

    private PlaceholderField titleField;
    private PlaceholderField descriptionField;
    private PlaceholderField sortOrderField;
    private PlaceholderField passwordField;
    private PanelButton uploadButton;

    private boolean submitting = false;
    private Component status;
    private boolean statusError;

    private int boxX, boxY, boxW, boxH;

    public BuiltinUploadScreen(Screen back, Screen origin, String profileName, String initialTitle, String initialDescription) {
        super(Component.translatable("firorize.config.title.builtinUpload"));
        this.back = back;
        this.origin = origin;
        this.profileName = profileName;
        this.initialTitle = initialTitle;
        this.initialDescription = initialDescription;
    }

    @Override
    protected void init() {
        Main.inConfig = true;
        boxW = Math.min(300, width - 40);
        boxH = 212;
        boxX = (width - boxW) / 2;
        boxY = (height - boxH) / 2;

        titleField = new PlaceholderField(this.font, boxX + 20, boxY + 46, boxW - 40, 20, Component.empty());
        titleField.setHint(Component.translatable("firorize.config.placeholder.presetTitle"));
        titleField.setMaxLength(32);
        titleField.setValue(initialTitle);
        addRenderableWidget(titleField);

        descriptionField = new PlaceholderField(this.font, boxX + 20, boxY + 74, boxW - 40, 20, Component.empty());
        descriptionField.setHint(Component.translatable("firorize.config.placeholder.presetDescription"));
        descriptionField.setMaxLength(150);
        descriptionField.setValue(initialDescription);
        addRenderableWidget(descriptionField);

        sortOrderField = new PlaceholderField(this.font, boxX + 20, boxY + 102, boxW - 40, 20, Component.empty());
        sortOrderField.setHint(Component.translatable("firorize.config.placeholder.sortOrder"));
        sortOrderField.setMaxLength(6);
        addRenderableWidget(sortOrderField);

        passwordField = new PlaceholderField(this.font, boxX + 20, boxY + 130, boxW - 40, 20, Component.empty());
        passwordField.setHint(Component.translatable("firorize.config.placeholder.builtinPassword"));
        passwordField.setMaxLength(128);
        addRenderableWidget(passwordField);

        uploadButton = new PanelButton(boxX + (boxW - 140) / 2, boxY + boxH - 28, 140, 20,
                Component.translatable("firorize.config.button.uploadBuiltin"), button -> submit());
        addRenderableWidget(uploadButton);

        addRenderableWidget(new PanelButton(boxX + 6, boxY + 6, 16, 16, Component.literal("<"), button -> onClose()));
        addRenderableWidget(new PanelButton(boxX + boxW - 22, boxY + 6, 16, 16, Component.literal("x"), button -> minecraft.setScreen(origin)));

        super.init();
    }

    private void submit() {
        if (submitting) return;
        String title = titleField.getValue().trim();
        if (title.isEmpty()) {
            setStatus(Component.translatable("firorize.config.status.titleRequired"), true);
            return;
        }
        String password = passwordField.getValue();
        if (password.isEmpty()) {
            setStatus(Component.translatable("firorize.config.status.passwordRequired"), true);
            return;
        }

        KeyValuePair<KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>, int[]>, ArrayList<Integer>> profile =
                Main.CONFIG_MANAGER.getFireColorPresets().get(profileName);
        if (profile == null) {
            setStatus(Component.translatable("firorize.config.status.uploadFailed"), true);
            return;
        }
        String data;
        try {
            data = ChangeFireColorScreen.serializeToString(profile);
        } catch (IOException e) {
            setStatus(Component.translatable("firorize.config.status.uploadFailed"), true);
            return;
        }

        int sortOrder = 0;
        try {
            String raw = sortOrderField.getValue().trim();
            if (!raw.isEmpty()) sortOrder = Integer.parseInt(raw);
        } catch (NumberFormatException ignored) {
            // blank or non-numeric → leave at 0
        }

        submitting = true;
        uploadButton.active = false;
        setStatus(Component.translatable("firorize.config.status.uploading"), false);

        String description = descriptionField.getValue().trim();
        OnlinePresetsClient.uploadBuiltin(data, title, description, sortOrder, password)
                .whenComplete((res, err) -> Minecraft.getInstance().execute(() -> onComplete(res, err)));
    }

    private void onComplete(OnlinePresetsClient.ApiResult res, Throwable err) {
        submitting = false;
        uploadButton.active = true;
        if (err != null) {
            OnlinePresetsClient.LOGGER.error("Built-in upload failed", err);
            setStatus(Component.translatable("firorize.config.status.uploadFailed"), true);
        } else if (res.success()) {
            minecraft.setScreen(origin);
        } else {
            setStatus(errorMessage(res.error()), true);
        }
    }

    /** Maps the Worker's rejection codes to localized messages. */
    private static Component errorMessage(String code) {
        return switch (code == null ? "" : code) {
            case "unauthorized" -> Component.translatable("firorize.config.status.wrongPassword");
            case "too_long" -> Component.translatable("firorize.config.status.tooLong");
            case "missing" -> Component.translatable("firorize.config.status.titleRequired");
            default -> Component.translatable("firorize.config.status.uploadFailed");
        };
    }

    private void setStatus(Component message, boolean error) {
        this.status = message;
        this.statusError = error;
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent click, boolean doubled) {
        this.setFocused(null);
        return super.mouseClicked(click, doubled);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(back);
    }

    @Override
    public void resize(int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        Main.setScale(width, height, minecraft);
        super.resize(minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        ChangeFireColorScreen.renderModalBackdrop(context, back, delta);
        context.fill(0, 0, this.width, this.height, 0xB0000000);
        context.fill(boxX - 1, boxY - 1, boxX + boxW + 1, boxY + boxH + 1, 0xFF000000);
        context.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xFF1A1A1A);
        context.outline(boxX, boxY, boxW, boxH, 0xFF8B8B8B);
        context.text(font, getTitle(), boxX + 26, boxY + 9, 0xFFFFFFFF);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);

        context.text(font,
                Component.translatable("firorize.config.label.profileName", profileName), boxX + 10, boxY + 28, 0xFFB0B0B0);

        if (status != null) {
            context.centeredText(font, status, width / 2, boxY + boxH - 44, statusError ? 0xFFE08080 : 0xFF80E080);
        }
    }
}
