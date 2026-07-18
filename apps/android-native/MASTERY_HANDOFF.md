# Progressive Sentence Mastery — Milestone 1

This is the structural foundation of the mastery mode in the native Android app
(`com.polyglotai.android.mastery`). The full learning loop runs end to end today:
word-by-word build → one-take full-sentence production → strict restart on a miss.
Speech grading and target audio are behind seams and currently mocked, so every
branch is reachable without a microphone, a network, or provider keys.

## What is real

- **The state machine.** `engine/MasteryEngine.kt` is a pure `reduce(session, event)`
  reducer with no Android, provider, or IO dependencies. It implements the unit loop,
  comprehension gate, full-sentence one-take, all three restart modes, manual override,
  environment/service errors (not counted as learner failure), and pause/resume. It is
  covered by 11 JVM unit tests (`app/src/test/.../MasteryEngineTest.kt`) that CI runs
  before it builds the APK.
- **The UI.** `ui/MasteryScreen.kt` drives the engine, plays audio through the voice
  seam, records real microphone audio (`voice/MasteryAudioRecorder.kt`, MediaRecorder,
  runtime RECORD_AUDIO permission), and logs every attempt to a dedicated Room database
  (`data/MasteryDb.kt`, `mastery_attempts`). A "simulate grade" control picks the
  outcome so you can walk any path.
- **Content.** Lessons are authored JSON under `assets/mastery/`. One ships:
  `lesson_pt_br_cafe.json` (pt-BR, three sentences, comprehension checks, segment
  starts). `data/MasteryContentRepository.kt` loads them.
- **Navigation.** Reachable from the dashboard via the "Sentence Mastery" card.

## What is mocked (the seams)

Both are single-line swaps in `MasteryContainer.kt` plus a new adapter class — nothing
downstream changes, because the engine and UI depend only on the normalized types.

| Seam | Interface | Milestone-1 impl | Real impl (later) |
|------|-----------|------------------|-------------------|
| Pronunciation grading | `grade.PronunciationGrader` → `PronunciationResult` | `MockPronunciationGrader` (UI picks Pass/Fail/Uncertain/Noise) | Azure Pronunciation Assessment adapter via the backend gateway |
| Target audio | `voice.VoicePlaybackProvider` | `MockVoicePlaybackProvider` (timed delay, no sound) | OpenAI TTS adapter (needs a new proxy endpoint) |

## Decision points before the real providers go in

1. **Azure account + keys.** Pronunciation Assessment confirmed for pt-BR, ja-JP, ru-RU,
   ar-EG/ar-SA, fr-FR (prosody is en-US only; we do not need it). Keys live on the
   backend gateway, never in the app. Roughly $1–1.32/hr of audio.
2. **TTS proxy endpoint.** The current proxy does chat + transcribe only. OpenAI TTS
   needs a new endpoint added to the gateway before `VoicePlaybackProvider` can call it.
3. **Grading thresholds.** `model/GradingProfile.kt` holds per-language thresholds as
   plain data. The mock ignores most of them; the Azure adapter will honor them. A
   calibration pass against real recordings sets the real numbers.

## Restart modes (spec §7)

- **A — Repeat sentence:** keep the whole assembled sentence, re-attempt the one-take.
- **B — Rebuild segment:** rebuild from the last authored `segmentStart`; falls back to
  full rebuild if the sentence has no segments.
- **C — Rebuild full (default):** wipe and rebuild from the first unit. The strict path.

## What Milestone 1 does not do

No real speech scoring, no real audio, no VAD/silence trimming, no lesson authoring tool,
no spaced-repetition scheduling of mastered sentences. Those are later milestones. CI
compiles the app and runs the engine tests; it cannot exercise the app on a device.
