# Mission: KMP Music Audio Playback Architecture

## Why
Ship a real Kotlin Multiplatform music player whose Android, iOS, and Desktop versions can play local music reliably while keeping shared UI, playback state, queue, favorites, and navigation cleanly separated from platform playback engines.

## Success looks like
- Choose a playback framework for Android, iOS, macOS Desktop, and Windows Desktop with clear tradeoffs.
- Design a small shared playback boundary that platform engines can implement without leaking Android, Apple, or Windows APIs into `commonMain`.
- Know which features belong in the platform layer: background playback, lock-screen or system media controls, audio focus or interruptions, local file permissions, and codec support.
- Explain why the current `InMemoryPlaybackRepository` is only a state placeholder, not the final audio engine.

## Constraints
- Prioritize the real KMP app over the high-fidelity prototype.
- Keep `core / domain / data / feature` boundaries intact.
- Prefer root-cause architecture choices over page-level or platform-specific patches.
- Use official or high-trust primary documentation as the learning base.

## Out of scope
- Online streaming service integration, DRM, lyrics synchronization, recommendation algorithms, and cloud sync implementation.
- Replacing the current UI or building the playback engine in this teaching step.
