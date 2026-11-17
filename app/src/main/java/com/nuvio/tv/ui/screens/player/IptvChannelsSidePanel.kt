package com.nuvio.tv.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import coil3.compose.AsyncImage
import com.nuvio.tv.R
import com.nuvio.tv.matrix.iptv.FavoriteChannel
import com.nuvio.tv.matrix.iptv.IptvChannelRef
import com.nuvio.tv.matrix.iptv.IptvSettingsRepository
import com.nuvio.tv.matrix.iptv.XtreamClient
import com.nuvio.tv.ui.theme.NuvioTheme
import kotlinx.coroutines.delay

/** One flat entry in the sectioned panel list: a section header or a channel row. */
private sealed interface PanelEntry {
    data class Header(val title: String, val count: Int) : PanelEntry
    data class Channel(val ref: IptvChannelRef, val number: Int, val section: String) : PanelEntry
}

/**
 * Matrix 3.3 — the in-player channel list, rebuilt TiviMate-style: three
 * sections in one list — Recently watched (last 10), Favorites, then the
 * browsed channel list. Rows show number, logo, name and a favorite star;
 * OK switches to the focused channel, long-press OK toggles its favorite
 * star, BACK closes the panel. The current channel is highlighted and
 * focused on open. Played from a recents/favorites row, the next
 * player's "All channels" section stays the browsed list.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun IptvChannelsSidePanel(
    uiState: PlayerUiState,
    channelsFocusRequester: FocusRequester,
    onClose: () -> Unit,
    onChannelSelected: (IptvChannelRef) -> Unit,
    modifier: Modifier = Modifier,
) {
    IptvSettingsRepository.ensureLoaded()
    val favorites by IptvSettingsRepository.favoriteChannels.collectAsState()
    val recents by IptvSettingsRepository.recentChannels.collectAsState()
    val channels = uiState.iptvChannels
    val currentId = uiState.currentIptvStreamId
    val listState = rememberLazyListState()

    val favoriteIds = remember(favorites) { favorites.map { it.streamId }.toSet() }
    // Recents/favorites carry no stream URL; rebuild it the same way the
    // Live TV tab does so the rows play through the same path.
    val recentRefs = remember(recents) {
        recents.map {
            IptvChannelRef(
                it.streamId,
                it.name,
                it.iconUrl,
                XtreamClient.liveStreamUrl(it.streamId),
                it.groupName,
            )
        }
    }
    val favoriteRefs = remember(favorites) {
        favorites.map {
            IptvChannelRef(
                it.streamId,
                it.name,
                it.iconUrl,
                XtreamClient.liveStreamUrl(it.streamId),
                it.groupName,
            )
        }
    }
    val recentsTitle = stringResource(R.string.iptv_panel_recently_watched)
    val favoritesTitle = stringResource(R.string.iptv_panel_favourites)
    val allTitle = stringResource(R.string.iptv_panel_all_channels)
    val entries = remember(recentRefs, favoriteRefs, channels, recentsTitle, favoritesTitle, allTitle) {
        buildList {
            if (recentRefs.isNotEmpty()) {
                add(PanelEntry.Header(recentsTitle, recentRefs.size))
                recentRefs.forEachIndexed { index, ref ->
                    add(PanelEntry.Channel(ref, index + 1, "recent"))
                }
            }
            if (favoriteRefs.isNotEmpty()) {
                add(PanelEntry.Header(favoritesTitle, favoriteRefs.size))
                favoriteRefs.forEachIndexed { index, ref ->
                    add(PanelEntry.Channel(ref, index + 1, "fav"))
                }
            }
            if (channels.isNotEmpty()) {
                add(PanelEntry.Header(allTitle, channels.size))
                channels.forEachIndexed { index, ref ->
                    add(PanelEntry.Channel(ref, index + 1, "all"))
                }
            }
        }
    }
    // Initial focus/scroll target: the current channel, or the first
    // channel row when the browsed list is gone (lost handoff).
    val initialEntryIndex = remember(entries, currentId) {
        entries.indexOfFirst {
            it is PanelEntry.Channel && it.ref.streamId == currentId
        }.takeIf { it >= 0 }
            ?: entries.indexOfFirst { it is PanelEntry.Channel }.coerceAtLeast(0)
    }

    LaunchedEffect(entries, initialEntryIndex) {
        if (entries.isEmpty()) return@LaunchedEffect
        runCatching {
            listState.scrollToItem(initialEntryIndex)
            delay(32)
            channelsFocusRequester.requestFocus()
        }
    }

    fun toggleFavorite(channel: IptvChannelRef) {
        IptvSettingsRepository.toggleFavoriteChannel(
            FavoriteChannel(
                streamId = channel.streamId,
                name = channel.name,
                iconUrl = channel.icon,
                groupName = channel.groupName,
            ),
        )
    }

    // Right panel only (scrim is handled in PlayerScreen).
    // Dark fading background: transparent at the video edge, solid dark
    // at the outer edge (his order: "القائمة بلون داكن يتلاشا").
    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(520.dp)
            .clip(RoundedCornerShape(topStart = NuvioTheme.spacing.lg, bottomStart = NuvioTheme.spacing.lg))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        NuvioTheme.colors.BackgroundElevated
                    )
                )
            )
    ) {
        Column(modifier = Modifier.padding(NuvioTheme.spacing.xl)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.player_channels) +
                        " (" + channels.size + ")",
                    style = MaterialTheme.typography.headlineSmall,
                    color = NuvioTheme.colors.TextPrimary
                )

                DialogButton(
                    text = stringResource(R.string.episodes_panel_close),
                    onClick = onClose,
                    isPrimary = false
                )
            }

            Text(
                text = stringResource(R.string.iptv_long_press_favorite_hint),
                style = MaterialTheme.typography.bodySmall,
                color = NuvioTheme.colors.TextSecondary,
            )

            Spacer(modifier = Modifier.height(NuvioTheme.spacing.lg))

            if (entries.isEmpty()) {
                Text(
                    text = stringResource(R.string.channels_panel_no_channels),
                    style = MaterialTheme.typography.bodyLarge,
                    color = NuvioTheme.colors.TextPrimary.copy(alpha = 0.7f)
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(
                        entries,
                        key = { _, entry ->
                            when (entry) {
                                is PanelEntry.Header -> "header-${entry.title}"
                                is PanelEntry.Channel -> "${entry.section}-${entry.ref.streamId}"
                            }
                        },
                    ) { index, entry ->
                        when (entry) {
                            is PanelEntry.Header -> IptvPanelSectionHeader(
                                title = entry.title,
                                count = entry.count,
                            )
                            is PanelEntry.Channel -> IptvChannelPanelItem(
                                channel = entry.ref,
                                number = entry.number,
                                isCurrent = entry.ref.streamId == currentId,
                                isFavorite = entry.ref.streamId in favoriteIds,
                                focusRequester = channelsFocusRequester,
                                requestInitialFocus = index == initialEntryIndex,
                                onClick = { onChannelSelected(entry.ref) },
                                onFavoriteToggle = { toggleFavorite(entry.ref) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IptvPanelSectionHeader(
    title: String,
    count: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = NuvioTheme.colors.TextPrimary,
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = NuvioTheme.colors.TextSecondary,
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun IptvChannelPanelItem(
    channel: IptvChannelRef,
    number: Int,
    isCurrent: Boolean,
    isFavorite: Boolean,
    focusRequester: FocusRequester,
    requestInitialFocus: Boolean,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
) {
    Card(
        onClick = onClick,
        onLongClick = onFavoriteToggle,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (requestInitialFocus) Modifier.focusRequester(focusRequester) else Modifier),
        colors = CardDefaults.colors(
            containerColor = if (isCurrent) {
                NuvioTheme.colors.FocusBackground.copy(alpha = 0.55f)
            } else {
                NuvioTheme.colors.BackgroundCard
            },
            focusedContainerColor = NuvioTheme.colors.FocusBackground
        ),
        border = CardDefaults.border(
            focusedBorder = Border(
                border = NuvioTheme.focusRing.border(NuvioTheme.spacing.xxs),
                shape = RoundedCornerShape(NuvioTheme.radii.xl)
            )
        ),
        scale = CardDefaults.scale(focusedScale = 1.01f),
        shape = CardDefaults.shape(shape = RoundedCornerShape(NuvioTheme.radii.xl))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = NuvioTheme.colors.TextSecondary,
                textAlign = TextAlign.End,
                modifier = Modifier.width(32.dp),
            )
            if (isCurrent) {
                Text(
                    text = "▶",
                    style = MaterialTheme.typography.bodyLarge,
                    color = NuvioTheme.colors.Secondary
                )
            }
            if (!channel.icon.isNullOrBlank()) {
                AsyncImage(
                    model = channel.icon,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp)
                )
            }
            Text(
                text = channel.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                ),
                color = NuvioTheme.colors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (isFavorite) "★" else "☆",
                style = MaterialTheme.typography.titleLarge,
                color = if (isFavorite) NuvioTheme.colors.Secondary
                else NuvioTheme.colors.TextSecondary,
            )
        }
    }
}
