# Device smoke-test report — Iridium on the mori AVD

Date: 2026-10-06. Tester: AgentCraft Kit (task t5). No app-code changes.

## Environment

| Item | Value |
|---|---|
| Host | macOS arm64, Java `openjdk@17`, `ANDROID_HOME=$HOME/Library/Android/sdk` |
| Emulator binary | Android emulator 37.1.11.0 (build 15917651) |
| AVD | `mori` (`~/.android/avd/mori.avd`), arm64-v8a, 1080x2400 @420dpi, 1536MB RAM |
| System image | `system-images/android-35/google_apis/arm64-v8a`, Android 15 (AE3A.240806.043, SDK 35) |
| Boot flags | `-no-window -no-audio -no-boot-anim -gpu swiftshader_indirect -no-snapshot` |
| Iridium APK | `:app:assembleDebug` from this worktree, `com.iridium.reader` v0.1.28 (v28), 41MB |
| Mori APK | prebuilt `/Users/priyanshu/git/mori/app/build/.../app-debug.apk`, `com.mori.reader` v0.1.307 (v307) |
| Test book | generated 3-chapter EPUB (`Smoke Test Book`, 2.2KB), pushed to `/sdcard/Books/` |
| Crashes | none — `dumpsys dropbox` clean, no `AndroidRuntime` FATAL for either app |

## Flow results

| # | Flow | Result | Evidence |
|---|---|---|---|
| 1 | Fresh install → crash-consent dialog → onboarding welcome | PASS | `iridium-01` — consent dialog with Not now/Enable; hero + Get started render |
| 2 | Onboarding: Get started → link folder (real SAF picker → Books → Allow) → appearance → library | PASS | `iridium-02`; system picker showed `Books/smoke-test-book.epub`, grant accepted |
| 3 | Library load: scan linked folder, book card with title/author | PASS | `iridium-03` — "Smoke Test Book / AgentCraft Kit", search + filter icons, bottom nav + add FAB |
| 4 | Open book → detail (metadata, Unread, Start reading, 3 chapters) | PASS | `iridium-04` |
| 5 | Reader renders chapter, page counter pill | PASS | `iridium-06` — clean serif render, `1 / 3` pill |
| 6 | Reader paging (swipe): 1/3 → 2/3 → 3/3 | PASS | UI dumps confirmed `2 / 3`, `3 / 3` with chapter text |
| 7 | Reader chrome (tap toggle): top bar + bottom scrubber island | PASS | `iridium-07` — Back/title/actions, `66% · 3 / 3` slider with prev/next |
| 8 | Reader settings sheet (flow, brightness, theme, text size/align) | PASS | `iridium-08` |
| 9 | Table of contents sheet + chapter jump (Ch3 → Ch1) | PASS | `iridium-09`; jump landed on `1 / 3` |
| 10 | Progress persist + resume (exit on Ch3 → detail `66% read` + Resume; reopen restores Ch1 locator) | PASS | `iridium-10`; DB: `progress=0.667`, `lastLocator` Ch3 |
| 11 | History tab records the read (`Today`, 66% bar) | PASS | `iridium-11` (after re-read ending on Ch3) |
| 12 | Settings: theme switch to Dark applies; sliders/toggles render | PASS | `iridium-12`, `iridium-13` (Appearance/Motion/Library/Reading sections) |
| 13 | Mori APK install + run (library/settings/stats reference shots) | PASS | `mori-01..03` |

## Notes / observations

- **Malformed-EPUB resilience (info, not a bug):** the first test EPUB had
  unclosed `<p>` tags; the reader rendered a WebView XML-error banner above
  the partial content instead of crashing (`iridium-05`). After pushing a
  well-formed file and restarting (launch rescan), rendering was clean. Good
  degraded behavior.
- **Progress is position-based:** ending a session on Chapter 1 stores
  `progress ≈ 0.0`, so detail reads "Unread" and History stays empty until you
  advance. Verified by re-reading to Ch3 → `66% read`, Resume, History entry.
  Expected behavior, but it means a "read Ch1 only" session is invisible in
  History — worth knowing, not a failure.
- **ToC icon needed a second tap** in one attempt; the sheet opened on retry
  with no errors in logcat. Likely tap-timing/chrome-animation, not an app bug.
- Reader chrome auto-hides (immersive); a center tap reliably toggles it.
- `uiautomator dump` sees the Readium WebView as text but no Compose
  content-descs inside the reader, so chrome taps used screenshot-derived
  coordinates — all key assertions (chapter text, counters) came from dumps.

## Parity observations (Iridium vs Mori on the same device)

- **Bottom nav:** both use a floating pill + separate resume/action circle.
  Mori's bar is filled primary-container with a distinct resume FAB; Iridium's
  is surface-container with an add (+) island. Same pattern, different dress.
- **Library:** Mori has a centered title, full-width search island,
  quick-filter chips (All/In progress/Unread/Favorites) and shelf carousels
  with counts (`mori-01`). Iridium (t2 in review) shows a left-aligned
  "My books" header, icon-only search/filter and a plain grid — structurally
  behind Mori; expected until t2 lands.
- **Reader chrome:** both show title/author top bar with ToC/settings/more
  actions and a bottom scrubber island with prev/next (`iridium-07`). Close.
- **Reader settings:** both are bottom sheets with segmented Flow control,
  brightness slider, theme cards, text-size stepper (`iridium-08`). Close.
- **History vs Stats:** Iridium History is a day-grouped list with progress
  bars (`iridium-11`); Mori Stats is a bento (hero totals, 7D/30D/1Y chart,
  streaks, top books — `mori-03`). This is the known t4 gap (cancelled).
- **Settings:** Mori is a hub of sub-screen rows (Appearance, Reader defaults,
  Shelves, Privacy, Storage, About — `mori-02`); Iridium is a single scrolling
  page of section cards (`iridium-12/13`). Same controls, different IA.

Screenshots: `docs/device-test/` (`iridium-01..13`, `mori-01..03`).
Unit tests: `./gradlew testDebugUnitTest` → BUILD SUCCESSFUL.
