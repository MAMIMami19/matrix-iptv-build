package com.nuvio.tv.matrix.iptv

import android.content.Context
import android.content.SharedPreferences

/**
 * App-level (not profile-scoped) persistence for the IPTV / Live TV settings.
 *
 * Same SharedPreferences file and keys as the 0.6.3-based Matrix builds, so
 * upgrading to 3.0 preserves the user's IPTV settings (PIN, favorites, locks).
 * Initialized from [com.nuvio.tv.NuvioApplication.onCreate].
 */
internal object IptvSettingsStorage {
    private const val preferencesName = "nuvio_iptv_settings"
    private const val lockAdultCategoriesKey = "iptv_lock_adult_categories"
    private const val clearHistoryOnCloseKey = "iptv_clear_history_on_close"
    private const val lockSettingsKey = "iptv_lock_settings"
    private const val iptvPinKey = "iptv_pin"
    private const val favoriteChannelsKey = "iptv_favorite_channels"

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

    fun loadLockAdultCategories(): Boolean? = loadBoolean(lockAdultCategoriesKey)
    fun saveLockAdultCategories(enabled: Boolean) = saveBoolean(lockAdultCategoriesKey, enabled)
    fun loadClearHistoryOnClose(): Boolean? = loadBoolean(clearHistoryOnCloseKey)
    fun saveClearHistoryOnClose(enabled: Boolean) = saveBoolean(clearHistoryOnCloseKey, enabled)
    fun loadLockSettings(): Boolean? = loadBoolean(lockSettingsKey)
    fun saveLockSettings(enabled: Boolean) = saveBoolean(lockSettingsKey, enabled)
    fun loadIptvPin(): String? = loadString(iptvPinKey)
    fun saveIptvPin(pin: String) = saveString(iptvPinKey, pin)
    /** JSON array of favorite Live TV channels (see FavoriteChannel). */
    fun loadFavoriteChannels(): String? = loadString(favoriteChannelsKey)
    fun saveFavoriteChannels(json: String) = saveString(favoriteChannelsKey, json)
}
