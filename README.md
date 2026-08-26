# Game.bot

AI/CV-based Android game automation agent — sees the screen, decides an action, performs it. Built for survival/open-world style mobile games (run, inventory, gather, fight, flee).

## Architecture

```text
Screen (MediaProjection) --> DetectorEngine (TFLite) --> GameState
                                                              |
                                                              v
                                                        StateMachine (FSM)
                                                              |
                                                              v
                                              ActionExecutor --> AccessibilityService (taps/swipes)
```

Layers, each independently swappable:

- **capture/** — `ScreenCaptureService`: continuous screen frames via `MediaProjection`.
- **perception/** — `DetectorEngine` (TFLite model) + `GameState` (structured snapshot: health, enemies, resources, inventory).
- **decision/** — `StateMachine` + `BotState` implementations (`ExploreState`, `FightState`, `FleeState`, `LootDropState`). Rule-based FSM to start; swap in a trained RL policy later without touching other layers.
- **action/** — `ActionExecutor`: named actions (`openInventory()`, `pressRun()`, `moveDirection()`) that translate to gestures.
- **accessibility/** — `GameAccessibilityService`: the only layer that actually touches the screen, via `dispatchGesture()`. No root required.
- **orchestrator/** — `AgentLoop`: ties it all together in a capture → detect → decide → act loop.

## Status

Skeleton stage — key pieces are `TODO`:

- [ ] `DetectorEngine.detect()` — load a real `.tflite` model into `assets/models/detector.tflite`, implement preprocessing + output parsing
- [ ] `ScreenCaptureService` — `Image` → `Bitmap` conversion in the `onImageAvailable` callback
- [ ] `ActionExecutor` — calibrate real screen coordinates for target game's UI
- [ ] `FightState` / `FleeState` — actual aim/attack/movement logic
- [ ] Health/hunger reading — crop fixed HUD region, classify or OCR

## Setup

1. Enable the Accessibility Service (Settings → Accessibility → Game.bot) after installing.
2. Grant screen capture permission when prompted (`MediaProjectionManager`).
3. Calibrate coordinates for your target game via `ActionExecutor.calibrate(...)`.
4. Drop a trained `.tflite` detection model into `app/src/main/assets/models/`.

## Notes

- Runs entirely on-device — no server, no root.
- Actions include small randomized delays (`ActionExecutor.humanDelay()`) to avoid perfectly robotic timing.
- **Policy risk**: most games' ToS prohibit automation/bots, especially online multiplayer ones — anti-cheat systems can and do detect and ban this kind of tooling. Best suited for offline/single-player use or personal experimentation.
