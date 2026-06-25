package com.simon.wodtimer.platform

import android.content.Context

actual object Settings {
    private const val PREFS_NAME = "wod_timer_kmp"

    private val prefs by lazy {
        AndroidPlatform.appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    actual fun getInt(key: String, default: Int): Int = prefs.getInt(key, default)

    actual fun putInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }

    actual fun getString(key: String): String? = prefs.getString(key, null)

    actual fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }
}
