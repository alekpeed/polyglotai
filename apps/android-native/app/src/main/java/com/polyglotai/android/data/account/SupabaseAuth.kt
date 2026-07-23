package com.polyglotai.android.data.account

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/** A signed-in session as returned by GoTrue. */
@Serializable
data class AuthSession(
    val access_token: String,
    val refresh_token: String,
    val expires_in: Long = 3600,
    val user: AuthUser = AuthUser(),
)

@Serializable
data class AuthUser(val id: String = "", val email: String? = null)

/** GoTrue rejected the request; [message] carries the human-readable reason from the server. */
class AuthException(message: String) : RuntimeException(message)

@Serializable
private data class Credentials(val email: String, val password: String)

@Serializable
private data class RefreshBody(val refresh_token: String)

@Serializable
private data class GoTrueError(
    val error_description: String? = null,
    val msg: String? = null,
    val message: String? = null,
    val error: String? = null,
)

/**
 * Talks to Supabase Auth (GoTrue) over its REST API — the same backend the web build reaches
 * through supabase-js, done here with raw OkHttp so the native app carries no JS runtime. The anon
 * key is public by design; row-level security on native_review_state is what isolates accounts.
 */
class SupabaseAuth(
    private val baseUrl: String = SUPABASE_URL,
    private val anonKey: String = ANON_KEY,
) {
    private val http = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }
    private val jsonMedia = "application/json".toMediaType()

    suspend fun signUp(email: String, password: String): AuthSession =
        post("$baseUrl/auth/v1/signup", json.encodeToString(Credentials.serializer(), Credentials(email, password)))

    suspend fun signIn(email: String, password: String): AuthSession =
        post(
            "$baseUrl/auth/v1/token?grant_type=password",
            json.encodeToString(Credentials.serializer(), Credentials(email, password)),
        )

    suspend fun refresh(refreshToken: String): AuthSession =
        post(
            "$baseUrl/auth/v1/token?grant_type=refresh_token",
            json.encodeToString(RefreshBody.serializer(), RefreshBody(refreshToken)),
        )

    private suspend fun post(url: String, body: String): AuthSession = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url(url)
            .addHeader("apikey", anonKey)
            .addHeader("content-type", "application/json")
            .post(body.toRequestBody(jsonMedia))
            .build()
        http.newCall(req).execute().use { res ->
            val text = res.body?.string().orEmpty()
            if (!res.isSuccessful) throw AuthException(parseError(text, res.code))
            // signup with email confirmation on can return a user but no session — treat a missing
            // access token as "check your email", not a silent success.
            val session = runCatching { json.decodeFromString(AuthSession.serializer(), text) }.getOrNull()
            if (session == null || session.access_token.isBlank()) {
                throw AuthException("Check your email to confirm the account, then sign in.")
            }
            session
        }
    }

    private fun parseError(body: String, code: Int): String {
        val parsed = runCatching { json.decodeFromString(GoTrueError.serializer(), body) }.getOrNull()
        return parsed?.error_description
            ?: parsed?.msg
            ?: parsed?.message
            ?: parsed?.error
            ?: "Auth failed (HTTP $code)."
    }

    companion object {
        const val SUPABASE_URL = "https://qddglfcuipmazrjoxpin.supabase.co"
        // Public anon key. Safe to ship: RLS policies on native_review_state, not key secrecy,
        // enforce that one account can't read another's rows.
        const val ANON_KEY =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InFkZGdsZmN1aXBtYXpyam94cGluIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODM3ODQyODcsImV4cCI6MjA5OTM2MDI4N30.ilOwQxx80JX06KWOcWzf5m7kyigC_TsA452knC6XG00"
    }
}
