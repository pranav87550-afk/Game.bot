# File Placement Guide

The zip's `game-bot/` folder is intended to be extracted directly into the `Game.bot/` repository root. The structure and purpose of each file are below.

```text
Game.bot/
├── .gitignore
├── README.md
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/models/
│       │   └── detector.tflite        ← YOU add this (not included)
│       ├── res/xml/
│       │   └── accessibility_service_config.xml
│       └── java/com/pranav/gamebot/
│           ├── capture/ScreenCaptureService.kt
│           ├── perception/DetectorEngine.kt
│           ├── perception/GameState.kt
│           ├── decision/BotState.kt
│           ├── decision/StateMachine.kt
│           ├── decision/states/ExploreState.kt
│           ├── action/ActionExecutor.kt
│           ├── accessibility/GameAccessibilityService.kt
│           └── orchestrator/AgentLoop.kt
```

## Quick reference

| You want to... | Edit this file |
|---|---|
| Change button coordinates | `action/ActionExecutor.kt` |
| Change bot behavior | `decision/states/ExploreState.kt` |
| Add a new bot state | new file in `decision/states/` |
| Change screen capture | `capture/ScreenCaptureService.kt` |
| Swap detection model | `perception/DetectorEngine.kt` + model file |
| Change game-state fields | `perception/GameState.kt` |
| Add low-level gestures | `accessibility/GameAccessibilityService.kt` |
| Change loop timing/threading | `orchestrator/AgentLoop.kt` |
| Add permissions/services | `AndroidManifest.xml` |
| Add dependencies | `app/build.gradle.kts` |

## Not included

- `app/src/main/assets/models/detector.tflite` — trained/pretrained detection model.
- App icon and launcher theme resources referenced by the manifest.
- `MainActivity.kt` — the entry point still needs to be built.
