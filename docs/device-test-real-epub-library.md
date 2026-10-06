# Device test — real-EPUB library load (pg64317-images-3.epub)

Date: 2026-10-07. Tester: AgentCraft Juniper (task t21). No app-code changes (docs + screenshots only).

## Environment

| Item | Value |
|---|---|
| Host | macOS arm64, `JAVA_HOME=/opt/homebrew/opt/openjdk@17` (17.0.20.1), `ANDROID_HOME=$HOME/Library/Android/sdk` |
| Emulator binary | Android emulator 37.1.11.0 (build 15917651) |
| AVD | `mori` (`~/.android/avd/mori.avd`), arm64-v8a, 1080x2400 @420dpi, 1536MB RAM |
| System image | `system-images/android-35/google_apis/arm64-v8a`, Android 15 (SDK 35) |
| Boot flags | AVD already booted headless (`sys.boot_completed=1`); `-no-snapshot` per harness |
| Iridium APK | `:app:assembleDebug` built from this worktree, `com.iridium.reader` v0.1.63 (v63), 39MB |
| Test book | `pg64317-images-3.epub` (345KB, Project Gutenberg 64317), pushed to `/sdcard/Books/` |
| Linked folder | persisted SAF grant `content://com.android.externalstorage.documents/tree/primary%3ABooks` (= `/sdcard/Books`) |
| EPUB facts (host-side unzip) | Title `The Great Gatsby`, creator `F. Scott Fitzgerald`; 4 spine items (cover wrapper, pg-header 158KB, chapter 138KB, pg-footer 20KB); NCX 12 navPoints all with `#fragment` hrefs collapsing to 3 base files; cover JPEG 230KB |

## Flow results

| # | Flow | Result | Evidence |
|---|---|---|---|
| 1 | `:app:assembleDebug` from this worktree | PASS | BUILD SUCCESSFUL, 367 tasks (301 executed); `app-debug.apk` 39MB |
| 2 | `adb install -r` (kept data/SAF grant) | PASS | `Success`, ~2.7s; `dumpsys` → v0.1.63 (v63) |
| 3 | `adb push` EPUB → `/sdcard/Books/` | PASS | 353457 bytes, instant |
| 4 | Launch → rescan-on-launch picks up Gatsby | PASS | dump shows `The Great Gatsby` / `F. Scott Fitzgerald`; `real-epub-lib-01` |
| 5 | Library book card: title + author + cover | PASS | shot shows Gatsby card with real cover art (blue eyes/Fitzgerald), dump confirms title+author text |
| 6 | Tap card → book detail (metadata + chapter count) | **FAIL — app crash** | `FATAL EXCEPTION` PID 1992, `real-epub-lib-02-crash.*` |
| 7 | Relaunch → library still loads, no further crash | PASS | PID 2332 alive, Gatsby/Fitzgerald in dump, logcat FATAL count unchanged (1) |
| 8 | Crash triage: `dumpsys dropbox` + logcat | INFO | 1 crash entry (`data_app_crash`); no crash on scan path |

## Bug found (reported, not fixed — out of scope for this goal)

- **Detail screen crashes on this book.** `java.lang.IllegalArgumentException: Key
  "OEBPS/716005862216510784_64317-h-0.htm.xhtml" was already used` (LazyColumn duplicate key).
- Mechanism (static, confirmed): `resolve()` in
  `epub-engine/.../IridiumEpubEngine.kt:189` strips `#fragment`, so the 12 NCX navPoints
  collapse to 3 duplicate base hrefs in `inspected.chapters` → stored ToC →
  `DetailScreen.kt:181` uses `items(state.toc, key = { it.href })` → duplicate keys → FATAL.
- Same hazard exists in the reader ToC sheet (`ReaderSheets.kt:62`, `key = { it.href }`) —
  heads-up for t23 reader flow. `LibraryScreen.kt:378` already uses a composite
  `"${bookId}#${href}"` key (precedent for a fix).
- Follow-up fix tracked separately; this task makes no app-code edits per the goal rules.

## Timings

| Step | Duration |
|---|---|
| `:app:assembleDebug` (cold, first build in fresh worktree + shared-machine contention) | 18m 14s |
| `adb install -r` | ~2.7s |
| `adb push` (345KB) | <1s |
| App start + rescan until Gatsby visible in dump | ~25s (includes process start; images-heavy scan not separately timed) |

## Evidence paths (all in-repo)

- `docs/device-test/real-epub-lib-01-library.png` — "My books" with Gatsby card + cover art
- `docs/device-test/real-epub-lib-01-library.xml` — uiautomator dump (title/author text nodes)
- `docs/device-test/real-epub-lib-02-crash.png` — post-crash screen state
- `docs/device-test/real-epub-lib-02-crash-log.txt` — FATAL stack excerpt (duplicate-key `IllegalArgumentException`)

## Notes

- The pre-installed v0.1.28 build on the emulator showed stale library rows (Cyberpunk/Ghost, files
  no longer on device) and no working rescan affordance; the fresh v0.1.63 build's rescan-on-launch
  (`LibraryViewModel` init `scan()`) picked up `/sdcard/Books` contents without needing the SAF picker.
- Emulator is shared: `com.mori.reader` was foregrounded by another session mid-run; Iridium
  verification steps are unaffected (fresh `am start` per pass), but device passes should stay
  serialized per the goal plan.
- `docs/device-test/real-epub-lib-*` naming reserves `real-epub-reader-*` for t23 (Kit).

## Verdict

Library load: **PASS** (APK installs, book card with title/author/cover in dump + shot).
Detail + no-crash: **FAIL** due to the duplicate-ToC-key crash above — ACCEPT criteria partially met;
detail verification is blocked on the fix.
