# Iridium UI Hierarchy & Design System

Migrated from Mori (`../mori`, 10-agent audit, Wave 1). Rule: clone Mori *chrome*,
adapt *semantics* to books. Reader engine, page-count semantics, and the
AppCompat theme parent are intentionally Iridium-only.

## 1. Principles

1. Emotion-driven aesthetics: bolder dynamic color, tactile surfaces,
   variable-font type (Google Sans Flex).
2. Physics-based motion (`IridiumMotion`): springs/momentum, never mechanical
   beziers. No raw `tween`/`spring` outside `IridiumMotion.kt`.
3. Tactile haptic grammar (`IridiumHaptic` via Pulsar) on nav/scrub/toggle/dialog.
   Fire only in `onClick`/`onCheckedChange`/`onValueChange` handlers, never in
   composition. Respect `LocalHapticsEnabled`.
4. Content-first immersion: controls float in detached islands/docks; reading
   surface stays edge-to-edge. Chrome toggles must never resize content
   (visibility-ignoring insets in the reader).
5. Calm motion + AMOLED + predictive-back + dynamic wallpaper throughout.

## 2. Type discipline (L1–L4 + Hero)

| Level | Style | Size/Leading | Weight | Use |
|---|---|---|---|---|
| L1 | `topBarTitle` | 28/28 | 900 Black | collapsing top bars |
| L1b | `displaySoft` hero | 28/36 | 700–800 | onboarding hero, step titles |
| L2 | `IridiumEmphasized.titleLarge` | 22/28 | 700 | section cards/headers |
| L3 | `titleMedium` | 16/24 | 700 | sheet/group headings + `heading()` |
| L4 | `bodyLarge` | 16/24 | 400/600 | rows (`titleStyle` param switches emphasis) |
| Hero numeral | `displayFlex` | 28–64, wdth-125, slant-10 | 900 | stats hero numbers |
| Caption | `bodySmall` | 12/16 | 400 | supporting copy |

Dialogs use `headlineSmall` (never Black display). `ScreenTitleSize 28.sp`,
`ScreenTitleLineHeight 28.sp`.

## 3. Color

- Dynamic wallpaper color on Android 12+, 11 static presets
  (Iridium/Ocean/Forest/Sunset/Catppuccin/Nord/Gruvbox/Dracula/Tokyo Night/
  Everforest/Monochrome) with `displayName` + `previewColor()`.
- Every container/error/surface role is **derived** (`containerFor`,
  `darkSurfaceRamp`/`lightSurfaceRamp`, 0.12 whisper) — presets only hand-tune
  accents, so non-dynamic schemes never render baseline containers.
- `surfaceContainerLow` = bento blocks; `High` = floating docks/scrubber/nav
  (4dp tonal, 6dp shadow); `Highest` = inactive chips/tracks;
  `primaryContainer` = active toggles; `outlineVariant` 1dp @ 0.2–0.25 borders.
- AMOLED: proportional `darkened()` toward black (tint survives), never flat gray.
- `LocalAmoled` provided as `amoled && darkTheme`.

## 4. Motion tokens

| Token | Value |
|---|---|
| Easings | Emphasized (0.2,0,0,1), Decelerate (0.05,0.7,0.1,1), Accelerate (0.3,0,0.8,0.15), EaseOutQuad, EaseInOutQuad |
| Spatial / effects | spring Medium/0.6, spring Medium/NoBouncy |
| Screen enter/exit | 400ms Dec / 200ms Acc; full-width push, 1/4 parallax counterpart |
| Pop exit | full-width slide, **no fade** (predictive-back scrub stays opaque) |
| Wizard handoff | fade-only enter/exit |
| Tab glide | 450/300 tweens (retargetable, calm = fade) |
| Reader route | 180/150 fades (slides read as lag on fullscreen bed) |
| Cover morph | 650ms Decelerate tween |
| Chrome auto-hide | 3000ms; accordion expand/collapse springs; calm = 200ms fade |
| `IridiumEnterKind` | CHROME_TOP/BOTTOM (±it/2), TOOLBAR, SEARCH, FAB (0.6 heroSpring), RISE (it/4), FADE_THROUGH (0.98 scale), FADE — all calm-gated |

## 5. Haptic grammar

| Event | Feel | Call sites |
|---|---|---|
| Select | light blip | tabs, chips, segmented, FAB, cards, chrome buttons |
| ToggleOn/Off | distinct on/off | switches, bookmark toggles |
| FrequentTick | light scrub | sliders/scrubbers per 1/20 travel |
| Tick | discrete step | slider release, refresh |
| Confirm | success chime | dialogs, destructive confirms, cache cleared |
| PrimaryAction | firm thud | resume/primary CTAs |
| Reject | error buzz | failures, destructive rows |
| LongPress | deep press | card long-press menus |

Pulsar 1.3.0: capability-tier check (`>= LIMITED_SUPPORT`), system semantic
presets only, `LocalHapticFeedback` fallback, cached closures.

## 6. Screen inventory

1. **Library (root tab):** collapsing top bar (28sp + Tune + active dot) →
   floating search island (56dp pill, float color/elevation on scroll) →
   quick-filter chips (first grid item) → book grid (Adaptive 128dp, 4 card
   variants, spine brush, badges, wavy progress) → bottom action island
   (Link/Rescan/Index) + Now-Reading hero.
   Shelves carousel: BUILT but UNWIRED (`LibraryShelvesCarousel` +
   `libraryShelves()` exist and are unit-tested; not called by
   `LibraryScreen` — quick chips are the live filter).
2. **Detail:** collapsing header (direct bookmark-toggle + remove icons, no
   overflow menu) → hero (120dp cover, shared element) → single full-width
   Resume/Start CTA → metadata rows (Format/File) → plain chapter TOC (no
   per-chapter completion marks) → bookmarks/highlights with delete →
   remove confirm dialog.
3. **Reader:** full-bleed Readium canvas + stable safe insets →
   floating transparent top/bottom chrome with Haze blur (live) +
   `BottomAppBar` slider scrubber with % + position label (live).
   BUILT but UNWIRED: `ReaderScrubberIsland` (tooltip bubble),
   `ReaderActionDock`, `ReaderChromeFloatingToolbar` — pure/tap-zone logic
   (`chromeZoneForTap`) is unit-tested, screen wiring pending. Settings/TOC/
   highlights sheets wired; overflow menu holds highlight-selection entry.
   Tap-zone overlay preview, tap-invert, swipe-hides-chrome, and
   scrubber-island swap-in remain pending.
4. **History:** plain (non-collapsing) top bar → day-grouped recency list
   (Today/Yesterday/dated, live). Stats bento BUILT but UNWIRED
   (`HistoryStatsBento`: hero time estimate, segments/finished cards,
   activity chart + range selector, streak, top books — unit-tested, not
   hosted in the tab). Collapsing header + in-tab wiring pending.
5. **Settings:** flat sections on a plain (non-collapsing) scaffold
   (Appearance/Motion/Library/Reading/Storage/About hero) → licenses route
   → haptics switch → scheme picker. All wired; collapsing scaffold pending.
   Settings hub / storage manager / folders CRUD deferred (Wave 3).
6. **Onboarding:** Welcome → Folder (SAF) → Appearance; segmented progress,
   `STEP x OF n`, morphing hero, calm-gated stagger.

## 7. Component registry

`IridiumButton` ×6 (Primary/Tonal/Outlined/Text/Icon/FilledTonalIcon, 48dp,
haptic-first) · `IridiumAlertDialog`/`IridiumConfirmDialog` (28dp, tonal 6dp) ·
`IridiumExpressiveCard` (press 0.97 spring) · `IridiumSectionCard` ·
`IridiumErrorCard` (loading swaps primary, hides secondary) ·
`IridiumCollapsingTopBar` (transparent, 28sp) · `IridiumSectionHeader` (badge) ·
`IridiumChoiceGroup`/`IridiumFilterPills` (20dp/8dp morph corners) ·
`IridiumLoadingIndicator` (wavy rings) · `IridiumLoading`/`IridiumEmptyState`
(icon bed, nullable action) · `IridiumProgressBar` (wavy) ·
`IridiumSliderRow` (squiggly, scrub ticks) · `IridiumSettingSwitch`
(56dp, icon bed, thumb glyphs, ToggleOn/Off) · `IridiumSettingRow` (72dp card) ·
`BookCoverArt` (canvas placeholder, mem-cache, no disk) ·
`IridiumScrimPill` (scrim 65%, icon support) · `IridiumContentWell` (840dp) ·
`IridiumMorphingShape` (blossom ornament) · `SchemePickerRow` (phone mockups) ·
`IridiumSheet` (28dp top, surfaceContainerLow, ColumnScope).

Sheets: 24dp horizontal, 32dp bottom + navBars insets, scrollable,
skipPartiallyExpanded for tall content, L3 group headings, test tags.

## 8. Changelog guide

User-facing entries only. Never mention internal refactors, Mori, or
reference repos. One line per user-visible change, grouped: Added / Changed /
Fixed.
