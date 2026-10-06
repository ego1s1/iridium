# Pre-push verification gate — main

Three report-only runs audited the gate. Runs 1–2 (t7 by Wren, t9 by Kit)
audited the same commit (`dab2ba3`) and agree; run 3 (t12 by Kit) re-ran the
full gate stepwise on current main after the t11 detekt fix landed. This single
file keeps all three runs' evidence with no duplicated or conflicting sections.

- Commits under test:
  - Runs 1–2: `dab2ba3` (Merge agentcraft/wren/t8-push-hygiene-secrets into main)
  - Run 3: `df3a92a` (current main tip at re-run; adds t11 detekt refactor
    `beae754`, t7-report merge `9761b11`, and t16 device report `7048e9a`.
    Source delta vs `dab2ba3` is the t11 `ReaderChrome.kt` refactor plus docs.)
- Mode: report-only. This task definition allows editing only this report file;
  no source fixes were applied here (the detekt fix came via t11 on main).
- Environment (all runs): `JAVA_HOME=/opt/homebrew/opt/openjdk@17` (JDK 17.0.20.1),
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

## Run 3 — t12 (Kit, 2026-10-07 UTC, branch `agentcraft/kit/t12-consolidate-pre-push`)

Full gate, stepwise for per-step results, on current main `df3a92a`
(fast-forward merged `main` at start; tree clean). Numbers below are from this
run's own Gradle output and a post-run count over `**/build/test-results/**/*.xml`.

| # | Command | Result |
|---|---------|--------|
| 0 | `./gradlew --version` (env sanity) | PASS — Gradle 8.9, JVM 17.0.20.1 |
| 1 | `./gradlew :app:assembleDebug --console=plain -q` | PASS — exit 0, no output |
| 2 | `./gradlew :app:lintDebug --console=plain` | PASS — BUILD SUCCESSFUL in 22s (623 tasks: 26 executed, 9 from cache, 588 up-to-date); `lint-results-debug.txt`: 0 errors, 94 warnings |
| 3 | `./gradlew test --console=plain` | PASS — BUILD SUCCESSFUL in 31s (1101 tasks: 36 executed, 4 from cache, 1061 up-to-date); 31 suites, **178 tests, 0 failures, 0 errors, 0 skipped** |
| 4 | `./gradlew detekt --console=plain` | PASS — BUILD SUCCESSFUL in 1s (26 tasks: 1 executed, 1 from cache, 24 up-to-date); plus `./gradlew :feature:reader:impl:detekt --rerun-tasks` → BUILD SUCCESSFUL in 12s (6 executed), confirming the former blocker module passes on fresh execution, not just cache |

Nothing skipped: the full gate ran as specified, no lightweight substitution.

## History: the detekt blocker seen by runs 1–2 (resolved by t11)

Runs 1–2 both failed on the same single pre-existing finding on clean main:

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

One weighted issue; all other modules' `detekt` tasks passed. This was left
unfixed by the audit tasks and resolved separately by t11 (`beae754`: refactored
`chromeZoneForTap` into per-nav-mode resolvers `zoneForThirds` / `zoneForLShape` /
`zoneForKindlish` / `zoneForEdge` / `zoneForSides` plus a `mirrorTapZone` helper,
behavior identical, `ReaderChromeTest` green). Run 3 above confirms the full gate
— including a forced `--rerun-tasks` re-execution of the formerly failing module —
is green on main with that fix.

## Verdict: READY (gate green on current main)

`assembleDebug`, `lintDebug`, `test` (31 suites / 178 tests, 0 failures), and
`detekt` all pass on `df3a92a`, so the full gate is green. Pushing still needs
Priyanshu's approval (and remains subject to the t6 push-preview audit and t8
hygiene check staying valid).
