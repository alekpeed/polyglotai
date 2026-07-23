package com.polyglotai.android.mastery.voice

import kotlinx.coroutines.delay

/**
 * The target-audio seam (spec §3.4). Milestone 1 ships [MockVoicePlaybackProvider], which simulates
 * playback with a short delay so the loop's audio phases behave correctly; Milestone 2/§11 swaps in
 * an OpenAI TTS provider that generates and caches real native audio. The engine never calls this
 * directly — the UI does, then feeds an AudioFinished event back to the engine.
 */
interface VoicePlaybackProvider {
    /** Play [text] in [locale]. [slow] requests a slowed replay. Suspends until playback finishes. */
    suspend fun play(text: String, locale: String, slow: Boolean = false)
}

/** Placeholder playback: no real audio, just a realistic pause so the UI's "playing…" state and the
 *  AudioFinished transition are exercised. Real TTS drops in behind the same interface. */
class MockVoicePlaybackProvider : VoicePlaybackProvider {
    override suspend fun play(text: String, locale: String, slow: Boolean) {
        delay(if (slow) 1100 else 650)
    }
}
