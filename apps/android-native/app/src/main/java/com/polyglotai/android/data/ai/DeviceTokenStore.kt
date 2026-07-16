package com.polyglotai.android.data.ai

import android.content.Context

/** Persists the proxy device token. It's a session-token-grade secret, not an OpenAI key. */
class DeviceTokenStore(context: Context) {
    private val prefs = context.getSharedPreferences("polyglotai_ai", Context.MODE_PRIVATE)

    var token: String?
        get() = prefs.getString(KEY, null)
        set(value) = prefs.edit().apply { if (value == null) remove(KEY) else putString(KEY, value) }.apply()

    private companion object {
        const val KEY = "device_token"
    }
}
