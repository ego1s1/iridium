# Push preview audit: origin/main..main

- Date: 2026-10-06
- Range: `origin/main..main` (read-only inspection, no push performed)
- origin/main tip: `9ea0514` — release: correct the keystore fallback's real limitation
- main tip: `a9f1dc5` — Merge agentcraft/kit/t5-smoke-test-iridium into main
- Ahead count: **14 commits**
- Commands used:
  - `git log origin/main..main --oneline`
  - `git diff --stat origin/main..main`
  - new-file list via `git diff --name-only --diff-filter=A origin/main..main` + `wc -c` sizes

## Commit table (14 ahead commits, chronological)

| # | Short SHA | Full SHA | Date | Author | Subject |
|---|-----------|----------|------|--------|---------|
| 1 | c07765f | c07765fdc91ea9c3383494c9b5d425bd32a8e01a | 2026-10-06 | AgentCraft Kit | t1: Expressive Design System Foundations |
| 2 | e32b0e4 | e32b0e49368f6d98094b0e43fb59ee597154d63a | 2026-10-06 | Priyanshu Sharma | Merge agentcraft/kit/t1-expressive-design-system into main |
| 3 | 2325662 | 232566243cee97220e70fb9f9036ef7dab71d83d | 2026-10-06 | AgentCraft Kit | t4: Settings Expressive Sliders & Modular Sections |
| 4 | 76aca71 | 76aca71748db81ecc240035ab88541ffc7ba1135 | 2026-10-06 | Priyanshu Sharma | Merge agentcraft/kit/t4-settings-expressive into main |
| 5 | 1ab8e4f | 1ab8e4fc73167ed9ab117d7ee88684fe396e39fa | 2026-10-06 | AgentCraft Juniper | t1: Parity-gap audit vs Mori UI hierarchy |
| 6 | a93767f | a93767f9be58b7f7156cc37feadc8cc32b9c9f9e | 2026-10-06 | Priyanshu Sharma | Merge agentcraft/juniper/t1-parity-gap-audit-vs-mori into main |
| 7 | 4ee7b3d | 4ee7b3d729f0c4826a52e5759c6942a6fb280fc6 | 2026-10-06 | AgentCraft Juniper | t4: History-to-Stats bento parity with Mori |
| 8 | 67f37cb | 67f37cb8c919bdcca1e2109ae7fcf61e5a0fb005 | 2026-10-06 | Priyanshu Sharma | Merge agentcraft/juniper/t4-history-to-stats-bento into main |
| 9 | 3951a02 | 3951a02e973852a73031af96e91a61fbd6288b9d | 2026-10-06 | AgentCraft Wren | t3: Mori reader-chrome parity (scrubber island, action dock, settings sheet) |
| 10 | f336989 | f3369895d151a25b36d806fe357e9d64a1c53c96 | 2026-10-06 | Priyanshu Sharma | Merge agentcraft/wren/t3-reader-chrome-parity into main |
| 11 | 62ba9fb | 62ba9fb2ad353985d8254ce79d4262ba7a68a05f | 2026-10-06 | AgentCraft Kit | t2: Library chrome parity with Mori (new composables + tests) |
| 12 | 666bf70 | 666bf70c86c25a38ce8acf358b07a35e91c58867 | 2026-10-06 | AgentCraft Kit | t5: Smoke-test Iridium on mori virtual device (report + screenshots) |
| 13 | 6c12bc5 | 6c12bc5c4e528259434a96102e53e52ba0493605 | 2026-10-06 | Priyanshu Sharma | Merge agentcraft/kit/t2-library-chrome-parity into main |
| 14 | a9f1dc5 | a9f1dc54d12954ac3d0d6ae0cb731ca8110526eb | 2026-10-06 | Priyanshu Sharma | Merge agentcraft/kit/t5-smoke-test-iridium into main |

Composition: 7 feature/report commits (Kit t1/t4/t2/t5, Juniper t1/t4, Wren t3) + 7 parity merges into main by Priyanshu Sharma.

## Diffstat summary

- `git diff --shortstat origin/main..main`: **50 files changed, 4349 insertions(+), 24 deletions(-)**
- Full `git diff --stat origin/main..main` (abbreviated, text files; Bin entries lack line counts):

```text
 build.gradle.kts                                   |   7 +
 .../datastore/DataStorePreferencesDataSource.kt    |   6 +
 .../DataStorePreferencesDataSourceTest.kt          |   4 +
 core/designsystem/build.gradle.kts                 |   2 +
 .../com/iridium/core/designsystem/AppFonts.kt      | 197 +++++++
 .../iridium/core/designsystem/IridiumComponents.kt |   6 +-
 .../iridium/core/designsystem/IridiumHaptics.kt    |  75 +++
 .../core/designsystem/IridiumPulsarHaptics.kt      |  86 ++++
 .../iridium/core/designsystem/IridiumSliderRow.kt  | 299 +++++++++++
 .../com/iridium/core/designsystem/IridiumTheme.kt  |   2 +
 .../iridium/core/designsystem/IridiumTypography.kt | 137 ++++-
 .../iridium/core/designsystem/SchemePickerRow.kt   |   9 +-
 .../src/main/res/font/google_sans_flex.ttf         | Bin 0 -> 4155832 bytes
 .../iridium/core/designsystem/IridiumHapticsTest.kt|  43 ++
 .../iridium/core/designsystem/IridiumSliderTest.kt |  32 ++
 .../core/designsystem/IridiumTypographyTest.kt     |  60 +++
 .../com/iridium/core/model/ReaderPreferences.kt    |   3 +
 docs/device-test-report.md                         |  78 +++
 docs/device-test/iridium-01-onboarding-welcome.png | Bin 0 -> 90396 bytes
 docs/device-test/iridium-02-onboarding-folder.png  | Bin 0 -> 111180 bytes
 docs/device-test/iridium-03-library-loaded.png     | Bin 0 -> 72457 bytes
 docs/device-test/iridium-04-detail-screen.png      | Bin 0 -> 85852 bytes
 .../iridium-05-reader-malformed-epub.png           | Bin 0 -> 411043 bytes
 docs/device-test/iridium-06-reader-chapter.png     | Bin 0 -> 421424 bytes
 docs/device-test/iridium-07-reader-chrome.png      | Bin 0 -> 487560 bytes
 .../iridium-08-reader-settings-sheet.png           | Bin 0 -> 285144 bytes
 docs/device-test/iridium-09-reader-toc.png         | Bin 0 -> 361199 bytes
 docs/device-test/iridium-10-detail-progress.png    | Bin 0 -> 85968 bytes
 docs/device-test/iridium-11-history-with-book.png  | Bin 0 -> 76138 bytes
 .../device-test/iridium-12-settings-appearance.png | Bin 0 -> 174506 bytes
 docs/device-test/iridium-13-settings-reading.png   | Bin 0 -> 169655 bytes
 docs/device-test/mori-01-library.png               | Bin 0 -> 151167 bytes
 docs/device-test/mori-02-settings.png              | Bin 0 -> 178492 bytes
 docs/device-test/mori-03-stats.png                 | Bin 0 -> 147884 bytes
 docs/parity-audit.md                               | 159 ++++++
 .../iridium/feature/history/impl/HistoryStats.kt   | 207 ++++++++
 .../feature/history/impl/HistoryStatsBento.kt      | 505 ++++++++++++++++++
 .../feature/history/impl/HistoryStatsTest.kt       | 131 +++++
 .../iridium/feature/library/impl/LibraryChrome.kt  | 504 ++++++++++++++++++
 .../feature/library/impl/LibraryChromeTest.kt      | 231 +++++++++
 .../feature/reader/impl/EpubPreferencesMapper.kt   |   2 +
 .../iridium/feature/reader/impl/ReaderChrome.kt    | 570 +++++++++++++++++++++
 .../reader/impl/ReaderChromeSettingsSheet.kt       | 545 ++++++++++++++++++++
 .../feature/reader/impl/ReaderChromeTest.kt        | 302 +++++++++++
 .../feature/settings/impl/SettingsScreen.kt        |  39 +-
 .../feature/settings/impl/SettingsUiState.kt       |   4 +
 .../feature/settings/impl/SettingsViewModel.kt     |   3 +
 .../feature/settings/impl/SettingsScreenTest.kt    | 109 ++++
 .../feature/settings/impl/SettingsViewModelTest.kt |  11 +
 gradle/libs.versions.toml                          |   5 +-
 50 files changed, 4349 insertions(+), 24 deletions(-)
```

Areas touched: designsystem (typography/fonts/haptics/sliders), history bento, library chrome, reader chrome + settings sheet, settings expressive, datastore/model prefs, `docs/parity-audit.md`, `docs/device-test-report.md`, build config (`libs.versions.toml`, `build.gradle.kts`).

## New binary files (17) with sizes

Measured on working tree via `wc -c` (exact bytes). Total payload: **7,465,897 bytes (~7.12 MB)**.

| File | Bytes | Approx |
|------|------:|--------|
| core/designsystem/src/main/res/font/google_sans_flex.ttf | 4155832 | **4.1 MB (3.96 MiB)** — FLAG: largest single payload |
| docs/device-test/iridium-01-onboarding-welcome.png | 90396 | 88.3 KB |
| docs/device-test/iridium-02-onboarding-folder.png | 111180 | 108.6 KB |
| docs/device-test/iridium-03-library-loaded.png | 72457 | 70.8 KB |
| docs/device-test/iridium-04-detail-screen.png | 85852 | 83.8 KB |
| docs/device-test/iridium-05-reader-malformed-epub.png | 411043 | 401.4 KB |
| docs/device-test/iridium-06-reader-chapter.png | 421424 | 411.5 KB |
| docs/device-test/iridium-07-reader-chrome.png | 487560 | 476.1 KB |
| docs/device-test/iridium-08-reader-settings-sheet.png | 285144 | 278.5 KB |
| docs/device-test/iridium-09-reader-toc.png | 361199 | 352.7 KB |
| docs/device-test/iridium-10-detail-progress.png | 85968 | 84.0 KB |
| docs/device-test/iridium-11-history-with-book.png | 76138 | 74.4 KB |
| docs/device-test/iridium-12-settings-appearance.png | 174506 | 170.4 KB |
| docs/device-test/iridium-13-settings-reading.png | 169655 | 165.7 KB |
| docs/device-test/mori-01-library.png | 151167 | 147.6 KB |
| docs/device-test/mori-02-settings.png | 178492 | 174.3 KB |
| docs/device-test/mori-03-stats.png | 147884 | 144.4 KB |

- FLAG: `google_sans_flex.ttf` (4.1 MB) dominates the push; confirm intentional font bundling.
- FLAG: 16 screenshots (`docs/device-test/*.png`, 13 iridium + 3 mori) total ~3.1 MB; confirm docs screenshots should ship on main.
- All 17 binaries are new (`A` in `git diff --name-status origin/main..main`); no binary modifications/deletions.

## Push status — BLOCKED awaiting Priyanshu approval

- Remote config: `origin` fetch = `git@github.com:ego1s1/iridium.git`, push = `agentcraft-push-blocked:///git@github.com:ego1s1/iridium.git`.
- **Push is blocked: do NOT push. Awaiting explicit Priyanshu approval before any push.**
- This audit is read-only (`git log` / `git diff` inspection only); no push, fetch, or pull was performed.
