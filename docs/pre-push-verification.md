# Pre-push verification gate — main

Two independent report-only runs (t7 by Wren, t9 by Kit) audited the same
commit and agree. This merged file keeps both runs' evidence.

- Commit under test: `dab2ba3` (Merge agentcraft/wren/t8-push-hygiene-secrets into main)
- Mode: report-only. No source files modified, no fixes applied, nothing pushed.
- Environment (both runs): `JAVA_HOME=/opt/homebrew/opt/openjdk@17` (JDK 17.0.20.1),
  `ANDROID_HOME=~/Library/Android/sdk`.
  Note: bare `java` is not on PATH on this machine; the gate needs the env above.
  No `local.properties` present; SDK resolved via `ANDROID_HOME`.

## Run 1 — t7 (Wren, 2026-10-06 23:25 IST, branch `agentcraft/wren/t7-pre-push-verification`)

Each gate step run as a separate invocation for unambiguous per-step results
(a single combined `./gradlew :app:assembleDebug :app:lintDebug test detekt`
run was also executed first and failed at the detekt step):

| Step | Command | Result |
|------|---------|--------|
| 1 | `./gradlew :app:assembleDebug --console=plain -q` | **PASS** (exit 0) — debug APK assembles cleanly |
| 2 | `./gradlew :app:lintDebug --console=plain -q` | **PASS** (exit 0) — 0 errors, 94 warnings (e.g. `UnusedAttribute` for `enableOnBackInvokedCallback`, minSdk 24 vs API 33). Full list: `app/build/reports/lint-results-debug.txt` |
| 3 | `./gradlew test --console=plain -q` | **PASS** (exit 0) — 31 suites, **178 tests, 0 failures, 0 errors, 0 skipped** |
| 4 | `./gradlew detekt --console=plain` | **FAIL** (exit 1) — 1 weighted issue, all other modules `UP-TO-DATE` / `NO-SOURCE` |

Failing tests: none.

## Run 2 — t9 (Kit, 2026-10-06, branch `agentcraft/kit/t9-pre-push-verification`)

Full gate, stepwise for per-step results. Tree state: clean, 0 commits behind
`main` (merged `main` at start; already up to date).

| # | Command | Result |
|---|---------|--------|
| 0 | `./gradlew --version` (env sanity) | PASS — Gradle 8.9, JVM 17.0.20.1 |
| 1 | `./gradlew :app:assembleDebug --console=plain -q` | PASS — exit 0 |
| 2 | `./gradlew :app:lintDebug --console=plain` | PASS — BUILD SUCCESSFUL in ~50s (623 tasks: 223 executed, 100 from cache, 300 up-to-date) |
| 3 | `./gradlew test --console=plain` | PASS — BUILD SUCCESSFUL in ~24s (1101 tasks: 378 executed, 407 from cache, 316 up-to-date; release unit-test tasks SKIP, unit `test` tasks UP-TO-DATE/PASS) |
| 4 | `./gradlew detekt --console=plain` | **FAIL** — BUILD FAILED in ~4s, exit 1 |

Nothing skipped: the full gate from the task ran as specified, no lightweight substitution.
Re-ran `:feature:reader:impl:detekt` after reverting the branch to base state to
confirm the failure reproduces on the audited tree.

## Failure detail (detekt, pre-existing on clean main — both runs agree)

```
> Task :feature:reader:impl:detekt FAILED
feature/reader/impl/src/main/kotlin/com/iridium/feature/reader/impl/ReaderChrome.kt:183:5:
The function chromeZoneForTap appears to be too complex based on Cyclomatic
Complexity (complexity: 35). Defined complexity threshold for methods is set
to '30' [CyclomaticComplexMethod]

FAILURE: Build failed with an exception.
* What went wrong:
Execution failed for task ':feature:reader:impl:detekt'.
> Analysis failed with 1 weighted issues.
```

One weighted issue; all other modules' `detekt` tasks passed.

## Fix proposal (tracked separately, NOT in this branch)

Decomposing `chromeZoneForTap` into per-nav-mode resolvers (`zoneForThirds`,
`zoneForLShape`, `zoneForKindlish`, `zoneForEdge`, `zoneForSides`) plus a
`mirrorTapZone` helper was verified to clear the gate on a scratch basis
(full gate BUILD SUCCESSFUL), but is intentionally excluded here to keep this
task audit-only. The proposal was sent to the lead for a separate follow-up
task (only the lead can create tasks).

## Verdict: NOT READY

`assembleDebug`, `lintDebug`, and `test` pass, but `detekt` fails on
`chromeZoneForTap` cyclomatic complexity (35 > 30), so the full gate is red
and a push cannot be verified clean. Fixing the complexity finding (or an
explicitly approved baseline/suppression) is required before the gate can go green —
subject also to the t6 push-preview audit and t8 hygiene check, and Priyanshu's approval.
