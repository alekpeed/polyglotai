package com.polyglotai.android.data.account

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/** One row of native_review_state. Field names are the Postgres column names (snake_case) so this
 *  serializes straight to/from PostgREST with no mapping layer. */
@Serializable
data class NativeReviewRow(
    val user_id: String? = null,
    val content_id: String,
    val pack_id: String,
    val item_type: String,
    val front: String,
    val back: String,
    val reading: String? = null,
    val easiness: Double,
    val interval_days: Int,
    val reps: Int,
    val lapses: Int,
    val due_at_millis: Long,
    val last_reviewed_at_millis: Long? = null,
)

/** The access token was rejected (expired). Callers refresh and retry. */
class SyncUnauthorized : RuntimeException("Session expired.")

/** The proxy/PostgREST returned an unexpected status. */
class SyncException(message: String) : RuntimeException(message)

/**
 * Reads and writes the signed-in user's review state through Supabase PostgREST. RLS restricts
 * every query to `auth.uid() = user_id`, so a stolen anon key still can't read another account.
 */
class SyncClient(
    private val baseUrl: String = SupabaseAuth.SUPABASE_URL,
    private val anonKey: String = SupabaseAuth.ANON_KEY,
) {
    private val http = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val jsonMedia = "application/json".toMediaType()
    private val endpoint = "$baseUrl/rest/v1/native_review_state"

    /** All of the signed-in user's cloud rows. */
    suspend fun fetch(accessToken: String): List<NativeReviewRow> = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("$endpoint?select=*")
            .addHeader("apikey", anonKey)
            .addHeader("authorization", "Bearer $accessToken")
            .get()
            .build()
        http.newCall(req).execute().use { res ->
            val text = res.body?.string().orEmpty()
            if (res.code == 401) throw SyncUnauthorized()
            if (!res.isSuccessful) throw SyncException("sync fetch ${res.code}: ${text.take(200)}")
            json.decodeFromString(kotlinx.serialization.builtins.ListSerializer(NativeReviewRow.serializer()), text)
        }
    }

    /** Upsert rows, overwriting on (user_id, content_id) conflict. `userId` is stamped onto each
     *  row so it satisfies the insert RLS check. */
    suspend fun upsert(accessToken: String, userId: String, rows: List<NativeReviewRow>) {
        if (rows.isEmpty()) return
        withContext(Dispatchers.IO) {
            val stamped = rows.map { it.copy(user_id = userId) }
            val payload = json.encodeToString(
                kotlinx.serialization.builtins.ListSerializer(NativeReviewRow.serializer()),
                stamped,
            )
            val req = Request.Builder()
                .url(endpoint)
                .addHeader("apikey", anonKey)
                .addHeader("authorization", "Bearer $accessToken")
                .addHeader("content-type", "application/json")
                .addHeader("prefer", "resolution=merge-duplicates")
                .post(payload.toRequestBody(jsonMedia))
                .build()
            http.newCall(req).execute().use { res ->
                val text = res.body?.string().orEmpty()
                if (res.code == 401) throw SyncUnauthorized()
                if (!res.isSuccessful) throw SyncException("sync upsert ${res.code}: ${text.take(200)}")
            }
        }
    }
}
