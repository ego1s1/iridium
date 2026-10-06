# Pre-push verification re-run — main tip `bef432c` (51 ahead)

- Commit under test: `bef432c` (Merge agentcraft/kit/t24-real-epub-device-reader
  into main). Branch: `agentcraft/kit/t27-re-run-pre-push` (t27, Kit).
- Mode: report-only. Only this new file was written; no source fixes applied.
- Environment: `JAVA_HOME=/opt/homebrew/opt/openjdk@17` (JDK 17.0.20.1),
  `ANDROID_HOME=~/Library/Android/sdk`. Each gate step run as a separate
  invocation for unambiguous per-step exit codes. Test/suite counts aggregated
  from `*/build/test-results/testDebugUnitTest/*.xml` across modules.
- Wall-clock time: 2026-10-07 ~01:40–01:50 IST.

## Per-step results

| Step | Command | Exit | Time | Result |
|------|---------|------|------|--------|
| 1 | `./gradlew :app:assembleDebug --console=plain` | 0 | ~7 s (up-to-date; ~27 s cold) | **PASS** — BUILD SUCCESSFUL, debug APK assembles |
| 2 | `./gradlew :app:lintDebug --console=plain` | 0 | 62 s | **PASS** — BUILD SUCCESSFUL; **0 errors, 94 warnings** (84 `GradleDependency`, 9 `AndroidGradlePluginVersion`, 1 `UnusedAttribute`). Report: `app/build/reports/lint-results-debug.xml` |
| 3 | `./gradlew test --console=plain` | 1 | 58 s | **FAIL** — **21 suites, 138 tests, 2 failures, 0 errors, 0 skipped** |
| 3b (rerun) | `./gradlew :feature:history:impl:testDebugUnitTest --rerun-tasks --console=plain` | 1 | 22 s | **FAIL** — same 2 failures reproduce (deterministic, not flaky) |
| 4 | `./gradlew detekt --console=plain` | 0 | 9 s | **PASS** — BUILD SUCCESSFUL; **0 findings** (all module `detekt.txt` reports empty) |

## Failing tests (step 3, both in `:feature:history:impl`)

1. `HistoryViewModelTest` — "books read yesterday are separated from today":
   `expected:<[Today, Yesterday]> but was:<[Today, 5 Oct 2026]>`
2. `HistoryStatsTest` — "consecutive read days form a streak":
   `expected:<HistoryStreak(current=2, longest=2)> but was:<HistoryStreak(current=1, longest=1)>`

Both are date-relative ("yesterday" / consecutive-day streak) assertions; the
gate ran at ~01:48 IST (20:18 UTC Oct 6), i.e. near a UTC/IST day boundary, so
a local-date vs instant day-boundary mismatch is the likely cause. Recorded
only — no source fix per this task's report-only scope.

## Verdict: BLOCKED

Assemble, lint, and detekt pass; `test` fails on 2 date-boundary tests in
`:feature:history:impl` (reproduced with `--rerun-tasks`). Push should wait for
a history-module owner to fix or quarantine these tests and re-run the gate.
Tree left clean; nothing pushed.
