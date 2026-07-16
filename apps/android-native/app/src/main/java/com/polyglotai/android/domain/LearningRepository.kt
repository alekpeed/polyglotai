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

/** Consecutive-day review streak. [last7] is oldest-to-newest, today last — the seven dots on the
 *  dashboard's streak card. */
data class Streak(val days: Int, val last7: List<Boolean>)

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
            reviewsToday = dao.countReviewedSince(packId, startOfDayMillis(nowMs)),
            dailyGoal = dailyGoal,
        )
    }

    /** Local-midnight for the day containing [nowMs]. Uses Calendar so it works on minSdk 24
     *  without java.time desugaring. */
    private fun startOfDayMillis(nowMs: Long): Long {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = nowMs
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    suspend fun listDue(packId: String): List<ReviewItem> =
        dao.listDue(packId, now())

    /** Computed straight from review timestamps — no separate activity log needed. A day "counts"
     *  if any card in the pack was reviewed during it. */
    suspend fun streak(packId: String): Streak {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = now()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val todayStart = cal.timeInMillis
        val dayMs = 24L * 60 * 60 * 1000

        val reviewedDays = dao.listReviewedMillis(packId)
            .map { Math.floorDiv(it - todayStart, dayMs) }
            .toSet()

        // Count backward from today; if today hasn't happened yet, start from yesterday so the
        // streak still reflects "as of your last active day" rather than snapping to zero at 12am.
        val start = if (reviewedDays.contains(0L)) 0 else 1
        var days = 0
        while (reviewedDays.contains(-(start + days).toLong())) days++

        val last7 = (6 downTo 0).map { offset -> reviewedDays.contains(-offset.toLong()) }
        return Streak(days, last7)
    }

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
