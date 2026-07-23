# Progressive Sentence Mastery — Handoff

This is the complete handoff for the mastery mode in the native Android app. It
covers why the feature exists, what shipped in Milestone 1, how the code is put
together, what is real versus mocked, the exact points where real providers slot
in, the decisions that gate that work, and the roadmap beyond Milestone 1.

Everything here lives under `apps/android-native`, package
`com.polyglotai.android`, in the module `com.polyglotai.android.mastery`.

---

## 1. Why this feature exists

The app is a **language input engine**, not a course. The loop it is built to
support runs outside the app as much as inside it:

1. **Mine** authored text — books, stories, lyrics — for high-reach language:
   the words and patterns you will actually reuse.
2. **Drill** those to automaticity, so production is fast and unconscious.
3. **Recombine** the pieces into your own sentences.
4. **Deploy** with real people, in the real world.

A few principles follow from that, and they shape every design choice below:

- **Sentences are disposable carriers.** A lesson sentence is a vehicle for the
  words and patterns inside it. Once those are automatic, the sentence has done
  its job. Nothing in the model treats a sentence as precious.
- **Correctness is context-specific.** Grading checks the *exact* word that was
  authored, not a synonym — the mental model is reciting lyrics or poetry, not
  free composition. "Close enough" is a different skill and belongs to a
  different mode.
- **Friction is the point.** A miss on the full-sentence take restarts the
  build. The strict restart (Mode C) is the default precisely because the
  struggle is what moves a pattern from recognized to automatic.
- **Grammar by absorption.** No rule tables. Comprehension checks confirm
  meaning per unit; the grammar is learned by producing correct sentences
  repeatedly.
- **Content fidelity matters.** When lessons are generated from authored source
  text, the source must be faithful. The agreed bar for OCR'd/ingested text is a
  two-AI cross-check; that lives upstream of this module, but it is why the
  content models keep `displayText`/`spokenText` explicit and authored.

Reading is the daily driver in the broader plan; a dedicated reading mode is a
separate future workstream, not part of this document. The script/writing
approach (reading-first, character-formation-from-memory as a recognition aid,
handwriting offloaded to paper, writing rationed by script size, forgiving on
execution but strict on identity) is likewise future work — noted in §8 so it
isn't lost.

---

## 2. What the mastery mode does

**Progressive Sentence Mastery**: a sentence is learned one unit at a time, then
produced whole in a single take, with a strict restart on a miss.

```
For each sentence in the lesson:
  For each unit (word / phrase) in order:
    play target audio  →  learner records  →  grade
      pass  →  (optional comprehension check)  →  add unit to the assembled sentence
      fail  →  retry the same unit (or manual override to accept)
  When every unit is assembled:
    play the whole sentence  →  learner records the whole thing in one take  →  grade
      pass  →  advance to the next sentence
      fail  →  restart per the selected mode (A / B / C)
When every sentence passes:
  lesson complete
```

Reachable from the dashboard via the **"Sentence Mastery"** card.

---

## 3. Architecture

The core idea: a **pure state machine** with no Android, provider, or IO
dependencies, surrounded by thin adapters that perform effects and feed results
back in as events. That separation is what makes the whole flow testable without
a microphone, a network, or provider keys (and it is what CI actually verifies).

```
            ┌─────────────────────────────────────────────┐
   events   │              MasteryEngine (pure)            │  new session
  ────────► │   reduce(session, event) -> session          │ ───────────►
            │   no Android · no IO · no providers           │
            └─────────────────────────────────────────────┘
                    ▲                         │
                    │ events                  │ phase
                    │                         ▼
            ┌─────────────────────────────────────────────┐
            │         MasteryScreen (Compose UI)           │
            │  interprets each phase → performs effects    │
            └───────┬───────────────┬───────────────┬──────┘
                    │               │               │
             VoicePlayback    PronunciationGrader   MasteryAudioRecorder
             (seam, mocked)   (seam, mocked)        (real MediaRecorder)
                    │               │
             [OpenAI TTS later]  [Azure PA later]        → Room: mastery_attempts
```

### The state machine (`engine/MasteryEngine.kt`)

- `MasterySession` — immutable snapshot: lesson, restart mode, indices, how many
  units are assembled, current phase, last result, counters. All derived values
  (`currentSentence`, `currentUnit`, `assembledText`, `isLastSentence`,
  `totalUnits`) are computed, so there is one source of truth.
- `MasteryPhase` — the formal states (unit presentation/audio/waiting/grading/
  result/comprehension, sentence ready/audio/waiting/grading/pass/fail,
  lesson complete, service/environment error, paused).
- `MasteryEvent` — everything the UI feeds back: `PlayAudio`, `AudioFinished`,
  `RecordingCaptured`, `UnitGraded(result)`, `ComprehensionAnswered(correct)`,
  `SentenceGraded(result)`, `OverrideAccepted`, `Advance`, `Retry`,
  `SelectMode(mode)`, `Pause`, `Resume`.
- `MasteryEngine.reduce(session, event)` — a total function: a `when` over the
  phase, then over the event. Unhandled event in a phase returns the session
  unchanged, so stray events can never corrupt state.

Key behaviors encoded there:
- A passed unit runs its comprehension check if it has one; a **wrong** answer
  repeats the unit and does **not** add it.
- Environment/service errors move to their own phases and are **not** counted as
  learner failures — retry re-enters the correct step.
- Manual override accepts a failed unit/sentence and advances (logged).
- Restart modes A/B/C (see §5).
- Pause stores the prior phase; resume restores it.

### The seams (`grade/Grading.kt`, `voice/VoicePlayback.kt`)

The engine and UI depend only on **normalized types**, never a provider's native
shape:

- `PronunciationGrader.gradeUnit(...)` / `gradeSentence(...)` → `PronunciationResult`
  (`status`, `recognizedText`, `overallScore`, `confidence`, per-unit
  `unitResults`, `errorTypes`, `providerMetadata`). `GradeStatus` is one of
  `PASS / FAIL / UNCERTAIN / ENVIRONMENT_ERROR / SERVICE_ERROR`.
- `VoicePlaybackProvider.play(text, locale, slow)` — suspends until playback
  ends.

Swapping to a real provider = write one adapter that produces a
`PronunciationResult` (or plays audio) and change one line in `MasteryContainer`.
Nothing in the engine, UI, or tests changes.

### Recording, persistence, content, wiring

- `voice/MasteryAudioRecorder.kt` — real capture via `MediaRecorder` (context
  ctor on API ≥31, no-arg below), records AAC/MP4 to `cacheDir`, returns the
  file. Runtime `RECORD_AUDIO` is requested by the screen; the permission is in
  the manifest.
- `data/MasteryDb.kt` — a **separate** Room database (`polyglotai_mastery.db`,
  table `mastery_attempts`). Independent from the review-flashcards DB by design:
  the mastery mode is its own module and shouldn't force a migration on the
  existing DB. Every unit and sentence attempt is logged with its normalized
  result and any override — this is the raw data grading calibration will use.
- `data/MasteryContentRepository.kt` — loads authored lessons from
  `assets/mastery/*.json` (`ignoreUnknownKeys = true`).
- `model/MasteryModels.kt` — pure `@Serializable` content models
  (`MasteryLesson`, `MasterySentence`, `LearningUnit`, `ComprehensionCheck`,
  `GradingProfile`, enums). Pure Kotlin so the engine tests run on the JVM.
- `MasteryContainer.kt` — manual DI for the module. Holds the DB, content repo,
  engine, the two seams (mock impls today), the default `GradingProfile`, and
  `logAttempt(...)`. Constructed once in `AppContainer` as `mastery`.
- `ui/MasteryScreen.kt` — loads the first bundled lesson, then `MasteryRunner`
  drives the engine: auto-plays audio on the audio phases, records on the
  waiting phases, grades through the seam, logs, and dispatches events back. A
  "simulate grade" control (Pass/Fail/Uncertain/Noise) lets you walk every
  branch without real grading.

---

## 4. What's real vs mocked

| Area | Status in Milestone 1 |
|------|-----------------------|
| State machine (full loop, restart modes, override, errors, pause) | **Real**, unit-tested |
| UI for the whole loop | **Real** |
| Microphone capture | **Real** (`MediaRecorder`) |
| Attempt logging to Room | **Real** |
| Bundled lesson content | **Real** (one pt-BR lesson) |
| Pronunciation grading | **Mocked** — `MockPronunciationGrader`, outcome chosen in-UI |
| Target audio | **Mocked** — `MockVoicePlaybackProvider`, timed delay, no sound |

The two mocks are the only stand-ins. Both are single-line swaps in
`MasteryContainer.kt` plus a new adapter class.

| Seam | Interface | Mock (now) | Real (later) |
|------|-----------|------------|--------------|
| Grading | `grade.PronunciationGrader` | `MockPronunciationGrader` | Azure Pronunciation Assessment adapter via the backend gateway |
| Audio | `voice.VoicePlaybackProvider` | `MockVoicePlaybackProvider` | OpenAI TTS adapter (needs a new proxy endpoint) |

---

## 5. Restart modes (the friction dial)

Selectable per session; **C is the default**.

- **A — Repeat sentence:** keep the whole assembled sentence, re-attempt the
  one-take. Lowest friction.
- **B — Rebuild segment:** rebuild from the last authored `segmentStart` (a unit
  index marking a clause boundary). If a sentence has no segments, B behaves like
  C. Middle ground.
- **C — Rebuild full:** wipe and rebuild from the first unit. The strict default,
  and the one the philosophy in §1 is built around.

`segmentStarts` are authored per sentence in the lesson JSON.

---

## 6. Decision points before the real providers go in

These are the human decisions that gate Milestones 2–3. None are code problems.

1. **Azure account + keys (grading).** Pronunciation Assessment is confirmed for
   the target languages: pt-BR, ja-JP, ru-RU, ar-EG/ar-SA, fr-FR. (Prosody
   scoring is en-US only; we don't need it.) Cost is roughly **$1–1.32 per hour**
   of assessed audio. Keys live on the **backend gateway**, never in the app —
   the app calls the gateway, which holds the Azure credentials. Needed: an Azure
   Speech resource and keys placed on the gateway.
2. **TTS proxy endpoint (audio).** The current proxy does chat + transcribe only.
   OpenAI TTS needs a **new endpoint added to the gateway** before
   `VoicePlaybackProvider`'s real adapter can call it. Until then, placeholder
   audio keeps the loop's timing correct.
3. **Grading thresholds / calibration.** `model/GradingProfile.kt` holds
   per-language thresholds (unit accuracy, sentence accuracy, completeness,
   insertion/omission tolerance, uncertain-confidence cutoff) as plain data. The
   mock ignores most of them; the Azure adapter will honor them. Real numbers
   come from a calibration pass against actual recordings — the `mastery_attempts`
   log is there to feed exactly that.

---

## 7. Build, test, publish

CI-only builds — there is no device in CI, so CI **compiles** and **runs the
engine unit tests**; it cannot exercise the app on a screen or a microphone.

- **`.github/workflows/android-native.yml`** (`workflow_dispatch`): runs
  `gradle testDebugUnitTest` (the 11 engine tests gate the build) then
  `gradle assembleDebug`, and uploads the debug APK as a run artifact.
- **`.github/workflows/android-publish-release.yml`** (`workflow_dispatch`,
  input `run_id`): downloads a build run's APK artifact and republishes it as a
  **GitHub Release asset** (Release assets come from github.com's CDN, so they're
  reliably downloadable for sideloading). Tag is `android-<run_id>`, marked
  prerelease. This is the step that gets an installable APK onto a phone; it is
  intentionally a separate, explicit action.

To verify a change: dispatch `android-native.yml` on the branch, confirm the run
is green (tests + APK), then — if you want it on a device — dispatch
`android-publish-release.yml` with that run's `run_id`.

The unit tests (`app/src/test/.../MasteryEngineTest.kt`, 11 cases) cover: the
full happy path, unit-fail-repeats, wrong-comprehension-repeats, Mode C wipe,
Mode A keep, Mode B last-segment rebuild, override-accepts, environment-error-
not-counted, pause/resume, and mode selection.

---

## 8. Roadmap beyond Milestone 1

**In this module, next:**
- **Real target audio.** Add the TTS proxy endpoint (§6.2), write an OpenAI TTS
  `VoicePlaybackProvider`, cache generated audio per (text, locale). Swap it in
  `MasteryContainer`.
- **Real grading.** Stand up the Azure resource + gateway route (§6.1), write the
  Azure `PronunciationGrader` adapter mapping Azure's response to
  `PronunciationResult`, honor `GradingProfile`. Swap it in `MasteryContainer`.
- **Calibration tooling.** A small surface to review `mastery_attempts` and tune
  per-language `GradingProfile` thresholds against real recordings.
- **Recording refinements.** Voice-activity detection, silence trimming, noise
  detection to raise `ENVIRONMENT_ERROR` automatically (today it's a simulate
  button). The engine already handles the phase; only capture-side detection is
  missing.
- **Content authoring & scheduling.** A pipeline to turn authored source text
  into lessons (with the two-AI fidelity check from §1), and spaced-repetition
  scheduling to bring mastered sentences back for maintenance.

**Separate workstreams (documented so they aren't lost, not part of this
module):**
- **Reading mode** — the intended daily driver: read books, sites, chats,
  signage, with tap-to-gloss and mining back into mastery.
- **Script / writing** — reading-first; character formation from memory as a
  recognition aid; handwriting practiced on paper, not in-app; writing rationed
  by script size; forgiving on execution, strict on identity. "Implement it, and
  cut it later if it doesn't earn its place."
- **Windows / Ubuntu clients** — later. The phone app is the priority; this repo
  is Kotlin/Compose for the phone.

---

## 9. File index

New (`app/src/main/java/com/polyglotai/android/mastery/`):
- `engine/MasteryEngine.kt` — pure state machine (phases, events, reducer).
- `model/MasteryModels.kt` — serializable content models + `GradingProfile`.
- `grade/Grading.kt` — `GradeStatus`, `PronunciationResult`, `PronunciationGrader`, `MockPronunciationGrader`.
- `voice/VoicePlayback.kt` — `VoicePlaybackProvider`, `MockVoicePlaybackProvider`.
- `voice/MasteryAudioRecorder.kt` — real `MediaRecorder` capture.
- `data/MasteryDb.kt` — Room entity/DAO/database for `mastery_attempts`.
- `data/MasteryContentRepository.kt` — loads `assets/mastery/*.json`.
- `MasteryContainer.kt` — module DI + `logAttempt`.
- `ui/MasteryScreen.kt` — Compose UI driving the engine.

New (other):
- `app/src/main/assets/mastery/lesson_pt_br_cafe.json` — bundled pt-BR lesson.
- `app/src/test/java/com/polyglotai/android/mastery/MasteryEngineTest.kt` — 11 tests.

Modified:
- `AppContainer.kt` — constructs `mastery`.
- `ui/PolyglotApp.kt` — `Screen.Mastery` route + dashboard "Sentence Mastery" card.
- `app/build.gradle.kts` — junit + coroutines-test for the engine tests.
- `.github/workflows/android-native.yml` — runs `testDebugUnitTest` before the APK build.

---

## 10. Status

Milestone 1 is committed and pushed on `claude/polyglotai-language-packs-phgizp`,
CI-green (tests + APK). Not yet done, pending a decision: publishing an
installable APK (Release asset) and opening a PR.
