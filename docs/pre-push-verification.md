# Pre-push Verification Gate — Report (t7)

- **Date:** 2026-10-06 23:25 IST
- **Branch:** `agentcraft/wren/t7-pre-push-verification`
- **Commit under test:** `dab2ba3` (main tip — fast-forward merged at start of this task, no divergence)
- **Worktree:** `/Users/priyanshu/.agentcraft/opencode/worktrees/iridium/wren-t7`
- **Environment:** `JAVA_HOME=/opt/homebrew/opt/openjdk@17` (OpenJDK 17.0.20.1), `ANDROID_HOME=~/Library/Android/sdk`
- **Mode:** report-only — no source files modified, no fixes applied, nothing pushed.

## Commands run

Each gate step was run as a separate invocation so per-step pass/fail is unambiguous
(single combined run `./gradlew :app:assembleDebug :app:lintDebug test detekt` was also
executed first and failed at the detekt step):

1. `./gradlew :app:assembleDebug --console=plain -q`
2. `./gradlew :app:lintDebug --console=plain -q`
3. `./gradlew test --console=plain -q`
4. `./gradlew detekt --console=plain`

## Results

| Step | Result | Detail |
|------|--------|--------|
| `:app:assembleDebug` | **PASS** (exit 0) | Debug APK assembles cleanly. |
| `:app:lintDebug` | **PASS** (exit 0) | 0 errors, 94 warnings (e.g. `UnusedAttribute` for `enableOnBackInvokedCallback`, minSdk 24 vs API 33). Full list: `app/build/reports/lint-results-debug.txt`. |
| `test` (all modules, unit) | **PASS** (exit 0) | 31 suites, **178 tests, 0 failures, 0 errors, 0 skipped**. |
| `detekt` (all modules) | **FAIL** (exit 1) | 1 weighted issue — see below. All other modules `UP-TO-DATE` / `NO-SOURCE`. |

## Failing tests

None — every unit test passed, so there are no failing test names to list.

## detekt failure (the single gate blocker)

- Failing task: `:feature:reader:impl:detekt` → `Analysis failed with 1 weighted issues.`
- Rule: `CyclomaticComplexMethod`
- Location: `feature/reader/impl/src/main/kotlin/com/iridium/feature/reader/impl/ReaderChrome.kt:183:5`
- Detail: `The function chromeZoneForTap appears to be too complex based on Cyclomatic Complexity (complexity: 35). Defined complexity threshold for methods is set to '30'`
- Per task instructions this was **not fixed** — left for a follow-up task/owner to refactor or suppress.

## Verdict

**NOT READY for push** — the verification gate is red on `detekt` (`:feature:reader:impl`, 1 complexity violation).
`assembleDebug`, `lintDebug`, and all 178 unit tests are green, so once the single
detekt finding is resolved (and the gate re-run), the tree should be pushable —
subject also to the t6 push-preview audit and t8 hygiene check, and Priyanshu's approval.
