# File Placement Guide

Extract this zip's `game-bot/` folder contents directly into your repo root (`Game.bot/`). Structure and purpose of each file below.

```text
Game.bot/                                  ← repo root
├── .gitignore                             ← repo root
├── README.md                              ← repo root
├── app/
│   ├── build.gradle.kts                   ← app/
│   └── src/main/
│       ├── AndroidManifest.xml            ← app/src/main/
│       ├── assets/
│       │   └── models/
│       │       └── detector.tflite        ← YOU add this (trained/pretrained model, not included)
│       ├── res/
│       │   └── xml/
│       │       └── accessibility_service_config.xml
│       └── java/com/pranav/gamebot/
│           ├── MainActivity.kt                    ← permission requests, starts AgentLoop
│           ├── capture/
│           │   └── ScreenCaptureService.kt        ← screen recording (MediaProjection)
│           ├── perception/
│           │   ├── DetectorEngine.kt              ← runs the .tflite model
│           │   └── GameState.kt                   ← structured "what bot sees" object
│           ├── decision/
│           │   ├── BotState.kt                    ← FSM interface
│           │   ├── StateMachine.kt                ← holds/advances current state
│           │   └── states/
│           │       └── ExploreState.kt            ← Explore/Fight/Flee/LootDrop states
│           ├── action/
│           │   └── ActionExecutor.kt              ← named actions (openInventory, pressRun, etc.)
│           ├── accessibility/
│           │   └── GameAccessibilityService.kt    ← actually taps/swipes the screen
│           └── orchestrator/
│               └── AgentLoop.kt                    ← ties capture→detect→decide→act together
```

## Quick reference — "I need to change X, which file?"

| You want to... | Edit this file |
|---|---|
| Change what button coordinates the bot taps | `action/ActionExecutor.kt` |
| Change bot behavior/logic (when to fight vs flee) | `decision/states/ExploreState.kt` |
| Add a new bot state (e.g. "CraftState") | new file in `decision/states/`, wire into `ExploreState.kt` transitions |
| Change how the screen is captured | `capture/ScreenCaptureService.kt` |
| Swap or update the detection model | `perception/DetectorEngine.kt` + replace file in `assets/models/` |
| Change what counts as a "game state" (add new fields like stamina) | `perception/GameState.kt` |
| Add a new low-level gesture (e.g. pinch, double-tap) | `accessibility/GameAccessibilityService.kt` |
| Change the main loop timing/threading | `orchestrator/AgentLoop.kt` |
| Add permissions or register a new service | `AndroidManifest.xml` |
| Add a new dependency/library | `app/build.gradle.kts` |

## Not included — you provide these

- `app/src/main/assets/models/detector.tflite` — your trained/pretrained detection model
- App icon, launcher theme resources (`res/mipmap`, `res/values/styles.xml`) — manifest references `@mipmap/ic_launcher`, add your own or build will fail on that reference
