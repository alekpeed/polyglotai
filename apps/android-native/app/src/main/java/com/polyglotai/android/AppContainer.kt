package com.polyglotai.android

import android.content.Context
import androidx.room.Room
import com.polyglotai.android.data.PackRepository
import com.polyglotai.android.data.SettingsStore
import com.polyglotai.android.data.account.AccountStore
import com.polyglotai.android.data.account.SupabaseAuth
import com.polyglotai.android.data.account.SyncClient
import com.polyglotai.android.data.ai.DeviceTokenStore
import com.polyglotai.android.data.ai.ProxyClient
import com.polyglotai.android.data.db.ReviewDatabase
import com.polyglotai.android.domain.LearningRepository
import com.polyglotai.android.domain.account.AccountRepository
import com.polyglotai.android.domain.ai.AiRepository

/** Minimal manual dependency wiring — one instance held by MainActivity. */
class AppContainer(context: Context) {
    private val app = context.applicationContext

    private val db = Room.databaseBuilder(
        app,
        ReviewDatabase::class.java,
        "polyglotai.db",
    ).build()

    val packs = PackRepository(app)
    val settings = SettingsStore(app)
    val learning = LearningRepository(packs, db.reviewDao())
    val ai = AiRepository(ProxyClient(), DeviceTokenStore(app))
    val account = AccountRepository(SupabaseAuth(), AccountStore(app), SyncClient(), db.reviewDao())
}
