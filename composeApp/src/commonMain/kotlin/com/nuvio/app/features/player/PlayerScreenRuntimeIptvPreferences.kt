package com.nuvio.app.features.player

import com.nuvio.app.features.iptv.IptvSettingsRepository

/**
 * IPTV-scoped player preferences. IPTV launches are identified by the
 * "iptv:<streamId>" parentMetaId set in MainAppContent's onXtreamPlay.
 * Non-IPTV launches always use the global player settings.
 */
internal val PlayerScreenRuntime.isIptvLaunch: Boolean
    get() = parentMetaId.startsWith("iptv:")

/** IPTV audio language override (default System); global setting otherwise. */
internal val PlayerScreenRuntime.effectivePreferredAudioLanguage: String
    get() = if (isIptvLaunch) {
        IptvSettingsRepository.audioLanguageValue
    } else {
        playerSettingsUiState.preferredAudioLanguage
    }

/** IPTV subtitle language override (default System); global setting otherwise. */
internal val PlayerScreenRuntime.effectivePreferredSubtitleLanguage: String
    get() = if (isIptvLaunch) {
        IptvSettingsRepository.subtitleLanguageValue
    } else {
        playerSettingsUiState.preferredSubtitleLanguage
    }

/**
 * IPTV launches use no secondary subtitle language: the IPTV settings screen
 * exposes a single subtitle language, so the global secondary must not leak in.
 */
internal val PlayerScreenRuntime.effectiveSecondaryPreferredSubtitleLanguage: String?
    get() = if (isIptvLaunch) {
        null
    } else {
        playerSettingsUiState.secondaryPreferredSubtitleLanguage
    }
