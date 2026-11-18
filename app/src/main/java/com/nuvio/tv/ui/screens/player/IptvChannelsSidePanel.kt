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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
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
import com.nuvio.tv.matrix.iptv.XtreamCategory
import com.nuvio.tv.matrix.iptv.XtreamClient
import com.nuvio.tv.ui.theme.NuvioTheme
import kotlinx.coroutines.delay

/** One flat entry in the sectioned panel list: a section header, a channel row or a group row. */
private sealed interface PanelEntry {
    data class Header(val title: String, val count: Int) : PanelEntry
    data class Channel(val ref: IptvChannelRef, val number: Int, val section: String) : PanelEntry
    data class Group(val categoryId: String, val name: String, val count: Int, val isSelected: Boolean) : PanelEntry
}

/**
 * Matrix 3.4 — the in-player channel list, TiviMate-style: four sections
 * in one list — Channels (the browsed list, or the selected group's
 * channels), Groups (one row per Xtream group, "All channels" resets the
 * filter), Favorites (the favorites picker), then Recently watched (last
 * 10) at the BOTTOM. Rows show number, logo, name and a favorite star;
 * OK switches to the focused channel, long-press OK toggles its favorite
 * star, BACK closes the panel. The current channel is highlighted and
 * focused on open. Played from a group/favorites/recents row, the next
 * player's list is the list the channel was picked from.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun IptvChannelsSidePanel(
    uiState: PlayerUiState,
    channelsFocusRequester: FocusRequester,
    onClose: () -> Unit,
    onChannelSelected: (IptvChannelRef, List<IptvChannelRef>) -> Unit,
    modifier: Modifier = Modifier,
) {
    IptvSettingsRepository.ensureLoaded()
    val favorites by IptvSettingsRepository.favoriteChannels.collectAsState()
    val recents by IptvSettingsRepository.recentChannels.collectAsState()
    val channels = uiState.iptvChannels
    val currentId = uiState.currentIptvStreamId
    val listState = rememberLazyListState()

    // Matrix 3.4: groups. The entry screen already loaded the categories
    // and the whole live catalog, so both calls below are cache hits in
    // the normal flow; the per-group fetch only runs if the catalog
    // cache is cold (played before the entry load finished).
    var groups by remember { mutableStateOf<List<XtreamCategory>?>(null) }
    var selectedGroup by remember { mutableStateOf<XtreamCategory?>(null) }
    var groupChannels by remember { mutableStateOf<List<IptvChannelRef>?>(null) }
    var groupLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        runCatching { groups = XtreamClient.liveCategories() }
    }
    LaunchedEffect(selectedGroup) {
        val group = selectedGroup
        if (group == null) {
            groupChannels = null
            groupLoading = false
            return@LaunchedEffect
        }
        groupLoading = true
        val cached = XtreamClient.cachedLiveStreams()
        groupChannels = if (cached != null) {
            cached.filter { it.categoryId == group.categoryId }.map { stream ->
                IptvChannelRef(
                    stream.streamId,
                    stream.name,
                    stream.streamIcon,
                    XtreamClient.liveStreamUrl(stream.streamId),
                    group.categoryName,
                )
            }
        } else {
            runCatching { XtreamClient.liveStreams(group.categoryId) }.getOrNull()
                ?.map { stream ->
                    IptvChannelRef(
                        stream.streamId,
                        stream.name,
                        stream.streamIcon,
                        XtreamClient.liveStreamUrl(stream.streamId),
                        group.categoryName,
                    )
                }.orEmpty()
        }
        groupLoading = false
    }
    // Per-group channel counts from the cached whole catalog (empty map
    // when the cache is cold — group rows then show no count).
    val groupCounts = remember(groups) {
        XtreamClient.cachedLiveStreams()
            ?.groupingBy { it.categoryId }
            ?.eachCount()
            .orEmpty()
    }

    // Matrix 3.5: favorites/recents belong to the source they came from —
    // stream ids collide across lines, so only this source's records play.
    val sourceIndex by XtreamClient.selectedSourceIndex.collectAsState()
    // Recents/favorites carry no stream URL; rebuild it the same way the
    // Live TV tab does so the rows play through the same path.
    val recentRefs = remember(recents, sourceIndex) {
        recents.filter { it.sourceIndex == sourceIndex }.map {
            IptvChannelRef(
                it.streamId,
                it.name,
                it.iconUrl,
                XtreamClient.liveStreamUrl(it.streamId),
                it.groupName,
            )
        }
    }
    val favoriteRefs = remember(favorites, sourceIndex) {
        favorites.filter { it.sourceIndex == sourceIndex }.map {
            IptvChannelRef(
                it.streamId,
                it.name,
                it.iconUrl,
                XtreamClient.liveStreamUrl(it.streamId),
                it.groupName,
            )
        }
    }
    val favoriteIds = remember(favoriteRefs) { favoriteRefs.map { it.streamId }.toSet() }
    val recentsTitle = stringResource(R.string.iptv_panel_recently_watched)
    val favoritesTitle = stringResource(R.string.iptv_panel_favourites)
    val allTitle = stringResource(R.string.iptv_panel_all_channels)
    val groupsTitle = stringResource(R.string.iptv_panel_groups)
    // The Channels section shows the browsed list, or the selected
    // group's channels once they are loaded.
    val shownChannels = if (selectedGroup == null) channels else (groupChannels ?: emptyList())
    val shownTitle = selectedGroup?.categoryName ?: allTitle
    val entries = remember(
        shownChannels, shownTitle, groups, groupCounts, selectedGroup,
        groupLoading, recentRefs, favoriteRefs,
        recentsTitle, favoritesTitle, allTitle, groupsTitle,
    ) {
        buildList {
            if (shownChannels.isNotEmpty() || groupLoading) {
                add(PanelEntry.Header(shownTitle, shownChannels.size))
                shownChannels.forEachIndexed { index, ref ->
                    add(PanelEntry.Channel(ref, index + 1, "shown"))
                }
            }
            val loadedGroups = groups
            if (loadedGroups != null) {
                add(PanelEntry.Header(groupsTitle, loadedGroups.size))
                add(PanelEntry.Group("", allTitle, channels.size, selectedGroup == null))
                loadedGroups.forEach { group ->
                    add(
                        PanelEntry.Group(
                            group.categoryId,
                            group.categoryName,
                            groupCounts[group.categoryId] ?: -1,
                            selectedGroup?.categoryId == group.categoryId,
                        ),
                    )
                }
            }
            if (favoriteRefs.isNotEmpty()) {
                add(PanelEntry.Header(favoritesTitle, favoriteRefs.size))
                favoriteRefs.forEachIndexed { index, ref ->
                    add(PanelEntry.Channel(ref, index + 1, "fav"))
                }
            }
            if (recentRefs.isNotEmpty()) {
                add(PanelEntry.Header(recentsTitle, recentRefs.size))
                recentRefs.forEachIndexed { index, ref ->
                    add(PanelEntry.Channel(ref, index + 1, "recent"))
                }
            }
        }
    }
    // Initial focus/scroll target: the current channel, or the first
    // channel row when the browsed list is gone (lost handoff) or the
    // selected group does not contain the current channel.
    val initialEntryIndex = remember(entries, currentId) {
        entries.indexOfFirst {
            it is PanelEntry.Channel && it.ref.streamId == currentId
        }.takeIf { it >= 0 }
            ?: entries.indexOfFirst { it is PanelEntry.Channel }.coerceAtLeast(0)
    }

    // Matrix 3.5 — section jump. The panel is one flat list; with the
    // full 12,508-channel catalog the Groups/Favorites/Recents sections
    // sit below ALL channels, practically unreachable by DOWN-stepping.
    // LEFT/RIGHT jumps focus straight to the previous/next section's
    // first row (the player lets the open panel handle all keys, and the
    // vertical list has no horizontal targets, so these keys were free).
    // The target is section-based, not an index, so it stays valid when
    // the entries rebuild (groups loading, group pick).
    var jumpSection by remember { mutableStateOf<String?>(null) }
    var jumpNonce by remember { mutableStateOf(0) }
    var focusedSection by remember { mutableStateOf<String?>(null) }
    val sectionOrder = remember(entries) {
        buildList {
            if (entries.any { it is PanelEntry.Channel && it.section == "shown" }) add("shown")
            if (entries.any { it is PanelEntry.Group }) add("groups")
            if (entries.any { it is PanelEntry.Channel && it.section == "fav" }) add("fav")
            if (entries.any { it is PanelEntry.Channel && it.section == "recent" }) add("recent")
        }
    }
    fun sectionFirstRowIndex(section: String): Int =
        entries.indexOfFirst { entry ->
            when (entry) {
                is PanelEntry.Channel -> entry.section == section
                is PanelEntry.Group -> section == "groups"
                else -> false
            }
        }
    val jumpTargetIndex = jumpSection
        ?.let { sectionFirstRowIndex(it) }
        ?.takeIf { it >= 0 }
    val focusTargetIndex = jumpTargetIndex ?: initialEntryIndex
    // When jumping, scroll to the section header (one above the first
    // row) so the header stays visible; focus goes to the first row.
    val scrollTargetIndex = if (jumpTargetIndex != null) {
        (jumpTargetIndex - 1).coerceAtLeast(0)
    } else {
        focusTargetIndex
    }

    LaunchedEffect(entries, focusTargetIndex, jumpNonce) {
        if (entries.isEmpty()) return@LaunchedEffect
        runCatching {
            listState.scrollToItem(scrollTargetIndex)
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
                // Matrix 3.5: tag the source — stream ids collide across lines.
                sourceIndex = XtreamClient.selectedSourceIndex.value,
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
            // Matrix 3.5: LEFT/RIGHT jumps between sections (see above).
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                if (event.nativeKeyEvent.repeatCount != 0) return@onPreviewKeyEvent false
                val direction = when (event.key) {
                    Key.DirectionLeft -> -1
                    Key.DirectionRight -> 1
                    else -> return@onPreviewKeyEvent false
                }
                val order = sectionOrder
                if (order.size < 2) return@onPreviewKeyEvent false
                val current = focusedSection?.let { order.indexOf(it) }
                    ?.takeIf { it >= 0 } ?: 0
                val next = (current + direction).coerceIn(0, order.lastIndex)
                if (next == current) return@onPreviewKeyEvent false
                jumpSection = order[next]
                jumpNonce++
                true
            }
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

            // Matrix 3.5: LEFT/RIGHT jumps between sections.
            Text(
                text = stringResource(R.string.iptv_panel_sections_hint),
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
                                is PanelEntry.Group -> "group-${entry.categoryId}"
                            }
                        },
                    ) { index, entry ->
                        when (entry) {
                            is PanelEntry.Header -> IptvPanelSectionHeader(
                                title = entry.title,
                                count = entry.count,
                            )
                            is PanelEntry.Group -> IptvGroupPanelItem(
                                name = entry.name,
                                count = entry.count,
                                isSelected = entry.isSelected,
                                onClick = {
                                    // Matrix 3.5: clear any section jump —
                                    // after picking a group, focus follows
                                    // the new list's initial entry.
                                    jumpSection = null
                                    selectedGroup = if (entry.categoryId.isEmpty()) {
                                        null
                                    } else {
                                        groups?.find { it.categoryId == entry.categoryId }
                                    }
                                },
                                onFocused = { focusedSection = "groups" },
                            )
                            is PanelEntry.Channel -> IptvChannelPanelItem(
                                channel = entry.ref,
                                number = entry.number,
                                isCurrent = entry.ref.streamId == currentId,
                                isFavorite = entry.ref.streamId in favoriteIds,
                                focusRequester = channelsFocusRequester,
                                requestFocus = index == focusTargetIndex,
                                onFocused = { focusedSection = entry.section },
                                onClick = {
                                    // The next player's list is the list
                                    // the channel was picked from.
                                    val source = when (entry.section) {
                                        "fav" -> favoriteRefs
                                        "recent" -> recentRefs
                                        else -> shownChannels
                                    }
                                    onChannelSelected(entry.ref, source)
                                },
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
    requestFocus: Boolean,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
) {
    Card(
        onClick = onClick,
        onLongClick = onFavoriteToggle,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (requestFocus) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { if (it.isFocused) onFocused() },
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

/**
 * Matrix 3.4 — one group row in the in-player panel: a chevron, the
 * group name and its channel count (hidden when the count is unknown,
 * i.e. the whole-catalog cache was cold). The selected group is
 * highlighted like the current channel.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun IptvGroupPanelItem(
    name: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    onFocused: () -> Unit = {},
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
            .onFocusChanged { if (it.isFocused) onFocused() },
        colors = CardDefaults.colors(
            containerColor = if (isSelected) {
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
                text = "▸",
                style = MaterialTheme.typography.bodyLarge,
                color = NuvioTheme.colors.TextSecondary,
            )
            Text(
                text = name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                ),
                color = NuvioTheme.colors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            if (count >= 0) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = NuvioTheme.colors.TextSecondary,
                )
            }
        }
    }
}
