package com.polyglotai.android.data.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

@Serializable data class ChatMessage(val role: String, val content: String)

@Serializable private data class ChatRequest(val model: String, val messages: List<ChatMessage>, val temperature: Double)
@Serializable private data class ChatChoiceMsg(val content: String = "")
@Serializable private data class ChatChoice(val message: ChatChoiceMsg = ChatChoiceMsg())
@Serializable private data class ChatResponse(val choices: List<ChatChoice> = emptyList())
@Serializable private data class RegisterRequest(val passcode: String)
@Serializable private data class RegisterResponse(val token: String? = null, val error: String? = null)
@Serializable private data class TranscriptionResponse(val text: String = "")

/** Raised when the proxy rejects a request; message carries the status + a body snippet. */
class ProxyException(message: String) : RuntimeException(message)

/**
 * Talks to the same Supabase openai-proxy the web/hybrid builds use: register once for an opaque
 * device token, then send OpenAI-shaped chat requests. The proxy holds the real key and enforces
 * the passcode gate + rate limits server-side.
 */
class ProxyClient(private val baseUrl: String = DEFAULT_BASE_URL) {
    private val http = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }
    private val jsonMedia = "application/json".toMediaType()

    /** Register a device. Returns the token, or null if the passcode was rejected / registration
     *  is closed. */
    suspend fun register(passcode: String): String? = withContext(Dispatchers.IO) {
        val body = json.encodeToString(RegisterRequest.serializer(), RegisterRequest(passcode)).toRequestBody(jsonMedia)
        val req = Request.Builder().url("$baseUrl/register").post(body).build()
        http.newCall(req).execute().use { res ->
            val text = res.body?.string().orEmpty()
            if (!res.isSuccessful) return@withContext null
            json.decodeFromString(RegisterResponse.serializer(), text).token
        }
    }

    suspend fun chat(
        token: String,
        messages: List<ChatMessage>,
        model: String = "gpt-4o-mini",
        temperature: Double = 0.3,
    ): String = withContext(Dispatchers.IO) {
        val payload = json.encodeToString(ChatRequest.serializer(), ChatRequest(model, messages, temperature))
        val req = Request.Builder()
            .url("$baseUrl/chat/completions")
            .addHeader("authorization", "Bearer $token")
            .post(payload.toRequestBody(jsonMedia))
            .build()
        http.newCall(req).execute().use { res ->
            val text = res.body?.string().orEmpty()
            if (!res.isSuccessful) throw ProxyException("proxy ${res.code}: ${text.take(200)}")
            json.decodeFromString(ChatResponse.serializer(), text).choices.firstOrNull()?.message?.content
                ?: throw ProxyException("empty AI reply")
        }
    }

    /** Whisper transcription (multipart). `language` is an ISO-639-1 hint (e.g. "pt") that
     *  improves accuracy. Returns the recognized text. */
    suspend fun transcribe(
        token: String,
        audio: File,
        model: String = "whisper-1",
        language: String? = null,
    ): String = withContext(Dispatchers.IO) {
        val form = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("model", model)
            .addFormDataPart("file", audio.name, audio.asRequestBody("audio/m4a".toMediaType()))
        if (language != null) form.addFormDataPart("language", language)
        val req = Request.Builder()
            .url("$baseUrl/audio/transcriptions")
            .addHeader("authorization", "Bearer $token")
            .post(form.build())
            .build()
        http.newCall(req).execute().use { res ->
            val text = res.body?.string().orEmpty()
            if (!res.isSuccessful) throw ProxyException("proxy ${res.code}: ${text.take(200)}")
            json.decodeFromString(TranscriptionResponse.serializer(), text).text
        }
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://qddglfcuipmazrjoxpin.supabase.co/functions/v1/openai-proxy"
    }
}
