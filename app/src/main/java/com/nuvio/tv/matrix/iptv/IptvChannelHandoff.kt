package com.nuvio.tv.matrix.iptv

/**
 * One channel entry for the in-player channel list: everything the player
 * needs to switch to it. The stream URL is built at handoff time so the
 * player and the navigation never touch XtreamClient directly.
 *
 * Public (not internal): it appears in PlayerUiState and PlayerScreen,
 * both public, so an internal type would fail the build.
 */
data class IptvChannelRef(
    val streamId: Int,
    val name: String,
    val icon: String?,
    val streamUrl: String,
)

/**
 * One-shot, in-memory handoff from the IPTV Live TV tab to the Player
 * ViewModel: the channel list the user was browsing (group, favorites or
 * search results) + the channel being played. Set immediately before
 * navigating to the player; consumed once when the PlayerViewModel is
 * created. A process kill between the two just loses the handoff — the
 * Channels button stays hidden and playback is unaffected.
 */
internal object IptvChannelHandoff {
    internal data class Payload(
        val channels: List<IptvChannelRef>,
        val currentStreamId: Int,
    )

    @Volatile
    private var pending: Payload? = null

    fun set(channels: List<IptvChannelRef>, currentStreamId: Int) {
        pending = Payload(channels, currentStreamId)
    }

    fun take(): Payload? {
        val payload = pending
        pending = null
        return payload
    }
}
