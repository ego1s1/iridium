# Performance baseline

Measurement harness and the numbers every later performance change is judged
against. Numbers are only meaningful on the same device, with the same build
type, and with the same definition of "done".

## What the harness measures

| Benchmark | File | Metric | Definition |
|---|---|---|---|
| Cold start | `benchmark/.../StartupBenchmark.kt` | `StartupTimingMetric` | Process cold, measured until the app's own `reportFullyDrawn()` |
| Library fling | `benchmark/.../LibraryScrollBenchmark.kt` | `FrameTimingMetric` | Three downward flings over the book grid, then one upward |

`reportFullyDrawn()` fires from `IridiumApp` once the onboarding gate resolves
and the shell is composed with the user's theme and destination. It is **not**
the first frame: that frame is the loading surface, which is cheap and would
flatter the numbers.

Run against a release-like build (`benchmark` build type: minified, shrunk,
debug-signed, `profileable`).

## How to run

```bash
# One benchmark at a time, on an idle device, nothing else running.
./gradlew :benchmark:connectedPlayBenchmarkAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.iridium.benchmark.StartupBenchmark

./gradlew :benchmark:connectedPlayBenchmarkAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.iridium.benchmark.LibraryScrollBenchmark
```

Results are written to
`benchmark/build/outputs/connected_android_test_additional_output/` as JSON
plus a readable HTML/text summary.

The library fling benchmark is skipped when the library has no books: it needs
a real shelf to scroll.

Two compilation modes are measured for startup on purpose. A change that only
improves the profile (`CompilationMode.Partial`) is a different fix from one
that improves the code itself (`CompilationMode.None`), and mixing them hides
regressions.

## Baseline to record before changing anything

Fill this in on the reference device (Pixel), not an emulator.

| Metric | Mode | Before | After |
|---|---|---|---|
| cold start median | baseline profile | _pending_ | |
| cold start median | no compilation | _pending_ | |
| cold start P90 | baseline profile | _pending_ | |
| library fling frame P90 | baseline profile | _pending_ | |
| library fling frame P99 / missed | baseline profile | _pending_ | |

## Static facts measured from the build (no device needed)

| Fact | Value | How |
|---|---|---|
| App-authored baseline profile entries | **0** | `grep -c 'com.iridium' app/build/intermediates/merged_art_profile/release/mergeReleaseArtProfile/baseline-prof.txt` |
| Merged baseline profile lines (all from dependencies) | 6,039 | same file, `wc -l` |
| Release APK size | ~13 MB | `app/build/outputs/apk/*/release/*.apk` |
| `classes.dex` | 8.4 MB | release APK |
| Readium classes kept by R8 | 1,544 | release `mapping.txt` |
| Variable font shipped | 3.96 MiB | `core/designsystem/src/main/res/font/google_sans_flex.ttf` |

The app has no baseline or startup profile of its own, so every launch-critical
frame from `IridiumApp` through the first library layout is unprofiled. That is
the single largest cold-start item on the list and is tracked separately.

## Trace markers

Long-running paths expose `android.os.Trace` sections so a Perfetto/systrace
capture lines up with the metrics:

| Section | Where | Covers |
|---|---|---|
| `iri.library.scan` | `LibraryViewModel.runScan` | Whole filesystem pass |
| `iri.reader.open` | `ReaderViewModel.openBook` | EPUB parse |
| `iri.reader.positions` | `ReaderViewModel` secondary load | Position table walk |
| `iri.library.query` | `OfflineFirstLibraryObserver` | Entity-to-model mapping and text matching |
| `iri.search.content` | `ContentSearchUseCase` | Full-text query and result grouping |

Capture with:

```bash
./gradlew :benchmark:connectedPlayBenchmarkAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=Macrobenchmark
```

and pull the trace from `benchmark/build/outputs/.../perfetto/`.
