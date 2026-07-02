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
        super(Component.translatable("firorize.config.title.newProfile"));
        this.parent = parent;
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent click, boolean doubled) {
        this.setFocused(null); // clear previous focus/outline; a genuinely-clicked widget re-acquires it via super
        return super.mouseClicked(click, doubled);
    }

    public Button blockButton;
    public Button tagButton;
    public Button biomeButton;
    public EditBox presetNameField;
    private Component nameError = null;

    private int boxX, boxY, boxW, boxH;
    // Caption (heading + wrapped grey description) drawn between the name field and the type buttons,
    // explaining that a profile recolours fire by one thing. Positions/lines computed once in init().
    private int promptY, descX, descY;
    private java.util.List<net.minecraft.util.FormattedCharSequence> descLines = java.util.List.of();

    @Override
    protected void init() {
        parent.isPresetAdd = false;
        // Box sized snugly to its content: title, name field, a short caption explaining the choice,
        // the three type buttons, and a reserved line for the validation message — no dead space.
        int pad = 10;
        boxW = Math.min(300, width - 40);

        // Caption wrapped to the box width; the box grows to fit however many lines it takes.
        descLines = font.split(Component.translatable("firorize.config.newProfile.description"), boxW - pad * 2);

        int nameTop = 28;
        int promptTop = nameTop + 20 + 8;                 // heading, below the name field
        int descTop = promptTop + font.lineHeight + 2;    // grey description under the heading
        int buttonsTop = descTop + descLines.size() * font.lineHeight + 8;
        boxH = buttonsTop + 20 + 18;                       // buttons + reserved validation line

        boxX = (width - boxW) / 2;
        boxY = (height - boxH) / 2;
        descX = boxX + pad;
        promptY = boxY + promptTop;
        descY = boxY + descTop;

        this.presetNameField = new PlaceholderField(this.font, boxX + pad, boxY + nameTop, boxW - pad * 2, 20, CommonComponents.GUI_DONE);
        presetNameField.setMaxLength(Integer.MAX_VALUE);

        // Pick the profile's single type; the profile starts empty for that category. Each button's
        // tooltip spells out what that category recolours by.
        int gap = 6;
        int btnW = (boxW - pad * 2 - gap * 2) / 3;
        this.blockButton = new Button.Builder(Component.translatable("firorize.config.type.block"), button -> createOfType(0))
                .bounds(boxX + pad, boxY + buttonsTop, btnW, 20).build();
        this.tagButton = new Button.Builder(Component.translatable("firorize.config.type.tag"), button -> createOfType(1))
                .bounds(boxX + pad + btnW + gap, boxY + buttonsTop, btnW, 20).build();
        this.biomeButton = new Button.Builder(Component.translatable("firorize.config.type.biome"), button -> createOfType(2))
                .bounds(boxX + pad + (btnW + gap) * 2, boxY + buttonsTop, btnW, 20).build();
        blockButton.setTooltip(Tooltip.create(Component.translatable("firorize.config.newProfile.tip.block")));
        tagButton.setTooltip(Tooltip.create(Component.translatable("firorize.config.newProfile.tip.tag")));
        biomeButton.setTooltip(Tooltip.create(Component.translatable("firorize.config.newProfile.tip.biome")));

        this.addRenderableWidget(new Button.Builder(Component.literal("x"), button -> onClose())
                .bounds(boxX + boxW - 22, boxY + 6, 16, 16).build());
        this.addRenderableWidget(presetNameField);
        this.addRenderableWidget(blockButton);
        this.addRenderableWidget(tagButton);
        this.addRenderableWidget(biomeButton);
        super.init();
        Main.inConfig = true;

        presetNameField.setHint(Component.translatable("firorize.config.placeholder.newProfileNameField"));
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
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public void resize(int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        Main.setScale(width, height, minecraft);
        super.resize(minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight());
    }

    public void addProfile(KeyValuePair<KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>, int[]>, ArrayList<Integer>> newProfile, int type) {
        if (newProfile != null) {
            if (presetNameField.getValue().isEmpty()) {
                nameError = Component.translatable("firorize.config.tooltip.empty");
            } else if (Main.CONFIG_MANAGER.getFireColorPresets().keySet().stream().anyMatch(presetNameField.getValue()::equalsIgnoreCase)) {
                nameError = Component.translatable("firorize.config.tooltip.exists");
            } else {
                parent.presetListWidget.addProfile(presetNameField.getValue(), newProfile, type);
                Main.setScale(width, height, minecraft);
                minecraft.setScreen(parent);
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
                                && pair.getRight() instanceof ArrayList
                                && isWellFormedProfile((ArrayList<?>) innerPair.getLeft(), (int[]) innerPair.getRight())) {
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

    /** True only for the exact shape the rest of the mod assumes: a base colour of {base, overlay} and
     *  three category maps (block/tag/biome), each a {@link ListOrderedMap} whose every value is an
     *  int[] of length 2. An untrusted import can pass the class allowlist but still be the wrong shape
     *  (missing maps, non-map entries, wrong-length colour arrays); rejecting it here stops the in-world
     *  colour lookup ({@code Main.resolveActiveFireColor}) from throwing on it later. */
    private static boolean isWellFormedProfile(ArrayList<?> maps, int[] base) {
        if (base.length != 2 || maps.size() != 3) return false;
        for (Object m : maps) {
            if (!(m instanceof ListOrderedMap<?, ?> lom)) return false;
            for (Object v : lom.values()) {
                if (!(v instanceof int[] arr) || arr.length != 2) return false;
            }
        }
        return true;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        // Draw the config screen behind (with its deferred 3D/colour-wheel elements suppressed so they
        // don't composite over this dialog), then a dim overlay and the dialog box (matching the
        // profile-delete confirm box), rather than blurring through to the game.
        ChangeFireColorScreen.renderModalBackdrop(context, parent, delta);
        context.fill(0, 0, this.width, this.height, 0xB0000000);
        context.fill(boxX - 1, boxY - 1, boxX + boxW + 1, boxY + boxH + 1, 0xFF000000);
        context.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xFF1A1A1A);
        context.outline(boxX, boxY, boxW, boxH, 0xFF8B8B8B);
        context.text(font, getTitle(), boxX + 10, boxY + 9, 0xFFFFFFFF);

        // Caption between the name field and the type buttons: a brighter heading and a wrapped grey
        // description, clarifying that the chosen button decides how this profile picks its colour.
        context.text(font, Component.translatable("firorize.config.newProfile.prompt"), descX, promptY, 0xFFD8D8D8);
        int ly = descY;
        for (net.minecraft.util.FormattedCharSequence line : descLines) {
            context.text(font, line, descX, ly, 0xFF9A9A9A);
            ly += font.lineHeight;
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta); // renderBackground (parent + dim + box) then the dialog widgets

        // Validation feedback as red text in the dialog (matching the other dialogs), not a tooltip.
        if (nameError != null) {
            context.centeredText(font, nameError, width / 2, boxY + boxH - 14, 0xFFE08080);
        }
    }
}
