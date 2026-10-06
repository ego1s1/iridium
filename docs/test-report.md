# Test report — Iridium

Date: 2026-10-06 (UTC). Commit: `7f96c31` (`7f96c3187435389a226ba124ba26c692dfdb92ee`, main incl. t14/t15/t16).
Scope: consolidation only — no app-code changes. Detail in the three part files:

- [Unit inventory + results](test-report-unit.md)
- [Static checks + CI gates](test-report-static.md)
- [Device / manual evidence](test-report-device.md)

## Summary (one screen)

| Area | Result | Key numbers |
|---|---|---|
| Unit (`testDebugUnitTest` + JVM `test`) | PASS, BUILD SUCCESSFUL | 14 modules, 31 classes, 178 tests, 0 fail / 0 error / 0 skipped |
| Static: detekt (`./gradlew detekt`) | PASS, BUILD SUCCESSFUL | 0 warnings / 0 errors across 21 module reports |
| Static: Android lint (`:app:lintDebug`) | PASS, BUILD SUCCESSFUL | 0 errors / 94 warnings (84 dependency-update, 9 AGP-update, 1 benign manifest) |
| Device / manual (emulator `mori`, Android 15) | 13 / 13 flows PASS | 0 crashes; 16 screenshots in `docs/device-test/` |

Totals: 178 unit tests green, both static gates green, 13/13 manual flows green.

## Top gaps

1. Uncovered modules: no `src/test` in `:epub-core`, `:core:common`, `:core:testing`, `:core:test-fakes`, feature API modules.
2. Lint debt: 94 warnings are dependency-update notices (AGP 8.7.3 → 9.4.1 available; repo pins 8.7.3); no correctness/security errors.
3. Manual-only areas: zero `src/androidTest/` files — 13 device flows were manual + `uiautomator`, not CI; release build/signing untested; single-device coverage only (no physical device, API levels, locales, TalkBack, perf/rotation/process-death).

## Rerun commands

```bash
export JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.20.1
export PATH=$JAVA_HOME/bin:$PATH
export ANDROID_HOME=$HOME/Library/Android/sdk
export ANDROID_SDK_ROOT=$HOME/Library/Android/sdk

./gradlew testDebugUnitTest --continue            # unit (Android modules)
./gradlew :core:model:test :epub-engine:test      # unit (pure-JVM modules)
./gradlew detekt                                  # static: detekt, all modules
./gradlew :app:lintDebug                          # static: lint, app debug
./gradlew :app:assembleDebug :app:lintDebug test detekt  # full gate (README)
```

How to regenerate: re-run the commands above, recount `**/build/test-results/test*/*.xml` (unit), `cat <module>/build/reports/detekt/detekt.txt` (empty == clean), `tail -1 app/build/reports/lint-results-debug.txt` (lint), then refresh the three part files and the table here.
