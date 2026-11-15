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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import coil3.compose.AsyncImage
import com.nuvio.tv.R
import com.nuvio.tv.matrix.iptv.IptvChannelRef
import com.nuvio.tv.ui.theme.NuvioTheme
import kotlinx.coroutines.delay

/**
 * Matrix IPTV: the in-player channel list. Slides in from the right like
 * the episodes panel. The list is the one the user was browsing when they
 * pressed play (group, favorites or search results); the current channel
 * is highlighted and focused on open. OK switches to the focused channel
 * (the player is replaced via navigation), BACK closes the panel.
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
    val channels = uiState.iptvChannels
    val currentId = uiState.currentIptvStreamId
    val listState = rememberLazyListState()
    val currentIndex = remember(channels, currentId) {
        channels.indexOfFirst { it.streamId == currentId }.coerceAtLeast(0)
    }

    LaunchedEffect(channels, currentIndex) {
        if (channels.isEmpty()) return@LaunchedEffect
        runCatching {
            listState.scrollToItem(currentIndex)
            delay(32)
            channelsFocusRequester.requestFocus()
        }
    }

    // Right panel only (scrim is handled in PlayerScreen)
    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(520.dp)
            .clip(RoundedCornerShape(topStart = NuvioTheme.spacing.lg, bottomStart = NuvioTheme.spacing.lg))
            .background(NuvioTheme.colors.BackgroundElevated)
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

            Spacer(modifier = Modifier.height(NuvioTheme.spacing.lg))

            if (channels.isEmpty()) {
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
                    itemsIndexed(channels, key = { _, channel -> channel.streamId }) { index, channel ->
                        IptvChannelPanelItem(
                            channel = channel,
                            isCurrent = channel.streamId == currentId,
                            focusRequester = channelsFocusRequester,
                            requestInitialFocus = index == currentIndex,
                            onClick = { onChannelSelected(channel) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun IptvChannelPanelItem(
    channel: IptvChannelRef,
    isCurrent: Boolean,
    focusRequester: FocusRequester,
    requestInitialFocus: Boolean,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
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
                color = NuvioTheme.colors.TextPrimary
            )
        }
    }
}
