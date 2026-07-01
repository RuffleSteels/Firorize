package com.oscimate.firorize.config;

import com.oscimate.firorize.Main;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
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
    private final Screen back;     // where close() returns (the upload screen)
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
    private Text status;
    private boolean statusError;

    private int boxX, boxY, boxW, boxH;

    public BuiltinUploadScreen(Screen back, Screen origin, String profileName, String initialTitle, String initialDescription) {
        super(Text.translatable("firorize.config.title.builtinUpload"));
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

        titleField = new PlaceholderField(this.textRenderer, boxX + 20, boxY + 46, boxW - 40, 20, Text.empty());
        titleField.setPlaceholder(Text.translatable("firorize.config.placeholder.presetTitle"));
        titleField.setMaxLength(32);
        titleField.setText(initialTitle);
        addDrawableChild(titleField);

        descriptionField = new PlaceholderField(this.textRenderer, boxX + 20, boxY + 74, boxW - 40, 20, Text.empty());
        descriptionField.setPlaceholder(Text.translatable("firorize.config.placeholder.presetDescription"));
        descriptionField.setMaxLength(150);
        descriptionField.setText(initialDescription);
        addDrawableChild(descriptionField);

        sortOrderField = new PlaceholderField(this.textRenderer, boxX + 20, boxY + 102, boxW - 40, 20, Text.empty());
        sortOrderField.setPlaceholder(Text.translatable("firorize.config.placeholder.sortOrder"));
        sortOrderField.setMaxLength(6);
        addDrawableChild(sortOrderField);

        passwordField = new PlaceholderField(this.textRenderer, boxX + 20, boxY + 130, boxW - 40, 20, Text.empty());
        passwordField.setPlaceholder(Text.translatable("firorize.config.placeholder.builtinPassword"));
        passwordField.setMaxLength(128);
        addDrawableChild(passwordField);

        uploadButton = new PanelButton(boxX + (boxW - 140) / 2, boxY + boxH - 28, 140, 20,
                Text.translatable("firorize.config.button.uploadBuiltin"), button -> submit());
        addDrawableChild(uploadButton);

        addDrawableChild(new PanelButton(boxX + 6, boxY + 6, 16, 16, Text.literal("<"), button -> close()));
        addDrawableChild(new PanelButton(boxX + boxW - 22, boxY + 6, 16, 16, Text.literal("x"), button -> client.setScreen(origin)));

        super.init();
    }

    private void submit() {
        if (submitting) return;
        String title = titleField.getText().trim();
        if (title.isEmpty()) {
            setStatus(Text.translatable("firorize.config.status.titleRequired"), true);
            return;
        }
        String password = passwordField.getText();
        if (password.isEmpty()) {
            setStatus(Text.translatable("firorize.config.status.passwordRequired"), true);
            return;
        }

        KeyValuePair<KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>, int[]>, ArrayList<Integer>> profile =
                Main.CONFIG_MANAGER.getFireColorPresets().get(profileName);
        if (profile == null) {
            setStatus(Text.translatable("firorize.config.status.uploadFailed"), true);
            return;
        }
        String data;
        try {
            data = ChangeFireColorScreen.serializeToString(profile);
        } catch (IOException e) {
            setStatus(Text.translatable("firorize.config.status.uploadFailed"), true);
            return;
        }

        int sortOrder = 0;
        try {
            String raw = sortOrderField.getText().trim();
            if (!raw.isEmpty()) sortOrder = Integer.parseInt(raw);
        } catch (NumberFormatException ignored) {
            // blank or non-numeric → leave at 0
        }

        submitting = true;
        uploadButton.active = false;
        setStatus(Text.translatable("firorize.config.status.uploading"), false);

        String description = descriptionField.getText().trim();
        OnlinePresetsClient.uploadBuiltin(data, title, description, sortOrder, password)
                .whenComplete((res, err) -> MinecraftClient.getInstance().execute(() -> onComplete(res, err)));
    }

    private void onComplete(OnlinePresetsClient.ApiResult res, Throwable err) {
        submitting = false;
        uploadButton.active = true;
        if (err != null) {
            OnlinePresetsClient.LOGGER.error("Built-in upload failed", err);
            setStatus(Text.translatable("firorize.config.status.uploadFailed"), true);
        } else if (res.success()) {
            client.setScreen(origin);
        } else {
            setStatus(errorMessage(res.error()), true);
        }
    }

    /** Maps the Worker's rejection codes to localized messages. */
    private static Text errorMessage(String code) {
        return switch (code == null ? "" : code) {
            case "unauthorized" -> Text.translatable("firorize.config.status.wrongPassword");
            case "too_long" -> Text.translatable("firorize.config.status.tooLong");
            case "missing" -> Text.translatable("firorize.config.status.titleRequired");
            default -> Text.translatable("firorize.config.status.uploadFailed");
        };
    }

    private void setStatus(Text message, boolean error) {
        this.status = message;
        this.statusError = error;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.setFocused(null);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void close() {
        client.setScreen(back);
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        Main.setScale(width, height, client);
        super.resize(client, client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight());
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        ChangeFireColorScreen.renderModalBackdrop(context, back, delta);
        context.fill(0, 0, this.width, this.height, 0xB0000000);
        context.fill(boxX - 1, boxY - 1, boxX + boxW + 1, boxY + boxH + 1, 0xFF000000);
        context.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xFF1A1A1A);
        context.drawBorder(boxX, boxY, boxW, boxH, 0xFF8B8B8B);
        context.drawTextWithShadow(textRenderer, getTitle(), boxX + 26, boxY + 9, 0xFFFFFFFF);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        context.drawTextWithShadow(textRenderer,
                Text.translatable("firorize.config.label.profileName", profileName), boxX + 10, boxY + 28, 0xFFB0B0B0);

        if (status != null) {
            context.drawCenteredTextWithShadow(textRenderer, status, width / 2, boxY + boxH - 44, statusError ? 0xFFE08080 : 0xFF80E080);
        }
    }
}
