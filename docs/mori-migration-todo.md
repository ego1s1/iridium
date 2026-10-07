# Mori → Iridium UI/UX Migration — TODO

Source: 10-agent audit (../mori). Rule: clone Mori *chrome*, adapt *semantics* to books.
Reader engine, page-count semantics, AppCompat theme parent stay untouched.
Pins: BOM 2025.06.00, AGP 8.7.3, M3E 1.4.0-alpha15, Kotlin 2.1.21, Hilt 2.57.2.
Test device: virtual device only (Pixel 10a NOT used). Screenshot-verify every wave.

## Wave 0 — Decisions (from user, locked)
- [x] Avatar photo in About: REUSE photo + GitHub mark
- [x] Now-Reading hero (replace Continue shelf)
- [x] Action island (replace FAB menu)
- [x] Keep History tab (stats bento layers into it)
- [x] Shelves/collections: OUT of scope (derived shelves only)
- [x] App lock / splash / share-a-book: DEFERRED
- [x] Transition feel: MORI (full-slide + parallax + opaque pop-exit)

## Wave 1 — Foundations (in progress)
- [x] Tokens: 11 color presets (Iridium + Ocean/Forest/Sunset/Catppuccin/Nord/Gruvbox/Dracula/Tokyo Night/Everforest/Monochrome), surface ramps, containerFor, AMOLED, LocalAmoled, darkTheme default, hapticsEnabled param
- [x] ContentWell (840.dp, breakpoints, 112.dp reserve)
- [x] ScreenTitleLineHeight 28.sp; OFL license text
- [x] Motion specs: accordion, tab 450/300, reader 180/150, zoom/fling, cover-morph, EaseQuads
- [x] IridiumEnter: calm gate, SEARCH kind, FAB 0.6, FADE_THROUGH scale
- [x] Screen transitions: 4-way + wizard pair + opaque pop-exit; expressive gate on sharedCover
- [x] hapticsEnabled pref → theme → LocalHapticsEnabled
- [x] Docs: Iridium UI hierarchy + token/motion/haptic tables + changelog guide
- [x] App-shell quick wins: SystemBarGlyphs, reader fade wire-up, wizard transitions, licenses screen, tab strings, nav tooltips+haptics, inset latch
- [x] Components: 6 buttons, dialogs, error card, expressive card, section header, collapsing top bar, choice group/filter pills, wavy loading, progress bar
- [x] Component adapts: sheet shape/color/ColumnScope, section card params, switch, setting row, empty state, cover art, scrim pill, icons union
- [x] Test: detekt + unit tests + emulator screenshots

## Wave 2 — Screens
- [x] Library: wire collapsing bar/dot, search island + float anim + 76dp reserve + scroll reset, quick chips (shelf carousel skipped: duplicates chips)
- [x] Library: FAB menu replaced by action island dock
- [x] Library: 4 card variants (author/% subtitles, bookmark badge, no pages-left)
- [x] Library: display modes + columns + sheet restyle; snackbar test tag; quick-filter/empty strings
- [x] Library: Now-Reading hero (replaced ContinueShelf)
- [x] Library: long-press menu sheet (wired: `menuBookId` → `LibraryMenuSheet` with delete confirm; `combinedClickable` + LongPress haptic on all card variants)
- [x] Detail: collapsing header + haptics, content well, full-width Resume/Start CTA, Format/File metadata rows, plain chapter TOC (index-qualified keys — shared-href crash fix), bookmarks/highlights with delete, remove confirm dialog (overflow menu deferred with share; no per-chapter completion marks)
- [x] Reader: island + EPUB dock wired (replaced BottomAppBar); tap-invert enum + overlay built; autohide/epoch/scrub guards; locator-change hide; volume keys + invert; counter offset; sheet rows; dictionary popup (Wiktionary) + INTERNET permission. Deferred: floating toolbar wiring (approved snippet pending), TapZoneOverlay wiring.
- [x] History/Stats: bento wired (collapsing header, range w/ haptics, animated hero, chart, streak, ranked) over recency list. Sessions/durations source built (ReadingSessionStore, in-memory; DataStore impl needs one build-file line).
- [x] Settings/About: redesign landed (About hero + links + licenses, haptics switch, icon segments, 43 strings, avatar reuse). Deferred: collapsing scaffold, hub, storage manager UI (API built), folders CRUD.
- [x] Strings: library/detail/reader/settings bulk-ported; reader sheets converted
- [x] Test: emulator screenshots per screen (library/detail/reader/settings/licenses/dictionary/history verified on virtual device, Pixel 10a untouched).

## Wave 3 — Polish + features
- [x] Motion polish landed (calm gates, enter kinds, transitions, shared-cover gate); EPUB dock wired; toolbar a11y pass done, wiring deferred.
- [x] Sessions source built (interface + in-memory impl + tests); bento still on estimates until VM integration lands.
- [ ] Settings hub; storage manager UI; folders CRUD (Wave 3 remainder — scoped in analysis report).
- [x] Onboarding tags/gating landed. Remaining: backup/restore (specced), multi-select (designed) — see analysis reports.
- [ ] Final full-pass screenshots + detekt + unit tests

## Storage revamp (all-files access) — DONE
- [x] MANAGE_EXTERNAL_STORAGE permission + onboarding Access step (grant detection, settings deep-link)
- [x] Filesystem EPUB walk (skips Android/, dot-dirs; fails safe, never prunes on partial walk) + prune-all-missing migration
- [x] ReadiumOpener resolves file:// + plain paths (stable row ids, progress/bookmarks preserved)
- [x] Onboarding: Welcome -> Access -> Reading (text size, line spacing, book colors) -> Appearance; folder step removed
- [x] Library: SAF launcher/linking removed; access-gated empty states + grant CTA; resume re-scan on grant; rescan/index island
- [x] Reader sheet: line-height slider (1.0-2.5x) + clamped actions
- [x] DataStore sourceTreeUri removed (interface/impl/fakes/tests); documentfile dependency removed
- [x] 8 new tests (filesystem walk, onboarding advance/reading persistence, scan triggers)
