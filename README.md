# PaceGuard

An Android app that detects fraudulent and impossible GPS activity data — the kind of abuse detection a Trust & Safety team at a fitness platform deals with in production.

## What It Does

PaceGuard loads a set of mock athlete activities and streams them through a fraud detection pipeline, flagging suspicious performances in real time. Three detection rules run against each activity:

- **Average Speed Check** — flags activities where the athlete's overall pace is physically impossible for their sport (e.g. a 22 m/s run)
- **Segment Teleportation Check** — detects GPS spoofing by computing speed between consecutive coordinate pairs; flags segments where movement implies teleportation
- **Duplicate Route Check** — flags activities whose GPS point sequence exactly matches another athlete's, a signal of copied or fabricated data

Each flag is classified as `WARN` or `CRITICAL` with a full explanation of which rule triggered and by how much.

## Why I Built This

This project was built to demonstrate Android development skills in a domain directly relevant to Trust & Safety engineering — detecting platform abuse, modeling fraud signals, and building moderation tooling. The detection logic mirrors real problems platforms like Strava face with GPS fraud and performance manipulation.

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material3 |
| Architecture | MVVM (ViewModel + StateFlow) |
| Async | Coroutines + cold Flow pipeline |
| Data | Local JSON via Gson, no network |
| Navigation | Jetpack Navigation Compose |

## Architecture

```
ActivityLoader (parses assets/activities.json)
    ↓
DetectionRepository — exposes cold Flow<ScanEvent>
    ↓ delay(300ms) per activity
DetectionViewModel — collects Flow, updates StateFlow<DetectionState>
    ↓
Compose UI — renders Idle / Running / Complete states
```

The pipeline emits two event types: `ActivityScanned` (drives the progress counter for every activity, including clean ones) and `FlagFound` (carries the flagged result). This separation ensures the progress bar advances even when no fraud is detected.

## Screens

**Dashboard** — scan button, live progress counter, flagged activity list color-coded by severity (amber = WARN, red = CRITICAL)

**Detail** — full activity breakdown, severity badge, human-readable explanation of the exact rule that triggered and by how much

## How to Build

1. Clone the repo
2. Open the project root in Android Studio (Hedgehog or newer)
3. Let Gradle sync complete
4. Run on an emulator (API 26+) or physical device

No API keys, no network calls, no external services required.

## Mock Data

Ten seeded activities covering all fraud scenarios: clean baselines for each sport, average speed violations at WARN and CRITICAL thresholds, a teleportation segment, a duplicate route, and a multi-rule violation.
