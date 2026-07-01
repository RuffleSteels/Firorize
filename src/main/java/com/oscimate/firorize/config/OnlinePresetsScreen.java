package com.oscimate.firorize.config;

import com.oscimate.firorize.Main;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmLinkScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Online profiles dialog, in one of two {@link View}s reached by their own buttons on the colour
 * editor:
 * <ul>
 *   <li><b>Community Profiles</b> — a searchable list of the player's own uploads (under a "My
 *       Uploads" header, with Delete) followed by everyone else's (Import). The Upload button lives
 *       here.</li>
 *   <li><b>Inbox</b> — profiles privately sent to the player ("Received", Import/Dismiss) and their
 *       own outgoing sends ("Sent", Cancel). Requires a Minecraft session.</li>
 * </ul>
 * Styled inside a centred panel matching {@link ChangeFireColorScreen#renderConfirm}.
 */
public class OnlinePresetsScreen extends Screen {
    public enum View { COMMUNITY, BUILTIN, INBOX }

    private enum State { LOADING, LOADED, ERROR, NEED_AUTH }

    public final ChangeFireColorScreen parent;
    private final View view;

    private OnlinePresetListWidget listWidget;
    private State state = null;

    // Community data: the player's own uploads and everyone else's (already split).
    private List<OnlinePreset> myUploads = List.of();
    private List<OnlinePreset> community = List.of();
    // Built-in (curated) data.
    private List<OnlinePreset> builtin = List.of();
    private PlaceholderField searchField;
    private ButtonWidget uploadButton;

    // Inbox data.
    private List<OnlinePreset> inboxReceived = List.of();
    private List<OnlinePreset> inboxSent = List.of();

    private Text flashText;
    private boolean flashError;
    private int flashTimer = 0;

    private static final float PRIVACY_SCALE = 0.82f;
    private Text privacyText;
    private int privacyX, privacyY, privacyW, privacyH;

    private int boxX, boxY, boxW, boxH;
    private int refreshIconX, refreshIconY;

    public OnlinePresetsScreen(ChangeFireColorScreen parent, View view) {
        super(Text.translatable(switch (view) {
            case BUILTIN -> "firorize.config.title.builtinProfiles";
            case INBOX -> "firorize.config.title.inbox";
            case COMMUNITY -> "firorize.config.title.communityProfiles";
        }));
        this.parent = parent;
        this.view = view;
    }

    @Override
    protected void init() {
        Main.inConfig = true;
        boxW = Math.min(340, width - 40);
        boxH = Math.min(264, height - 40);
        boxX = (width - boxW) / 2;
        boxY = (height - boxH) / 2;

        boolean hasSearch = view != View.INBOX;   // Community/Built-in are searchable; Inbox isn't
        boolean hasAction = view != View.BUILTIN;  // Community=Upload, Inbox=Send; Built-in has no action

        int listY;
        if (hasSearch) {
            searchField = new PlaceholderField(this.textRenderer, boxX + 10, boxY + 24, boxW - 20, 16, Text.empty());
            searchField.setPlaceholder(Text.translatable("firorize.config.placeholder.search"));
            searchField.setMaxLength(48);
            searchField.setChangedListener(s -> applyList());
            addDrawableChild(searchField);
            listY = boxY + 46;
        } else {
            // Inbox: leave room for the wrapped description under the title.
            listY = boxY + 52;
        }
        int listH = boxH - (listY - boxY) - (hasAction ? 34 : 22);

        listWidget = new OnlinePresetListWidget(boxX + 10, listY, boxW - 20, listH, this, this.textRenderer);
        addDrawableChild(listWidget);

        if (hasAction) {
            Text label = Text.translatable(view == View.INBOX
                    ? "firorize.config.button.inboxSend" : "firorize.config.button.uploadPreset");
            int w = Math.max(100, textRenderer.getWidth(label) + 24);
            uploadButton = new ButtonWidget.Builder(label, b -> client.setScreen(new ChooseProfileScreen(this, this)))
                    .dimensions(boxX + boxW - 10 - w, boxY + boxH - 26, w, 18).build();
            addDrawableChild(uploadButton);
        } else {
            uploadButton = null;
        }

        // Privacy policy: small gray underlined clickable text (not a button), bottom-left of the panel.
        privacyText = Text.translatable("firorize.config.button.privacy").styled(s -> s.withUnderline(true));
        privacyW = (int) Math.ceil(textRenderer.getWidth(privacyText) * PRIVACY_SCALE);
        privacyH = (int) Math.ceil(textRenderer.fontHeight * PRIVACY_SCALE);
        privacyX = boxX + 10;
        privacyY = boxY + boxH - 16;

        addDrawableChild(new ButtonWidget.Builder(Text.literal("x"), b -> close())
                .dimensions(boxX + boxW - 22, boxY + 6, 16, 16).build());

        // Refresh: re-pulls the current view from the Worker. Same 16×16 footprint as the close
        // button beside it; the refresh.png sprite is drawn over it in render() (see drawRefreshIcon).
        refreshIconX = boxX + boxW - 42;
        refreshIconY = boxY + 6;
        ButtonWidget refreshButton = new ButtonWidget.Builder(Text.empty(), b -> { state = null; load(); })
                .dimensions(refreshIconX, refreshIconY, 16, 16).build();
        refreshButton.setTooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.translatable("firorize.config.tooltip.refresh")));
        addDrawableChild(refreshButton);

        super.init();
        load();
    }

    private void load() {
        switch (view) {
            case COMMUNITY -> loadCommunity();
            case BUILTIN -> loadBuiltin();
            case INBOX -> loadInbox();
        }
    }

    private void loadCommunity() {
        if (state == State.LOADING) return;
        if (state == State.LOADED) {
            applyList();
            return;
        }
        state = State.LOADING;
        OnlinePresetsClient.McAuth auth = OnlinePresetsClient.currentIdentity();
        CompletableFuture<List<OnlinePreset>> mineF = auth != null
                ? OnlinePresetsClient.fetchMine(auth)
                : CompletableFuture.completedFuture(List.of());
        OnlinePresetsClient.fetchPresets()
                .thenCombine(mineF, KeyValuePair::of)
                .whenComplete((pair, err) -> MinecraftClient.getInstance().execute(() -> {
                    if (err != null) {
                        OnlinePresetsClient.LOGGER.error("Failed to fetch community profiles", err);
                        state = State.ERROR;
                    } else {
                        List<OnlinePreset> all = pair.getLeft();
                        myUploads = pair.getRight();
                        Set<Integer> mineIds = myUploads.stream().map(OnlinePreset::id).collect(Collectors.toSet());
                        community = all.stream().filter(p -> !mineIds.contains(p.id())).toList();
                        state = State.LOADED;
                        applyList();
                    }
                }));
    }

    private void loadBuiltin() {
        if (state == State.LOADING) return;
        if (state == State.LOADED) {
            applyList();
            return;
        }
        state = State.LOADING;
        OnlinePresetsClient.fetchBuiltin()
                .whenComplete((list, err) -> MinecraftClient.getInstance().execute(() -> {
                    if (err != null) {
                        OnlinePresetsClient.LOGGER.error("Failed to fetch built-in profiles", err);
                        state = State.ERROR;
                    } else {
                        builtin = list;
                        state = State.LOADED;
                        applyList();
                    }
                }));
    }

    /** Re-filters the cached Community/Built-in data by the search box and pushes it to the list. */
    private void applyList() {
        if (listWidget == null) return;
        String q = searchField == null ? "" : searchField.getText().trim().toLowerCase();
        if (view == View.BUILTIN) {
            listWidget.setBuiltin(filter(builtin, q));
        } else {
            listWidget.setBrowse(filter(myUploads, q), filter(community, q));
        }
    }

    private static List<OnlinePreset> filter(List<OnlinePreset> presets, String q) {
        if (q.isEmpty()) return presets;
        return presets.stream().filter(p ->
                p.displayTitle().toLowerCase().contains(q)
                        || p.displayAuthor().toLowerCase().contains(q)
                        || p.displayDescription().toLowerCase().contains(q)).toList();
    }

    private void loadInbox() {
        if (state == State.LOADING) return;
        if (state == State.LOADED) {
            listWidget.setInbox(inboxReceived, inboxSent);
            return;
        }
        OnlinePresetsClient.McAuth auth = OnlinePresetsClient.currentIdentity();
        if (auth == null) {
            state = State.NEED_AUTH;
            return;
        }
        state = State.LOADING;
        OnlinePresetsClient.fetchInbox(auth)
                .thenCombine(OnlinePresetsClient.fetchSent(auth), KeyValuePair::of)
                .whenComplete((pair, err) -> MinecraftClient.getInstance().execute(() -> {
                    if (err != null) {
                        OnlinePresetsClient.LOGGER.error("Failed to load inbox", err);
                        state = State.ERROR;
                    } else {
                        inboxReceived = pair.getLeft();
                        inboxSent = pair.getRight();
                        state = State.LOADED;
                        listWidget.setInbox(inboxReceived, inboxSent);
                    }
                }));
    }

    /** Called by the list widget (My Uploads section) when the player confirms deleting an upload. */
    public void deleteUpload(int id) {
        OnlinePresetsClient.McAuth auth = OnlinePresetsClient.currentIdentity();
        if (auth == null) {
            flashMessage(Text.translatable("firorize.config.status.signIn"), true);
            return;
        }
        OnlinePresetsClient.deletePreset(auth, id)
                .whenComplete((res, err) -> MinecraftClient.getInstance().execute(() -> {
                    if (err == null && res != null && res.success()) {
                        flashMessage(Text.translatable("firorize.config.status.deleted"), false);
                        state = null;
                        load();
                    } else {
                        flashMessage(Text.translatable("firorize.config.status.deleteFailed"), true);
                    }
                }));
    }

    /** Called by the list widget to cancel an outgoing send or dismiss an inbox item; reloads the Inbox. */
    public void deleteSend(int id) {
        OnlinePresetsClient.McAuth auth = OnlinePresetsClient.currentIdentity();
        if (auth == null) {
            flashMessage(Text.translatable("firorize.config.status.signIn"), true);
            return;
        }
        OnlinePresetsClient.deleteSend(auth, id)
                .whenComplete((res, err) -> MinecraftClient.getInstance().execute(() -> {
                    if (err == null && res != null && res.success()) {
                        state = null;
                        load();
                    } else {
                        flashMessage(Text.translatable("firorize.config.status.deleteFailed"), true);
                    }
                }));
    }

    /** Called by {@link UploadPresetScreen} after a successful upload to re-pull the browse lists. */
    public void refresh() {
        if (view == View.COMMUNITY) {
            state = null;
            load();
        }
    }

    /** Called by {@link UploadPresetScreen} after a successful private send to re-pull the Inbox. */
    public void refreshInbox() {
        if (view == View.INBOX) {
            state = null;
            load();
        }
    }

    public void flashMessage(Text message, boolean error) {
        this.flashText = message;
        this.flashError = error;
        this.flashTimer = 80;
    }

    @Override
    public void tick() {
        super.tick();
        if (flashTimer > 0) flashTimer--;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && overPrivacy(mouseX, mouseY)) {
            ConfirmLinkScreen.open(this, OnlinePresetsClient.PRIVACY_URL);
            return true;
        }
        this.setFocused(null);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean overPrivacy(double mx, double my) {
        return mx >= privacyX && mx <= privacyX + privacyW && my >= privacyY && my <= privacyY + privacyH;
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        Main.setScale(width, height, client);
        super.resize(client, client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight());
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Overlay the live config screen (dimmed) rather than cutting through to the blurred game.
        // renderAsBackdrop suppresses the config's deferred 3D/colour-wheel elements, which otherwise
        // composite in a later pass and would draw on top of this dialog.
        ChangeFireColorScreen.renderModalBackdrop(context, parent, delta);
        context.fill(0, 0, this.width, this.height, 0xB0000000);
        context.fill(boxX - 1, boxY - 1, boxX + boxW + 1, boxY + boxH + 1, 0xFF000000);
        context.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xFF1A1A1A);
        context.drawBorder(boxX, boxY, boxW, boxH, 0xFF8B8B8B);
        context.drawTextWithShadow(textRenderer, getTitle(), boxX + 10, boxY + 9, 0xFFFFFFFF);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        DonationTracker.onConfigFrame();
        super.render(context, mouseX, mouseY, delta);

        if (view == View.INBOX) {
            int dy = boxY + 28; // padded below the title so it clears the refresh/close buttons
            for (net.minecraft.text.OrderedText line : textRenderer.wrapLines(Text.translatable("firorize.config.label.inboxDescription"), boxW - 20)) {
                context.drawTextWithShadow(textRenderer, line, boxX + 10, dy, 0xFF9A9A9A);
                dy += 10;
            }
        }

        int centerY = boxY + boxH / 2 - 14;
        Text status = statusText();
        if (status != null) {
            boolean err = state == State.ERROR;
            context.drawCenteredTextWithShadow(textRenderer, status, width / 2, centerY, err ? 0xFFE08080 : 0xFFC0C0C0);
        }

        if (flashTimer > 0 && flashText != null) {
            context.drawCenteredTextWithShadow(textRenderer, flashText, width / 2, boxY + boxH - 40, flashError ? 0xFFE08080 : 0xFF80E080);
        }

        // refresh.png sprite, centred over its (label-less) button.
        drawRefreshIcon(context, refreshIconX, refreshIconY);

        // Privacy policy link: small, gray, underlined; brighter on hover.
        int privacyColor = overPrivacy(mouseX, mouseY) ? 0xFFCFCFCF : 0xFF8C8C8C;
        context.getMatrices().push();
        context.getMatrices().scale(PRIVACY_SCALE, PRIVACY_SCALE, 1.0f);
        context.drawText(textRenderer, privacyText, Math.round(privacyX / PRIVACY_SCALE), Math.round(privacyY / PRIVACY_SCALE), privacyColor, false);
        context.getMatrices().pop();
    }

    /** Draws the {@code firorize:block/refresh} sprite centred in the 16×16 refresh button at (px,py).
     *  Rendered smaller than the button so it doesn't crowd the edges. Uses the block atlas the same
     *  way {@link UndoButton} draws its icon. */
    private static final int REFRESH_ICON_SIZE = 11;
    @SuppressWarnings("deprecation") // BLOCK_ATLAS_TEXTURE is deprecated but still the supported atlas id in 1.21
    private void drawRefreshIcon(DrawContext context, int px, int py) {
        Sprite refresh = new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, Identifier.of("firorize:block/refresh")).getSprite();
        int off = (16 - REFRESH_ICON_SIZE) / 2;
        context.drawSprite(px + off, py + off, 10, REFRESH_ICON_SIZE, REFRESH_ICON_SIZE, refresh);
    }

    private Text statusText() {
        if (state == State.LOADING) return Text.translatable("firorize.config.status.loading");
        if (state == State.ERROR) return Text.translatable("firorize.config.status.loadFailed");
        if (state == State.NEED_AUTH) return Text.translatable("firorize.config.status.signIn");
        if (state == State.LOADED && listWidget != null && listWidget.isEmpty()) {
            String key = switch (view) {
                case INBOX -> "firorize.config.status.noInbox";
                case BUILTIN -> "firorize.config.status.noBuiltin";
                case COMMUNITY -> "firorize.config.status.noPresets";
            };
            return Text.translatable(key);
        }
        return null;
    }
}
