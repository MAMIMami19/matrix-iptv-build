package com.nuvio.app.features.iptv

/**
 * One playable entry for the in-player list offered during IPTV / Live TV
 * playback. The list is fixed at launch time (the tab's current list) and
 * travels with the [com.nuvio.app.features.player.PlayerLaunch]; selecting
 * an entry switches the player source in the same session.
 *
 * Only playable items belong here: live channels, movies, and series
 * episodes. Series themselves are not playable (they open an episode list
 * in the tab) and are therefore never playlist entries.
 */
internal data class IptvPlayerListItem(
    val title: String,
    val streamId: Int,
    val url: String,
    /** "tv", "movie" or "series". */
    val contentType: String,
    val iconUrl: String? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    /** Live TV group name, when known (used for favorite adult filtering). */
    val groupName: String? = null,
)

internal fun XtreamLiveStream.toPlayerListItem(groupName: String? = null): IptvPlayerListItem =
    IptvPlayerListItem(
        title = name,
        streamId = streamId,
        url = XtreamClient.liveStreamUrl(streamId),
        contentType = "tv",
        iconUrl = streamIcon,
        groupName = groupName,
    )

internal fun FavoriteChannel.toPlayerListItem(): IptvPlayerListItem =
    IptvPlayerListItem(
        title = name,
        streamId = streamId,
        url = XtreamClient.liveStreamUrl(streamId),
        contentType = "tv",
        iconUrl = iconUrl,
        groupName = groupName,
    )

internal fun XtreamVodStream.toPlayerListItem(): IptvPlayerListItem =
    IptvPlayerListItem(
        title = name,
        streamId = streamId,
        url = XtreamClient.movieStreamUrl(streamId, containerExtension),
        contentType = "movie",
        iconUrl = streamIcon,
    )

internal fun XtreamEpisode.toPlayerListItem(
    seriesName: String,
    episodeTitle: String,
    seasonNumber: Int?,
): IptvPlayerListItem =
    IptvPlayerListItem(
        title = "$seriesName - $episodeTitle",
        streamId = id,
        url = XtreamClient.episodeStreamUrl(id, containerExtension),
        contentType = "series",
        iconUrl = null,
        seasonNumber = seasonNumber,
        episodeNumber = episodeNum,
    )
