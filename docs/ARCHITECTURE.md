# PixAvatar Architecture

## Runtime pipeline

```text
                 ┌──────────────┐
Voice / Touch →  │ Input Layer  │
                 └──────┬───────┘
                        ↓
                 ┌──────────────┐
                 │ Local LLM    │
                 │ 0.5B–2B      │
                 └──────┬───────┘
                        ↓
             ┌──────────────────────┐
             │ Response Controller  │
             │ text + emotion +     │
             │ expression + action  │
             └──────┬───────────────┘
                    ↓
          ┌─────────┴─────────┐
          ↓                   ↓
    ┌───────────┐       ┌────────────┐
    │    TTS    │       │   Avatar   │
    │   audio   │       │  renderer  │
    └─────┬─────┘       └─────┬──────┘
          │                   │
          └────────┬──────────┘
                   ↓
              Lip Sync
                   ↓
              2D Avatar
```

## Avatar state machine

Initial states:

- IDLE
- LISTENING
- THINKING
- SPEAKING
- HAPPY
- CONFUSED
- SURPRISED
- ANGRY
- SLEEPING

The renderer should remain independent from the LLM backend so model/runtime changes do not require rewriting the avatar system.

## Hardware strategy

### Pixel 6

Use as the primary development target. Start with a 1B-class 4-bit model and measure sustained inference, memory, temperature, TTS latency, and animation frame rate.

### Galaxy A16 5G SM-A166U

4 GB RAM is the constrained target. Keep the renderer lightweight and support a smaller model profile. The app should avoid loading unnecessary assets and should release model resources when idle where practical.

## Model abstraction

The app should expose a simple inference interface:

```text
LocalModel
  load()
  unload()
  generate(prompt)
  cancel()
  memoryProfile()
```

Possible backends can be added later without coupling the avatar to a specific inference engine.
