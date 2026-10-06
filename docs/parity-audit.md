# Iridium ↔ Mori UI Parity Audit (t1)

- Date: 2026-10-06. Auditor: Juniper (read-only; no app-code changes).
- Mori reference: `/Users/priyanshu/git/mori/UI hierarchy.md` (§1–§4)
  + `/Users/priyanshu/git/mori/plan.md` (typography unification, Steps 1–4).
- Iridium base: worktree `juniper-t1`, branch
  `agentcraft/juniper/t1-parity-gap-audit-vs-mori` @ `76aca71`
  (main incl. expressive design-system foundations + settings sliders).
- Iridium scope: `feature/library`, `feature/reader`, `feature/detail`,
  `feature/history` (vs Mori `feature/stats`), `feature/onboarding`,
  `feature/settings`.
- Verdicts: `MATCH` = present with same intent · `GAP` = missing in Iridium ·
  `ADAPT` = present-but-diverged for EPUB, or must be adapted (not cloned)
  because comics (fixed pages) ≠ EPUB (reflowable chapters).
- Rule of thumb: adopt Mori **chrome** (collapsing bars, floating islands,
  docks, bento, motion/haptics discipline); keep Iridium **semantics**
  (books/authors/TOC/highlights/% progress vs comics/pages/shelves).

## 0. Design-system baseline (context, not scored)

| Item | Verdict | Note |
|---|---|---|
| Expressive DS module (`IridiumTheme/Type/Motion/Haptics/SliderRow/...`) | MATCH | Mirrors Mori `core:designsystem` (`MoriCollapsingTopBar`, `MoriSectionCard`, `MoriSettingRow/Switch`, `MoriChoiceGroup`, `MoriSheet/Dialog`, `MoriMotion/Haptic`). |
| Typography tiers L1–L4 + Hero data/label (plan.md) | ADAPT | Foundations merged; per-screen application (28sp collapsing titles, `titleLarge` section headers, `headlineSmall` dialogs, `displayFlex` hero numerals) still needs verification screen-by-screen (t2–t4). |
| Settings sliders (`IridiumSliderRow`: font size / margins / line height) | ADAPT | EPUB-typography equivalent of Mori image-pipeline sliders (brightness/contrast/nightTint). Correct divergence; keep. |

## 1. Library (Mori §3.1 ↔ Iridium `feature/library`)

Mori impl: `LibraryScreen.kt` (`LibraryTopBar`, `LibraryContent`,
`comicItems`, `ShelfSectionHeader`, `LibraryEmptyState`), `ComicCard.kt`
(4 variants), `LibrarySortFilterSheet.kt`, `LibraryMenuSheet.kt`,
`NowReadingHeroCard.kt`, `LibraryQuickFilters.kt`.
Iridium impl: `LibraryScreen.kt` (`LibraryTopBar`, `LibraryContent`,
`ContinueShelf`, `ContentHitRow`), `BookCard.kt` (single), `LibrarySortFilterSheet.kt`.

| # | Mori element | Verdict | Iridium state · Adapt-vs-clone |
|---|---|---|---|
| L1 | Collapsing top bar, 28sp L1 title, filter/sort actions | GAP | Mori `LibraryTopBar` = `MoriCollapsingTopBar` + `enterAlwaysScrollBehavior` + tonal Tune button + active-dot. Iridium `LibraryTopBar` = plain `TopAppBar(headlineSmall)` + Search-toggle + Tune buttons; no `scrollBehavior`/`nestedScroll`. t2: adopt collapsing bar, keep EPUB actions. |
| L2 | Dynamic Search Island (persistent floating pill, clear, filter badges) | GAP | Mori: always-visible floating capsule (`Surface` + `TextField`, animated float color/elevation, leading Search / trailing clear). Iridium: `AnimatedVisibility(searchOpen)` + `OutlinedTextField` toggled from top bar, `FocusRequester` keyboard mgmt; no float, no clear icon, no badges. t2: build search island; badges optional. |
| L3 | Segmented quick-filters (All, In Progress, Unread, Favorites [+Finished]) | GAP | Mori `LibraryQuickFilters`: horizontal `FilterChip` row (`ALL/IN_PROGRESS/UNREAD/FAVORITES/FINISHED`) as first grid item, one-tap. Iridium: same enum (`LibraryFilter`) exists but lives only inside `LibrarySortFilterSheet` `FlowRow`; no in-grid row. t2: add chip row (clone pattern, EPUB labels identical). |
| L4 | Comic/book grid: asymmetric covers, progress pills, bookmarks | ADAPT | Mori `comicItems` → 4 display modes (`ComicCard`/`Comfortable`/`CoverOnly`/`ListRow`), adaptive 128–160dp cells, `MoriScrimPill(pages_left)` + bookmark scrim + `MoriProgressBar` + spine brush + error pill + `combinedClickable` resume/menu + shared-element cover. Iridium: single `BookCard` (2:3 + `BookCoverArt` + title/author + `LinearProgressIndicator` when started), adaptive 128dp, `combinedClickable` read/details + `sharedCover`. Keep EPUB card (author + % replaces pages-left); adopt scrim-pill bookmark, error pill, display-mode variants in t2. |
| L5 | Shelves carousel / shelf sections + picker | GAP | Mori `ShelfSection` + `ShelfSectionHeader` (title + count pill + spring chevron + collapse) + sheet `MoriFilterPills(All + shelves)` + `SelectCollection/ToggleShelfCollapsed`. Iridium has no shelves model; analogues are different features: `ContinueShelf` (in-progress horizontal row) and `ContentHitRow` (full-text hits). EPUB decision for t2: shelves = collections feature (new), not a rename of Continue row. |
| L6 | Floating Action Island / persistent bottom dock | GAP | Neither library file implements the §3 island (Mori reserves `FloatingChromeBottomReserve` for shell chrome). Iridium instead has a non-parity FAB speed-dial (`ImportFolder/Rescan/IndexLibrary`). t2: follow shell-level decision; do not treat FAB menu as parity. |
| L7 | Long-press menu sheet (Resume/Details/Bookmark/Delete) | GAP | Mori `LibraryMenuSheet` + `MoriAlertDialog` delete. Iridium: no menu sheet; long-press routes to details. Clone pattern in t2 if desired. |
| L8 | Now-reading hero vs Continue shelf | ADAPT | Mori `NowReadingHeroCard` (single resume card w/ % + progress bar + Resume/Details) when filter=ALL. Iridium `ContinueShelf` (multi-card horizontal row). Both valid; keep Iridium form, restyle to hero/bento tokens in t2. |
| L9 | Sort/filter sheet scope | ADAPT | Mori: shelf + sort + displayMode + columns + hideErrors (`MoriSheet` + `MoriChoiceGroup` + `MoriSettingSwitch`). Iridium: sort + filter + hideErrors (`ModalBottomSheet` + `FilterChip FlowRow` + `Switch`). Keep EPUB scope; adopt expressive sheet chrome + L3 group headings in t2. |

## 2. Detail (Mori §3.2 ↔ Iridium `feature/detail`)

Mori: `DetailScreen.kt` (`DetailRoute/Screen/TopActions/Content`,
`MetadataRows`, `MoriComicErrorCard`, `RemoveDialog`, `ShelvesDialog`).
Iridium: `DetailScreen.kt` (`detailScreen/DetailRoute/DetailScreen`,
`DetailHero`, `AnnotationRow`).

| # | Mori element | Verdict | Iridium state · Adapt-vs-clone |
|---|---|---|---|
| D1 | Hero cover-art header (backdrop + parallax) | ADAPT | Both: 120dp 2:3 hero row + shared-element cover + `headlineSmall` title. Mori shows `series`; Iridium shows `author` + `% read` — correct EPUB adaptation, keep. Neither implements true parallax backdrop; optional t-work. |
| D2 | Metadata card (format badges, page count, file size, reading state) | GAP | Mori inline meta (`pageCount`, `format.name`, `sourceDisplayName`, position `lastPageIndex+1/pageCount`, `MoriProgressBar`). Iridium: author + `%` + `LinearProgressIndicator` only; no `MetadataRows` equivalent, no format/page/file rows. EPUB adaptation: show chapters count / file name / updated time, not comic `format`. |
| D3 | Action island (Resume split button + bookmark toggle) | ADAPT | Mori: full-width `MoriPrimaryButton` Resume/Start + `MoriTonalButton` Shelves; bookmark lives in top bar + overflow (Share/Refresh/Remove). Iridium: `Button` Resume/Start in hero + top-bar bookmark toggle + direct delete button; no overflow, no shelves/share/refresh. Keep EPUB actions; adopt island styling (split button) in t-work, not comic shelves. |
| D4 | Chapter / issue navigator with completion indicators | ADAPT | Mori has no chapter list (single-comic resume) — spec item unimplemented in Mori itself. Iridium correctly exceeds it: `LazyColumn` TOC (`tocHeader` + `items(toc, key=href)` + dividers) plus EPUB-only bookmarks/highlights (`AnnotationRow` + delete). GAP within: no per-chapter completion indicators — add if cheap. |
| D5 | States/dialogs/chrome (`Loading/Ready/Missing`, refresh, shelves, share, snackbars) | GAP | Mori: `Loading/Ready/Missing` + `MoriLoading/EmptyState` + `LargeFlexibleTopAppBar` + `SnackbarHost` (rescan/remove/shelf/share errors) + `RemoveDialog` + `ShelvesDialog` (member toggles + create). Iridium: `Loading/Success/Gone` + `IridiumLoading` + plain `TopAppBar` + `AlertDialog` remove only; no Missing empty-state, no refreshing flag, no shelves/share/snackbar bus. Adopt Missing/snackbar discipline; shelves = collections feature. |

## 3. Reader (Mori §3.3 ↔ Iridium `feature/reader`)

Core divergence (do not clone): Mori = fixed-page pager (`HorizontalPager` +
`ZoomablePage`, integer pages, archive/viewer-page mapping). Iridium =
reflowable Readium WebView (`NavigatorHost`/`ReaderHostFragment`, float
`progression`, `positionText`, `toc`, highlights). Chrome ideas transfer;
content/navigation models do not.

| # | Mori element | Verdict | Iridium state · Adapt-vs-clone |
|---|---|---|---|
| R1 | Immersive canvas + chrome shell (overlay, auto-hide 5s, settle delay, transient bars) | ADAPT | Mori: fullscreen `Box` pager under overlay chrome, `WindowInsetsControllerCompat` transient bars, `CHROME_SETTLE_DELAY_MS`, 5s auto-hide. Iridium: `Scaffold(topBar/bottomBar)` inset around fragment. Keep WebView; adopt auto-hide timer + fullscreen-follows-chrome in t3. |
| R2 | Reader top bar (L1 title, subtitle, bookmark, incognito badge) | ADAPT | Mori `ReaderTopBar` (back + title/subtitle + bookmark + incognito badge, `surfaceContainer@0.95`). Iridium `TopAppBar` (back + title/author + ToC + Highlight + Settings + More menu, `surface@0.92`). Keep EPUB entries (ToC/highlights); consider bookmark affordance; do not port incognito/file-subtitle. |
| R3 | Overlay tap zones (6 `ReaderNavMode` box partitions + invert + RTL + rhythm/double-tap pairing) | GAP | Mori `ReaderZone.zoneForTap` + `TapZoneOverlay` + `ZoneTapDetector` (250ms pair window, pan-vs-turn, pinch gate). Iridium: none (content tap toggles chrome; WebView owns links/selection). Comic-only — do not port (would break EPUB links/selection). Intentional GAP. |
| R4 | Floating scrubber island (pill + scrub tooltip bubble + tick haptics + disabled ends + RTL mirror) | ADAPT | Mori `ReaderBottomChrome`: floating `CircleShape surfaceContainerHigh` island, `Prev \| cur/total + Slider + total \| Next`, integer scrub + bubble (`primary` pill, scale-in) + `FrequentTick` + drag blocks auto-hide. Iridium `ReaderBottomBar`: stock `BottomAppBar` (`Prev + Slider/label + Next`), float `progression`, `42% · 12/173` label, no bubble/haptics/disabled-ends/RTL. t3: restyle to floating island + bubble + auto-hide suppression; keep float model (`SeekTo(Float)` ≠ `SeekPage(Int)`). |
| R5 | Persistent mini page-counter pill when chrome hidden | MATCH | Mori `MoriScrimPill("12 / 173")` ≡ Iridium `IridiumScrimPill(positionText)`, both gated (`showPageCounter`, hidden with sheets/chrome). Keep. |
| R6 | Floating action dock (Direction \| Fit \| Crop \| Overview \| Settings) | GAP | Mori segmented dock (480dp, dividers, crop tint). Iridium: no dock; actions split across top/bottom bars. Comic segments do not transfer — EPUB dock (if built in t3) should be Flow \| Theme \| ToC \| Highlights \| Settings. Do not clone Direction/Fit/Crop. |
| R7 | Settings sheet: direction / fit / crop / tap-zone cards / invert + advanced nav | ADAPT | Mori `ReaderSettingsSheet` (LTR/RTL, Width/Height/Original, crop switch, 6 `NavModeChoiceCards` + invert H/V/BOTH + preview, volume keys + invert, keep-on, incognito, page counter, swipe-to-turn, dual split/invert). Iridium sheet has none of these — correctly, except three headless prefs that should gain rows in t3: Keep Screen On, Volume Keys, Show Page Counter (all exist in `ReaderPreferences`, no UI). Rest (direction/fit/crop/zones/dual/incognito/swipe/display-filter tone/blend) are comic-only — intentional GAPs. |
| R8 | Appearance rows (display-filter post-processing vs EPUB typography) | ADAPT | Mori: tone/blend choice groups + brightness/contrast/nightTint sliders + grayscale/invert + reset. Iridium: theme swatches (Light/Sepia/Grey/Dark/Black) + brightness slider + font-scale stepper + text-align group (Lithium-style). Both cover "appearance" in their medium — keep Iridium set. Adopt Mori commit discipline (`onValueChangeFinished` preview-then-commit) and scrollable, `heading()`-tagged sheet in t3. |
| R9 | Overview sheet (page-thumb grid, animated seek crossfade) | ADAPT | Mori `ReaderOverviewSheet`: `Adaptive 96dp` thumb grid, 3dp selected ring, retry/error, `SeekPage(expandedIndex)` + fade. Iridium `ReaderTocSheet` (href `LazyColumn`) + `ReaderHighlightsSheet` (color dots/notes/delete) + `AddHighlightDialog` — EPUB-correct, no fixed thumbs. Keep; preserve highlights/ToC in any dock redesign. |
| R10 | Volume-key handling, keep-on, brightness override, a11y/test-tags | ADAPT | Interceptor pattern MATCHes; Iridium gates on `volumeKeys && no sheet`, consumes down+up; Mori adds invert + `VolumeKeyOutcome`. Iridium-only brightness override is EPUB-correct (keep). Mori richer a11y (custom actions, `heading()`, test tags) — adopt in t3. |

## 4. Stats (Mori §3.4) vs History (Iridium `feature/history`) — different features

Mori: `StatsScreen.kt` (`TotalsGrid`, `ReadingTimeHeroCard`,
`StaggeredDurationHero`, `SessionsInsightCard`, `RangeSelector`,
`ReadingBarChart`, `StreakCard`, `TopBookRow`), bento over reading sessions.
Iridium: `HistoryScreen.kt` (`HistoryRoute`, `HistoryGroup`, `HistoryRow`) —
recency list over `progress > 0` books grouped by day. No statistics engine.

| # | Mori element | Verdict | Iridium state · Adapt-vs-clone |
|---|---|---|---|
| S1 | Screen header 28sp collapsing | GAP | Mori `MoriCollapsingTopBar(stats_title)`. Iridium plain `TopAppBar("History")`. t4: collapsing header for the new stats surface. |
| S2 | Range selector (Week/Month/Year segmented) | GAP | Mori `RangeSelector` (`MoriChoiceGroup` + test tags). Iridium: none (no `StatsRange`/buckets). t4 builds over history data. |
| S3 | Hero bento grid (total time ticker + pace/avg pill; pages turned; finished volumes) | GAP | Mori `TotalsGrid` → `ReadingTimeHeroCard(primaryContainer)` + `StaggeredDurationHero(AnimatedContent h/m/s, displayFlex)` + `ExpressiveStatCard` pages/finished + `SessionsInsightCard` avg pill + spark. Iridium: none. t4: EPUB metrics (time, pages≈locations, finished books, avg session) — needs a sessions/durations source; history `progress` alone is insufficient. |
| S4 | Expressive activity chart (rounded bars, peak accent, tooltips) | GAP | Mori `ReadingBarChart` (`Canvas` rounded bars, `surfaceContainerHighest` track, `primary` bars, max-normalized). Iridium: only per-row `LinearProgressIndicator`. t4: new chart over aggregated history/sessions. |
| S5 | Streak journey card (flame badge + current/longest) | GAP | Mori `StreakCard` (`surfaceContainerHigh`, Fire badge, `HeroNumber` current/longest). Iridium day labels (`Today/Yesterday/d MMM`) are grouping, not streaks. t4: compute from reading days. |
| S6 | Top books list (ranked cover + time/pages) | ADAPT | Forms MATCH (cover 44–48dp + title + secondary line on tonal card). Semantics differ: Mori ranked by `durationMs/pagesTurned`; Iridium grouped by recency with author + `%`. t4: add ranked-by-time section; EPUB line = author + time (+ % optional). |
| S7 | Empty/loading (`MoriLoading` + zero-sessions empty state) | ADAPT | Mori empty on `totalSessions == 0` (`BarChart` + title/body). Iridium empty on no-progress (`"Books you read will show up here."`). Keep both conditions on the new surface. |

## 5. Settings & About (Mori §3.5 ↔ Iridium `feature/settings`)

Mori (1666 lines): hub `NavHost` (`SettingsHub/Detail`), 6 categories
(APPEARANCE, READER, SHELVES, PRIVACY, STORAGE, ABOUT), `SettingsScaffold`
collapsing bar, `Appearance/Reader/Storage/Shelves/Privacy/AboutSection`,
`RevealRow` stagger, `SchemePickerRow`.
Iridium (252 lines): flat `Column` of `IridiumSectionCard`
(Appearance/Motion/Library/Reading/Storage/About); no hub/nav enum.

| # | Mori element | Verdict | Iridium state · Adapt-vs-clone |
|---|---|---|---|
| T1 | Categories hub + detail nav (icons, chevrons, enter/exit transitions) | GAP | Mori `SettingsCategory` + `HubRow` (72dp, icon + chevron) + nested `NavHost`. Iridium: flat cards, no hub. Scale decision: hub pays off if shelves/privacy/folders land; otherwise keep flat + adopt collapsing scaffold. |
| T2 | Appearance (theme segmented + contrast icons, dynamic color, haptics, AMOLED gate, motion Expressive/Calm, scheme picker) | ADAPT | Shared `SchemePickerRow` MATCHes. Iridium splits Appearance (mode segmented text-only + wallpaper + pure-black gate) and Motion (Expressive/Calm `ButtonGroup` + caption) — reasonable. GAPs: haptics switch, segmented icons. Consider porting. |
| T3 | Reader category (direction/fit/navMode/tapInvert/volume+invert/keep-on/crop/counter/swipe/display-filter/incognito) | ADAPT | Mori set is comic-specific. Iridium `Reading` card is the EPUB adaptation (font/margin/line-height sliders + keep-on + counter + volume-keys) — keep. Do not port comic rows. |
| T4 | Storage (usage hero, clear cache, folder sources add/relink/remove, unreachable-row treatment) + Shelves (groups CRUD) + Privacy (app lock, incognito) | GAP | Mori `StorageSection` (bytes hero, clear-cache button, folder cards, `AddSourceTree`, remove dialog) + `ShelvesSection` (`GroupRow`, create dialog) + `PrivacySection`. Iridium `Storage` card is a static placeholder row; `Library` card covers sort/filter only. Folders/shelves/privacy are feature GAPs, not chrome — track separately from parity chrome. |
| T5 | About hero + attribution + link rows (brand mark, Flex title, tagline, version + dev-avatar pills, GitHub/Issues/Changelog/Privacy/Licenses, 45ms staggered reveal, licenses nav) | GAP | Mori `AboutSection` (hero `surfaceContainerLow` card, `MenuBook` mark, version `Sparkle` pill, creator pill with avatar + handle, 5 `RevealRow`s, privacy dialog). Iridium `About` card: version row + crash-reporting switch only. Biggest About GAP; recommended t-work: hero + version pill + link rows (clone layout, Iridium strings/URLs); crash switch is Iridium-only, keep. |

## 6. Onboarding (plan.md L1b ↔ both `feature/onboarding`)

Both: `OnboardingRoute/Screen`, `WelcomeContent`, `FolderOptions`,
`WizardStep`, `AppearanceOptions`, `MorphingHero`; 3 steps
(Welcome/Folder 0-2/Appearance 1-2), `AnimatedContent(FADE_THROUGH)`, SAF
`OpenDocumentTree`, `Mori/IridiumProgressBar`, `SchemePickerRow`.

| # | Element | Verdict | Note |
|---|---|---|---|
| O1 | Wizard chrome (back / `STEP x OF n` / Skip + segmented progress + L1b hero title + body + bottom CTA sheet) | MATCH | Structures identical; Iridium title uses `IridiumEmphasized.displaySmall` italic-primary accent vs Mori `displaySoft` Black — both satisfy L1b intent. Keep per-brand type. |
| O2 | Welcome collage + `MorphingHero` + GetStarted; Folder (pick + hint + note); Appearance (mode + scheme + AMOLED) | MATCH | Same copy keys, geometry, delays. Deltas are DS-internal (ungated stagger vs `LocalExpressiveMotionEnabled` gating, text-only segmented vs icon `MoriChoiceGroup`, no test tags in Iridium). Adopt calm-gating + test tags opportunistically. |

## Adapt-vs-clone rules (EPUB ≠ comic)

1. Clone the chrome, not the content model: collapsing 28sp bars, search
   island, quick-filter chips, floating scrubber island + bubble, segmented
   dock shape, bento hero cards + chart + streak, About hero + link rows,
   motion/haptic/test-tag discipline.
2. Adapt the semantics: chapters/TOC/highlights/bookmarks/author/%/locations
   (EPUB) instead of pages/archive-index/fit/crop/tap-zones/dual-page/format
   (comic). Items marked ADAPT above name the exact substitution.
3. Intentional GAPs (do not "fix"): tap zones + overlay + invert, page fit,
   crop, dual-page, swipe-to-turn, incognito-as-comic, display-filter
   tone/blend, page-thumb overview grid, direction toggle. EPUB equivalents
   already exist (flow/theme/font/align/ToC/highlights/brightness-override)
   or are inapplicable.
4. Data GAP behind Stats: history `progress`-by-day cannot power time/streak/
   top-books bento alone — t4 needs a sessions/durations source (or defined
   fallback) before cloning the bento visuals.

## Suggested build order (for t2–t4; no code changed in t1)

- t2 Library: L1 collapsing bar → L2 search island → L3 quick-filters →
  L4 card scrims/error pill → L9 sheet chrome → L5 shelves (feature) →
  L7 menu sheet; hero restyle alongside.
- t3 Reader: R4 scrubber island + bubble → R7 headless-pref rows
  (keep-on/volume/counter) → R2 top-bar polish → R8 sheet scroll/tags →
  R6 EPUB dock (Flow/Theme/ToC/Highlights/Settings) → R10 invert/a11y.
- t4 Stats-over-history: S1 header → S2 range → S3 hero bento (needs
  durations source) → S4 chart → S5 streak → S6 ranked section → S7 empty
  states; About T5 can ride with any of t2–t4 or standalone.
