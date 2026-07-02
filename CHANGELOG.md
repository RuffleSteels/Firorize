# Changelog

All notable changes to **Firorize** are documented here. Firorize is a client-side Fabric mod
that gives full colour customization over fire — per biome, per block, and per block tag — plus a
fire-height slider and a fix for the missing soul-fire first-person overlay.

The mod is maintained across several Minecraft versions on parallel branches; a single release
number (e.g. `1.5.0`) is published for each supported Minecraft version. Dates are `YYYY-MM-DD`.

## Supported Minecraft versions

| Branch            | Minecraft            | Status                 |
| ----------------- | -------------------- | ---------------------- |
| `master`          | 26.1.2               | Active                 |
| `1.21.11`         | 1.21.11              | Active                 |
| `1.21-1.21.1`     | 1.21, 1.21.1         | Active                 |
| `1.20.x` branches | 1.20.1 – 1.20.6      | Legacy                 |
| `1.16.5`–`1.19`   | 1.16.5, 1.17.1, 1.18.2, 1.19 | Legacy         |

---

## [1.5.0] — 2026-07-02

The **profile overhaul** release. Profiles are now single-purpose and stackable, with a curated
built-in gallery and a large round of hardening.

### Changed
- **Single-type profiles.** Each profile now targets exactly **one** category — a **block**, a
  **block tag**, or a **biome** — instead of mixing all three. Much simpler to build and reason about.
- **Priority is now list order.** The topmost active profile whose category matches wins; reorder
  profiles to change priority. The old per-profile priority-arrow system has been removed.
- **Editing and activation are separate.** Selecting a profile row edits it; a green/red checkbox
  toggles whether it is applied in-world.
- Streamlined single-type editor (reorder icon, search header, magnifier) exposing only the
  profile's one category.
- Legacy multi-category profiles are automatically split into single-type profiles on load, and
  previously shared profile codes still import correctly.

### Added
- **Multiple active profiles at once** — stack a block profile, a tag profile and a biome profile
  and they resolve by list order.
- **Built-in profile gallery** — a curated set of profiles you can browse and apply directly.
- **Developer-gated profile upload** (password-protected, with capped input field lengths).
- Long profile names now **marquee-scroll** on hover; profile name length is capped.
- Clearer profile tooltips, with a delayed tooltip on the profile type badge.
- "Import profiles…" heading, section separators, and panels framing the profiles/search columns.

### Fixed
- Config dialogs now keep the world / panorama background behind them instead of showing through.
- Fixed world fire breaking after the Ko-fi popup (a stuck `inConfig` flag).
- Fire-height preview overlay is screen/north-locked in-world so it no longer drifts.
- Fixed the inverted config fire preview when Sodium is installed *(26.1.2)*.
- Fixed a crash / NPE when opening config with no world loaded (null tag/biome lists), and empty
  tag/biome lists now explain that no world is loaded *(1.21/1.21.1)*.

### Security
- Hardened untrusted shared-profile deserialization against Java gadget-chain RCE with a strict
  `ObjectInputFilter` allowlist plus depth/reference/size caps.
- Hardened config loading and profile import against malformed / corrupt data.
- Hardened fire-colour resolution and the sprite / network code paths.

---

## [1.4.1] — 2026-06-27

- **Ported the entire mod to Minecraft 26.1.2** (Mojang official mappings / unobfuscated jar): build
  config, mixins, render pipeline, config GUI, block-preview rendering and custom tint shaders all
  migrated. Requires the JDK 25 toolchain.
- Fixed upload / send profile title clipping under the back button.
- CI: JDK 25 toolchain for 26.1.2, made `gradlew` executable in release, and namespaced GitHub
  release tags by Minecraft version.

---

## [1.4.0] — 2026-06-26

- **Added Minecraft 1.21.11 support** (config GUI ported to the 1.21.11 render pipeline).
- **Online profile sharing** — share profiles, an inbox for received profiles, and a community
  gallery.
- Cross-version tolerance: profiles silently ignore blocks / tags / biomes that don't exist on the
  running version.
- Added a Ko-fi donation banner, popup, and modal dialog backdrop.
- Config dialog polish (icons, alignment, layout) and a Markdown README.

---

## [1.3.2] — 2026-06-20

- **Multi-level undo/redo** in the config screen, plus a batch of config bug fixes and a
  warning-clean build.
- Preserve search-list scroll position across colour-apply and search.
- Fixed an NPE when an entity's fire colour was unset at render time.
- Publishing: advertise both 1.21 and 1.21.1, rename the jar to `firorize`, consolidate publish
  metadata, and split CI into check-build and release workflows.

---

## Earlier history

Firorize began as a **soul-fire overlay** mod and grew into a full fire-colour customizer.

### Colour customization era (2024)
- **Colour fire support** — recolour fire, with a searchable block list, colour wheel + tint
  rendering, and a first undo button.
- Customized fire colours apply to **every block a fire can be placed on**.
- Config can be opened via keybind without ModMenu installed.
- **Sodium compatibility** for the custom fire rendering.
- Traditional Chinese (`zh_tw`) translation added.

### Soul-fire overlay origins (2021–2023)
- Added the missing **soul-fire first-person overlay** and a mob soul-fire overlay.
- Consistent fire logic across all corners of a fire block; entities hit by a soul-fire mob
  (arrow / zombie / husk) are correctly set on soul fire.
- Initial ports across **1.18.2, 1.19, and 1.20**.

[1.5.0]: #150--2026-07-02
[1.4.1]: #141--2026-06-27
[1.4.0]: #140--2026-06-26
[1.3.2]: #132--2026-06-20
