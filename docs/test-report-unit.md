# Unit test report — Iridium

Date: 2026-10-06. Branch: `agentcraft/juniper/t14-collect-unit-test`.
Scope: inventory + results only — no app-code changes.

## Run summary (fresh run, BUILD SUCCESSFUL)

| Item | Value |
|---|---|
| Host | macOS arm64, `openjdk@17` (Homebrew; no system JDK — `JAVA_HOME=/opt/homebrew/opt/openjdk@17`), `ANDROID_HOME=$HOME/Library/Android/sdk` |
| Command 1 | `./gradlew testDebugUnitTest --continue` → BUILD SUCCESSFUL (575 tasks: 258 executed, 317 from cache) |
| Command 2 | `./gradlew :core:model:test :epub-engine:test --continue` → BUILD SUCCESSFUL (pure-JVM modules expose `test`, not `testDebugUnitTest`; fallback per task spec) |
| Result XML | `**/build/test-results/test*/*.xml` — 31 files, one per test class |
| Totals | 31 classes, 178 tests, 0 failures, 0 errors, 0 skipped |

## Per-module results (from XML)

| Gradle module | Test classes | Tests | Failures | Errors | Skipped |
|---|---|---|---|---|---|
| `:app` | 2 | 7 | 0 | 0 | 0 |
| `:core:data` | 3 | 20 | 0 | 0 | 0 |
| `:core:database` | 2 | 8 | 0 | 0 | 0 |
| `:core:datastore` | 1 | 9 | 0 | 0 | 0 |
| `:core:designsystem` | 3 | 11 | 0 | 0 | 0 |
| `:core:model` (`test`) | 1 | 5 | 0 | 0 | 0 |
| `:epub-engine` (`test`) | 3 | 19 | 0 | 0 | 0 |
| `:epub-native` | 2 | 7 | 0 | 0 | 0 |
| `:feature:detail:impl` | 1 | 6 | 0 | 0 | 0 |
| `:feature:history:impl` | 2 | 12 | 0 | 0 | 0 |
| `:feature:library:impl` | 3 | 24 | 0 | 0 | 0 |
| `:feature:onboarding:impl` | 2 | 13 | 0 | 0 | 0 |
| `:feature:reader:impl` | 4 | 26 | 0 | 0 | 0 |
| `:feature:settings:impl` | 2 | 11 | 0 | 0 | 0 |
| **Total (14 modules)** | **31** | **178** | **0** | **0** | **0** |

## Inventory: file → module → class

Glob `**/src/test/**/*.kt` found 31 files (spec expected ~35 across app, core/*, feature/*, epub-engine, epub-native).

| Source file | Module | Class | Tests |
|---|---|---|---|
| `app/src/test/.../MainNavigatorTest.kt` | `:app` | `com.iridium.app.MainNavigatorTest` | 2 |
| `app/src/test/.../CrashReporterTest.kt` | `:app` | `com.iridium.app.CrashReporterTest` | 5 |
| `core/data/src/test/.../ChapterIndexerTest.kt` | `:core:data` | `com.iridium.core.data.ChapterIndexerTest` | 4 |
| `core/data/src/test/.../FtsQueryTest.kt` | `:core:data` | `com.iridium.core.data.FtsQueryTest` | 6 |
| `core/data/src/test/.../OfflineFirstBooksRepositoryTest.kt` | `:core:data` | `com.iridium.core.data.OfflineFirstBooksRepositoryTest` | 10 |
| `core/database/src/test/.../BookDaoTest.kt` | `:core:database` | `com.iridium.core.database.BookDaoTest` | 4 |
| `core/database/src/test/.../ChapterTextDaoTest.kt` | `:core:database` | `com.iridium.core.database.ChapterTextDaoTest` | 4 |
| `core/datastore/src/test/.../DataStorePreferencesDataSourceTest.kt` | `:core:datastore` | `com.iridium.core.datastore.DataStorePreferencesDataSourceTest` | 9 |
| `core/designsystem/src/test/.../IridiumTypographyTest.kt` | `:core:designsystem` | `com.iridium.core.designsystem.IridiumTypographyTest` | 2 |
| `core/designsystem/src/test/.../IridiumHapticsTest.kt` | `:core:designsystem` | `com.iridium.core.designsystem.IridiumHapticsTest` | 6 |
| `core/designsystem/src/test/.../IridiumSliderTest.kt` | `:core:designsystem` | `com.iridium.core.designsystem.IridiumSliderTest` | 3 |
| `core/model/src/test/.../BookQueryTest.kt` | `:core:model` | `com.iridium.core.model.BookQueryTest` | 5 |
| `epub-engine/src/test/.../IridiumEpubEngineTest.kt` | `:epub-engine` | `com.iridium.epub.engine.IridiumEpubEngineTest` | 10 |
| `epub-engine/src/test/.../EpubBenchmarkTest.kt` | `:epub-engine` | `com.iridium.epub.engine.EpubBenchmarkTest` | 2 |
| `epub-engine/src/test/.../HtmlTextTest.kt` | `:epub-engine` | `com.iridium.epub.engine.HtmlTextTest` | 7 |
| `epub-native/src/test/.../NativeBenchmarkTest.kt` | `:epub-native` | `com.iridium.epub.nativecore.NativeBenchmarkTest` | 1 |
| `epub-native/src/test/.../NativeEpubTest.kt` | `:epub-native` | `com.iridium.epub.nativecore.NativeEpubTest` | 6 |
| `feature/detail/impl/src/test/.../DetailViewModelTest.kt` | `:feature:detail:impl` | `com.iridium.feature.detail.impl.DetailViewModelTest` | 6 |
| `feature/history/impl/src/test/.../HistoryStatsTest.kt` | `:feature:history:impl` | `com.iridium.feature.history.impl.HistoryStatsTest` | 8 |
| `feature/history/impl/src/test/.../HistoryViewModelTest.kt` | `:feature:history:impl` | `com.iridium.feature.history.impl.HistoryViewModelTest` | 4 |
| `feature/library/impl/src/test/.../LibraryChromeTest.kt` | `:feature:library:impl` | `com.iridium.feature.library.impl.LibraryChromeTest` | 10 |
| `feature/library/impl/src/test/.../LibraryViewModelTest.kt` | `:feature:library:impl` | `com.iridium.feature.library.impl.LibraryViewModelTest` | 10 |
| `feature/library/impl/src/test/.../LibraryScreenTest.kt` | `:feature:library:impl` | `com.iridium.feature.library.impl.LibraryScreenTest` | 4 |
| `feature/onboarding/impl/src/test/.../OnboardingScreenTest.kt` | `:feature:onboarding:impl` | `com.iridium.feature.onboarding.impl.OnboardingScreenTest` | 5 |
| `feature/onboarding/impl/src/test/.../OnboardingViewModelTest.kt` | `:feature:onboarding:impl` | `com.iridium.feature.onboarding.impl.OnboardingViewModelTest` | 8 |
| `feature/reader/impl/src/test/.../ReaderChromeTest.kt` | `:feature:reader:impl` | `com.iridium.feature.reader.impl.ReaderChromeTest` | 13 |
| `feature/reader/impl/src/test/.../EpubPreferencesMapperTest.kt` | `:feature:reader:impl` | `com.iridium.feature.reader.impl.EpubPreferencesMapperTest` | 5 |
| `feature/reader/impl/src/test/.../ReaderSessionStoreTest.kt` | `:feature:reader:impl` | `com.iridium.feature.reader.impl.ReaderSessionStoreTest` | 6 |
| `feature/reader/impl/src/test/.../ReaderViewModelTest.kt` | `:feature:reader:impl` | `com.iridium.feature.reader.impl.ReaderViewModelTest` | 2 |
| `feature/settings/impl/src/test/.../SettingsViewModelTest.kt` | `:feature:settings:impl` | `com.iridium.feature.settings.impl.SettingsViewModelTest` | 8 |
| `feature/settings/impl/src/test/.../SettingsScreenTest.kt` | `:feature:settings:impl` | `com.iridium.feature.settings.impl.SettingsScreenTest` | 3 |

(`...` abbreviates the package-path middle, e.g. `kotlin/com/iridium/app/`.)

## Notes

- Native tests (`:epub-native` — `NativeEpubTest`, `NativeBenchmarkTest`) guard every
  native case with `assumeTrue(NativeEpub.isAvailable, ...)`, so on a host without the
  C++ toolchain / host shared library (`compileHostNative`) they report as skipped
  instead of failing. In this run the toolchain was present (SDK `cmake/` installed),
  the host library built, and all 7 native tests ran and passed (skipped=0 in both XML files).
- Modules with no `src/test` sources (hence no unit-test task output): `:epub-core`,
  `:core:common`, `:core:testing`, `:core:test-fakes`, and the feature API modules
  (`:feature:onboarding:api`, `:feature:detail:api`, `:feature:reader:api`).
- `:core:model` and `:epub-engine` are pure JVM/Kotlin modules: their suite runs via the
  plain `test` task (`build/test-results/test/`), not `testDebugUnitTest`.
- Several module results were served `FROM-CACHE` (identical inputs already tested);
  `:app:testDebugUnitTest` executed fresh in this run. All 31 XML files on disk are
  from the 2026-10-06 run above.
