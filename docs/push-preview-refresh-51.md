# Push preview refresh audit: origin/main..main (51 ahead)

- Date: 2026-10-06 (UTC)
- Range: `origin/main..main` (read-only inspection, no push performed)
- origin/main tip: `9ea0514654fe6e1dfe8bc78a5f754827c384a3a5` (`9ea0514`) — release: correct the keystore fallback's real limitation (2026-09-25, Priyanshu Sharma)
- main tip: `bef432c82e0b705988e5fd9bd8a4684477bdc938` (`bef432c`) — Merge agentcraft/kit/t24-real-epub-device-reader into main (2026-10-07, Priyanshu Sharma)
- Ahead count: **51 commits** (`git rev-list --count origin/main..main`)
- This is a refresh of `docs/push-preview-audit.md` (14-ahead era, main tip `a9f1dc5`). That file is left untouched; this file is the 51-ahead record.
- Commands used (read-only, no fetch/pull/push/remote change):
  - `git rev-parse main; git rev-parse origin/main`
  - `git rev-list --count origin/main..main`
  - `git log origin/main..main --reverse --format='%h|%H|%ad|%an|%s' --date=short`
  - `git diff --shortstat origin/main..main`
  - `git diff --stat origin/main..main`
  - `git diff --name-only --diff-filter=A origin/main..main`
  - `git diff --numstat --diff-filter=A origin/main..main` (binary = `- -`) + `wc -c` sizes on working tree
  - `git remote -v` (inspected only)

## Commit table (51 ahead commits, chronological / oldest-first)

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
| 15 | b6af51f | b6af51f9f593d7ee59c40bf1938555bc5840a2aa | 2026-10-06 | AgentCraft Juniper | t6: Push preview audit vs origin/main |
| 16 | c84e537 | c84e537537e40c805b4e954b8820eb483eabacd4 | 2026-10-06 | AgentCraft Wren | t8: Push hygiene: secrets binaries remote check |
| 17 | 15728f4 | 15728f49a61435ecf830c0b6941aa0c5b3165044 | 2026-10-06 | Priyanshu Sharma | Merge agentcraft/juniper/t6-push-preview-audit-vs into main |
| 18 | dab2ba3 | dab2ba360ddd83a8d111ebc2dd6455733f770ea3 | 2026-10-06 | Priyanshu Sharma | Merge agentcraft/wren/t8-push-hygiene-secrets into main |
| 19 | 07b49e0 | 07b49e02101ce9fb22e2f812273e8b62cb642fd7 | 2026-10-06 | AgentCraft Kit | t9: Pre-push verification gate retry on main |
| 20 | 5e8f14d | 5e8f14d4c0e8dd3e97a1dc980386f867ddcd9c58 | 2026-10-06 | Priyanshu Sharma | Merge agentcraft/kit/t9-pre-push-verification into main |
| 21 | ce66d35 | ce66d3581580e7de69febe11fb983c48ca73abf8 | 2026-10-06 | AgentCraft Wren | t7: Pre-push verification gate on main |
| 22 | 89d036b | 89d036bf2b8a549f20f9740aac8b1ad21ce67eae | 2026-10-06 | AgentCraft Wren | Merge branch 'main' into agentcraft/wren/t7-pre-push-verification |
| 23 | 9761b11 | 9761b117d9f98a5cfcb088325dd5bf2cafcaa540 | 2026-10-06 | Priyanshu Sharma | Merge agentcraft/wren/t7-pre-push-verification into main |
| 24 | beae754 | beae754c2123b75ce7c53ae421c16306a31df689 | 2026-10-06 | AgentCraft Wren | t11: Fix detekt blocker: refactor chromeZoneForTap complexity |
| 25 | 9170809 | 91708092edbc05cd89e085d96c8ffb84a5070832 | 2026-10-06 | Priyanshu Sharma | Merge agentcraft/wren/t11-fix-detekt-blocker into main |
| 26 | 7048e9a | 7048e9a8d25fb6b037a259c991b7305bc7f46b7c | 2026-10-06 | AgentCraft Wren | t16: Consolidate device and manual test evidence |
| 27 | df3a92a | df3a92a3055afb8a758581e103224fc8251fe3ff | 2026-10-06 | Priyanshu Sharma | Merge agentcraft/wren/t16-consolidate-device into main |
| 28 | 5b09518 | 5b09518d21567d18f78de6db8e51329dad31d914 | 2026-10-06 | AgentCraft Kit | t12: Consolidate pre-push verification report and re-run gate |
| 29 | 4245c9a | 4245c9a662f334f7ce0976acce09cc214269bdd0 | 2026-10-06 | Priyanshu Sharma | Merge agentcraft/kit/t12-consolidate-pre-push into main |
| 30 | 7b5b8cb | 7b5b8cb41841b76aad3f00aace14befcc4bc4254 | 2026-10-06 | AgentCraft Juniper | t10: Expand regression tests: library chrome + settings expressive |
| 31 | 4c3cc17 | 4c3cc17d4c1889f33f91f497dc3f4df271ef5cb4 | 2026-10-06 | Priyanshu Sharma | Merge agentcraft/juniper/t10-expand-regression-tests into main |
| 32 | 14ad3ad | 14ad3ad08ee0dd590bfe74c0975ee3bcafa06153 | 2026-10-06 | AgentCraft Kit | t13: Expand regression tests: reader chrome + history stats |
| 33 | a2a97fc | a2a97fca1271a2bf183f914f51976555a82fe4a5 | 2026-10-06 | Priyanshu Sharma | Merge agentcraft/kit/t13-expand-regression-tests into main |
| 34 | ace2d60 | ace2d60225c1e61da5317f25418ed888c3128e2b | 2026-10-06 | AgentCraft Juniper | t14: Collect unit test inventory and results |
| 35 | d418860 | d4188609ff490dd66f86c9a7b6d984fd3622c184 | 2026-10-06 | Priyanshu Sharma | Merge agentcraft/juniper/t14-collect-unit-test into main |
| 36 | 4950b3a | 4950b3a65fbb67491e9ad09cc99a11bb844e9bbb | 2026-10-06 | AgentCraft Wren | t20: Adopt newer expressive chrome (FloatingToolbar) |
| 37 | ae8f5b0 | ae8f5b06a84555c77de856e382e02778fce1beea | 2026-10-06 | Priyanshu Sharma | Merge agentcraft/wren/t20-adopt-newer-expressive into main |
| 38 | bd0ceaa | bd0ceaaa661c04efc0e6b43ccf0006e3e3abcd99 | 2026-10-06 | AgentCraft Kit | t15: Collect static checks and CI gate status |
| 39 | 7f96c31 | 7f96c3187435389a226ba124ba26c692dfdb92ee | 2026-10-06 | Priyanshu Sharma | Merge agentcraft/kit/t15-collect-static-checks into main |
| 40 | 32e24bf | 32e24bfa6d452c05fd99cb4c46c76f29c938ae5c | 2026-10-06 | AgentCraft Kit | t19: Harden Pulsar haptics to Mori parity |
| 41 | 31317d6 | 31317d62543d5cb257f838bd7e6a2f48460d26b7 | 2026-10-06 | Priyanshu Sharma | Merge agentcraft/kit/t19-harden-pulsar-haptics into main |
| 42 | 75123d0 | 75123d0358b6953d99801b2335e3149b91244d06 | 2026-10-06 | AgentCraft Juniper | t18: Align M3 Expressive SDK baseline to Mori |
| 43 | 27428bd | 27428bd81087ed2c9b9451fcb9a6e74e0e619ae1 | 2026-10-06 | Priyanshu Sharma | Merge agentcraft/juniper/t18-align-m3-expressive-sdk into main |
| 44 | cc7e5d2 | cc7e5d2afc6748f30a3994aba1944db6e0321e8a | 2026-10-06 | AgentCraft Juniper | t17: Assemble consolidated test report |
| 45 | 7fa8fde | 7fa8fdef57861ba41a794550205f1bc4e30e8a28 | 2026-10-06 | Priyanshu Sharma | Merge agentcraft/juniper/t17-assemble-consolidated into main |
| 46 | 9b361fe | 9b361fea1795286108785481e58ba7c3b4ceb02a | 2026-10-06 | AgentCraft Wren | t22: Real-EPUB host-side static parse check |
| 47 | 8138074 | 8138074f5a09426dd46cdb86a94f3bd20af5be8c | 2026-10-06 | Priyanshu Sharma | Merge agentcraft/wren/t22-real-epub-host-side into main |
| 48 | 0931ee0 | 0931ee0217d7b3a97466c92dfa9ddfb695c55a9e | 2026-10-07 | AgentCraft Juniper | t21: Real-EPUB device library load |
| 49 | 89d387c | 89d387ce47cc69767f7aefce7e67ece78c0dbd8f | 2026-10-07 | Priyanshu Sharma | Merge agentcraft/juniper/t21-real-epub-device-library into main |
| 50 | f63d85c | f63d85c5d3420d8b331c7d8aed9b322ae99d5de2 | 2026-10-07 | AgentCraft Kit | t24: Real-EPUB device reader retry |
| 51 | bef432c | bef432c82e0b705988e5fd9bd8a4684477bdc938 | 2026-10-07 | Priyanshu Sharma | Merge agentcraft/kit/t24-real-epub-device-reader into main |

Composition: 25 non-merge feature/report commits (Kit 10, Juniper 7 + 1 branch-internal? no — Juniper 8 total incl. t21/t10/t14/t17/t18 + t1/t4/t6; Wren 8 incl. t3/t8/t7/t11/t16/t20/t22 + 1 branch merge 89d036b) + 26 merges (25 into main by Priyanshu Sharma + 1 branch-internal merge by Wren `89d036b`). Author totals: AgentCraft Kit 10, AgentCraft Juniper 8, AgentCraft Wren 8, Priyanshu Sharma 25.

## Diffstat summary

- `git diff --shortstat origin/main..main`: **95 files changed, 6625 insertions(+), 27 deletions(-)**
- Name-status breakdown: **80 added (A), 15 modified (M), 0 deleted** in this range.
- Modified (15): `build.gradle.kts`, `core/datastore/.../DataStorePreferencesDataSource.kt` (+ test), `core/designsystem/build.gradle.kts`, `core/designsystem/.../IridiumComponents.kt`, `IridiumTheme.kt`, `IridiumTypography.kt`, `SchemePickerRow.kt`, `core/model/.../ReaderPreferences.kt`, `feature/reader/.../EpubPreferencesMapper.kt`, `feature/settings/.../SettingsScreen.kt`, `SettingsUiState.kt`, `SettingsViewModel.kt`, `feature/settings/.../SettingsViewModelTest.kt`, `gradle/libs.versions.toml`.
- Full `git diff --stat origin/main..main`:

```text
 build.gradle.kts                                   |   7 +
 .../datastore/DataStorePreferencesDataSource.kt    |   6 +
 .../DataStorePreferencesDataSourceTest.kt          |   4 +
 core/designsystem/build.gradle.kts                 |   2 +
 .../com/iridium/core/designsystem/AppFonts.kt      | 197 +++++++
 .../iridium/core/designsystem/IridiumComponents.kt |   6 +-
 .../iridium/core/designsystem/IridiumHaptics.kt    |  75 +++
 .../core/designsystem/IridiumPulsarHaptics.kt      | 119 +++++
 .../iridium/core/designsystem/IridiumSliderRow.kt  | 299 +++++++++++
 .../com/iridium/core/designsystem/IridiumTheme.kt  |   2 +
 .../iridium/core/designsystem/IridiumTypography.kt | 137 ++++-
 .../iridium/core/designsystem/SchemePickerRow.kt   |   9 +-
 .../src/main/res/font/google_sans_flex.ttf         | Bin 0 -> 4155832 bytes
 .../core/designsystem/IridiumHapticsTest.kt        |  53 ++
 .../iridium/core/designsystem/IridiumSliderTest.kt |  32 ++
 .../core/designsystem/IridiumTypographyTest.kt     |  60 +++
 .../com/iridium/core/model/ReaderPreferences.kt    |   3 +
 docs/device-test-real-epub-library.md              |  75 +++
 docs/device-test-real-epub-reader-retry.md         |  94 ++++
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
 docs/device-test/real-epub-lib-01-library.png      | Bin 0 -> 599547 bytes
 docs/device-test/real-epub-lib-01-library.xml      |   1 +
 docs/device-test/real-epub-lib-02-crash-log.txt    |  23 +
 docs/device-test/real-epub-lib-02-crash.png        | Bin 0 -> 138930 bytes
 .../real-epub-reader-retry-01-library.png          | Bin 0 -> 516116 bytes
 .../real-epub-reader-retry-01-library.xml          |   1 +
 .../real-epub-reader-retry-02-crash-log.txt        |  58 ++
 .../real-epub-reader-retry-02-detail.png           | Bin 0 -> 1381048 bytes
 .../real-epub-reader-retry-02-detail.xml           |   1 +
 .../real-epub-reader-retry-03-reader.png           | Bin 0 -> 396919 bytes
 .../real-epub-reader-retry-03-reader.xml           |   1 +
 .../real-epub-reader-retry-04-paging.png           | Bin 0 -> 152575 bytes
 .../real-epub-reader-retry-04-paging.xml           |   1 +
 .../real-epub-reader-retry-05-chrome.png           | Bin 0 -> 448739 bytes
 .../real-epub-reader-retry-05-chrome.xml           |   1 +
 .../real-epub-reader-retry-06-chrome.png           | Bin 0 -> 176793 bytes
 .../real-epub-reader-retry-06-chrome.xml           |   1 +
 .../real-epub-reader-retry-07-settings.png         | Bin 0 -> 131685 bytes
 .../real-epub-reader-retry-07-settings.xml         |   1 +
 .../real-epub-reader-retry-08-toc-crash-log.txt    |  25 +
 .../real-epub-reader-retry-08-toc-launcher.xml     |   1 +
 docs/device-test/real-epub-reader-retry-08-toc.png | Bin 0 -> 178575 bytes
 docs/device-test/real-epub-reader-retry-08-toc.xml |   1 +
 .../real-epub-reader-retry-09-progress.png         | Bin 0 -> 598619 bytes
 .../real-epub-reader-retry-09-progress.xml         |   1 +
 .../real-epub-reader-retry-10-library-after.png    | Bin 0 -> 682215 bytes
 .../real-epub-reader-retry-10-library-after.xml    |   1 +
 .../real-epub-reader-retry-11-history.png          | Bin 0 -> 157051 bytes
 .../real-epub-reader-retry-11-history.xml          |   1 +
 docs/parity-audit.md                               | 159 ++++++
 docs/pre-push-verification.md                      |  98 ++++
 docs/push-hygiene-check.md                         |  90 ++++
 docs/push-preview-audit.md                         | 127 +++++
 docs/real-epub-static-check.md                     | 187 +++++++
 docs/test-report-device.md                         |  67 +++
 docs/test-report-static.md                         | 103 ++++
 docs/test-report-unit.md                           |  90 ++++
 docs/test-report.md                                |  42 ++
 .../iridium/feature/history/impl/HistoryStats.kt   | 207 ++++++++
 .../feature/history/impl/HistoryStatsBento.kt      | 505 ++++++++++++++++++
 .../feature/history/impl/HistoryStatsParityTest.kt | 209 ++++++++
 .../feature/history/impl/HistoryStatsTest.kt       | 131 +++++
 .../iridium/feature/library/impl/LibraryChrome.kt  | 504 ++++++++++++++++++
 .../library/impl/LibraryChromeParityTest2.kt       | 235 +++++++++
 .../feature/library/impl/LibraryChromeTest.kt      | 231 ++++++++
 .../feature/reader/impl/EpubPreferencesMapper.kt   |   2 +
 .../iridium/feature/reader/impl/ReaderChrome.kt    | 582 +++++++++++++++++++++
 .../reader/impl/ReaderChromeFloatingToolbar.kt     | 160 ++++++
 .../reader/impl/ReaderChromeSettingsSheet.kt       | 545 +++++++++++++++++++
 .../reader/impl/ReaderChromeFloatingToolbarTest.kt | 145 +++++
 .../feature/reader/impl/ReaderChromeParityTest2.kt | 174 ++++++
 .../feature/reader/impl/ReaderChromeTest.kt        | 302 +++++++++++
 .../feature/settings/impl/SettingsScreen.kt        |  39 +-
 .../feature/settings/impl/SettingsUiState.kt       |   4 +
 .../feature/settings/impl/SettingsViewModel.kt     |   3 +
 .../settings/impl/SettingsExpressiveParityTest.kt  | 199 +++++++
 .../feature/settings/impl/SettingsScreenTest.kt    | 109 ++++
 .../feature/settings/impl/SettingsViewModelTest.kt |  11 +
 gradle/libs.versions.toml                          |  15 +-
 95 files changed, 6625 insertions(+), 27 deletions(-)
```

## New binary files (30) with sizes

Binary = `git diff --numstat --diff-filter=A` shows `- -` (all 30 are new `A`, no binary modifications/deletions). Sizes measured on working tree via `wc -c` (exact bytes). Total payload: **13,024,709 bytes (~12.42 MiB / ~12.74 MB)**.

| File | Bytes (`wc -c`) | Approx |
|------|------:|--------|
| core/designsystem/src/main/res/font/google_sans_flex.ttf | 4155832 | 4.1 MB (3.96 MiB) — FLAG: largest single payload |
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
| docs/device-test/real-epub-lib-01-library.png | 599547 | 585.5 KB — NEW since 14-ahead audit |
| docs/device-test/real-epub-lib-02-crash.png | 138930 | 135.7 KB — NEW since 14-ahead audit |
| docs/device-test/real-epub-reader-retry-01-library.png | 516116 | 504.0 KB — NEW since 14-ahead audit |
| docs/device-test/real-epub-reader-retry-02-detail.png | 1381048 | 1.32 MB (1.38 MB) — NEW, 2nd largest |
| docs/device-test/real-epub-reader-retry-03-reader.png | 396919 | 387.6 KB — NEW since 14-ahead audit |
| docs/device-test/real-epub-reader-retry-04-paging.png | 152575 | 149.0 KB — NEW since 14-ahead audit |
| docs/device-test/real-epub-reader-retry-05-chrome.png | 448739 | 438.2 KB — NEW since 14-ahead audit |
| docs/device-test/real-epub-reader-retry-06-chrome.png | 176793 | 172.6 KB — NEW since 14-ahead audit |
| docs/device-test/real-epub-reader-retry-07-settings.png | 131685 | 128.6 KB — NEW since 14-ahead audit |
| docs/device-test/real-epub-reader-retry-08-toc.png | 178575 | 174.4 KB — NEW since 14-ahead audit |
| docs/device-test/real-epub-reader-retry-09-progress.png | 598619 | 584.6 KB — NEW since 14-ahead audit |
| docs/device-test/real-epub-reader-retry-10-library-after.png | 682215 | 666.2 KB — NEW since 14-ahead audit |
| docs/device-test/real-epub-reader-retry-11-history.png | 157051 | 153.4 KB — NEW since 14-ahead audit |

- Delta vs 14-ahead audit: 17 → 30 binaries (+13: 2 real-epub-lib + 11 real-epub-reader-retry screenshots); total 7,465,897 → 13,024,709 bytes (+5,558,812 bytes, ~5.30 MiB).
- FLAG: `google_sans_flex.ttf` (4.1 MB) still dominates; confirm intentional font bundling.
- FLAG: 29 screenshots (`docs/device-test/*.png`, 13 iridium + 3 mori + 15 real-epub) total ~8.9 MB; confirm docs screenshots should ship on main.
- All 30 binaries are new (`A` in `git diff --name-status origin/main..main`); no binary modifications/deletions.
- Companion text evidence (not binary, counted in 80 added files): 13 `.xml` UI dumps + 3 crash/toc `.txt` logs under `docs/device-test/real-epub-*`.

## Areas touched

- `core/designsystem`: fonts (`AppFonts.kt` + `google_sans_flex.ttf`), typography, theme/components/slider-row/scheme-picker tweaks, haptics + Pulsar hardening + tests (haptics/slider/typography).
- `core/datastore` + `core/model`: `ReaderPreferences` / `DataStorePreferencesDataSource` prefs (+ tests).
- `feature/history`: `HistoryStats` + `HistoryStatsBento` + parity/unit tests.
- `feature/library`: `LibraryChrome` + parity tests (incl. `LibraryChromeParityTest2`).
- `feature/reader`: `ReaderChrome` + `ReaderChromeSettingsSheet` + newer `ReaderChromeFloatingToolbar` + parity/unit tests; `EpubPreferencesMapper` tweak.
- `feature/settings`: expressive sliders/modular sections + `SettingsExpressiveParityTest` + screen/viewmodel tests.
- `docs`: `parity-audit.md`, `device-test-report.md`, `device-test-real-epub-library.md`, `device-test-real-epub-reader-retry.md`, `real-epub-static-check.md`, `test-report*.md` (unit/static/device/consolidated), `pre-push-verification.md`, `push-hygiene-check.md`, `push-preview-audit.md` (the 14-ahead record itself now part of the push), plus `docs/device-test/*` screenshots/XML/logs.
- Build config: `build.gradle.kts`, `core/designsystem/build.gradle.kts`, `gradle/libs.versions.toml` (M3 Expressive SDK baseline alignment).

## Push status — BLOCKED awaiting Priyanshu approval

- Remote config (inspected only): `origin` fetch = `git@github.com:ego1s1/iridium.git`, push = `agentcraft-push-blocked:///git@github.com:ego1s1/iridium.git`.
- **Push is blocked: do NOT push. Awaiting explicit Priyanshu approval before any push/merge decision.**
- This refresh audit is read-only (`git log` / `git diff` / `wc -c` inspection only); no push, fetch, pull, or remote change was performed.
