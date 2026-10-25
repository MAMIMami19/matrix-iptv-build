package com.nuvio.app.features.player

import androidx.compose.ui.Modifier
import com.nuvio.app.features.party.PartyMedia

internal data class PlayerScreenArgs(
    val profileId: Int,
    val title: String,
    val sourceUrl: String,
    val sourceAudioUrl: String?,
    val sourceHeaders: Map<String, String>,
    val sourceResponseHeaders: Map<String, String>,
    val streamType: String?,
    val providerName: String,
    val streamTitle: String,
    val streamSubtitle: String?,
    val initialBingeGroup: String?,
    val pauseDescription: String?,
    val onBack: () -> Unit,
    val onOpenInExternalPlayer: ((ExternalPlayerPlaybackRequest) -> Unit)?,
    val onOpenExternalUrl: ((String) -> Unit)?,
    val modifier: Modifier,
    val logo: String?,
    val poster: String?,
    val background: String?,
    val seasonNumber: Int?,
    val episodeNumber: Int?,
    val episodeTitle: String?,
    val episodeThumbnail: String?,
    val contentType: String?,
    val videoId: String?,
    val parentMetaId: String,
    val parentMetaType: String,
    val providerAddonId: String?,
    val externalSubtitles: List<com.nuvio.app.features.streams.StreamSubtitle> = emptyList(),
    val torrentInfoHash: String?,
    val torrentFileIdx: Int?,
    val torrentFilename: String?,
    val torrentTrackers: List<String>,
    val initialPositionMs: Long,
    val initialProgressFraction: Float?,
    val contentLanguage: String? = null,
    val launchId: Long? = null,
    val sourceFilename: String? = null,
    val onOpenPartyMedia: (PartyMedia) -> Unit = {},
    val stream: com.nuvio.app.features.streams.StreamItem? = null,
    val iptvPlaylist: List<com.nuvio.app.features.iptv.IptvPlayerListItem> = emptyList(),
    /**
     * Mini-player mode (YouTube-style): the player is hosted at the app
     * root above the tabs instead of as a full-screen destination. When
     * true, only the video surface plus a compact mini bar render; all
     * overlays, controls, modals and gestures are skipped. The surface
     * itself stays in the same composition slot as in full mode (only its
     * size modifier changes), so minimizing/expanding never restarts
     * playback. Null callbacks mean "no mini-player host" (classic route).
     */
    val minimized: Boolean = false,
    val onExpandMiniPlayer: (() -> Unit)? = null,
    val onCloseMiniPlayer: (() -> Unit)? = null,
)
