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

    var hudDeskMode: Boolean
        get() = prefs.getBoolean(KEY_HUD_DESK, false)
        set(value) {
            prefs.edit().putBoolean(KEY_HUD_DESK, value).apply()
        }

    var fullscreenMode: Boolean
        get() = prefs.getBoolean(KEY_FULLSCREEN, false)
        set(value) {
            prefs.edit().putBoolean(KEY_FULLSCREEN, value).apply()
        }

    var keepScreenOn: Boolean
        get() = prefs.getBoolean(KEY_KEEP_SCREEN_ON, true)
        set(value) {
            prefs.edit().putBoolean(KEY_KEEP_SCREEN_ON, value).apply()
        }

    companion object {
        const val PREFS_NAME = "hp_monitor"
        private const val KEY_URL = "server_url"
        private const val KEY_COMPACT = "compact_mode"
        private const val KEY_HUD_DESK = "hud_desk_mode"
        private const val KEY_FULLSCREEN = "fullscreen_mode"
        private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
        const val DEFAULT_URL = "http://192.168.1.2:8787"
    }
}
