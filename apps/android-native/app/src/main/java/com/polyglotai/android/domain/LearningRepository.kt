package com.polyglotai.android.domain

import com.polyglotai.android.data.PackRepository
import com.polyglotai.android.data.db.ReviewDao
import com.polyglotai.android.data.db.ReviewItem

data class DashboardStats(
    val dueCount: Int,
    val totalCards: Int,
    val reviewsToday: Int,
    val dailyGoal: Int,
)

/**
 * Ties bundled content to persisted review state. Seeds a pack's vocabulary into the review
 * table the first time it's opened, exposes the due queue, and records grades through SM-2.
 */
class LearningRepository(
    private val packs: PackRepository,
    private val dao: ReviewDao,
    private val now: () -> Long = { System.currentTimeMillis() },
) {
    /** Insert any of the pack's vocabulary not already tracked (idempotent — IGNORE on conflict). */
    suspend fun seedPack(packId: String) {
        val vocab = packs.vocabulary(packId)
        val nowMs = now()
        val items = vocab.map { v ->
            val init = Sm2.initial(nowMs)
            ReviewItem(
                contentId = "$packId::${v.key}",
                packId = packId,
                itemType = "vocab",
                front = v.lemma,
                back = v.translation,
                reading = listOfNotNull(v.reading, v.romaji).joinToString(" · ").ifBlank { null },
                easiness = init.easiness,
                intervalDays = init.intervalDays,
                reps = init.reps,
                lapses = init.lapses,
                dueAtMillis = init.dueAtMillis,
                lastReviewedAtMillis = init.lastReviewedAtMillis,
            )
        }
        dao.insertAllIgnoring(items)
    }

    suspend fun dashboard(packId: String, dailyGoal: Int = 20): DashboardStats {
        val nowMs = now()
        return DashboardStats(
            dueCount = dao.countDue(packId, nowMs),
            totalCards = dao.countForPack(packId),
            reviewsToday = 0, // wired to a per-day count in a later pass
            dailyGoal = dailyGoal,
        )
    }

    suspend fun listDue(packId: String): List<ReviewItem> =
        dao.listDue(packId, now())

    suspend fun grade(item: ReviewItem, rating: Int) {
        val nowMs = now()
        val next = Sm2.schedule(
            Sm2.State(item.easiness, item.intervalDays, item.reps, item.lapses, item.dueAtMillis, item.lastReviewedAtMillis),
            rating,
            nowMs,
        )
        dao.update(
            item.copy(
                easiness = next.easiness,
                intervalDays = next.intervalDays,
                reps = next.reps,
                lapses = next.lapses,
                dueAtMillis = next.dueAtMillis,
                lastReviewedAtMillis = next.lastReviewedAtMillis,
            ),
        )
    }
}
