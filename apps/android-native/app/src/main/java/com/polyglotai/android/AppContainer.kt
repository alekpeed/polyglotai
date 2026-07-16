package com.polyglotai.android

import android.content.Context
import androidx.room.Room
import com.polyglotai.android.data.PackRepository
import com.polyglotai.android.data.db.ReviewDatabase
import com.polyglotai.android.domain.LearningRepository

/** Minimal manual dependency wiring — one instance held by MainActivity. */
class AppContainer(context: Context) {
    private val db = Room.databaseBuilder(
        context.applicationContext,
        ReviewDatabase::class.java,
        "polyglotai.db",
    ).build()

    val packs = PackRepository(context.applicationContext)
    val learning = LearningRepository(packs, db.reviewDao())
}
