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

    public ButtonWidget blockButton;
    public ButtonWidget tagButton;
    public ButtonWidget biomeButton;
    public TextFieldWidget presetNameField;
    private Text nameError = null;

    private int boxX, boxY, boxW, boxH;
    // Caption (heading + wrapped grey description) drawn between the name field and the type buttons,
    // explaining that a profile recolours fire by one thing. Positions/lines computed once in init().
    private int promptY, descX, descY;
    private java.util.List<net.minecraft.text.OrderedText> descLines = java.util.List.of();

    @Override
    protected void init() {
        parent.isPresetAdd = false;
        // Box sized snugly to its content: title, name field, a short caption explaining the choice,
        // the three type buttons, and a reserved line for the validation message — no dead space.
        int pad = 10;
        boxW = Math.min(300, width - 40);

        // Caption wrapped to the box width; the box grows to fit however many lines it takes.
        descLines = textRenderer.wrapLines(Text.translatable("firorize.config.newProfile.description"), boxW - pad * 2);

        int nameTop = 28;
        int promptTop = nameTop + 20 + 8;                       // heading, below the name field
        int descTop = promptTop + textRenderer.fontHeight + 2;  // grey description under the heading
        int buttonsTop = descTop + descLines.size() * textRenderer.fontHeight + 8;
        boxH = buttonsTop + 20 + 18;                            // buttons + reserved validation line

        boxX = (width - boxW) / 2;
        boxY = (height - boxH) / 2;
        descX = boxX + pad;
        promptY = boxY + promptTop;
        descY = boxY + descTop;

        this.presetNameField = new PlaceholderField(this.textRenderer, boxX + pad, boxY + nameTop, boxW - pad * 2, 20, ScreenTexts.DONE);
        presetNameField.setMaxLength(Integer.MAX_VALUE);

        // Pick the profile's single type; the profile starts empty for that category. Each button's
        // tooltip spells out what that category recolours by.
        int gap = 6;
        int btnW = (boxW - pad * 2 - gap * 2) / 3;
        this.blockButton = new ButtonWidget.Builder(Text.translatable("firorize.config.type.block"), button -> createOfType(0))
                .dimensions(boxX + pad, boxY + buttonsTop, btnW, 20).build();
        this.tagButton = new ButtonWidget.Builder(Text.translatable("firorize.config.type.tag"), button -> createOfType(1))
                .dimensions(boxX + pad + btnW + gap, boxY + buttonsTop, btnW, 20).build();
        this.biomeButton = new ButtonWidget.Builder(Text.translatable("firorize.config.type.biome"), button -> createOfType(2))
                .dimensions(boxX + pad + (btnW + gap) * 2, boxY + buttonsTop, btnW, 20).build();
        blockButton.setTooltip(Tooltip.of(Text.translatable("firorize.config.newProfile.tip.block")));
        tagButton.setTooltip(Tooltip.of(Text.translatable("firorize.config.newProfile.tip.tag")));
        biomeButton.setTooltip(Tooltip.of(Text.translatable("firorize.config.newProfile.tip.biome")));

        this.addDrawableChild(new ButtonWidget.Builder(Text.literal("x"), button -> close())
                .dimensions(boxX + boxW - 22, boxY + 6, 16, 16).build());
        this.addDrawableChild(presetNameField);
        this.addDrawableChild(blockButton);
        this.addDrawableChild(tagButton);
        this.addDrawableChild(biomeButton);
        super.init();
        Main.inConfig = true;

        presetNameField.setPlaceholder(Text.translatable("firorize.config.placeholder.newProfileNameField"));
    }

    /** Creates an empty single-type profile (all three maps present but only the chosen one will ever
     *  be filled) with a default base colour. */
    private void createOfType(int type) {
        ArrayList<ListOrderedMap<String, int[]>> maps = new ArrayList<>();
        maps.add(new ListOrderedMap<>());
        maps.add(new ListOrderedMap<>());
        maps.add(new ListOrderedMap<>());
        KeyValuePair<KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>, int[]>, ArrayList<Integer>> profile =
                KeyValuePair.of(KeyValuePair.of(maps, new int[]{-7456000, -6456034}), new ArrayList<>(java.util.List.of(0, 1, 2)));
        addProfile(profile, type);
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

    public void addProfile(KeyValuePair<KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>, int[]>, ArrayList<Integer>> newProfile, int type) {
        if (newProfile != null) {
            if (presetNameField.getText().isEmpty()) {
                nameError = Text.translatable("firorize.config.tooltip.empty");
            } else if (Main.CONFIG_MANAGER.getFireColorPresets().keySet().stream().anyMatch(presetNameField.getText()::equalsIgnoreCase)) {
                nameError = Text.translatable("firorize.config.tooltip.exists");
            } else {
                parent.presetListWidget.addProfile(presetNameField.getText(), newProfile, type);
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

        // Caption between the name field and the type buttons: a brighter heading and a wrapped grey
        // description, clarifying that the chosen button decides how this profile picks its colour.
        context.drawTextWithShadow(textRenderer, Text.translatable("firorize.config.newProfile.prompt"), descX, promptY, 0xFFD8D8D8);
        int ly = descY;
        for (net.minecraft.text.OrderedText line : descLines) {
            context.drawTextWithShadow(textRenderer, line, descX, ly, 0xFF9A9A9A);
            ly += textRenderer.fontHeight;
        }
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
