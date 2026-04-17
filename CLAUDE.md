# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
# Sync and build (requires Android SDK)
./gradlew assembleDebug

# Install to connected device/emulator
./gradlew installDebug

# Run all unit tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.paceguard.detection.DetectionRulesTest"
```

> The `gradle/wrapper/gradle-wrapper.jar` binary is not committed. Open the project in Android Studio and it will generate it, or run `gradle wrapper --gradle-version 8.4`.

## Architecture

**MVVM + cold Flow pipeline.** No Hilt — the ViewModel is instantiated via a manual `ViewModelProvider.Factory` in its companion object.

```
data/model/       — pure domain types (Activity, FlaggedActivity, ScanEvent, Sport)
data/             — ActivityLoader (Gson + assets), DetectionRepository (Flow<ScanEvent>)
detection/        — SportThresholds (constants), DetectionRules (one function per rule)
ui/               — DetectionState (sealed), DetectionViewModel (StateFlow)
ui/dashboard/     — DashboardScreen (LazyColumn, scan button, progress)
ui/detail/        — DetailScreen (flag breakdown + GPS point list)
ui/navigation/    — NavGraph (creates shared ViewModel, two destinations)
ui/theme/         — PaceGuardTheme (Material3 lightColorScheme)
```

## Key Design Decisions

**`ScanEvent` as the pipeline type.** `DetectionRepository.scan()` emits `ScanEvent` (not raw `FlaggedActivity`) so clean activities still advance the progress counter. `FlagFound` and `ActivityScanned` are both collected in the ViewModel.

**Cold Flow.** `scan()` is a plain `flow { }` builder — a new scan runs each time it is collected. Calling `startScan()` cancels the previous `Job` and starts a fresh collection. No `shareIn`, no multicasting.

**State updates on `ActivityScanned` only.** Flags are accumulated in a local `mutableListOf` and snapshotted into state only when `ActivityScanned` fires. This batches recompositions to one per activity (~300ms cadence) rather than one per flag.

**Duplicate route check — one direction only.** `checkDuplicateRoute` compares against earlier activities in the list to avoid flagging both sides of a pair.

## Detection Rules

All thresholds live in `detection/SportThresholds.kt`. Rules live in `detection/DetectionRules.kt`.

| Rule | File function | WARN trigger | CRITICAL trigger |
|---|---|---|---|
| Avg speed | `checkAverageSpeed` | > 1× sport max | ≥ 2× sport max |
| Segment speed | `checkSegmentAcceleration` | — | any segment > 3× sport max |
| Duplicate route | `checkDuplicateRoute` | identical GPS sequence | — |

## Mock Data

`app/src/main/assets/activities.json` — 10 activities:

| ID | Athlete | Sport | Expected Flags |
|---|---|---|---|
| act_001 | Alice Chen | RUN | — clean |
| act_002 | Bob Martinez | CYCLE | — clean |
| act_003 | Carol Wright | SWIM | — clean |
| act_004 | Dave Kim | RUN | WARN avg speed (12 m/s) |
| act_005 | Eve Santos | RUN | CRITICAL avg speed (22 m/s) |
| act_006 | Frank Okafor | CYCLE | CRITICAL teleportation (~88 m/s segment) |
| act_007 | Grace Liu | SWIM | WARN duplicate of act_003 |
| act_008 | Heidi Nakamura | RUN | CRITICAL avg speed + CRITICAL teleportation |
| act_009 | Ivan Petrov | CYCLE | — clean |
| act_010 | Judy Osei | SWIM | — clean |

## Dependencies

- **Kotlin 1.9.22 / AGP 8.3.0 / Gradle 8.4**
- **Compose BOM 2024.02.00** — UI, Material3, Navigation
- **lifecycle-viewmodel-compose 2.7.0** — `viewModel()`, `collectAsStateWithLifecycle`
- **kotlinx-coroutines-android 1.7.3** — Flow, delay, viewModelScope
- **Gson 2.10.1** — JSON parsing from assets
