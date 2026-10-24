package com.nuvio.app.features.iptv

import android.content.Context
import android.content.SharedPreferences

actual object IptvSettingsStorage {
    private const val preferencesName = "nuvio_iptv_settings"
    private const val lockAdultCategoriesKey = "iptv_lock_adult_categories"
    private const val clearHistoryOnCloseKey = "iptv_clear_history_on_close"
    private const val lockSettingsKey = "iptv_lock_settings"
    private const val iptvPinKey = "iptv_pin"
    private const val systemPipEnabledKey = "iptv_system_pip_enabled"
    private const val osdDurationMsKey = "iptv_osd_duration_ms"
    private const val audioLanguageKey = "iptv_audio_language"
    private const val subtitleLanguageKey = "iptv_subtitle_language"
    private const val onlineSubtitleUsernameKey = "iptv_online_subtitle_username"
    private const val onlineSubtitlePasswordKey = "iptv_online_subtitle_password"
    private const val onlineSubtitleSourcesKey = "iptv_online_subtitle_sources"
    private const val onlineSubtitlePreferredLanguagesKey = "iptv_online_subtitle_preferred_languages"

    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    }

    private fun loadBoolean(key: String): Boolean? {
        val prefs = preferences ?: return null
        return if (prefs.contains(key)) prefs.getBoolean(key, false) else null
    }

    private fun saveBoolean(key: String, value: Boolean) {
        preferences?.edit()?.putBoolean(key, value)?.apply()
    }

    private fun loadString(key: String): String? = preferences?.getString(key, null)

    private fun saveString(key: String, value: String) {
        preferences?.edit()?.putString(key, value)?.apply()
    }

    actual fun loadLockAdultCategories(): Boolean? = loadBoolean(lockAdultCategoriesKey)
    actual fun saveLockAdultCategories(enabled: Boolean) = saveBoolean(lockAdultCategoriesKey, enabled)
    actual fun loadClearHistoryOnClose(): Boolean? = loadBoolean(clearHistoryOnCloseKey)
    actual fun saveClearHistoryOnClose(enabled: Boolean) = saveBoolean(clearHistoryOnCloseKey, enabled)
    actual fun loadLockSettings(): Boolean? = loadBoolean(lockSettingsKey)
    actual fun saveLockSettings(enabled: Boolean) = saveBoolean(lockSettingsKey, enabled)
    actual fun loadIptvPin(): String? = loadString(iptvPinKey)
    actual fun saveIptvPin(pin: String) = saveString(iptvPinKey, pin)
    actual fun loadSystemPipEnabled(): Boolean? = loadBoolean(systemPipEnabledKey)
    actual fun saveSystemPipEnabled(enabled: Boolean) = saveBoolean(systemPipEnabledKey, enabled)

    actual fun loadOsdDurationMs(): Int? {
        val prefs = preferences ?: return null
        return if (prefs.contains(osdDurationMsKey)) prefs.getInt(osdDurationMsKey, 5000) else null
    }

    actual fun saveOsdDurationMs(durationMs: Int) {
        preferences?.edit()?.putInt(osdDurationMsKey, durationMs)?.apply()
    }

    actual fun loadAudioLanguage(): String? = loadString(audioLanguageKey)
    actual fun saveAudioLanguage(code: String) = saveString(audioLanguageKey, code)
    actual fun loadSubtitleLanguage(): String? = loadString(subtitleLanguageKey)
    actual fun saveSubtitleLanguage(code: String) = saveString(subtitleLanguageKey, code)
    actual fun loadOnlineSubtitleUsername(): String? = loadString(onlineSubtitleUsernameKey)
    actual fun saveOnlineSubtitleUsername(username: String) = saveString(onlineSubtitleUsernameKey, username)
    actual fun loadOnlineSubtitlePassword(): String? = loadString(onlineSubtitlePasswordKey)
    actual fun saveOnlineSubtitlePassword(password: String) = saveString(onlineSubtitlePasswordKey, password)
    actual fun loadOnlineSubtitleSources(): String? = loadString(onlineSubtitleSourcesKey)
    actual fun saveOnlineSubtitleSources(json: String) = saveString(onlineSubtitleSourcesKey, json)
    actual fun loadOnlineSubtitlePreferredLanguages(): String? = loadString(onlineSubtitlePreferredLanguagesKey)
    actual fun saveOnlineSubtitlePreferredLanguages(codesCsv: String) = saveString(onlineSubtitlePreferredLanguagesKey, codesCsv)
}
