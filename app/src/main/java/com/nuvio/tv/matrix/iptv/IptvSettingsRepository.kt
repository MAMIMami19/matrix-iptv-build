package com.nuvio.tv.matrix.iptv

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * One favorite Live TV channel. The full mini-record is stored (not just the
 * id) so the Favoris section can render and play without re-fetching the
 * whole channel catalog. Scope is Live TV channels only.
 */
@Serializable
internal data class FavoriteChannel(
    val streamId: Int,
    val name: String = "",
    val iconUrl: String? = null,
    val groupName: String? = null,
)

/**
 * IPTV / Live TV settings, ported from the 0.6.3-based Matrix build.
 *
 * Deliberately reduced vs the old build: the settings whose backend does not
 * exist in the 1.2.0 player are gone (system PiP, OSD duration, IPTV-scoped
 * audio/subtitle language overrides, online-subtitle sources, subtitle-cache
 * clear). Audio/subtitle languages and autoplay now live in the app's global
 * Playback settings, which the new player already reads; subtitles come from
 * the installed subtitle addons (e.g. OpenSubtitles v3).
 */
internal object IptvSettingsRepository {
    private var hasLoaded = false

    private val _lockAdultCategories = MutableStateFlow(false)
    val lockAdultCategories: StateFlow<Boolean> = _lockAdultCategories.asStateFlow()

    private val _clearHistoryOnClose = MutableStateFlow(false)
    val clearHistoryOnClose: StateFlow<Boolean> = _clearHistoryOnClose.asStateFlow()

    private val _lockSettings = MutableStateFlow(false)
    val lockSettings: StateFlow<Boolean> = _lockSettings.asStateFlow()

    /** Empty means no lock is configured. */
    private val _pin = MutableStateFlow("")
    val pin: StateFlow<String> = _pin.asStateFlow()

    /** Favorite Live TV channels, persisted as a JSON array (see storage). */
    private val _favoriteChannels = MutableStateFlow<List<FavoriteChannel>>(emptyList())
    val favoriteChannels: StateFlow<List<FavoriteChannel>> =
        _favoriteChannels.asStateFlow()

    /** Session-only unlock flag: in-memory, never persisted. */
    private val _sessionUnlocked = MutableStateFlow(false)
    val sessionUnlocked: StateFlow<Boolean> = _sessionUnlocked.asStateFlow()

    /** (videoId, parentMetaId) pairs played in this IPTV session. */
    private val playedIptvKeys = mutableSetOf<Pair<String, String>>()

    fun addPlayedIptvKey(videoId: String, parentMetaId: String) {
        playedIptvKeys += videoId to parentMetaId
    }

    fun takePlayedIptvKeys(): List<Pair<String, String>> {
        val keys = playedIptvKeys.toList()
        playedIptvKeys.clear()
        return keys
    }

    fun setSessionUnlocked(unlocked: Boolean) {
        _sessionUnlocked.value = unlocked
    }

    fun ensureLoaded() {
        if (hasLoaded) return
        hasLoaded = true
        _lockAdultCategories.value = IptvSettingsStorage.loadLockAdultCategories() ?: false
        _clearHistoryOnClose.value = IptvSettingsStorage.loadClearHistoryOnClose() ?: false
        _lockSettings.value = IptvSettingsStorage.loadLockSettings() ?: false
        _pin.value = IptvSettingsStorage.loadIptvPin().orEmpty()
        _favoriteChannels.value =
            IptvSettingsStorage.loadFavoriteChannels()?.let { raw ->
                runCatching { Json.decodeFromString<List<FavoriteChannel>>(raw) }.getOrNull()
            }.orEmpty()
    }

    fun setLockAdultCategories(enabled: Boolean) {
        ensureLoaded()
        if (_lockAdultCategories.value == enabled) return
        _lockAdultCategories.value = enabled
        IptvSettingsStorage.saveLockAdultCategories(enabled)
    }

    fun setClearHistoryOnClose(enabled: Boolean) {
        ensureLoaded()
        if (_clearHistoryOnClose.value == enabled) return
        _clearHistoryOnClose.value = enabled
        IptvSettingsStorage.saveClearHistoryOnClose(enabled)
    }

    fun setLockSettings(enabled: Boolean) {
        ensureLoaded()
        if (_lockSettings.value == enabled) return
        _lockSettings.value = enabled
        IptvSettingsStorage.saveLockSettings(enabled)
    }

    fun setPin(pin: String) {
        ensureLoaded()
        if (_pin.value == pin) return
        _pin.value = pin
        IptvSettingsStorage.saveIptvPin(pin)
    }

    fun isFavoriteChannel(streamId: Int): Boolean =
        _favoriteChannels.value.any { it.streamId == streamId }

    fun toggleFavoriteChannel(channel: FavoriteChannel) {
        ensureLoaded()
        val current = _favoriteChannels.value
        val updated = if (current.any { it.streamId == channel.streamId }) {
            current.filterNot { it.streamId == channel.streamId }
        } else {
            current + channel
        }
        _favoriteChannels.value = updated
        IptvSettingsStorage.saveFavoriteChannels(Json.encodeToString(updated))
    }

    /** Adult categories are matched by name (Xtream has no adult flag). */
    fun isAdultCategory(categoryName: String): Boolean =
        categoryName.contains("adult", ignoreCase = true) ||
            categoryName.contains("xxx", ignoreCase = true) ||
            categoryName.contains("porn", ignoreCase = true)
}
