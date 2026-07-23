package com.polyglotai.android.domain.account

import com.polyglotai.android.data.account.AccountStore
import com.polyglotai.android.data.account.AuthSession
import com.polyglotai.android.data.account.NativeReviewRow
import com.polyglotai.android.data.account.SupabaseAuth
import com.polyglotai.android.data.account.SyncClient
import com.polyglotai.android.data.account.SyncUnauthorized
import com.polyglotai.android.data.db.ReviewDao
import com.polyglotai.android.data.db.ReviewItem

/** Outcome of a sync: how many cards were pulled down and pushed up. */
data class SyncResult(val pulled: Int, val pushed: Int)

/**
 * Accounts + cloud sync for the native app. Sign in once; [sync] does a two-way merge of review
 * state between this device and the account's cloud rows, resolving each card to whichever side
 * reviewed it more recently. Signed out, the app stays fully usable on local state alone.
 */
class AccountRepository(
    private val auth: SupabaseAuth,
    private val store: AccountStore,
    private val sync: SyncClient,
    private val dao: ReviewDao,
) {
    val isSignedIn: Boolean get() = store.isSignedIn
    val email: String? get() = store.email

    suspend fun signUp(email: String, password: String) {
        store.save(auth.signUp(email.trim(), password))
    }

    suspend fun signIn(email: String, password: String) {
        store.save(auth.signIn(email.trim(), password))
    }

    fun signOut() = store.clear()

    /**
     * Two-way merge. Pulls the account's cloud rows, resolves every card against local state by
     * last-reviewed recency, writes the winners back to the local DB, and pushes the full merged
     * set up. Refreshes an expired token once and retries.
     */
    suspend fun sync(): SyncResult = withFreshToken { token ->
        val cloud = sync.fetch(token).associateBy { it.content_id }
        val local = dao.listAll().associateBy { it.contentId }

        val allIds = cloud.keys + local.keys
        val mergedLocal = mutableListOf<ReviewItem>()
        var pulled = 0

        for (id in allIds) {
            val c = cloud[id]
            val l = local[id]
            when {
                c == null && l != null -> mergedLocal += l // local-only, pushed below
                c != null && l == null -> {
                    mergedLocal += c.toReviewItem() // cloud-only card from another device
                    pulled++
                }
                c != null && l != null -> {
                    val cloudNewer = (c.last_reviewed_at_millis ?: -1) > (l.lastReviewedAtMillis ?: -1)
                    if (cloudNewer) {
                        mergedLocal += c.toReviewItem()
                        pulled++
                    } else {
                        mergedLocal += l
                    }
                }
            }
        }

        dao.upsertAll(mergedLocal)
        val rows = mergedLocal.map { it.toRow() }
        sync.upsert(token, requireUserId(), rows)
        SyncResult(pulled = pulled, pushed = rows.size)
    }

    private fun requireUserId(): String = store.userId ?: throw IllegalStateException("Not signed in.")

    /** Run [block] with the current access token; on 401, refresh once and retry. */
    private suspend fun <T> withFreshToken(block: suspend (String) -> T): T {
        val token = store.accessToken ?: throw IllegalStateException("Not signed in.")
        return try {
            block(token)
        } catch (e: SyncUnauthorized) {
            val refreshToken = store.refreshToken ?: throw e
            val session: AuthSession = auth.refresh(refreshToken)
            store.save(session)
            block(session.access_token)
        }
    }
}

private fun NativeReviewRow.toReviewItem() = ReviewItem(
    contentId = content_id,
    packId = pack_id,
    itemType = item_type,
    front = front,
    back = back,
    reading = reading,
    easiness = easiness,
    intervalDays = interval_days,
    reps = reps,
    lapses = lapses,
    dueAtMillis = due_at_millis,
    lastReviewedAtMillis = last_reviewed_at_millis,
)

private fun ReviewItem.toRow() = NativeReviewRow(
    content_id = contentId,
    pack_id = packId,
    item_type = itemType,
    front = front,
    back = back,
    reading = reading,
    easiness = easiness,
    interval_days = intervalDays,
    reps = reps,
    lapses = lapses,
    due_at_millis = dueAtMillis,
    last_reviewed_at_millis = lastReviewedAtMillis,
)
