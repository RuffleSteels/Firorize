package com.oscimate.firorize.config;

import com.oscimate.firorize.Main;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.apache.commons.collections4.map.ListOrderedMap;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Second step of the share flow: give the chosen profile a title and description, then publish it to
 * the gallery ({@link OnlinePresetsClient#upload}) or send it privately to usernames
 * ({@link OnlinePresetsClient#share}). The profile and the public/private mode are decided in
 * {@link ChooseProfileScreen}, so this screen has no profile picker and no mode toggle.
 */
public class UploadPresetScreen extends Screen {
    /** Minecraft username shape; mirrors the Worker's validation. */
    private static final String USERNAME_RE = "[A-Za-z0-9_]{3,16}";

    private final Screen back;                 // where close() returns (the chooser)
    private final Screen origin;               // where we land on success
    private final OnlinePresetsScreen online;  // non-null when reached from the online screen; refreshed on success
    private final String profileName;
    private final boolean privateMode;

    private PlaceholderField titleField;
    private PlaceholderField descriptionField;
    private PlaceholderField recipientsField;
    private ButtonWidget primaryButton;

    private boolean submitting = false;
    private Text status;
    private boolean statusError;

    private int boxX, boxY, boxW, boxH;

    public UploadPresetScreen(Screen back, Screen origin, OnlinePresetsScreen online, String profileName, boolean privateMode) {
        super(Text.translatable(privateMode ? "firorize.config.button.sendToFriend" : "firorize.config.button.uploadOnline"));
        this.back = back;
        this.origin = origin;
        this.online = online;
        this.profileName = profileName;
        this.privateMode = privateMode;
    }

    @Override
    protected void init() {
        Main.inConfig = true;
        boxW = Math.min(300, width - 40);
        boxH = privateMode ? 196 : 156;
        boxX = (width - boxW) / 2;
        boxY = (height - boxH) / 2;

        titleField = new PlaceholderField(this.textRenderer, boxX + 20, boxY + 46, boxW - 40, 20, Text.empty());
        titleField.setPlaceholder(Text.translatable("firorize.config.placeholder.presetTitle"));
        titleField.setMaxLength(32);
        // Letters/numbers/spaces only (international letters allowed); no symbols/emoji. Empty allowed while typing.
        titleField.setTextPredicate(s -> s.matches("[\\p{L}\\p{N} ]*"));
        addDrawableChild(titleField);

        descriptionField = new PlaceholderField(this.textRenderer, boxX + 20, boxY + 74, boxW - 40, 20, Text.empty());
        descriptionField.setPlaceholder(Text.translatable("firorize.config.placeholder.presetDescription"));
        descriptionField.setMaxLength(150);
        addDrawableChild(descriptionField);

        if (privateMode) {
            recipientsField = new PlaceholderField(this.textRenderer, boxX + 20, boxY + 102, boxW - 40, 20, Text.empty());
            recipientsField.setPlaceholder(Text.translatable("firorize.config.placeholder.recipients"));
            recipientsField.setMaxLength(400);
            addDrawableChild(recipientsField);
        }

        primaryButton = new ButtonWidget.Builder(
                Text.translatable(privateMode ? "firorize.config.button.sendPreset" : "firorize.config.button.uploadPreset"), button -> submit())
                .dimensions(boxX + (boxW - 120) / 2, boxY + boxH - 28, 120, 20).build();
        addDrawableChild(primaryButton);

        addDrawableChild(new ButtonWidget.Builder(Text.literal("<"), button -> close())
                .dimensions(boxX + 6, boxY + 6, 16, 16).build());
        addDrawableChild(new ButtonWidget.Builder(Text.literal("x"), button -> client.setScreen(origin))
                .dimensions(boxX + boxW - 22, boxY + 6, 16, 16).build());

        super.init();
    }

    private void submit() {
        if (submitting) return;

        // Hidden developer path: Shift+Alt while pressing Upload opens the built-in upload dialog
        // (publishes to the curated built-in table behind a server-side password) instead of the
        // public community upload. Only available for the public (non-private) upload flow.
        if (!privateMode && devUploadCombo()) {
            client.setScreen(new BuiltinUploadScreen(this, origin, profileName,
                    titleField.getText().trim(), descriptionField.getText().trim()));
            return;
        }

        String title = titleField.getText().trim();
        if (title.isEmpty()) {
            setStatus(Text.translatable("firorize.config.status.titleRequired"), true);
            return;
        }

        List<String> recipients = null;
        if (privateMode) {
            recipients = parseRecipients(recipientsField.getText());
            if (recipients.isEmpty()) {
                setStatus(Text.translatable("firorize.config.status.noRecipients"), true);
                return;
            }
            if (recipients.stream().anyMatch(r -> !r.matches(USERNAME_RE))) {
                setStatus(Text.translatable("firorize.config.status.invalidUsername"), true);
                return;
            }
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

        OnlinePresetsClient.McAuth auth = OnlinePresetsClient.currentIdentity();
        if (auth == null) {
            setStatus(Text.translatable("firorize.config.status.signIn"), true);
            return;
        }

        submitting = true;
        primaryButton.active = false;
        setStatus(Text.translatable(privateMode ? "firorize.config.status.sending" : "firorize.config.status.uploading"), false);

        String description = descriptionField.getText().trim();
        if (privateMode) {
            OnlinePresetsClient.share(auth, data, title, description, recipients)
                    .whenComplete((res, err) -> MinecraftClient.getInstance().execute(() -> onComplete(res, err, true)));
        } else {
            OnlinePresetsClient.upload(auth, data, title, description)
                    .whenComplete((res, err) -> MinecraftClient.getInstance().execute(() -> onComplete(res, err, false)));
        }
    }

    private void onComplete(OnlinePresetsClient.ApiResult res, Throwable err, boolean sent) {
        submitting = false;
        primaryButton.active = true;
        if (err != null) {
            OnlinePresetsClient.LOGGER.error(sent ? "Send failed" : "Upload failed", err);
            setStatus(Text.translatable(sent ? "firorize.config.status.sendFailed" : "firorize.config.status.uploadFailed"), true);
        } else if (res.success()) {
            if (online != null) {
                if (sent) online.refreshInbox(); else online.refresh();
            }
            if (sent) OnlinePresetsClient.refreshInboxCount();
            client.setScreen(origin);
        } else {
            setStatus(errorMessage(res.error()), true);
        }
    }

    /** True while Shift+Alt are both held (queried from the keyboard, since the button's onPress
     *  carries no modifier info). Reveals the hidden developer built-in upload path. */
    private static boolean devUploadCombo() {
        long handle = MinecraftClient.getInstance().getWindow().getHandle();
        boolean shift = InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_LEFT_SHIFT) || InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_RIGHT_SHIFT);
        boolean alt = InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_LEFT_ALT) || InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_RIGHT_ALT);
        return shift && alt;
    }

    /** Splits a comma/whitespace-separated list into lowercased, de-duplicated usernames. */
    private static List<String> parseRecipients(String raw) {
        List<String> out = new ArrayList<>();
        for (String part : raw.split("[,\\s]+")) {
            String name = part.trim().toLowerCase();
            if (!name.isEmpty() && !out.contains(name)) out.add(name);
        }
        return out;
    }

    /** Maps the Worker's rejection codes to localized messages. */
    private static Text errorMessage(String code) {
        return switch (code == null ? "" : code) {
            case "profanity" -> Text.translatable("firorize.config.status.profanity");
            case "invalid_title" -> Text.translatable("firorize.config.status.invalidTitle");
            case "too_long" -> Text.translatable("firorize.config.status.tooLong");
            case "unauthorized" -> Text.translatable("firorize.config.status.signIn");
            case "rate_limited" -> Text.translatable("firorize.config.status.rateLimited");
            case "duplicate_title" -> Text.translatable("firorize.config.status.duplicateTitle");
            case "invalid_username" -> Text.translatable("firorize.config.status.invalidUsername");
            case "no_recipients", "too_many_recipients" -> Text.translatable("firorize.config.status.noRecipients");
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
        // Overlay the chooser we came from (which in turn renders the config behind it), rather than
        // cutting through to the blurred game.
        ChangeFireColorScreen.renderModalBackdrop(context, back, delta);
        context.fill(0, 0, this.width, this.height, 0xB0000000);
        context.fill(boxX - 1, boxY - 1, boxX + boxW + 1, boxY + boxH + 1, 0xFF000000);
        context.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xFF1A1A1A);
        context.drawBorder(boxX, boxY, boxW, boxH, 0xFF8B8B8B);
        // Start past the back button (boxX+6, 16px wide) so the title doesn't clip underneath it.
        context.drawTextWithShadow(textRenderer, getTitle(), boxX + 26, boxY + 9, 0xFFFFFFFF);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        // Selected profile, for confirmation.
        context.drawTextWithShadow(textRenderer,
                Text.translatable("firorize.config.label.profileName", profileName), boxX + 10, boxY + 28, 0xFFB0B0B0);

        if (status != null) {
            context.drawCenteredTextWithShadow(textRenderer, status, width / 2, boxY + boxH - 44, statusError ? 0xFFE08080 : 0xFF80E080);
        }
    }
}
