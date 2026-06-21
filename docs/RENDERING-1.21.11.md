# Firorize rendering internals — 1.21.11 port notes

Hard-won, version-specific rendering knowledge from porting Firorize from 1.21/1.21.1 to
**1.21.11**. This is the stuff that is *not* obvious from the code and that broke (or silently
mis-rendered) during the port. Read this before touching anything under `FirorizePipelines`,
`test/TestModel`, `config/render/*`, the mixins in `mixin/fire_overlays`, or the custom shaders.

Always verify API shapes against the actual named-mapped merged jar + sources jar (see CLAUDE.md
for the path and `javap` recipe). Mappings and method shapes change between point releases.

---

## 1. The two *separate* fire-recolour mechanisms

There are **two completely different** recolour paths. Confusing them is the #1 source of bugs.

### (a) In-world fire — *pre-baked recoloured sprites* (no shader)
- `SpriteLoaderMixin` generates, at atlas-stitch time, a recoloured sprite per colour:
  `block/fire_<n>_<R>_<B>` (n = 0/1 layer; R, B encode the colour). It reconstructs the animation
  frames (`AnimationResourceMetadata`, frame order `[16..31, 0..15]`) and builds a 6-arg
  `SpriteContents`.
- `TestModel.emitQuads` (when **not** in config) resolves the colour for that fire position
  (`computeColor`, block-under → priority over block/tag/biome) and **re-textures** the fire quads
  onto the matching `fire_<n>_<R>_<B>` sprite via `fromBakedQuad` → normalise UVs → `spriteBake`.
- The quads then render through the **normal terrain/block render layer**. There is *no* custom
  shader involved in-world — the sprite already carries the right colours.

### (b) Config-screen preview — *grayscale sprite + custom_tint shader*
- `TestModel.emitQuads` (when `Main.inConfig`) re-textures the fire quads onto a **grayscale**
  sprite `block/blank_fire_1_config` (or `block/blank_fire_overlay_1_config` for soul fire).
- These quads render through the **`FirorizePipelines.CUSTOM_TINT`** pipeline (`getCustomTint()`
  render layer). The fragment shader reads the **target colour from the vertex colour** and
  recolours the grayscale fire's luminance toward that hue/saturation/lightness (HSV math ported
  from the original 1.21.1 core shader).
- Therefore the preview colour **must be carried in the vertex colour** of the fire quads.

> Because of (a)/(b), `CUSTOM_TINT` / `COLOR_WHEEL` are **GUI-only** in this port. The in-world
> tinting that "just works" does not exercise them at all.

---

## 2. Custom render pipelines replace core-shader injection

Pre-1.21.5 the mod injected vanilla *core shaders*. In 1.21.11 you build a
`com.mojang.blaze3d.pipeline.RenderPipeline` instead (`FirorizePipelines`):

- `RenderPipeline.builder(<SNIPPET>)` + `.withLocation(...)` + `.withVertexShader(...)` +
  `.withFragmentShader(...)` + `.withSampler("Sampler0")` + `.withBlend(...)` +
  `.withDepthTestFunction(...)` + `.withVertexFormat(VertexFormats.X, DrawMode.QUADS)` + `.build()`.
- Pipelines compile **lazily on first use** (`GlBackend.compilePipelineCached`). You do **not** have
  to register them into vanilla's pipeline map — just hold the static and reference it.
- Snippets: `RenderPipelines.TRANSFORMS_PROJECTION_FOG_SNIPPET` (has fog uniforms) vs
  `RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET` (no fog).
- `CUSTOM_TINT` uses `VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL` (block/terrain-like).
  `COLOR_WHEEL` uses `POSITION_TEXTURE_COLOR`.
- To draw a render layer with a pipeline: `RenderLayer.of(name, RenderSetup.builder(PIPELINE)
  .texture("Sampler0", SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE).expectedBufferSize(n).build())`.
  (`BLOCK_ATLAS_TEXTURE` is deprecated but still the correct atlas id in 1.21 — suppress the warning
  with a justifying comment.)

### Shader source format (assets/minecraft/shaders/core/firorize/*.vsh/.fsh)
- `#version 330` + UBO includes: `#moj_import <minecraft:fog.glsl>`,
  `#moj_import <minecraft:dynamictransforms.glsl>`, `#moj_import <minecraft:projection.glsl>`.
- `dynamictransforms.glsl` provides `ModelViewMat`, `ColorModulator`, `ModelOffset`, `TextureMat`.
  `projection.glsl` provides `ProjMat`. Vertex position: `gl_Position = ProjMat * ModelViewMat *
  vec4(Position + ModelOffset, 1.0)`.
- Vertex attributes are by **name** (`Position`, `Color`, `UV0`, `UV2`, `Normal`). Unused declared
  attributes (e.g. `UV2`, `Normal`) produce **benign** link warnings
  (`Could not find vertex shader attribute 'UV2'/'Normal'`) — vanilla shaders log the exact same
  ones. They are not errors.

### ⚠️ Fog in GUI context — the white-fire trap
`fog.glsl`'s `apply_fog` does `mix(inColor.rgb, FogColor.rgb, fogValue * FogColor.a)`. The
`SpecialGuiElementRenderer` (below) sets up projection + modelview but **never sets the Fog UBO**,
so a fog-using shader runs against whatever fog state happens to be bound during the GUI pass —
which can blend the whole element toward the fog colour. **GUI-only pipelines should not depend on
fog**: build them from `TRANSFORMS_AND_PROJECTION_SNIPPET` and don't `apply_fog` in the fragment
shader. (This was *a* candidate cause of the white fire; the actual root cause turned out to be
§4, but the fog dependency is still a latent GUI hazard worth removing.)

---

## 3. 3D-in-GUI: `SpecialGuiElementRenderer`

Since 1.21.5 the GUI matrix stack is **2D** (`org.joml.Matrix3x2fStack`: `pushMatrix/popMatrix/
translate(x,y)/scale(x,y)`). To draw 3D block models in a screen you register a
`SpecialGuiElementRenderer<T>` via Fabric `SpecialGuiElementRegistry.register(ctx -> new
Renderer(ctx.vertexConsumers()))` and queue a `SpecialGuiElementRenderState` with
`context.state.addSpecialElement(state)`.

Key facts about the base class (verified in decompiled source):
- It renders into an **offscreen RGBA8 texture** sized `(x2-x1)*windowScale × (y2-y1)*windowScale`,
  then composites it back as a textured quad over `[x1,y1,x2,y2]`.
- It hands your `render(T, MatrixStack)` a **real 3D `MatrixStack`** pre-set to: origin at the box
  **centre-bottom** (`translate(width/2, getYOffset(height), 0)`), pre-scaled by
  `windowScaleFactor * state.scale()` with a **negated Z** (`scale(f, f, -f)`).
- It sets the **orthographic** projection (`ProjectionMatrix2`, near/far ±1000) and calls
  `this.vertexConsumers.draw()` (flushes all buffered layers) after your `render`.
- It **does not** touch the Fog UBO, lightmap, or overlay — you get whatever global state is current
  (see §2 fog trap; set diffuse lighting yourself via `DiffuseLighting.Type.ENTITY_IN_UI`).
- `state.scissorArea()` clips the composited quad; `createBounds(x1,y1,x2,y2,scissor)` builds bounds.

Op-to-screen mapping: a screen-pixel offset maps to model units by
`(px - boxCentre) / (state.scale())`. Placement/scale generally needs in-game tuning.

---

## 4. ⚠️ `BlockModelRenderer.render` (static) does NOT run `emitQuads` — use `FabricBlockModelRenderer`

**This was the actual root cause of the white config fire.** Two distinct static methods exist:

### `net.minecraft.client.render.block.BlockModelRenderer.render(MatrixStack.Entry, VertexConsumer, BlockStateModel, float r, float g, float b, int light, int overlay)`
- Iterates `model.getParts(Random.create(42L))` and buffers the **baked quads directly**.
- It **never calls `emitQuads`** — so a `WrapperBlockStateModel` that overrides `emitQuads`
  (our `TestModel`, which does the config re-texture) is **bypassed entirely**.
- The `r,g,b` are applied **only to quads with a tint index** (`bakedQuad.hasTint()`); untinted
  quads (fire!) are forced to **(1,1,1) = white**. Passing a tint colour for fire does nothing.
- Fine for plain blocks (the grid cells), useless for our fire preview.

### `net.fabricmc.fabric.api.renderer.v1.render.FabricBlockModelRenderer.render(MatrixStack.Entry, BlockVertexConsumerProvider, BlockStateModel, float r, float g, float b, int light, int overlay, BlockRenderView blockView, BlockPos pos, BlockState state)`
- **This one runs `emitQuads`** (so `TestModel`'s config re-texture happens) and correctly buffers
  models with geometry on multiple render layers.
- `blockView` **may be empty** (`EmptyBlockView.INSTANCE`); `pos` should then be `BlockPos.ORIGIN`,
  `state` the block's state (passed into `emitQuads`).
- Implemented by Indigo's `SimpleBlockRenderContext.bufferModel` → calls
  `model.emitQuads(emitter, blockView, pos, state, random, cullFace -> false)`.
- **Crucial Indigo detail:** `tintQuad` still only multiplies `r,g,b` into quads with
  `tintIndex() != -1`. **Untinted fire quads are NOT tinted by the r,g,b param.** So the preview
  colour must be written **inside `emitQuads`** via `emitter.color(argb, argb, argb, argb)` — it
  cannot be delivered through the `r,g,b` arguments.
- The output buffer is chosen per render layer via the `BlockVertexConsumerProvider` you pass
  (`layer -> immediate.getBuffer(...)`). To force the custom-tint pipeline, return
  `immediate.getBuffer(FirorizePipelines.getCustomTint())` for every layer.

> Net rule for the config fire preview: render through `FabricBlockModelRenderer.render` with a
> `BlockVertexConsumerProvider` returning the custom-tint buffer, **and** have `TestModel.emitQuads`
> stamp the preview colour onto the vertices in config mode. The `r,g,b` args are irrelevant for
> fire (pass `1,1,1`).

Block model lookup: `BlockRenderManager.getModel(BlockState)` → `BlockStateModel` (our wrapped
`TestModel` for FIRE/SOUL_FIRE). Plain-block render layer for the grid:
`BlockRenderLayers.getEntityBlockLayer(state)`.

---

## 5. Model wrapping (Fabric renderer-api-v1 8.0.3)

- `TestModel extends WrapperBlockStateModel`, overrides
  `emitQuads(QuadEmitter, BlockRenderView, BlockPos, BlockState, Random, Predicate<Direction>)`.
- Wrap registration: `ModelLoadingPlugin` → `pluginContext.modifyBlockModelAfterBake().register(
  ModelModifier.WRAP_PHASE, (model, context) -> ...)`. 1.21.4+ wraps **whole block-state models**;
  the context exposes `context.state()` (a `BlockState`) — dispatch on the **block** (`Blocks.FIRE`,
  `Blocks.SOUL_FIRE`), not on per-sub-model resource ids (those are gone).
- Emit loop: for each `BlockModelPart` from `getParts(random)`, for each face index up to
  `ModelHelper.NULL_FACE_ID`, honour `cullTest`, then per `BakedQuad`: `emitter.fromBakedQuad(q)` →
  normalise the source-sprite UVs → `emitter.spriteBake(target, MutableQuadView.BAKE_NORMALIZED)` →
  `emitter.cullFace(d)` → (config: `emitter.color(...)`) → `emitter.emit()`.
- Sprite lookup replacement for the removed `SpriteIdentifier.getSprite()`: go through
  `FireSprites` (`AtlasManager.getSprite(new SpriteIdentifier(ATLAS, Identifier.of(...)))`). Use
  `Identifier.of("firorize", "block/...")` so namespaced ids resolve.

---

## 6. GUI 2D draw API changes (`DrawContext`)

- `getMatrices()` → 2D `Matrix3x2fStack`: `pushMatrix()/popMatrix()`, `translate(x,y)`,
  `scale(x,y)`, `multiply(Matrix3x2fc)`, `peek()`. **No Z, no `push()/pop()`, no quaternion.** Drop
  pure Z-layering translates — 2D draw order is call order.
- `drawBorder` → `drawStrokedRectangle(x,y,w,h,color)`.
- `drawSprite(x,y,z,w,h,sprite)` → `drawSpriteStretched(RenderPipeline, Sprite, x,y,w,h[,color])`
  (drop z; pass `RenderPipelines.GUI_TEXTURED`).
- `setShaderColor` removed → fold alpha into the ARGB `color` int.
- `drawTexturedQuad` is now public, signature `(Identifier, int x1,int x2,int y1,int y2, float u1,
  float u2,float v1,float v2)` (no z). Needs an access-widener entry. (Used for the UV-mirrored
  redo-icon trick — but note the project now ships a real `redo.png`, so prefer drawing the sprite
  directly.)
- **Text alpha gotcha:** `drawText*` silently **skips** text whose colour has **alpha 0**.
  `0xFFFFFF` == `0x00FFFFFF` → invisible. Always use `0xFFFFFFFF` for opaque white.
- Custom 2D pipeline quad: implement `SimpleGuiElementRenderState` (`setupVertices(VertexConsumer)`,
  `pipeline()`, `textureSetup()`, `scissorArea()`, `bounds()`); queue via
  `context.state.addSimpleElement(...)`. Used for the colour wheel (`COLOR_WHEEL`, lightness carried
  in vertex-colour alpha).
- `RenderSystem` removed in GUIs: `setShader`, `setShaderColor`, `setShaderTexture`, `depthFunc`,
  `depthMask`, `enableBlend/disableBlend`, `enableDepthTest`, `applyModelViewMatrix`. State lives in
  the `RenderPipeline`. `BufferRenderer.drawWithGlobalProgram` is gone.

---

## 7. Input API migration (screens & widgets)

- `mouseClicked(Click, boolean)`, `mouseReleased(Click)`, `mouseDragged(Click, double, double)`,
  `keyPressed(KeyInput)`, `charTyped(CharInput)`. Accessors: `Click.x()/.y()/.button()`,
  `KeyInput.key()`, `CharInput.codepoint()/.modifiers()`.
- `PressableWidget.renderWidget` is **final** (it just calls `drawIcon`). Override the abstract
  `drawIcon(DrawContext,int,int,float)`; call `this.drawButton(context)` for the background.
  `onPress(AbstractInput)` (has `.hasShift()`); `onClick(Click, boolean)`. `ButtonWidget` is now
  abstract — custom buttons must pass `Text.empty()` (never `null`; a null message NPEs the new
  inactivity-indicator narration and aborts the whole screen `init`).
- `Screen.resize(int,int)`, `Screen.applyBlur(DrawContext)` (note: `applyBlur` does **not** dim the
  in-world background — to dim like the confirm box, render parent + `context.fill(0,0,w,h,
  0xB0000000)`). `KeyBinding.Category` (use `.MISC`). `Window.setScaleFactor(int)`.
  `InputUtil.isKeyPressed(Window,int)`.

---

## 8. `EntryListWidget` refactor

- ctor `(client, width, height, int y, int itemHeight)`.
- Entries self-position: `Entry.getX()/getY()/getWidth()/getHeight()`.
- `Entry.render(DrawContext, int mouseX, int mouseY, boolean hovered, float tickDelta)`;
  `renderEntry(...)`; `drawSelectionHighlight(DrawContext, E, int color)`.
- `getScrollAmount` removed (only `setScrollY(double)`); `isSelectedEntry`/`getEntry` removed (use
  `children().get(i)` / `removeEntry(E)`). Use `removeEntry(self)` for a live-relayout delete —
  `children().remove(...)` does not refresh the layout.

---

## 9. Registries / world / misc

- `DynamicRegistryManager.getOrThrow(RegistryKey)`.
- `Registry.streamTags()` → `Stream<RegistryEntryList.Named<T>>` (`.getTag()` for the `TagKey`);
  `Registry.iterateEntries(TagKey)`.
- `Entity.getWorld()` → `getEntityWorld()`.
- `Main.blockTagList` / `biomeKeyList` default to **empty** lists (never null) so the tags tab works
  without a world without NPE. Only the **biomes** tab actually needs a world (biome registry is
  world/server-provided); blocks and tags do not.
- Atlas auto-stitch: vanilla's block atlas has a directory source scanning every namespace's
  `textures/block/`, so `firorize:block/*` (undo, redo, arrows, blank_fire_*_config, fire_*_*_*)
  stitch automatically — no atlas config json needed.

---

## 10. Build / toolchain gotchas

- **Java 21 required.** Build:
  `JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home sh ./gradlew compileJava`.
  Use `--rerun-tasks` to force re-emit of warnings (compileJava caches `UP-TO-DATE`).
- **javac error masking:** when *any* compilation unit has an unresolved import, javac aborts
  attribution and **hides** real errors in other units — so a "compiles clean except 2 lines" result
  can be a lie. Fix import errors first, then re-run to see the true error surface.
- `genSources` may OOM at 1G — rerun with `GRADLE_OPTS="-Xmx5g"`.
- `build.gradle`: `project.archivesBaseName` → `base.archivesName.get()` (Gradle 9.x).
- Keep the build warning-clean (`-Xlint:all` minus `-classfile`/`-processing`). The
  `BLOCK_ATLAS_TEXTURE` deprecation is the one sanctioned suppression.
</content>
</invoke>
