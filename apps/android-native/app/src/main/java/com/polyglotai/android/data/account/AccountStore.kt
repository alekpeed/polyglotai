package com.polyglotai.android.data.account

import android.content.Context

/** Persists the signed-in session across launches. Tokens live in app-private SharedPreferences;
 *  clearing them (sign-out) drops the account back to local-only. */
class AccountStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("polyglotai_account", Context.MODE_PRIVATE)

    var accessToken: String?
        get() = prefs.getString(KEY_ACCESS, null)
        set(value) = prefs.edit().apply { if (value == null) remove(KEY_ACCESS) else putString(KEY_ACCESS, value) }.apply()

    var refreshToken: String?
        get() = prefs.getString(KEY_REFRESH, null)
        set(value) = prefs.edit().apply { if (value == null) remove(KEY_REFRESH) else putString(KEY_REFRESH, value) }.apply()

    var userId: String?
        get() = prefs.getString(KEY_USER, null)
        set(value) = prefs.edit().apply { if (value == null) remove(KEY_USER) else putString(KEY_USER, value) }.apply()

    var email: String?
        get() = prefs.getString(KEY_EMAIL, null)
        set(value) = prefs.edit().apply { if (value == null) remove(KEY_EMAIL) else putString(KEY_EMAIL, value) }.apply()

    val isSignedIn: Boolean get() = accessToken != null && userId != null

    fun save(session: AuthSession) {
        prefs.edit()
            .putString(KEY_ACCESS, session.access_token)
            .putString(KEY_REFRESH, session.refresh_token)
            .putString(KEY_USER, session.user.id)
            .apply {
                session.user.email?.let { putString(KEY_EMAIL, it) }
            }
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_ACCESS = "access_token"
        const val KEY_REFRESH = "refresh_token"
        const val KEY_USER = "user_id"
        const val KEY_EMAIL = "email"
    }
}
