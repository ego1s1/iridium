# Device / manual test report — Iridium

Source: `docs/device-test-report.md` (2026-10-06, AgentCraft Kit task t5).
Scope: consolidation only — no new emulator run, no app-code changes.

## Environment (from source report)

| Item | Value |
|---|---|
| Host | macOS arm64, `openjdk@17`, `ANDROID_HOME=$HOME/Library/Android/sdk` |
| Emulator | Android emulator 37.1.11.0 (build 15917651) |
| AVD | `mori` (`~/.android/avd/mori.avd`), arm64-v8a, 1080x2400 @420dpi, 1536MB RAM |
| System image | `system-images/android-35/google_apis/arm64-v8a`, Android 15 (AE3A.240806.043, SDK 35) |
| Boot flags | `-no-window -no-audio -no-boot-anim -gpu swiftshader_indirect -no-snapshot` |
| Iridium APK | `:app:assembleDebug`, `com.iridium.reader` v0.1.28 (v28), 41MB |
| Mori reference APK | prebuilt `app-debug.apk`, `com.mori.reader` v0.1.307 (v307) |
| Test book | Generated 3-chapter EPUB (`Smoke Test Book`, 2.2KB) in `/sdcard/Books/` |
| Crashes | None — `dumpsys dropbox` clean, no `AndroidRuntime` FATAL for either app |
| Unit-test note (source) | `./gradlew testDebugUnitTest` → BUILD SUCCESSFUL at time of device run |

## Flow results (all 13 PASS per source)

| # | Flow | Result | Evidence (`docs/device-test/`) |
|---|---|---|---|
| 1 | Fresh install → crash-consent dialog → onboarding welcome | PASS | `iridium-01-onboarding-welcome.png` — consent dialog (Not now/Enable); hero + Get started |
| 2 | Onboarding: Get started → link folder (real SAF picker → Books → Allow) → appearance → library | PASS | `iridium-02-onboarding-folder.png` — system picker showed `Books/smoke-test-book.epub`, grant accepted |
| 3 | Library load: scan linked folder, book card with title/author | PASS | `iridium-03-library-loaded.png` — "Smoke Test Book / AgentCraft Kit", search + filter icons, bottom nav + add FAB |
| 4 | Open book → detail (metadata, Unread, Start reading, 3 chapters) | PASS | `iridium-04-detail-screen.png` |
| 5 | Reader renders chapter, page counter pill | PASS | `iridium-06-reader-chapter.png` — clean serif render, `1 / 3` pill |
| 6 | Reader paging (swipe): 1/3 → 2/3 → 3/3 | PASS | UI dumps confirmed `2 / 3`, `3 / 3` (no separate PNG; chapter render per flow 5) |
| 7 | Reader chrome (tap toggle): top bar + bottom scrubber island | PASS | `iridium-07-reader-chrome.png` — Back/title/actions, `66% · 3 / 3` slider with prev/next |
| 8 | Reader settings sheet (flow, brightness, theme, text size/align) | PASS | `iridium-08-reader-settings-sheet.png` |
| 9 | Table of contents sheet + chapter jump (Ch3 → Ch1) | PASS | `iridium-09-reader-toc.png` — jump landed on `1 / 3` |
| 10 | Progress persist + resume (exit on Ch3 → detail `66% read` + Resume; reopen restores locator) | PASS | `iridium-10-detail-progress.png` — DB verified `progress=0.667`, `lastLocator` Ch3 |
| 11 | History tab records the read (`Today`, 66% bar) | PASS | `iridium-11-history-with-book.png` (after re-read ending on Ch3) |
| 12 | Settings: theme switch to Dark applies; sliders/toggles render | PASS | `iridium-12-settings-appearance.png`, `iridium-13-settings-reading.png` (Appearance/Motion/Library/Reading sections) |
| 13 | Mori APK install + run (library/settings/stats reference shots) | PASS | `mori-01-library.png`, `mori-02-settings.png`, `mori-03-stats.png` |

Supplementary evidence (no flow number, supports flow 5 diagnosis): `iridium-05-reader-malformed-epub.png` — WebView XML-error banner on malformed EPUB.

## Behavioural notes (from source report)

- **Malformed-EPUB resilience (info, not a bug):** first test EPUB had unclosed `<p>` tags; reader rendered a WebView XML-error banner above partial content instead of crashing (`iridium-05-reader-malformed-epub.png`). After pushing a well-formed file + restart (launch rescan), rendering was clean.
- **Progress is position-based:** ending a session on Chapter 1 stores `progress ≈ 0.0`, so detail reads "Unread" and History stays empty until you advance. Verified by re-reading to Ch3 → `66% read`, Resume, History entry. Expected behaviour, but a "read Ch1 only" session is invisible in History.
- **ToC icon needed a second tap** in one attempt; sheet opened on retry with no logcat errors. Suspected tap-timing/chrome-animation, not an app bug.
- Reader chrome auto-hides (immersive); a center tap reliably toggles it.
- `uiautomator dump` sees the Readium WebView as text but no Compose content-descs inside the reader, so chrome taps used screenshot-derived coordinates; key assertions (chapter text, counters) came from dumps.

## Parity gaps vs Mori (from source report, same device)

- **Library:** Mori has centered title, full-width search island, quick-filter chips (All/In progress/Unread/Favorites), shelf carousels with counts (`mori-01-library.png`). Iridium shows left-aligned "My books" header, icon-only search/filter, plain grid — structurally behind Mori (t2 in review at the time).
- **History vs Stats:** Iridium History is a day-grouped list with progress bars (`iridium-11-history-with-book.png`); Mori Stats is a bento (hero totals, 7D/30D/1Y chart, streaks, top books — `mori-03-stats.png`). Known t4 gap (cancelled at the time).
- **Settings IA:** Mori is a hub of sub-screen rows (Appearance, Reader defaults, Shelves, Privacy, Storage, About — `mori-02-settings.png`); Iridium is a single scrolling page of section cards (`iridium-12/13`). Same controls, different IA.
- **Close parity:** floating-pill bottom nav + resume/action circle, reader chrome (title/author top bar, ToC/settings/more, bottom scrubber island), reader settings bottom sheet (segmented Flow, brightness slider, theme cards, text-size stepper).

## Manual / coverage gaps

1. **No automated on-device UI/instrumentation tests:** repo has zero `src/androidTest/` files (verified). Compose UI tests (`createComposeRule`) exist but run as local JVM unit tests under `src/test/` with Robolectric, not on an emulator/device. The 13 device flows above were manual + `uiautomator dump`, not repeatable CI tests.
2. **Release build and signing untested:** device run used `:app:assembleDebug` only. `app/build.gradle.kts` defines a `release` build type with no `signingConfig`, no minify/shrink; no release APK/AAB was built, signed, or smoke-tested.
3. **Single-device coverage only:** one emulator (mori AVD, Android 15, arm64, 1080x2400) — no physical device, no other API levels / screen sizes / foldables / locales / dark-mode-first boot tested.
4. **Reader accessibility and input gaps:** `uiautomator` sees no Compose content-descriptions inside the Readium WebView; TalkBack traversal, font-scale extremes, and keyboard/D-pad navigation were not exercised.
5. **No performance / resource evidence:** cold-start time, library scan time on large collections, memory/battery under long reads, and rotation/process-death restore were not measured or tested.
6. **Edge-case library/import flows untested on device:** malformed-EPUB only incidentally (see note); DRM/large-file imports, permission-deny and folder-unlink paths, and interrupted-copy behaviour have no device evidence.

## Evidence index

`docs/device-test/`: `iridium-01-onboarding-welcome.png`, `iridium-02-onboarding-folder.png`, `iridium-03-library-loaded.png`, `iridium-04-detail-screen.png`, `iridium-05-reader-malformed-epub.png`, `iridium-06-reader-chapter.png`, `iridium-07-reader-chrome.png`, `iridium-08-reader-settings-sheet.png`, `iridium-09-reader-toc.png`, `iridium-10-detail-progress.png`, `iridium-11-history-with-book.png`, `iridium-12-settings-appearance.png`, `iridium-13-settings-reading.png`, `mori-01-library.png`, `mori-02-settings.png`, `mori-03-stats.png` (16 files, all present).
