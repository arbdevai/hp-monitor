package com.sahalos.hpmonitor.data

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Preferences(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

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

    fun getSavedDevices(): List<DeviceTarget> {
        val raw = prefs.getString(KEY_DEVICES, null) ?: return listOf(
            DeviceTarget("Host Utama", serverUrl)
        )
        return try {
            val list = json.decodeFromString<List<DeviceTarget>>(raw)
            if (list.isEmpty()) listOf(DeviceTarget("Host Utama", serverUrl)) else list
        } catch (_: Exception) {
            listOf(DeviceTarget("Host Utama", serverUrl))
        }
    }

    fun addOrUpdateDevice(name: String, url: String) {
        val current = getSavedDevices().toMutableList()
        val existingIndex = current.indexOfFirst { it.url.equals(url.trim(), ignoreCase = true) }
        val newTarget = DeviceTarget(name.trim().ifEmpty { "Target Host" }, url.trim())
        if (existingIndex >= 0) {
            current[existingIndex] = newTarget
        } else {
            current.add(newTarget)
        }
        prefs.edit().putString(KEY_DEVICES, json.encodeToString(current)).apply()
        serverUrl = newTarget.url
    }

    fun removeDevice(url: String) {
        val current = getSavedDevices().filterNot { it.url.equals(url.trim(), ignoreCase = true) }
        prefs.edit().putString(KEY_DEVICES, json.encodeToString(current)).apply()
    }

    companion object {
        const val PREFS_NAME = "hp_monitor"
        private const val KEY_URL = "server_url"
        private const val KEY_COMPACT = "compact_mode"
        private const val KEY_HUD_DESK = "hud_desk_mode"
        private const val KEY_FULLSCREEN = "fullscreen_mode"
        private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
        private const val KEY_DEVICES = "saved_devices_json"
        const val DEFAULT_URL = "http://192.168.1.2:8787"
    }
}
