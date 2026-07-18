package com.polyglotai.android.mastery.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

/**
 * One persisted attempt (spec §4.5 / §3.8). Every unit and full-sentence attempt is logged with its
 * normalized result and any manual override, so grading calibration and error review have real data
 * later. Independent Room database from the review flashcards — the mastery engine is its own module.
 */
@Entity(tableName = "mastery_attempts")
data class MasteryAttempt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val lessonId: String,
    val sentenceId: String,
    val unitId: String?,
    /** "unit" or "sentence". */
    val attemptType: String,
    val status: String,
    val recognizedText: String?,
    val score: Double,
    val confidence: Double,
    val errorTypes: String?,
    val provider: String,
    val restartMode: String,
    val restartCount: Int,
    val manualOverride: Boolean,
    val overrideReason: String?,
    val createdAtMillis: Long,
)

@Dao
interface MasteryAttemptDao {
    @Insert
    suspend fun insert(attempt: MasteryAttempt): Long

    @Query("SELECT * FROM mastery_attempts WHERE sessionId = :sessionId ORDER BY createdAtMillis ASC")
    suspend fun forSession(sessionId: String): List<MasteryAttempt>

    @Query("SELECT COUNT(*) FROM mastery_attempts WHERE manualOverride = 1")
    suspend fun overrideCount(): Int

    @Query("SELECT * FROM mastery_attempts ORDER BY createdAtMillis DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<MasteryAttempt>
}

@Database(entities = [MasteryAttempt::class], version = 1, exportSchema = false)
abstract class MasteryDatabase : RoomDatabase() {
    abstract fun attemptDao(): MasteryAttemptDao
}
