package com.polyglotai.android.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update

/**
 * One reviewable card's persisted state. `contentId` is "packId::itemKey" (matches the TS app's
 * scheme). SM-2 scheduling fields live here; `dueAtMillis` drives the due queue.
 */
@Entity(tableName = "review_items")
data class ReviewItem(
    @PrimaryKey val contentId: String,
    val packId: String,
    val itemType: String,
    val front: String,
    val back: String,
    val reading: String?,
    val easiness: Double,
    val intervalDays: Int,
    val reps: Int,
    val lapses: Int,
    val dueAtMillis: Long,
    val lastReviewedAtMillis: Long?,
)

@Dao
interface ReviewDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnoring(items: List<ReviewItem>)

    @Update
    suspend fun update(item: ReviewItem)

    @Query("SELECT COUNT(*) FROM review_items WHERE packId = :packId")
    suspend fun countForPack(packId: String): Int

    @Query("SELECT COUNT(*) FROM review_items WHERE packId = :packId AND dueAtMillis <= :now")
    suspend fun countDue(packId: String, now: Long): Int

    @Query("SELECT * FROM review_items WHERE packId = :packId AND dueAtMillis <= :now ORDER BY dueAtMillis ASC")
    suspend fun listDue(packId: String, now: Long): List<ReviewItem>

    @Query("SELECT COUNT(*) FROM review_items WHERE packId = :packId AND lastReviewedAtMillis IS NOT NULL")
    suspend fun countReviewed(packId: String): Int

    /** Every tracked card, across all packs — the push side of cloud sync. */
    @Query("SELECT * FROM review_items")
    suspend fun listAll(): List<ReviewItem>

    /** Replace local rows with cloud winners (used by the pull side of sync). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ReviewItem>)
}

@Database(entities = [ReviewItem::class], version = 1, exportSchema = false)
abstract class ReviewDatabase : RoomDatabase() {
    abstract fun reviewDao(): ReviewDao
}
