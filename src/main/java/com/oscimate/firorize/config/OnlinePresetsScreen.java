package com.oscimate.firorize.config;

import com.oscimate.firorize.FireSprites;
import com.oscimate.firorize.Main;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * The Import Profiles hub, opened from the colour editor. One panel with a custom tab strip switching
 * between three views:
 * <ul>
 *   <li><b>Community</b> — the player's own uploads (Delete) and everyone else's (Import); Upload lives here.</li>
 *   <li><b>Built-in</b> — curated default profiles served from the Worker's {@code /builtin} table (Import only).</li>
 *   <li><b>Inbox</b> — profiles privately sent to the player (Import/Dismiss) and their own sends (Cancel); Send lives here.</li>
 * </ul>
 * Everything is drawn on the dark Firorize panel with {@link PanelButton}s rather than vanilla buttons,
 * so the whole section reads as one surface.
 */
public class OnlinePresetsScreen extends Screen {
    public enum View { COMMUNITY, BUILTIN, INBOX }

    private enum State { LOADING, LOADED, ERROR, NEED_AUTH }

    public final ChangeFireColorScreen parent;
    private View view;

    private OnlinePresetListWidget listWidget;
    private State state = null;

    // Community data: the player's own uploads and everyone else's (already split).
    private List<OnlinePreset> myUploads = List.of();
    private List<OnlinePreset> community = List.of();
    // Built-in (curated) data.
    private List<OnlinePreset> builtin = List.of();
    private PlaceholderField searchField;
    private PanelButton actionButton; // Upload (Community) / Send (Inbox); null on Built-in

    // Inbox data.
    private List<OnlinePreset> inboxReceived = List.of();
    private List<OnlinePreset> inboxSent = List.of();

    private Component flashText;
    private boolean flashError;
    private int flashTimer = 0;

    private static final float PRIVACY_SCALE = 0.82f;
    private Component privacyText;
    private int privacyX, privacyY, privacyW, privacyH;

    private int boxX, boxY, boxW, boxH;
    private int refreshIconX, refreshIconY;

    public OnlinePresetsScreen(ChangeFireColorScreen parent, View view) {
        super(Component.translatable(switch (view) {
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

        boolean hasSearch = view != View.INBOX;
        boolean hasAction = view != View.BUILTIN;

        int listY;
        int listH;
        if (hasSearch) {
            searchField = new PlaceholderField(this.font, boxX + 10, boxY + 24, boxW - 20, 16, Component.empty());
            searchField.setHint(Component.translatable("firorize.config.placeholder.search"));
            searchField.setMaxLength(48);
            searchField.setResponder(s -> applyList());
            addRenderableWidget(searchField);
            listY = boxY + 46;
        } else {
            // Inbox: leave room for the wrapped description under the title.
            listY = boxY + 52;
        }
        listH = boxH - (listY - boxY) - (hasAction ? 34 : 22);

        listWidget = new OnlinePresetListWidget(boxX + 10, listY, boxW - 20, listH, this, this.font);
        addRenderableWidget(listWidget);

        if (hasAction) {
            Component label = Component.translatable(view == View.INBOX
                    ? "firorize.config.button.inboxSend" : "firorize.config.button.uploadPreset");
            int w = Math.max(100, font.width(label) + 24);
            actionButton = new PanelButton(boxX + boxW - 10 - w, boxY + boxH - 26, w, 18, label,
                    b -> minecraft.setScreen(new ChooseProfileScreen(this, this)));
            addRenderableWidget(actionButton);
        } else {
            actionButton = null;
        }

        // Privacy policy: small gray underlined clickable text (not a button), bottom-left of the panel.
        privacyText = Component.translatable("firorize.config.button.privacy").withStyle(s -> s.withUnderlined(true));
        privacyW = (int) Math.ceil(font.width(privacyText) * PRIVACY_SCALE);
        privacyH = (int) Math.ceil(font.lineHeight * PRIVACY_SCALE);
        privacyX = boxX + 10;
        privacyY = boxY + boxH - 16;

        addRenderableWidget(new PanelButton(boxX + boxW - 22, boxY + 6, 16, 16, Component.literal("x"), b -> onClose()));

        // Refresh: re-pulls the current view from the Worker. The refresh.png sprite is drawn over it
        // in render() (see drawRefreshIcon).
        refreshIconX = boxX + boxW - 42;
        refreshIconY = boxY + 6;
        PanelButton refreshButton = new PanelButton(refreshIconX, refreshIconY, 16, 16, Component.empty(),
                b -> { state = null; load(); });
        refreshButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("firorize.config.tooltip.refresh")));
        addRenderableWidget(refreshButton);

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
                .whenComplete((pair, err) -> Minecraft.getInstance().execute(() -> {
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
                .whenComplete((list, err) -> Minecraft.getInstance().execute(() -> {
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
        String q = searchField == null ? "" : searchField.getValue().trim().toLowerCase();
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
                .whenComplete((pair, err) -> Minecraft.getInstance().execute(() -> {
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
            flashMessage(Component.translatable("firorize.config.status.signIn"), true);
            return;
        }
        OnlinePresetsClient.deletePreset(auth, id)
                .whenComplete((res, err) -> Minecraft.getInstance().execute(() -> {
                    if (err == null && res != null && res.success()) {
                        flashMessage(Component.translatable("firorize.config.status.deleted"), false);
                        state = null;
                        load();
                    } else {
                        flashMessage(Component.translatable("firorize.config.status.deleteFailed"), true);
                    }
                }));
    }

    /** Called by the list widget to cancel an outgoing send or dismiss an inbox item; reloads the Inbox. */
    public void deleteSend(int id) {
        OnlinePresetsClient.McAuth auth = OnlinePresetsClient.currentIdentity();
        if (auth == null) {
            flashMessage(Component.translatable("firorize.config.status.signIn"), true);
            return;
        }
        OnlinePresetsClient.deleteSend(auth, id)
                .whenComplete((res, err) -> Minecraft.getInstance().execute(() -> {
                    if (err == null && res != null && res.success()) {
                        state = null;
                        load();
                    } else {
                        flashMessage(Component.translatable("firorize.config.status.deleteFailed"), true);
                    }
                }));
    }

    /** Called by {@link UploadPresetScreen} after a successful upload to re-pull the Community lists. */
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

    public void flashMessage(Component message, boolean error) {
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
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent click, boolean doubled) {
        if (click.button() == 0 && overPrivacy(click.x(), click.y())) {
            ConfirmLinkScreen.confirmLinkNow(this, OnlinePresetsClient.PRIVACY_URL);
            return true;
        }
        this.setFocused(null);
        return super.mouseClicked(click, doubled);
    }

    private boolean overPrivacy(double mx, double my) {
        return mx >= privacyX && mx <= privacyX + privacyW && my >= privacyY && my <= privacyY + privacyH;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public void resize(int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        Main.setScale(width, height, minecraft);
        super.resize(minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        // Overlay the live config screen (dimmed) rather than cutting through to the blurred game.
        ChangeFireColorScreen.renderModalBackdrop(context, parent, delta);
        context.fill(0, 0, this.width, this.height, 0xB0000000);
        context.fill(boxX - 1, boxY - 1, boxX + boxW + 1, boxY + boxH + 1, 0xFF000000);
        context.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xFF1A1A1A);
        context.outline(boxX, boxY, boxW, boxH, 0xFF8B8B8B);
        context.text(font, getTitle(), boxX + 10, boxY + 9, 0xFFFFFFFF);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        DonationTracker.onConfigFrame();
        super.extractRenderState(context, mouseX, mouseY, delta);

        if (view == View.INBOX) {
            int dy = boxY + 28;
            for (net.minecraft.util.FormattedCharSequence line : font.split(Component.translatable("firorize.config.label.inboxDescription"), boxW - 20)) {
                context.text(font, line, boxX + 10, dy, 0xFF9A9A9A);
                dy += 10;
            }
        }

        int centerY = boxY + boxH / 2 - 14;
        Component status = statusText();
        if (status != null) {
            boolean err = state == State.ERROR;
            context.centeredText(font, status, width / 2, centerY, err ? 0xFFE08080 : 0xFFC0C0C0);
        }

        if (flashTimer > 0 && flashText != null) {
            context.centeredText(font, flashText, width / 2, boxY + boxH - 40, flashError ? 0xFFE08080 : 0xFF80E080);
        }

        drawRefreshIcon(context, refreshIconX, refreshIconY);

        int privacyColor = overPrivacy(mouseX, mouseY) ? 0xFFCFCFCF : 0xFF8C8C8C;
        context.pose().pushMatrix();
        context.pose().scale(PRIVACY_SCALE, PRIVACY_SCALE);
        context.text(font, privacyText, Math.round(privacyX / PRIVACY_SCALE), Math.round(privacyY / PRIVACY_SCALE), privacyColor, false);
        context.pose().popMatrix();
    }

    /** Draws the {@code firorize:block/refresh} sprite centred in the 16×16 refresh button at (px,py). */
    private static final int REFRESH_ICON_SIZE = 11;
    private void drawRefreshIcon(GuiGraphicsExtractor context, int px, int py) {
        TextureAtlasSprite refresh = FireSprites.block(FireSprites.atlasManager(), "firorize:block/refresh");
        int off = (16 - REFRESH_ICON_SIZE) / 2;
        context.blitSprite(RenderPipelines.GUI_TEXTURED, refresh,
                px + off, py + off, REFRESH_ICON_SIZE, REFRESH_ICON_SIZE);
    }

    private Component statusText() {
        if (state == State.LOADING) return Component.translatable("firorize.config.status.loading");
        if (state == State.ERROR) return Component.translatable("firorize.config.status.loadFailed");
        if (state == State.NEED_AUTH) return Component.translatable("firorize.config.status.signIn");
        if (state == State.LOADED && listWidget != null && listWidget.isEmpty()) {
            String key = switch (view) {
                case INBOX -> "firorize.config.status.noInbox";
                case BUILTIN -> "firorize.config.status.noBuiltin";
                case COMMUNITY -> "firorize.config.status.noPresets";
            };
            return Component.translatable(key);
        }
        return null;
    }
}
