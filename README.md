# PixAvatar

A lightweight, offline-first Android AI companion designed to run on phones such as the Pixel 6 and low-memory devices like the Galaxy A16 5G.

## Vision

PixAvatar combines a small local language model with a responsive 2D avatar:

- Local LLM conversation
- Text-to-speech
- Speech input
- Lip-sync driven by speech
- Eyes, mouth, expressions, and idle animation
- Avatar state machine controlled by the assistant
- Hardware-aware model selection
- Offline-first operation

## Initial target

**Primary development device:** Google Pixel 6 (8 GB RAM)

**Low-memory target:** Samsung Galaxy A16 5G SM-A166U (4 GB RAM)

## Architecture

```text
Input → Local LLM → Response/Avatar State → TTS → Lip Sync
                    ↓
                 2D Avatar
```

The first implementation intentionally avoids neural 3D rendering. The goal is a convincing, animated 2D companion with a small memory footprint.

## Status

🚧 Initial project scaffold.
