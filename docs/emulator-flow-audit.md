# Emulator flow audit — 2026-10-11 (emulator-5554, API 35)

Build under test: `app-debug.apk` from `1ad39a6` (AGP 8.13.2 / Gradle 8.13 /
Kotlin 2.2.21), installed with `install -r` over the previous build (SAF
grants + DataStore preserved, so the library already contained books).

Method: scripted `adb` driving — `am start`, `input tap`/`swipe`/`text`,
`uiautomator dump` for hierarchy assertions, `screencap` for visual review,
`logcat` scanned for `FATAL EXCEPTION`. (AppleScript cannot drive an Android
emulator; `adb` + uiautomator is the scriptable equivalent. No VM was spun
up — the existing `mori` AVD was reused.)

## Unit tests

`./gradlew testDebugUnitTest`: **267 tests, 0 failures, 0 errors.**
`logcat` across the whole session: **0 FATALs.**

## Flows verified (all PASS)

| # | Flow | Result |
|---|------|--------|
| 1 | Cold launch (`am start`) | No crash, library renders with books |
| 2 | Bottom-nav Library → History → Settings | All three tabs render; Settings shows Appearance/Haptics/Motion rows; History shows empty states |
| 3 | Tap book card → reader | Instant open, no blackscreen; position `1 / 317` |
| 4 | Tap right third → page forward | Real book text renders (license page, then preface) |
| 5 | Tap center → chrome toggle | Top bar + scrubber island + EPUB dock appear; auto-hide after ~3 s |
| 6 | Back arrow → library | Returns; **Resume button appears** (progress persisted) |
| 7 | Long-press card → quick-actions sheet | Resume / Add to favorites / Delete; **no Details row** |
| 8 | Search `gatsby` | Grid filters to the matching title only |
| 9 | Search `odyssey` | `In books (6)` plural header + chapter hits with snippets (FTS end-to-end works) |
| 10 | Dock theme button → Reading theme sheet | Opens; Light / Sepia✓ / Grey / Dark / Pitch black cards with miniature page previews |
| 11 | Select Grey / Light / Sepia in sheet | Applies immediately with checkmark; sheet stays open for comparison; page re-renders in the new theme |
| 12 | Theme sheet overflow | Scrolls; Pitch black reachable (not clipped) |

## Bugs found

### B1 (medium): reader page letterboxed by visibility-ignoring insets
`NavigatorHost` pads with `statusBarsIgnoringVisibility` /
`navigationBarsIgnoringVisibility`, which are non-zero even when the system
bars are hidden. In immersive mode (chrome hidden) this leaves a permanent
dark strip (~140 px) above the page and a black band below the counter —
the window background shows where the WebView never draws. On API 35 this
is more visible because enforced edge-to-edge ignores the legacy
`statusBarColor` tint. The stable insets are deliberate for the *chrome*
bars (no animation jump); the *content host* should use visibility-aware
insets instead.
File: `feature/reader/impl/.../ReaderScreen.kt:270-278`.

### B2 (minor): bookmark row subtitle is static
Menu sheet shows `Add to favorites` with subtitle `Bookmarked` even when
the book is not bookmarked. The subtitle should reflect state (or be
dropped for the add case).
File: `feature/library/impl/.../LibraryMenuSheet.kt`, `library_card_bookmarked`.

## Non-issues confirmed during the pass

- Disabled scrubber prev-button at position 1 renders ghosted (alpha 0.4) —
  intended, not clipping.
- Page-counter pill briefly overlaps the dock only mid chrome-toggle
  transition; steady states are exclusive (`!chromeVisible` gate).
- Scripted dock taps kept missing: chrome auto-hides 3 s after showing,
  and x=349 px sits ~at the PREV/MENU third boundary (0.323 vs 0.333), so
  the tap paged instead of hitting the button. Harness flakiness, not an
  app bug. Exact dock bounds from hierarchy dump: theme `[286,2171][412,2297]`.
- Menu-sheet `Bookmarked` subtitle and `0% read` (position 13/317 rounds to
  0%) are consistent with current formatters.

## Not covered (needs manual or SAF-grant harness)

- Fresh-install onboarding → SAF folder linking (system picker not
  scriptable via adb); revoked-folder Settings row never exercised live.
- Dictionary popup (needs text selection inside the WebView).
- Volume-key paging, night light, highlight add/delete, TOC navigation.
- Release (R8-minified) build on device — only debug was installed here.
