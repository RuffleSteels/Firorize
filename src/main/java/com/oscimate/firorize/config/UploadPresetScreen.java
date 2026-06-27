package com.oscimate.firorize.config;

import com.oscimate.firorize.Main;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.apache.commons.collections4.map.ListOrderedMap;

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

    private final Screen back;                 // where onClose() returns (the chooser)
    private final Screen origin;               // where we land on success
    private final OnlinePresetsScreen online;  // non-null when reached from the online screen; refreshed on success
    private final String profileName;
    private final boolean privateMode;

    private PlaceholderField titleField;
    private PlaceholderField descriptionField;
    private PlaceholderField recipientsField;
    private Button primaryButton;

    private boolean submitting = false;
    private Component status;
    private boolean statusError;

    private int boxX, boxY, boxW, boxH;

    public UploadPresetScreen(Screen back, Screen origin, OnlinePresetsScreen online, String profileName, boolean privateMode) {
        super(Component.translatable(privateMode ? "firorize.config.button.sendToFriend" : "firorize.config.button.uploadOnline"));
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

        titleField = new PlaceholderField(this.font, boxX + 20, boxY + 46, boxW - 40, 20, Component.empty());
        titleField.setHint(Component.translatable("firorize.config.placeholder.presetTitle"));
        titleField.setMaxLength(32);
        // Letters/numbers/spaces only (international letters allowed); no symbols/emoji. Empty allowed while typing.
 // TODO(26.1.2): EditBox has no setFilter; input validation handled on submit.
        addRenderableWidget(titleField);

        descriptionField = new PlaceholderField(this.font, boxX + 20, boxY + 74, boxW - 40, 20, Component.empty());
        descriptionField.setHint(Component.translatable("firorize.config.placeholder.presetDescription"));
        descriptionField.setMaxLength(150);
        addRenderableWidget(descriptionField);

        if (privateMode) {
            recipientsField = new PlaceholderField(this.font, boxX + 20, boxY + 102, boxW - 40, 20, Component.empty());
            recipientsField.setHint(Component.translatable("firorize.config.placeholder.recipients"));
            recipientsField.setMaxLength(400);
            addRenderableWidget(recipientsField);
        }

        primaryButton = new Button.Builder(
                Component.translatable(privateMode ? "firorize.config.button.sendPreset" : "firorize.config.button.uploadPreset"), button -> submit())
                .bounds(boxX + (boxW - 120) / 2, boxY + boxH - 28, 120, 20).build();
        addRenderableWidget(primaryButton);

        addRenderableWidget(new Button.Builder(Component.literal("<"), button -> onClose())
                .bounds(boxX + 6, boxY + 6, 16, 16).build());
        addRenderableWidget(new Button.Builder(Component.literal("x"), button -> minecraft.setScreen(origin))
                .bounds(boxX + boxW - 22, boxY + 6, 16, 16).build());

        super.init();
    }

    private void submit() {
        if (submitting) return;
        String title = titleField.getValue().trim();
        if (title.isEmpty()) {
            setStatus(Component.translatable("firorize.config.status.titleRequired"), true);
            return;
        }

        List<String> recipients = null;
        if (privateMode) {
            recipients = parseRecipients(recipientsField.getValue());
            if (recipients.isEmpty()) {
                setStatus(Component.translatable("firorize.config.status.noRecipients"), true);
                return;
            }
            if (recipients.stream().anyMatch(r -> !r.matches(USERNAME_RE))) {
                setStatus(Component.translatable("firorize.config.status.invalidUsername"), true);
                return;
            }
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

        OnlinePresetsClient.McAuth auth = OnlinePresetsClient.currentIdentity();
        if (auth == null) {
            setStatus(Component.translatable("firorize.config.status.signIn"), true);
            return;
        }

        submitting = true;
        primaryButton.active = false;
        setStatus(Component.translatable(privateMode ? "firorize.config.status.sending" : "firorize.config.status.uploading"), false);

        String description = descriptionField.getValue().trim();
        if (privateMode) {
            OnlinePresetsClient.share(auth, data, title, description, recipients)
                    .whenComplete((res, err) -> Minecraft.getInstance().execute(() -> onComplete(res, err, true)));
        } else {
            OnlinePresetsClient.upload(auth, data, title, description)
                    .whenComplete((res, err) -> Minecraft.getInstance().execute(() -> onComplete(res, err, false)));
        }
    }

    private void onComplete(OnlinePresetsClient.ApiResult res, Throwable err, boolean sent) {
        submitting = false;
        primaryButton.active = true;
        if (err != null) {
            OnlinePresetsClient.LOGGER.error(sent ? "Send failed" : "Upload failed", err);
            setStatus(Component.translatable(sent ? "firorize.config.status.sendFailed" : "firorize.config.status.uploadFailed"), true);
        } else if (res.success()) {
            if (online != null) {
                if (sent) online.refreshInbox(); else online.refresh();
            }
            if (sent) OnlinePresetsClient.refreshInboxCount();
            minecraft.setScreen(origin);
        } else {
            setStatus(errorMessage(res.error()), true);
        }
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
    private static Component errorMessage(String code) {
        return switch (code == null ? "" : code) {
            case "profanity" -> Component.translatable("firorize.config.status.profanity");
            case "invalid_title" -> Component.translatable("firorize.config.status.invalidTitle");
            case "too_long" -> Component.translatable("firorize.config.status.tooLong");
            case "unauthorized" -> Component.translatable("firorize.config.status.signIn");
            case "rate_limited" -> Component.translatable("firorize.config.status.rateLimited");
            case "duplicate_title" -> Component.translatable("firorize.config.status.duplicateTitle");
            case "invalid_username" -> Component.translatable("firorize.config.status.invalidUsername");
            case "no_recipients", "too_many_recipients" -> Component.translatable("firorize.config.status.noRecipients");
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
        // Overlay the chooser we came from (which in turn renders the config behind it), rather than
        // cutting through to the blurred game.
        ChangeFireColorScreen.renderModalBackdrop(context, back, delta);
        context.fill(0, 0, this.width, this.height, 0xB0000000);
        context.fill(boxX - 1, boxY - 1, boxX + boxW + 1, boxY + boxH + 1, 0xFF000000);
        context.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xFF1A1A1A);
        context.outline(boxX, boxY, boxW, boxH, 0xFF8B8B8B);
        // Start past the back button (boxX+6, 16px wide) so the title doesn't clip underneath it.
        context.text(font, getTitle(), boxX + 26, boxY + 9, 0xFFFFFFFF);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);

        // Selected profile, for confirmation.
        context.text(font,
                Component.translatable("firorize.config.label.profileName", profileName), boxX + 10, boxY + 28, 0xFFB0B0B0);

        if (status != null) {
            context.centeredText(font, status, width / 2, boxY + boxH - 44, statusError ? 0xFFE08080 : 0xFF80E080);
        }
    }
}
