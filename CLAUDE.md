# Firorize — developer guide for Claude

## What this mod is

Firorize is a **client-side** Fabric mod for **Minecraft 1.21.1** that gives full colour
customization over fire. Players can recolour fire per **biome**, per **block it sits on**, and per
**block tag**, with a configurable priority order resolving clashes. It also adds a **fire-height
slider** (shrinks the first-person fire overlay) and fixes the vanilla bug where **soul fire** had no
blue first-person overlay. Colour setups are grouped into named **profiles** that can be
exported/imported as clipboard codes.

- Mod id: `firorize` (`Main.MODID`). Environment: client only.
- Entrypoints (`fabric.mod.json`): client = `com.oscimate.firorize.Main`; modmenu =
  `config.ModMenuApiImpl`; preLaunch = MixinExtras bootstrap.
- Default keybind to open config: **I** (`Main.configKeybind`).

## Build & run

- **Java 21 is required** and Gradle 8.8 will **not** run on a newer JDK. The machine's default
  `java` may be too new (you'll see `Unsupported class file major version NN`). Always build with:
  ```bash
  JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home sh ./gradlew compileJava
  ```
  (`./gradlew` may need `sh ./gradlew` if not executable.) Use `--rerun-tasks` to force a recompile —
  Gradle caches `compileJava` as `UP-TO-DATE` and won't re-emit warnings otherwise.
- Run the client with the `runClient` task (Fabric loom). GUI behaviour (the config screen, undo/redo,
  fire colours in-world) must be verified **in-game** — it can't be checked headlessly.
- `build.gradle` enables `-Xlint:all` minus `-classfile`/`-processing` (those are unfixable
  third-party/Mixin noise). Keep the build warning-clean.

## Architecture map

- `Main` — `ClientModInitializer`. Registers the keybind, loads biome/block-tag lists on
  `TAGS_LOADED`, registers a model-loading plugin that wraps fire/soul-fire models with `TestModel`,
  and loads config on startup. `Main.settingFireColor(entity)` is the core in-world logic: it walks
  the blocks in an entity's bounding box, finds the block under the fire, and resolves the colour by
  **priority order** (block → tag → biome) before stamping it onto the entity via
  `RenderFireColorAccessor.firorize$setRenderFireColor`.
- `mixin/fire_overlays/client/*` — the rendering integration. Mixins/accessors into
  `GameRenderer`, `InGameOverlayRenderer`, `EntityRenderDispatcher`, sprite/atlas loaders, and the
  client network/entity classes recolour both the in-world fire and the first-person overlay, and
  carry the per-entity colour. `CustomRenderLayer` + `GameRendererSetting` + the custom shaders
  (`assets/minecraft/shaders/core/firorize/rendertype_custom_tint`, `…/rendertype_color_wheel`) do
  the tinting. `RenderFireColorAccessor` is a duck-interface mixed into `Entity`.
- `config/*` — the GUI. `ConfigScreen` is the entry screen; `ChangeFireColorScreen` is the big
  colour editor (colour wheel, lightness slider, profile list, block/tag/biome search list,
  base/overlay toggle, priority arrows, undo/redo). Custom widgets: `PresetListWidget` (profiles),
  the inner `SearchScreenListWidget` (blocks/tags/biomes), `MoveableButton`, `ColoredCycleButton`,
  `UndoButton`/`RedoButton`, text fields. `ConfigManager` holds and (de)serializes config.

## Config data model (read this before touching colours)

`Main.CONFIG_MANAGER` (`ConfigManager`) holds everything, persisted as `firorize.json` via Gson:

- `getCurrentBlockFireColors()` → `KeyValuePair<ArrayList<ListOrderedMap<String,int[]>>, int[]>`
  - `.getLeft()` = **3** `ListOrderedMap`s, index **0 = blocks, 1 = tags, 2 = biomes**; each maps an
    id string → `int[]{baseRGB, overlayRGB}`.
  - `.getRight()` = `int[]{baseRGB, overlayRGB}` — the global **base fire colour**.
- `getPriorityOrder()` → `ArrayList<Integer>` — an ordering of `{0,1,2}` deciding which category wins.
- `getFireColorPresets()` → `ListOrderedMap<String, profile>` where a profile is
  `KeyValuePair<KeyValuePair<ArrayList<ListOrderedMap<String,int[]>>,int[]>, ArrayList<Integer>>`.
  The active profile is `curPresetID`.

Gotchas:
- **`KeyValuePair` is `Serializable` and is Java-serialized + Base64-encoded for the shareable
  profile codes** (`ChangeFireColorScreen.serializeToString` / `AddProfileScreen.deserializeFromString`).
  Do **not** add a `serialVersionUID` — it would break codes users have already shared. It carries
  `@SuppressWarnings("serial")` for this reason.
- `Collections.copy(presetLeft, currentLeft)` copies **map references**, so the active preset and the
  live `currentBlockFireColors` deliberately share map objects after a commit. Preserve this idiom.
- `Main.setScale` does custom GUI-scale math; screens call it on resize/close.

## Undo/redo system (config screen)

`ChangeFireColorScreen` owns a bounded undo/redo history (`undoStack`/`redoStack`, cap
`MAX_HISTORY = 50`). Only **breaking** actions record an entry: **Apply** (`save()`), **reorder**
(`SearchScreenListWidget.moveEntryUp/Down`), **reset** (`PresetListWidget.resetProfile`), and a
**colour-wheel/slider gesture** (one entry per press→release, captured in `mouseClicked`/`mouseReleased`).
Selecting a list element is **not** recorded.

- `HistoryEntry` is either `COLOR` (before/after `pickedColor` pair) or `CONFIG` (deep before/after
  snapshot of `currentBlockFireColors` + priority order). Apply/reorder/reset all use `CONFIG`.
- `undo()`/`redo()` apply the inverse/forward state then `navigateTo(...)` — which switches the search
  tab, re-selects the associated list entry, and flips base/overlay — so it "clicks into" the element
  the action belonged to. `isUndoRedoing` guards against re-recording during this.
- History clears on **profile switch** (`PresetListWidget.setSelected`), never persists to disk.
- The redo icon is the undo sprite drawn **UV-mirrored** (not matrix-mirrored — that reverses winding
  and the GUI culls it to blank). This needs `DrawContext.drawTexturedQuad`, widened in
  `firorize.accesswidener`.

## Working against the decompiled Minecraft jar (important)

For anything that **integrates with vanilla** — rendering, mixin targets/signatures, GUI/`DrawContext`
calls, registries, model loading, events — **do not guess the API from memory or training data.**
Minecraft's mappings and method shapes change between versions. Verify against the actual
**named-mapped merged jar** this project builds against:

```
.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-1df56c0d60/\
  1.21.1-net.fabricmc.yarn.1_21_1.1.21.1+build.3-v2/\
  minecraft-merged-1df56c0d60-1.21.1-net.fabricmc.yarn.1_21_1.1.21.1+build.3-v2.jar
```
(There is also a global copy under `~/.gradle/caches/fabric-loom/...`. Find it with
`find ~/.gradle .gradle -path '*minecraft-merged*' -name '*.jar'`.)

Use `javap` to confirm real signatures, access levels, and field names before writing code:
```bash
JAR=$(find ~/.gradle .gradle -path '*minecraft-merged*1.21.1*' -name '*.jar' | head -1)
javap -p -cp "$JAR" net.minecraft.client.gui.DrawContext | grep -i drawTextured
javap -p -cp "$JAR" net.minecraft.client.texture.Sprite
```
This is exactly how the redo-icon fix was found: `drawTexturedQuad` exists but is **package-private**,
so it had to be access-widened and called with swapped U coords. Always check:
- **Access level** — package-private/protected members need an entry in `firorize.accesswidener`
  (`accessible method <owner> <name> <descriptor>`), or an `@Invoker`/`@Accessor` mixin.
- **Exact descriptor** — copy parameter order/types from `javap` (or the jar's class) rather than
  assuming; this is what the accesswidener descriptor must match.
- **How vanilla itself calls it** — search the jar/sources for existing call sites to copy the correct
  usage pattern (winding, render layers, matrix/scissor handling, etc.).

When deeper understanding (full decompiled source, mixin target validation) is needed, use the
**`minecraft-fabric-dev`** skill, which wires up MCP servers for decompilation, Yarn-mapping lookup,
and mixin validation. Prefer it over ad-hoc guessing for non-trivial mixin or rendering work.

## Conventions

- Package root `com.oscimate.firorize`; mixins under `…firorize.mixin` (see `firorize.mixins.json`).
- Lang keys live in `assets/firorize/lang/{en_us,zh_tw}.json`; add new UI strings to **both**.
- Config screens clear stale widget focus on click (`mouseClicked` → `setFocused(null)` then `super`)
  so button outlines don't stick — keep this when adding screens with clickable widgets.
- Keep the diff warning-clean; suppress with a justifying comment only where a fix is unsafe (e.g. the
  `BLOCK_ATLAS_TEXTURE` deprecation, which is still the supported atlas id in 1.21).
