package com.oscimate.firorize.config;

import com.oscimate.firorize.Main;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import org.apache.commons.collections4.map.ListOrderedMap;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Set;

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
        super(Text.translatable("firorize.config.title.newProfile"));
        this.parent = parent;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.setFocused(null); // clear previous focus/outline; a genuinely-clicked widget re-acquires it via super
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public ButtonWidget fromExistingButton;
    public ButtonWidget fromNewButton;
    public TextFieldWidget presetNameField;
    private Text nameError = null;

    private int boxX, boxY, boxW, boxH;

    @Override
    protected void init() {
        parent.isPresetAdd = false;
        boxW = Math.min(300, width - 40);
        boxH = 120;
        boxX = (width - boxW) / 2;
        boxY = (height - boxH) / 2;

        this.presetNameField = new PlaceholderField(this.textRenderer, boxX + 20, boxY + 34, boxW - 40, 20, ScreenTexts.DONE);
        presetNameField.setMaxLength(Integer.MAX_VALUE);

        int btnW = (boxW - 40 - 6) / 2;
        this.fromExistingButton = new ButtonWidget.Builder(Text.translatable("firorize.config.button.profileFromCurrentButton"), button -> addFromExisting())
                .dimensions(boxX + 20, boxY + 64, btnW, 20).build();
        this.fromNewButton = new ButtonWidget.Builder(Text.translatable("firorize.config.button.profileFromNewButton"), button -> addFromNew())
                .dimensions(boxX + 20 + btnW + 6, boxY + 64, btnW, 20).build();

        this.addDrawableChild(new ButtonWidget.Builder(Text.literal("x"), button -> close())
                .dimensions(boxX + boxW - 22, boxY + 6, 16, 16).build());
        this.addDrawableChild(presetNameField);
        this.addDrawableChild(fromExistingButton);
        this.addDrawableChild(fromNewButton);
        super.init();
        Main.inConfig = true;

        fromExistingButton.setTooltip(Tooltip.of(Text.translatable("firorize.config.tooltip.profileFromCurrentButton")));
        fromExistingButton.setTooltipDelay(Duration.ofMillis(750L));
        fromNewButton.setTooltip(Tooltip.of(Text.translatable("firorize.config.tooltip.profileFromNewButton")));
        fromNewButton.setTooltipDelay(Duration.ofMillis(750L));
        presetNameField.setPlaceholder(Text.translatable("firorize.config.placeholder.newProfileNameField"));
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
    public void resize(MinecraftClient client, int width, int height) {
        Main.setScale(width, height, client);
        super.resize(client, client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight());
    }

    public void addProfile(KeyValuePair<KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>, int[]>, ArrayList<Integer>> newProfile) {
        if (newProfile != null) {
            if (presetNameField.getText().isEmpty()) {
                nameError = Text.translatable("firorize.config.tooltip.empty");
            } else if (Main.CONFIG_MANAGER.getFireColorPresets().keySet().stream().anyMatch(presetNameField.getText()::equalsIgnoreCase)) {
                nameError = Text.translatable("firorize.config.tooltip.exists");
            } else {
                parent.presetListWidget.addProfile(presetNameField.getText(), newProfile);
                Main.setScale(width, height, client);
                client.setScreen(parent);
            }
        }
    }

    // Only these classes may be instantiated while deserializing a shared/imported profile. Profile
    // codes come from untrusted sources (the gallery, other players, the clipboard), so the stream is
    // locked to the exact data-holder shape — KeyValuePair + the collections/primitives it contains.
    // Everything else (i.e. any Java deserialization "gadget" class) is rejected before readObject can
    // construct it, which is what makes processing this external data safe.
    private static final Set<Class<?>> ALLOWED_DESERIALIZE_CLASSES = Set.of(
            KeyValuePair.class, ListOrderedMap.class, ArrayList.class, HashMap.class,
            // Number is Integer's superclass; its class descriptor is resolved while reading any
            // boxed Integer in the priority list, so it must be allowed or every import is rejected.
            Integer.class, Number.class, String.class);

    // Array component types that legitimately appear as the backing store of the collections above
    // (ArrayList's Object[] elementData, HashMap's HashMap.Entry[]/Object[] table). Arrays carry no
    // behaviour of their own — each element is filtered individually as it resolves — so allowing
    // these structural arrays is safe and is required for the profile graph to deserialize at all.
    private static final Set<Class<?>> ALLOWED_ARRAY_COMPONENTS = Set.of(
            Object.class, java.util.Map.Entry.class);

    /** Hard limit on a decoded profile blob; the Worker caps uploads at 100 000 chars, this guards the
     *  clipboard path and bounds resource use during deserialization. */
    private static final int MAX_PROFILE_BYTES = 200_000;

    private static final ObjectInputFilter PROFILE_FILTER = info -> {
        // Resource-exhaustion guards (apply to the whole stream, serialClass is null for these).
        if (info.depth() > 50 || info.references() > 10_000 || info.streamBytes() > MAX_PROFILE_BYTES) {
            return ObjectInputFilter.Status.REJECTED;
        }
        if (info.arrayLength() > 100_000) return ObjectInputFilter.Status.REJECTED;
        Class<?> c = info.serialClass();
        if (c == null) return ObjectInputFilter.Status.UNDECIDED; // not a class check (depth/refs/array)
        if (c.isArray()) {
            Class<?> base = c;
            while (base.isArray()) base = base.getComponentType();
            // int[] etc. (primitive), arrays of allowlisted holders, and the structural Object[]/
            // Map.Entry[] backing arrays. Anything else stays rejected.
            return base.isPrimitive()
                    || ALLOWED_DESERIALIZE_CLASSES.contains(base)
                    || ALLOWED_ARRAY_COMPONENTS.contains(base)
                    ? ObjectInputFilter.Status.ALLOWED : ObjectInputFilter.Status.REJECTED;
        }
        return ALLOWED_DESERIALIZE_CLASSES.contains(c)
                ? ObjectInputFilter.Status.ALLOWED : ObjectInputFilter.Status.REJECTED;
    };

    @SuppressWarnings("unchecked") // shape is validated by the instanceof checks above the cast
    public static KeyValuePair<KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>, int[]>, ArrayList<Integer>> deserializeFromString(String str) {
        try {
            byte[] data = Base64.getDecoder().decode(str);
            if (data.length > MAX_PROFILE_BYTES) return null;

            try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
                 ObjectInputStream ois = new ObjectInputStream(bais)) {
                // Lock the stream to the known profile shape *before* reading, so untrusted gadget
                // classes are refused at resolve time rather than constructed.
                ois.setObjectInputFilter(PROFILE_FILTER);
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
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Render the config screen behind as a modal backdrop (pushed back in z, previews suppressed),
        // then a dim overlay and the dialog box (matching the profile-delete confirm box), rather than
        // blurring through to the game.
        ChangeFireColorScreen.renderModalBackdrop(context, parent, delta);
        context.fill(0, 0, this.width, this.height, 0xB0000000);
        context.fill(boxX - 1, boxY - 1, boxX + boxW + 1, boxY + boxH + 1, 0xFF000000);
        context.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xFF1A1A1A);
        context.drawBorder(boxX, boxY, boxW, boxH, 0xFF8B8B8B);
        context.drawCenteredTextWithShadow(textRenderer, getTitle(), width / 2, boxY + 9, 0xFFFFFFFF);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta); // renderBackground (parent + dim + box) then the dialog widgets

        // Validation feedback as red text in the dialog (matching the other dialogs), not a tooltip.
        if (nameError != null) {
            context.drawCenteredTextWithShadow(textRenderer, nameError, width / 2, boxY + boxH - 18, 0xFFE08080);
        }
    }
}
