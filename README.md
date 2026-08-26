# Game.bot

AI/CV-based Android game automation agent — sees the screen, decides an action, performs it. Built for survival/open-world style mobile games (run, inventory, gather, fight, flee).

## Architecture

```
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

- [x] `ScreenCaptureService` — `Image` → `Bitmap` conversion done (handles rowStride padding), throttled to 10fps, exposed via bound service
- [x] `MainActivity` — requests accessibility + screen capture permissions, starts `AgentLoop`
- [x] `DetectorEngine.detect()` — TFLite inference wired up (preprocessing, SSD-style output parsing, label map loading). Still needs: a real `.tflite` file at `assets/models/detector.tflite` — interpreter safely no-ops (returns empty detections) until one is present
- [x] `FightState` / `FleeState` — attacks/flees using real bearing calculation (`ActionExecutor.bearingTo/bearingAwayFrom`) toward/away from the detected enemy position
- [ ] `ActionExecutor` — calibrate real screen coordinates for target game's UI (inventory button, run button, joystick center, player screen position)
- [ ] Health/hunger reading — crop fixed HUD region, classify or OCR
- [ ] Launcher icon + theme resources (`res/mipmap`, `res/values/styles.xml`)

## Setup

1. Enable the Accessibility Service (Settings → Accessibility → Game.bot) after installing.
2. Grant screen capture permission when prompted (`MediaProjectionManager`).
3. Calibrate coordinates for your target game via `ActionExecutor.calibrate(...)`.
4. Drop a trained `.tflite` detection model into `app/src/main/assets/models/`.

## Notes

- Runs entirely on-device — no server, no root.
- Actions include small randomized delays (`ActionExecutor.humanDelay()`) to avoid perfectly robotic timing.
- **Policy risk**: most games' ToS prohibit automation/bots, especially online multiplayer ones — anti-cheat systems can and do detect and ban this kind of tooling. Best suited for offline/single-player use or personal experimentation.
