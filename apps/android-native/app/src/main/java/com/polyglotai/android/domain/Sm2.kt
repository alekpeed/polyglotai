package com.polyglotai.android.domain

/**
 * SM-2 spaced repetition, the algorithm the spec lists as the documented fallback to FSRS.
 * Used for the first native cut; an FSRS port can replace this behind the same call shape later.
 *
 * Grade mapping from the four review buttons → SM-2 quality q:
 *   1 Again → 1 (fail)   2 Hard → 3   3 Good → 4   4 Easy → 5
 * A fail resets reps and makes the card due immediately, so it comes back within the session.
 */
object Sm2 {
    private const val DAY_MS = 86_400_000L

    data class State(
        val easiness: Double,
        val intervalDays: Int,
        val reps: Int,
        val lapses: Int,
        val dueAtMillis: Long,
        val lastReviewedAtMillis: Long?,
    )

    fun initial(nowMillis: Long): State =
        State(easiness = 2.5, intervalDays = 0, reps = 0, lapses = 0, dueAtMillis = nowMillis, lastReviewedAtMillis = null)

    fun schedule(current: State, rating: Int, nowMillis: Long): State {
        val q = when (rating) {
            1 -> 1
            2 -> 3
            3 -> 4
            else -> 5
        }
        if (q < 3) {
            return current.copy(
                reps = 0,
                intervalDays = 0,
                lapses = current.lapses + 1,
                dueAtMillis = nowMillis, // relearn now, same session
                lastReviewedAtMillis = nowMillis,
            )
        }
        val reps = current.reps + 1
        val interval = when (reps) {
            1 -> 1
            2 -> 6
            else -> Math.round(current.intervalDays * current.easiness).toInt().coerceAtLeast(1)
        }
        val easiness = (current.easiness + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))).coerceAtLeast(1.3)
        return current.copy(
            easiness = easiness,
            intervalDays = interval,
            reps = reps,
            dueAtMillis = nowMillis + interval * DAY_MS,
            lastReviewedAtMillis = nowMillis,
        )
    }
}
