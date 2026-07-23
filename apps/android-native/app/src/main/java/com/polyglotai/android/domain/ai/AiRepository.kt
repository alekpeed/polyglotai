package com.polyglotai.android.domain.ai

import com.polyglotai.android.data.ai.ChatMessage
import com.polyglotai.android.data.ai.DeviceTokenStore
import com.polyglotai.android.data.ai.ProxyClient
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class AiCorrection(
    val corrected: String = "",
    val natural: String? = null,
    val formal: String? = null,
    val casual: String? = null,
    val grammarExplanation: String? = null,
    val registerExplanation: String? = null,
)

@Serializable
data class AiExample(val target: String = "", val translation: String = "", val note: String? = null)

@Serializable
data class AiTranslation(val translation: String = "", val note: String? = null)

@Serializable
private data class AiExamplesEnvelope(val examples: List<AiExample> = emptyList())

/** Thrown when AI features can't run because there's no device token yet (needs a passcode). */
class NeedsAccessCode : RuntimeException("Enter the access code to enable AI features.")

/**
 * Native AI features over the proxy. Holds the device token; screens call [connect] with the
 * access code once, then [correct] / [examples] work. Malformed model JSON surfaces as an
 * exception rather than half-rendering.
 */
class AiRepository(
    private val proxy: ProxyClient,
    private val tokens: DeviceTokenStore,
) {
    private val json = Json { ignoreUnknownKeys = true }

    val isConnected: Boolean get() = tokens.token != null

    /** Register with the shared access code. Returns true on success. */
    suspend fun connect(accessCode: String): Boolean {
        val token = proxy.register(accessCode) ?: return false
        tokens.token = token
        return true
    }

    private fun requireToken(): String = tokens.token ?: throw NeedsAccessCode()

    private fun extractJson(raw: String): String {
        val t = raw.trim()
        val fence = Regex("^```(?:json)?\\s*([\\s\\S]*?)\\s*```$").find(t)
        return fence?.groupValues?.get(1) ?: t
    }

    suspend fun correct(targetLanguage: String, text: String): AiCorrection {
        val token = requireToken()
        val messages = listOf(
            ChatMessage("system", Prompts.system(targetLanguage, "You return only strict JSON.")),
            ChatMessage("user", Prompts.correctionTask(targetLanguage, text)),
        )
        val raw = proxy.chat(token, messages, temperature = 0.2)
        return json.decodeFromString(AiCorrection.serializer(), extractJson(raw))
    }

    /** One conversation turn: the running [history] (user/assistant messages) plus the new user
     *  line, returning the partner's reply in the target language. */
    suspend fun converse(
        targetLanguage: String,
        scenario: String,
        history: List<ChatMessage>,
        userText: String,
    ): String {
        val token = requireToken()
        val messages = buildList {
            add(ChatMessage("system", Prompts.conversationSystem(targetLanguage, scenario)))
            addAll(history)
            add(ChatMessage("user", userText))
        }
        return proxy.chat(token, messages, temperature = 0.8).trim()
    }

    /** Transcribe a recorded audio file via Whisper. `language` is an ISO-639-1 hint. */
    suspend fun transcribe(audio: java.io.File, language: String?): String =
        proxy.transcribe(requireToken(), audio, language = language)

    /** Interpret free text between English and the target language. [toTarget] picks the direction. */
    suspend fun interpret(targetLanguage: String, text: String, toTarget: Boolean): AiTranslation {
        val token = requireToken()
        val messages = listOf(
            ChatMessage("system", Prompts.system(targetLanguage, "You return only strict JSON.")),
            ChatMessage("user", Prompts.interpretTask(targetLanguage, text, toTarget)),
        )
        val raw = proxy.chat(token, messages, temperature = 0.3)
        return json.decodeFromString(AiTranslation.serializer(), extractJson(raw))
    }

    suspend fun examples(targetLanguage: String, word: String, meaning: String, count: Int = 3): List<AiExample> {
        val token = requireToken()
        val messages = listOf(
            ChatMessage("system", Prompts.system(targetLanguage, "You return only strict JSON.")),
            ChatMessage("user", Prompts.examplesTask(targetLanguage, word, meaning, count)),
        )
        val raw = proxy.chat(token, messages, temperature = 0.7)
        return json.decodeFromString(AiExamplesEnvelope.serializer(), extractJson(raw)).examples
    }
}
