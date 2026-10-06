# Device test — real-EPUB reader retry (pg64317-images-3.epub)

Date: 2026-10-07. Tester: AgentCraft Kit (task t24, retry of t23). No app-code changes (docs + screenshots only).

## Environment

| Item | Value |
|---|---|
| Host | macOS arm64, `JAVA_HOME=/opt/homebrew/opt/openjdk@17`, `ANDROID_HOME=$HOME/Library/Android/sdk` |
| Emulator binary | Android emulator 37.1.11.0 (build 15917651) |
| AVD | `mori` (`~/.android/avd/mori.avd`), arm64-v8a, 1080x2400 @420dpi, 1536MB RAM |
| System image | `system-images/android-35/google_apis/arm64-v8a`, Android 15 (SDK 35) |
| Boot | fresh `adb reboot`, `sys.boot_completed=1` before install |
| Iridium APK | `:app:assembleDebug` built from this worktree, `com.iridium.reader` v0.1.67 (v67), 39MB |
| Test book | `pg64317-images-3.epub` (353457 bytes, md5 `72be9296a409488d1791e987c545ff21` host == device), pushed to `/sdcard/Books/` |
| Linked folder | persisted SAF grant reused (`/sdcard/Books`); rescan-on-launch picks up the book |

## Crash repro: YES (same bug as t21, now in two screens)

Three FATALs this session, all identical:

```text
java.lang.IllegalArgumentException: Key "OEBPS/716005862216510784_64317-h-0.htm.xhtml" was already used.
If you are using LazyColumn/Row please make sure you provide a unique key for each item.
```

- PID 3151 — tap book card → detail (see `real-epub-reader-retry-02-crash-log.txt`).
- PID 3809 — tap Resume FAB → detail (same stack; the FAB routes through `navigateToDetail`, so it is NOT a bypass).
- PID 4465 — tap reader top-bar ToC icon → reader ToC sheet (see `real-epub-reader-retry-08-toc-crash-log.txt`).

Root cause (unchanged from t21, still not fixed per goal rules): the 12 NCX/nav ToC entries
collapse to 3 duplicate base hrefs, and both `DetailScreen.kt:181` (`items(state.toc, key = { it.href })`)
and `ReaderSheets.kt:62` (`items(toc, key = { it.href })`) use the bare href as the LazyColumn key.

## Search-hit bypass: WORKS

`LibraryScreen.kt:383` content-hit taps call `onOpenChapter(bookId, href)` →
`navigateToReader(bookId, href)`, which never composes the detail screen. Steps used:
`Library actions → Index for search`, then search `gatsby` → `In books (3)` hits → tap first hit.
The reader opened directly at position 2 with zero crashes. The `href` argument only selects the
base resource (matched by file name in `locatorForHref`), so the landing point is the resource start.

## Flow results

| # | Flow | Result | Evidence |
|---|---|---|---|
| 1 | Fresh boot + `:app:assembleDebug` + `adb install -r` (v0.1.67) | PASS | BUILD_SUCCESS, `Success`, md5 match host==device |
| 2 | `adb push` EPUB → `/sdcard/Books/`, launch → rescan shows Gatsby card (title/author/cover) | PASS | `real-epub-reader-retry-01-library.*` — cover art renders, dump has `The Great Gatsby` / `F. Scott Fitzgerald` |
| 3 | Tap card → book detail | **FAIL — crash** | PID 3151 FATAL (same duplicate-key); `…-02-detail.*` + `…-02-crash-log.txt` |
| 4 | Resume FAB → reader (hoped-for bypass) | **FAIL — crash** | PID 3809, same stack; FAB routes to detail (`IridiumApp.kt:86`) |
| 5 | Search-hit bypass → reader opens directly | PASS | `…-03-reader.*` — PG header renders, pill `2 / 117`, PID alive, no FATAL |
| 6 | Reader render incl. links/typography (header, ToC page, ch. I) | PASS | `…-03`, `…-06`, `…-09` shots — serif body, blue Gutenberg links, dedication verse |
| 7 | Reader paging (6× swipe-left; viewport header → ch. I start → ch. I body) | PASS w/ note | `…-04-paging.*`, `…-09-progress.*`; pill frozen at `2 / 117` (see note) |
| 8 | Reader chrome toggle (top bar + bottom scrubber `2% · 2 / 117`) | PASS | `…-06-chrome.*` — Back/title/author/icons + prev/slider/next in dump + shot |
| 9 | Reader settings sheet (Flow/Brightness/Theme/Text size/Text align) | PASS | `…-07-settings.*` — Auto/Paged/Scrolled, slider, 5 theme swatches, 100% stepper |
| 10 | Reader ToC/Contents sheet open | **FAIL — crash** | PID 4465 FATAL (same duplicate-key, `ReaderSheets.kt:62`); `…-08-toc.*` + `…-08-toc-crash-log.txt`; launcher shot in `…-08-toc-launcher.xml` |
| 11 | ToC chapter jump | **FAIL — blocked** | sheet never composes, nothing to tap |
| 12 | Progress persist + resume + History (`Today`, Gatsby `2% read` + cover; Resume FAB present) | PASS | `…-10-library-after.*`, `…-11-history.*` — Back-out clean, no FATAL |

## Notes / observations

- **Position pill frozen at `2 / 117` across ~6 forward swipes** while the viewport visibly advanced
  (header → epigraph/ch. I → ch. I body). Suspect `positionText()` in `ReaderViewModel.kt:373`:
  `positions.indexOfFirst { it.href == current.href }` always matches the first position sharing this
  resource's base href (the whole of chapters I–V live in one 158KB file), so the counter never leaves 2.
  Cosmetic, same-href family as the crash bug; the persisted progress (2%) agrees with the pill.
- **One center-tap appeared to jump backward** (ch. V region → ch. I start); most likely a tap-vs-chrome
  timing artifact, not data loss — subsequent paging forward was consistent. Recorded, not a failure.
- **`adb shell input text` needs a pre-tap on the search field** on this AVD; without focus the text is silently dropped.
- Bottom-nav taps near the gesture bar once summoned Circle-to-Search ("Just a moment…"); retrying the same
  coordinates reached the app. Device quirk, not an app bug.
- No other crashes: library scan, `Index for search`, content search, reader render/paging/chrome/settings,
  Back-out, and History all run clean around the duplicate-ToC-key bug.

## Evidence paths (all in-repo, `docs/device-test/`)

- `real-epub-reader-retry-01-library.png/xml` — Gatsby card + cover after fresh boot/install/push
- `real-epub-reader-retry-02-detail.png/xml` — post-crash state; `…-02-crash-log.txt` — PID 3151 FATAL
- `real-epub-reader-retry-03-reader.png/xml` — bypass landing (PG header, `2 / 117`)
- `real-epub-reader-retry-04-paging.png/xml` — after swipe (ch. II region)
- `real-epub-reader-retry-05-chrome.png/xml` — chrome auto-hidden state
- `real-epub-reader-retry-06-chrome.png/xml` — chrome visible (top bar + `2% · 2 / 117` scrubber)
- `real-epub-reader-retry-07-settings.png/xml` — settings sheet controls
- `real-epub-reader-retry-08-toc.png/xml` — pre-crash chrome frame + reader dump; `…-08-toc-crash-log.txt` — PID 4465 FATAL; `…-08-toc-launcher.xml` — post-crash launcher dump
- `real-epub-reader-retry-09-progress.png/xml` — ch. I viewport after 4 more swipes
- `real-epub-reader-retry-10-library-after.png/xml` — clean Back-out to library
- `real-epub-reader-retry-11-history.png/xml` — History (`Today` Gatsby `2% read`, `Yesterday` smoke book 66%)

## Verdict

Reader render/paging/chrome/settings/progress/history: **PASS** via the search-hit bypass. Library→detail and
reader ToC: **FAIL** — crash repro **YES**, same duplicate-ToC-href-key bug in two screens (detail + reader sheet),
plus a frozen position pill on this multi-chapter-per-file book. Fix (out of scope here): composite/deduped
LazyColumn keys as already done in `LibraryScreen.kt:378`.
