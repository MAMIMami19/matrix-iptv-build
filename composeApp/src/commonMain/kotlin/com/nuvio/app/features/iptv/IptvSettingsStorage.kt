package com.nuvio.app.features.iptv

/**
 * App-level (not profile-scoped) persistence for the IPTV / Live TV settings.
 *
 * The online-subtitle sources below are user-filled only: they are never
 * prefilled and no credential from anywhere else is ever written here.
 */
internal expect object IptvSettingsStorage {
    fun loadLockAdultCategories(): Boolean?
    fun saveLockAdultCategories(enabled: Boolean)
    fun loadClearHistoryOnClose(): Boolean?
    fun saveClearHistoryOnClose(enabled: Boolean)
    fun loadLockSettings(): Boolean?
    fun saveLockSettings(enabled: Boolean)
    fun loadIptvPin(): String?
    fun saveIptvPin(pin: String)
    fun loadSystemPipEnabled(): Boolean?
    fun saveSystemPipEnabled(enabled: Boolean)
    fun loadOsdDurationMs(): Int?
    fun saveOsdDurationMs(durationMs: Int)
    fun loadAudioLanguage(): String?
    fun saveAudioLanguage(code: String)
    fun loadSubtitleLanguage(): String?
    fun saveSubtitleLanguage(code: String)
    fun loadOnlineSubtitleUsername(): String?
    fun saveOnlineSubtitleUsername(username: String)
    fun loadOnlineSubtitlePassword(): String?
    fun saveOnlineSubtitlePassword(password: String)
    /** JSON array of online-subtitle sources (see OnlineSubtitleSource). */
    fun loadOnlineSubtitleSources(): String?
    fun saveOnlineSubtitleSources(json: String)
    fun loadOnlineSubtitlePreferredLanguages(): String?
    fun saveOnlineSubtitlePreferredLanguages(codesCsv: String)
}
