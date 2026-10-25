package com.nuvio.app.features.iptv

import com.nuvio.app.features.player.AudioLanguageOption
import com.nuvio.app.features.player.SubtitleLanguageOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** One online-subtitle source account, filled in by the user (never prefilled). */
@Serializable
internal data class OnlineSubtitleSource(
    val username: String = "",
    val password: String = "",
)

/**
 * One favorite Live TV channel. The full mini-record is stored (not just the
 * id) so the Favoris section can render and play without re-fetching the
 * whole channel catalog. Scope is Live TV channels only (nova live).
 */
@Serializable
internal data class FavoriteChannel(
    val streamId: Int,
    val name: String = "",
    val iconUrl: String? = null,
    val groupName: String? = null,
)

/**
 * IPTV / Live TV settings, copied from the reference IPTV app's settings screens.
 *
 * Reuse map (existing Nuvio behavior is reused, not duplicated):
 * - PIN entry reuses the profile [PinEntryDialog]; the PIN itself is IPTV-scoped.
 * - Auto-play series reuses the global next-episode autoplay setting (see
 *   IptvSettingsScreen); it is NOT a second copy of that setting.
 * - Audio / subtitle languages are IPTV-scoped overrides (default System) that
 *   the player applies to IPTV launches only; regular playback keeps the
 *   global player settings untouched.
 * - Biometric unlock and in-app PiP are intentionally absent: the app has no
 *   biometric API and no mini-player infrastructure to gate them on.
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

    private val _systemPipEnabled = MutableStateFlow(false)
    val systemPipEnabled: StateFlow<Boolean> = _systemPipEnabled.asStateFlow()

    /** OSD display duration in milliseconds (reference default: 5 seconds). */
    private val _osdDurationMs = MutableStateFlow(5000)
    val osdDurationMs: StateFlow<Int> = _osdDurationMs.asStateFlow()
    val osdDurationMsValue: Int get() = _osdDurationMs.value

    /** IPTV-scoped audio language; "device" = System. */
    private val _audioLanguage = MutableStateFlow(AudioLanguageOption.DEVICE)
    val audioLanguage: StateFlow<String> = _audioLanguage.asStateFlow()
    val audioLanguageValue: String get() = _audioLanguage.value

    /** IPTV-scoped subtitle language; "device" = System. */
    private val _subtitleLanguage = MutableStateFlow(SubtitleLanguageOption.DEVICE)
    val subtitleLanguage: StateFlow<String> = _subtitleLanguage.asStateFlow()
    val subtitleLanguageValue: String get() = _subtitleLanguage.value

    /**
     * Online-subtitle sources: MULTIPLE accounts, each filled in by the user
     * (never prefilled). Stored as one JSON array; an earlier single login
     * is migrated into a one-element list on first load.
     */
    private val _onlineSubtitleSources = MutableStateFlow<List<OnlineSubtitleSource>>(emptyList())
    val onlineSubtitleSources: StateFlow<List<OnlineSubtitleSource>> =
        _onlineSubtitleSources.asStateFlow()

    /**
     * Preferred languages for online subtitles. Arabic is pre-selected;
     * everything else is unchecked by default.
     */
    private val _onlineSubtitlePreferredLanguages = MutableStateFlow(setOf("ar"))
    val onlineSubtitlePreferredLanguages: StateFlow<Set<String>> =
        _onlineSubtitlePreferredLanguages.asStateFlow()

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
        _systemPipEnabled.value = IptvSettingsStorage.loadSystemPipEnabled() ?: false
        _osdDurationMs.value = IptvSettingsStorage.loadOsdDurationMs() ?: 5000
        _audioLanguage.value = IptvSettingsStorage.loadAudioLanguage() ?: AudioLanguageOption.DEVICE
        _subtitleLanguage.value = IptvSettingsStorage.loadSubtitleLanguage() ?: SubtitleLanguageOption.DEVICE
        val storedSources = IptvSettingsStorage.loadOnlineSubtitleSources()
        val decodedSources = storedSources?.let { raw ->
            runCatching { Json.decodeFromString<List<OnlineSubtitleSource>>(raw) }.getOrNull()
        }
        _onlineSubtitleSources.value = when {
            !decodedSources.isNullOrEmpty() -> decodedSources
            else -> {
                // One-time migration from the earlier single login.
                val legacyUser = IptvSettingsStorage.loadOnlineSubtitleUsername().orEmpty()
                val legacyPass = IptvSettingsStorage.loadOnlineSubtitlePassword().orEmpty()
                if (legacyUser.isNotEmpty() || legacyPass.isNotEmpty()) {
                    listOf(OnlineSubtitleSource(legacyUser, legacyPass)).also { migrated ->
                        IptvSettingsStorage.saveOnlineSubtitleSources(
                            Json.encodeToString(migrated),
                        )
                    }
                } else {
                    emptyList()
                }
            }
        }
        _onlineSubtitlePreferredLanguages.value =
            IptvSettingsStorage.loadOnlineSubtitlePreferredLanguages()
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.toSet()
                ?.takeIf { it.isNotEmpty() }
                ?: setOf("ar")
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

    fun setSystemPipEnabled(enabled: Boolean) {
        ensureLoaded()
        if (_systemPipEnabled.value == enabled) return
        _systemPipEnabled.value = enabled
        IptvSettingsStorage.saveSystemPipEnabled(enabled)
    }

    fun setOsdDurationMs(durationMs: Int) {
        ensureLoaded()
        if (_osdDurationMs.value == durationMs) return
        _osdDurationMs.value = durationMs
        IptvSettingsStorage.saveOsdDurationMs(durationMs)
    }

    fun setAudioLanguage(code: String) {
        ensureLoaded()
        if (_audioLanguage.value == code) return
        _audioLanguage.value = code
        IptvSettingsStorage.saveAudioLanguage(code)
    }

    fun setSubtitleLanguage(code: String) {
        ensureLoaded()
        if (_subtitleLanguage.value == code) return
        _subtitleLanguage.value = code
        IptvSettingsStorage.saveSubtitleLanguage(code)
    }

    fun setOnlineSubtitleSources(sources: List<OnlineSubtitleSource>) {
        ensureLoaded()
        if (_onlineSubtitleSources.value == sources) return
        _onlineSubtitleSources.value = sources
        IptvSettingsStorage.saveOnlineSubtitleSources(Json.encodeToString(sources))
    }

    fun addOnlineSubtitleSource() {
        setOnlineSubtitleSources(_onlineSubtitleSources.value + OnlineSubtitleSource())
    }

    fun updateOnlineSubtitleSource(index: Int, username: String, password: String) {
        val current = _onlineSubtitleSources.value
        if (index !in current.indices) return
        setOnlineSubtitleSources(
            current.toMutableList().also { it[index] = OnlineSubtitleSource(username, password) },
        )
    }

    fun removeOnlineSubtitleSource(index: Int) {
        val current = _onlineSubtitleSources.value
        if (index !in current.indices) return
        setOnlineSubtitleSources(
            current.toMutableList().also { it.removeAt(index) },
        )
    }

    fun setOnlineSubtitlePreferredLanguages(codes: Set<String>) {
        ensureLoaded()
        if (_onlineSubtitlePreferredLanguages.value == codes) return
        _onlineSubtitlePreferredLanguages.value = codes
        IptvSettingsStorage.saveOnlineSubtitlePreferredLanguages(codes.joinToString(","))
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
