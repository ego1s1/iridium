# Static checks + CI gate status — Iridium

Date: 2026-10-06 (UTC). Commit: `4c3cc17` (branch `agentcraft/kit/t15-collect-static-checks`, based on main).
Scope: report only — no app-code changes. Both gates were actually run locally (not config-only).

## CI gate table (`.github/workflows/ci.yml`, job `build`, `ubuntu-latest`)

Single job; steps run in order on push to `main` and on every PR:

| # | Step | Command / action | Status (this report) |
|---|---|---|---|
| 1 | Checkout (full history, `fetch-depth: 0` — app version derives from commit count) | `actions/checkout@v4` | config (CI infra, not run locally) |
| 2 | Set up JDK 17 | `actions/setup-java@v4` (temurin 17) | local equiv: Homebrew openjdk 17.0.20.1 — OK |
| 3 | Locate Android SDK tooling | shell (resolves `sdkmanager`) | local: `~/Library/Android/sdk` — OK |
| 4 | Accept SDK licenses | `yes \| sdkmanager --licenses` | n/a locally |
| 5 | Install missing SDK packages (NDK 27.2.12479018, CMake 3.22.1, platforms;android-35, build-tools;35.0.0) | `sdkmanager` guarded installs | local SDK already has platforms `android-35`, build-tools `35.0.0` — OK |
| 6 | Verify toolchain (NDK + CMake dirs exist) | shell `test -d` | not re-run (native core optional; JVM fallback) |
| 7 | Set up Gradle | `gradle/actions/setup-gradle@v4` | local `./gradlew` wrapper — OK |
| 8 | Run unit tests | `./gradlew test` | covered by t14 (not re-run here) |
| 9 | Run detekt | `./gradlew detekt` | **BUILD SUCCESSFUL, 0 issues** (run 2026-10-06, ~16 s) |
| 10 | Run lint | `./gradlew :app:lintDebug` | **BUILD SUCCESSFUL, 0 errors / 94 warnings** (run 2026-10-06, ~48 s) |
| 11 | Assemble debug APK | `./gradlew :app:assembleDebug` | not run here (device report t16 used `:app:assembleDebug` v0.1.28) |

README gate line (`README.md` "Build it yourself") matches CI:

```bash
./gradlew :app:assembleDebug     # debug APK
./gradlew test                   # unit tests
./gradlew detekt                 # static analysis
./gradlew :app:lintDebug         # Android lint

# full gate
./gradlew :app:assembleDebug :app:lintDebug test detekt
```

## How each gate is configured

- **detekt 1.23.7** (`gradle/libs.versions.toml` → `detekt = "1.23.7"`, plugin `io.gitlab.arturbosch.detekt`).
  Applied to every subproject from root `build.gradle.kts` (`subprojects { pluginManager.apply(...) }`)
  so a new module cannot silently skip the gate. Task config: `config.setFrom(rootProject.file("config/detekt/detekt.yml"))`,
  `buildUponDefaultConfig = true`, `parallel = true`; reports: html + txt required, xml/sarif off.
  Project config `config/detekt/detekt.yml` starts from defaults and relaxes only: `LongMethod` threshold 120
  (ignores `@Composable`), `LongParameterList` 10/12, `CyclomaticComplexMethod` 30, `NestedBlockDepth` 8,
  `TooManyFunctions` 25/25/25/30, `ComplexCondition` 6; style off: `LoopWithTooManyJumpStatements`, `MagicNumber`
  (ZIP/EPUB format constants), `ForbiddenComment`, `UnnecessaryAbstractClass`, `DataClassContainsFunctions`;
  `MaxLineLength` 140 (test fixtures excluded); exceptions off: `TooGenericExceptionCaught`, `SwallowedException`,
  `InstanceOfCheckForException`, `ThrowingExceptionsWithoutMessageOrCause` (malformed-file resilience is the contract);
  `GlobalCoroutineUsage` on, `InjectDispatcher` off; `SpreadOperator` off; `EmptyFunctionBlock ignoreOverridden`.
- **Android lint:** no custom config — no `lint.xml`, no `lint {}` block in `build-logic/` or `app/build.gradle.kts`;
  AGP 8.7.3 defaults. Gate runs `:app:lintDebug` (debug variant of the app module only); no `abortOnError` /
  `warningsAsErrors` override, so warnings do not fail the build.

## Current results (actually run, 2026-10-06)

Env: macOS arm64, `JAVA_HOME` = Homebrew openjdk 17.0.20.1, `ANDROID_HOME/SDK_ROOT` = `~/Library/Android/sdk`
(platforms `android-35`, build-tools `35.0.0`). No `local.properties` needed.

### detekt — `./gradlew detekt` → BUILD SUCCESSFUL

- 26 actionable tasks: 6 executed, 20 from cache. Per-module `detekt` tasks all pass
  (`:app`, `epub-core`, `epub-engine`, `epub-native`, `core:*`, `feature/*` — aggregator modules report NO-SOURCE).
- Issue count: **0 warnings / 0 errors across all 21 module reports** — every
  `*/build/reports/detekt/detekt.txt` is empty (metrics-only `.md`, e.g. app: 1,018 loc / 873 sloc).
- Reports (local build output, gitignored): `<module>/build/reports/detekt/detekt.{html,txt,md}`.

### lint — `./gradlew :app:lintDebug` → BUILD SUCCESSFUL

- 623 actionable tasks: 350 executed, 269 from cache. `lint-results-debug.txt` footer: **0 errors, 94 warnings**.
- Breakdown from `lint-results-debug.xml`: 94 × `Warning`, 0 × Error/Fatal —
  84 `GradleDependency` (newer library versions available), 9 `AndroidGradlePluginVersion`
  (AGP 8.7.3 → 9.4.1 available; repo pins 8.7.3, see `core-1.15.0` force note in root `build.gradle.kts`),
  1 `UnusedAttribute` (`app/src/main/AndroidManifest.xml:11` `enableOnBackInvokedCallback`, minSdk 24 < 33 — benign, ignored on old devices).
- No code-correctness, security, or performance errors. Warnings are dependency-update notices + one benign manifest note.
- Reports (local build output, gitignored): `app/build/reports/lint-results-debug.{html,xml,txt}`.

## Not-run / deferred (explicit)

- `./gradlew test` (unit tests): not re-run here — owned by t14 (unit-test inventory). Device source report notes
  `testDebugUnitTest` was BUILD SUCCESSFUL at device-run time.
- `./gradlew :app:assembleDebug`: not re-run here — owned by t16 device evidence (APK v0.1.28, 41 MB).
  Release build/signing remains untested (see `docs/test-report-device.md` gap 2).

## Exact rerun commands

```bash
export JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.20.1
export PATH=$JAVA_HOME/bin:$PATH
export ANDROID_HOME=$HOME/Library/Android/sdk
export ANDROID_SDK_ROOT=$HOME/Library/Android/sdk

./gradlew detekt                 # static analysis, all modules
./gradlew :app:lintDebug         # Android lint, app debug variant

# full gate (README)
./gradlew :app:assembleDebug :app:lintDebug test detekt

# where to read results
cat <module>/build/reports/detekt/detekt.txt          # empty == clean (21 modules)
tail -1 app/build/reports/lint-results-debug.txt      # "0 errors, 94 warnings"
python3 -c "import xml.etree.ElementTree as ET; \
  t=ET.parse('app/build/reports/lint-results-debug.xml'); \
  print(len(t.getroot().findall('issue')), 'issues')"
```
