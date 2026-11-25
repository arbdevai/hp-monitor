package com.sahalos.hpmonitor.data

import android.content.Context

class Preferences(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var serverUrl: String
        get() = prefs.getString(KEY_URL, DEFAULT_URL) ?: DEFAULT_URL
        set(value) {
            prefs.edit().putString(KEY_URL, value.trim()).apply()
        }

    var compactMode: Boolean
        get() = prefs.getBoolean(KEY_COMPACT, false)
        set(value) {
            prefs.edit().putBoolean(KEY_COMPACT, value).apply()
        }

    companion object {
        const val PREFS_NAME = "hp_monitor"
        private const val KEY_URL = "server_url"
        private const val KEY_COMPACT = "compact_mode"
        const val DEFAULT_URL = "http://192.168.1.2:8787"
    }
}
