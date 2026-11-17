package com.nuvio.tv.matrix.iptv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nuvio.tv.R
import com.nuvio.tv.ui.theme.NuvioTheme
import kotlinx.coroutines.launch

/**
 * Play callback: title, stream id, resolved stream URL, content type
 * ("tv", "movie", "series"), poster URL, season and episode (series only).
 *
 * Live TV plays hand the shown list to the player via
 * [IptvChannelHandoff] so its Channels panel offers the same list
 * in-player. The player gets the standard metadata (contentId
 * "iptv:<id>") so Continue Watching works.
 */
internal typealias XtreamPlayHandler = (
    title: String,
    streamId: Int,
    url: String,
    contentType: String,
    poster: String?,
    season: Int?,
    episode: Int?,
) -> Unit

/**
 * Matrix 3.1: open the merged streams panel for an IPTV VOD title —
 * videoId (IMDb id), content type ("movie"/"series"), clean title, poster,
 * season/episode (series only), episode name and year. The exact IPTV
 * stream travels via [IptvMergedStreamHandoff], never in the route.
 */
internal typealias MergedStreamsHandler = (
    videoId: String,
    contentType: String,
    title: String,
    poster: String?,
    season: Int?,
    episode: Int?,
    episodeName: String?,
    year: String?,
) -> Unit

@Composable
private fun IptvLoading() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun IptvError(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        // Generic text only: transport errors can embed request URLs, so they
        // are never surfaced here.
        Text("Failed to load")
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onRetry) { Text("Retry") }
    }
}

@Composable
private fun IptvEmpty(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Text(message)
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onRetry) { Text("Retry") }
    }
}

@Composable
private fun IptvNotConfigured() {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Text("IPTV is not configured in this build")
    }
}

@Composable
private fun IptvSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    label: String = "Search",
) {
    // TV remote: this field was the only IPTV element without a focus ring,
    // so focus on it was invisible ("don't know where I am"). The violet
    // ring + the Search IME action (hides the keyboard, focus stays on the
    // visibly-ringed field) fix the stuck-at-search feeling.
    //
    // Matrix 3.2: the ring alone did not fix the trap — a focused text
    // field swallows DPAD up/down (cursor movement), so the remote stayed
    // stuck on the search line. Intercept the keys BEFORE the field sees
    // them (onPreviewKeyEvent) and move focus out: DOWN to the channel
    // list below, UP to the element above (Back button / tabs). UP from
    // the first list row back to the field is plain focus search.
    //
    // Matrix 3.4: the 3.2 intercept was NOT enough — once the on-screen
    // keyboard opened (focus alone opened it), DPAD DOWN never reached
    // the app at all (the IME ate it), so focus could not leave the field
    // until BACK dismissed the keyboard. The field now stays readOnly —
    // focusable, but no keyboard — until the user presses OK on it, which
    // enters edit mode and opens the keyboard. DOWN from the (readOnly)
    // field moves straight to the channel list.
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var editing by remember { mutableStateOf(false) }
    // Show the keyboard once the field actually becomes editable (the
    // recomposition is async, so request it from an effect, not from the
    // key handler).
    LaunchedEffect(editing) {
        if (editing) keyboardController?.show()
    }
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        label = { Text(label) },
        singleLine = true,
        readOnly = !editing,
        modifier = Modifier.fillMaxWidth()
            .onFocusChanged { state ->
                // Leaving the field always exits edit mode (and hides the
                // keyboard if it was open).
                if (!state.hasFocus && editing) {
                    editing = false
                    keyboardController?.hide()
                }
            }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionCenter, Key.Enter -> {
                        // OK on the (readOnly) field enters edit mode.
                        if (!editing) {
                            editing = true
                            true
                        } else {
                            false
                        }
                    }
                    Key.DirectionDown, Key.DirectionUp -> {
                        val direction = if (event.key == Key.DirectionDown) {
                            FocusDirection.Down
                        } else {
                            FocusDirection.Up
                        }
                        focusManager.moveFocus(direction)
                    }
                    else -> false
                }
            }
            .tvFocusRing(cornerRadius = 8.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = {
            keyboardController?.hide()
            editing = false
        }),
        trailingIcon = if (query.isNotEmpty()) {
            { TextButton(onClick = { onQueryChange("") }) { Text("✕") } }
        } else {
            null
        },
    )
    Spacer(modifier = Modifier.height(8.dp))
}

/**
 * Poster card: poster image on top, title always visible below it.
 * Used in VOD shelves (fixed 120dp width) and in the full-category grid
 * (fills the grid cell width).
 */
@Composable
private fun IptvPosterCard(
    imageUrl: String?,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .then(
                if (focusRequester != null) {
                    Modifier.focusRequester(focusRequester)
                } else {
                    Modifier
                },
            )
            .tvFocusRing(cornerRadius = 8.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(170.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Full-width channel/episode row: icon on the left, name on top and an info
 * line underneath (EPG placeholder "Pas d'informations" for live channels).
 */
@Composable
private fun IptvChannelRow(
    iconUrl: String?,
    name: String,
    infoLine: String,
    onClick: () -> Unit,
    isFavorite: Boolean? = null,
    onFavoriteToggle: (() -> Unit)? = null,
) {
    Row(
        // Long-press OK on a channel toggles its favorite star (the ★ button
        // at the row's end is hard to reach by remote); the star still works.
        modifier = Modifier.fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { onFavoriteToggle?.invoke() },
            )
            .tvFocusRing(cornerRadius = 8.dp)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(56.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (!iconUrl.isNullOrBlank()) {
                AsyncImage(
                    model = iconUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    text = "▶",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(modifier = Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = infoLine,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // Favorite star: only for live channels (null = no star, e.g. episodes).
        if (isFavorite != null && onFavoriteToggle != null) {
            TextButton(onClick = onFavoriteToggle) {
                Text(if (isFavorite) "★" else "☆")
            }
        }
    }
}

/**
 * Matrix 3.3 — TiviMate-style Live TV groups. The rail offers the whole
 * catalog, favorites and recents first, then the Xtream groups.
 */
private sealed interface LiveTvGroup {
    data object All : LiveTvGroup
    data object Favorites : LiveTvGroup
    data object Recents : LiveTvGroup
    data class Category(val category: XtreamCategory) : LiveTvGroup
}

/**
 * One row in the channel list, normalized from Xtream streams, favorites
 * and recents so the list, the handoff and playback all share one shape.
 */
private data class LiveTvRowItem(
    val streamId: Int,
    val name: String,
    val iconUrl: String?,
    val groupName: String?,
    val isFavorite: Boolean,
)

private fun XtreamLiveStream.toRowItem(
    categoryNameById: Map<String, String>,
    favorites: List<FavoriteChannel>,
): LiveTvRowItem = LiveTvRowItem(
    streamId = streamId,
    name = name,
    iconUrl = streamIcon,
    groupName = categoryNameById[categoryId],
    isFavorite = favorites.any { it.streamId == streamId },
)

private fun FavoriteChannel.toRowItem(isFavorite: Boolean): LiveTvRowItem =
    LiveTvRowItem(
        streamId = streamId,
        name = name,
        iconUrl = iconUrl,
        groupName = groupName,
        isFavorite = isFavorite,
    )

/**
 * Matrix 3.3 — the Live TV tab, rebuilt TiviMate-style: a two-pane layout.
 * The groups rail (All channels, Favorites, Recents, then the Xtream
 * groups with counts) sits on the starting side; the selected group's
 * channels fill the rest, with number, logo, name and favorite star.
 * Group channels come from the already-loaded whole catalog when it is
 * available (instant switching), otherwise from a per-group fetch cached
 * for the session. OK plays, long-press OK toggles the favorite star.
 * BACK clears an active search; otherwise it propagates to the root
 * exit flow.
 */
@Composable
internal fun LiveTvScreen(
    modifier: Modifier = Modifier,
    onPlay: XtreamPlayHandler,
) {
    IptvSettingsRepository.ensureLoaded()
    val pin by IptvSettingsRepository.pin.collectAsState()
    val sessionUnlocked by IptvSettingsRepository.sessionUnlocked.collectAsState()
    val lockAdult by IptvSettingsRepository.lockAdultCategories.collectAsState()
    val needsPin = pin.isNotEmpty() && !sessionUnlocked
    val adultHidden = lockAdult && !sessionUnlocked
    // TV remote (DPAD): focus the first rail item when this tab becomes active.
    val initialFocus = rememberTvInitialFocus()

    if (!XtreamConfig.isConfigured) {
        IptvNotConfigured()
        return
    }

    if (needsPin) {
        Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
            Text("Live TV", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(32.dp))
            Text("Locked")
        }
        MatrixPinDialog(
            title = "IPTV",
            onVerify = { entered -> entered == pin },
            onDismiss = {},
            onVerified = { IptvSettingsRepository.setSessionUnlocked(true) },
            dismissable = false,
        )
        return
    }

    var categories by remember { mutableStateOf<List<XtreamCategory>?>(null) }
    var selectedGroup by remember { mutableStateOf<LiveTvGroup>(LiveTvGroup.All) }
    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    // Full catalog, loaded once in the background: powers the "All
    // channels" group, the per-group count badges and instant group
    // switching. Null while loading; a failure leaves it null (group
    // channels then come from per-group fetches, never guessed).
    var allChannels by remember { mutableStateOf<List<XtreamLiveStream>?>(null) }
    var allChannelsFailed by remember { mutableStateOf(false) }
    // Per-group fetch cache, used only before the whole catalog arrives.
    val groupCache = remember { mutableStateMapOf<String, List<XtreamLiveStream>>() }
    var groupLoading by remember { mutableStateOf(false) }
    var groupFailed by remember { mutableStateOf(false) }
    val favorites by IptvSettingsRepository.favoriteChannels.collectAsState()
    val recents by IptvSettingsRepository.recentChannels.collectAsState()
    val scope = rememberCoroutineScope()
    val channelListState = rememberLazyListState()

    // TV remote BACK clears an active search instead of leaving the tab.
    BackHandler(enabled = query.isNotBlank()) {
        query = ""
    }

    fun loadCategories() {
        scope.launch {
            loading = true
            failed = false
            try {
                categories = XtreamClient.liveCategories()
            } catch (_: Exception) {
                failed = true
            }
            loading = false
        }
    }

    fun loadAllChannels() {
        if (allChannels != null || allChannelsFailed) return
        scope.launch {
            try {
                allChannels = XtreamClient.liveStreamsAll()
            } catch (_: Exception) {
                allChannelsFailed = true
            }
        }
    }

    fun loadGroupChannels(category: XtreamCategory, force: Boolean = false) {
        if (!force && groupCache.containsKey(category.categoryId)) return
        scope.launch {
            groupLoading = true
            groupFailed = false
            try {
                groupCache[category.categoryId] = XtreamClient.liveStreams(category.categoryId)
            } catch (_: Exception) {
                groupFailed = true
            }
            groupLoading = false
        }
    }

    fun playRowItem(item: LiveTvRowItem, list: List<LiveTvRowItem>) {
        IptvSettingsRepository.addPlayedIptvKey(
            "iptv:${item.streamId}",
            "iptv:${item.streamId}",
        )
        // Hand the shown list to the player so its Channels panel can
        // offer the same list in-player.
        IptvChannelHandoff.set(
            list.map {
                IptvChannelRef(
                    it.streamId,
                    it.name,
                    it.iconUrl,
                    XtreamClient.liveStreamUrl(it.streamId),
                    it.groupName,
                )
            },
            item.streamId,
        )
        onPlay(
            item.name,
            item.streamId,
            XtreamClient.liveStreamUrl(item.streamId),
            "tv",
            item.iconUrl,
            null,
            null,
        )
    }

    fun toggleFavorite(item: LiveTvRowItem) {
        IptvSettingsRepository.toggleFavoriteChannel(
            FavoriteChannel(
                streamId = item.streamId,
                name = item.name,
                iconUrl = item.iconUrl,
                groupName = item.groupName,
            ),
        )
    }

    LaunchedEffect(Unit) {
        loadCategories()
        loadAllChannels()
    }

    // Fetch the selected group's channels when the whole catalog is not
    // available yet (and the group is not already cached).
    val selectedCategory = (selectedGroup as? LiveTvGroup.Category)?.category
    LaunchedEffect(selectedCategory, allChannels) {
        val category = selectedCategory ?: return@LaunchedEffect
        if (allChannels == null && !groupCache.containsKey(category.categoryId)) {
            loadGroupChannels(category)
        }
    }

    // Back to the top whenever the group changes.
    LaunchedEffect(selectedGroup) {
        channelListState.scrollToItem(0)
    }

    val categoryNameById = categories.orEmpty()
        .associate { it.categoryId to it.categoryName }
    val isAdultGroup: (String?) -> Boolean = { groupName ->
        adultHidden && groupName != null &&
            IptvSettingsRepository.isAdultCategory(groupName)
    }
    val visibleFavorites = favorites.filterNot { isAdultGroup(it.groupName) }
    val visibleRecents = recents.filterNot { isAdultGroup(it.groupName) }
    val visibleCategories = categories.orEmpty()
        .filterNot { isAdultGroup(it.categoryName) }
    val groupCounts: Map<String, Int>? = allChannels
        ?.groupBy { it.categoryId ?: "" }
        ?.mapValues { (_, items) -> items.size }

    // The rows for the selected group, before the search filter. Null
    // while the group's channels are still loading.
    val groupRows: List<LiveTvRowItem>? = when (val group = selectedGroup) {
        LiveTvGroup.All -> allChannels?.map { it.toRowItem(categoryNameById, favorites) }
        LiveTvGroup.Favorites -> visibleFavorites.map { it.toRowItem(isFavorite = true) }
        LiveTvGroup.Recents -> visibleRecents.map {
            it.toRowItem(isFavorite = favorites.any { fav -> fav.streamId == it.streamId })
        }
        is LiveTvGroup.Category -> {
            val streams = allChannels?.filter { it.categoryId == group.category.categoryId }
                ?: groupCache[group.category.categoryId]
            streams?.map { it.toRowItem(categoryNameById, favorites) }
        }
    }
    val shownRows = groupRows.orEmpty().filter {
        query.isBlank() || it.name.contains(query, ignoreCase = true)
    }
    val groupTitle = when (val group = selectedGroup) {
        LiveTvGroup.All -> stringResource(R.string.iptv_group_all)
        LiveTvGroup.Favorites -> stringResource(R.string.iptv_group_favorites)
        LiveTvGroup.Recents -> stringResource(R.string.iptv_group_recents)
        is LiveTvGroup.Category -> group.category.categoryName
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "Live TV",
            style = MaterialTheme.typography.titleLarge,
            color = NuvioTheme.colors.TextPrimary,
        )
        Spacer(modifier = Modifier.height(12.dp))
        when {
            loading -> IptvLoading()
            failed -> IptvError(onRetry = ::loadCategories)
            else -> {
                Row(modifier = Modifier.weight(1f)) {
                    // Groups rail (starting side — right in RTL).
                    LazyColumn(
                        modifier = Modifier.width(340.dp).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        item(key = "group-all") {
                            LiveTvGroupRailItem(
                                label = stringResource(R.string.iptv_group_all),
                                icon = Icons.Default.LiveTv,
                                count = allChannels?.size,
                                selected = selectedGroup == LiveTvGroup.All,
                                onClick = { selectedGroup = LiveTvGroup.All; query = "" },
                                focusRequester = initialFocus,
                            )
                        }
                        item(key = "group-favorites") {
                            LiveTvGroupRailItem(
                                label = stringResource(R.string.iptv_group_favorites),
                                icon = Icons.Default.Star,
                                count = visibleFavorites.size,
                                selected = selectedGroup == LiveTvGroup.Favorites,
                                onClick = { selectedGroup = LiveTvGroup.Favorites; query = "" },
                            )
                        }
                        item(key = "group-recents") {
                            LiveTvGroupRailItem(
                                label = stringResource(R.string.iptv_group_recents),
                                icon = Icons.Default.History,
                                count = visibleRecents.size,
                                selected = selectedGroup == LiveTvGroup.Recents,
                                onClick = { selectedGroup = LiveTvGroup.Recents; query = "" },
                            )
                        }
                        items(visibleCategories, key = { it.categoryId }) { category ->
                            LiveTvGroupRailItem(
                                label = category.categoryName,
                                icon = null,
                                count = groupCounts?.get(category.categoryId)
                                    ?: groupCache[category.categoryId]?.size,
                                selected = selectedGroup == LiveTvGroup.Category(category),
                                onClick = {
                                    selectedGroup = LiveTvGroup.Category(category)
                                    query = ""
                                },
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    // Channel pane.
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        IptvSearchField(
                            query = query,
                            onQueryChange = { query = it },
                            label = stringResource(R.string.iptv_search_channels),
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = groupTitle,
                                style = MaterialTheme.typography.titleLarge,
                                color = NuvioTheme.colors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            if (groupRows != null) {
                                Box(
                                    modifier = Modifier.clip(RoundedCornerShape(50))
                                        .background(NuvioTheme.colors.BackgroundCard)
                                        .padding(horizontal = 12.dp, vertical = 4.dp),
                                ) {
                                    Text(
                                        text = groupRows.size.toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = NuvioTheme.colors.TextPrimary,
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        when {
                            groupRows == null && groupLoading -> Box(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                contentAlignment = Alignment.Center,
                            ) { CircularProgressIndicator() }
                            groupRows == null && groupFailed -> Column {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Failed to load",
                                    color = NuvioTheme.colors.TextPrimary,
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        selectedCategory?.let { loadGroupChannels(it, force = true) }
                                    },
                                ) { Text("Retry") }
                            }
                            groupRows == null -> Box(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                contentAlignment = Alignment.Center,
                            ) { CircularProgressIndicator() }
                            shownRows.isEmpty() -> {
                                val message = if (query.isNotBlank()) {
                                    "No results"
                                } else when (selectedGroup) {
                                    LiveTvGroup.Favorites -> stringResource(R.string.iptv_no_favorites_yet)
                                    LiveTvGroup.Recents -> stringResource(R.string.iptv_no_recents_yet)
                                    else -> stringResource(R.string.iptv_no_channels_in_group)
                                }
                                Box(
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        message,
                                        color = NuvioTheme.colors.TextSecondary,
                                    )
                                }
                            }
                            else -> LazyColumn(
                                state = channelListState,
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                itemsIndexed(shownRows, key = { _, item -> item.streamId }) { index, item ->
                                    LiveTvChannelRow(
                                        number = index + 1,
                                        item = item,
                                        onClick = { playRowItem(item, shownRows) },
                                        onFavoriteToggle = { toggleFavorite(item) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * One group in the Live TV rail: optional icon, name and a count badge.
 * The selected group gets the violet accent treatment.
 */
@Composable
private fun LiveTvGroupRailItem(
    label: String,
    icon: ImageVector?,
    count: Int?,
    selected: Boolean,
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) NuvioTheme.colors.Secondary.copy(alpha = 0.22f)
                else Color.Transparent,
            )
            .clickable(onClick = onClick)
            .then(
                if (focusRequester != null) Modifier.focusRequester(focusRequester)
                else Modifier,
            )
            .tvFocusRing(cornerRadius = 12.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Color.White else NuvioTheme.colors.TextSecondary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
        }
        Text(
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = if (selected) Color.White else NuvioTheme.colors.TextPrimary,
            modifier = Modifier.weight(1f),
        )
        if (count != null) {
            Box(
                modifier = Modifier.clip(RoundedCornerShape(50))
                    .background(
                        if (selected) NuvioTheme.colors.Secondary
                        else NuvioTheme.colors.BackgroundCard,
                    )
                    .padding(horizontal = 10.dp, vertical = 2.dp),
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) Color.White else NuvioTheme.colors.TextSecondary,
                )
            }
        }
    }
}

/**
 * One channel in the Live TV list: TiviMate-style number, logo, name,
 * group line and favorite star. OK plays; long-press OK toggles the
 * favorite star (the star itself is an indicator, not a focus target,
 * so DPAD up/down never gets stuck on it).
 */
@Composable
private fun LiveTvChannelRow(
    number: Int,
    item: LiveTvRowItem,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(NuvioTheme.colors.BackgroundCard)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onFavoriteToggle,
            )
            .tvFocusRing(cornerRadius = 12.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = NuvioTheme.colors.TextSecondary,
            textAlign = TextAlign.End,
            modifier = Modifier.width(36.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier.size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(NuvioTheme.colors.BackgroundElevated),
            contentAlignment = Alignment.Center,
        ) {
            if (!item.iconUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.iconUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(4.dp),
                )
            } else {
                Text(
                    text = "▶",
                    style = MaterialTheme.typography.titleMedium,
                    color = NuvioTheme.colors.TextSecondary,
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium,
                color = NuvioTheme.colors.TextPrimary,
            )
            if (!item.groupName.isNullOrBlank()) {
                Text(
                    text = item.groupName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = NuvioTheme.colors.TextSecondary,
                )
            }
        }
        Text(
            text = if (item.isFavorite) "★" else "☆",
            style = MaterialTheme.typography.titleLarge,
            color = if (item.isFavorite) NuvioTheme.colors.Secondary
            else NuvioTheme.colors.TextSecondary,
        )
    }
}

private enum class VodType { Movies, Series }

/**
 * One VOD shelf: section header ("Ajouts récents - <group>") plus a
 * horizontally scrollable row of poster cards with titles below.
 * Items load on first composition and are cached per (type, category).
 */
@Composable
private fun VodSectionShelf(
    category: XtreamCategory,
    vodType: VodType,
    status: Int?,
    movies: List<XtreamVodStream>?,
    series: List<XtreamSeries>?,
    onLoad: () -> Unit,
    onOpenCategory: () -> Unit,
    onPlayMovie: (XtreamVodStream) -> Unit,
    onOpenSeries: (XtreamSeries) -> Unit,
    /**
     * TV remote (DPAD) entry point: when set, the first poster of this shelf
     * requests focus (pass it only for the first shelf of the tab).
     */
    firstItemFocusRequester: FocusRequester? = null,
) {
    LaunchedEffect(category.categoryId, vodType) { onLoad() }
    val shownMovies = if (vodType == VodType.Movies) movies.orEmpty().take(20) else emptyList()
    val shownSeries = if (vodType == VodType.Series) series.orEmpty().take(20) else emptyList()
    // Skip sections that loaded but are empty; loading/failed sections still
    // render their header so the user sees progress or can retry.
    if (status == 1 && shownMovies.isEmpty() && shownSeries.isEmpty()) return
    // Per-category count from the actually loaded section items (full list,
    // not just the 20 shown); absent while loading.
    val sectionCount = if (status == 1) {
        if (vodType == VodType.Movies) movies?.size else series?.size
    } else {
        null
    }
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = buildString {
                    append("Ajouts récents - ${category.categoryName}")
                    if (sectionCount != null) append(" ($sectionCount)")
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onOpenCategory) { Text(">") }
        }
        Spacer(modifier = Modifier.height(8.dp))
        when (status) {
            0, null -> Box(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            2 -> TextButton(onClick = onLoad) { Text("Retry") }
            else -> {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (vodType == VodType.Movies) {
                        itemsIndexed(shownMovies, key = { _, it -> it.streamId }) { posterIndex, movie ->
                            IptvPosterCard(
                                imageUrl = movie.streamIcon,
                                title = movie.name,
                                onClick = { onPlayMovie(movie) },
                                modifier = Modifier.width(120.dp),
                                focusRequester = if (posterIndex == 0) firstItemFocusRequester else null,
                            )
                        }
                    } else {
                        itemsIndexed(shownSeries, key = { _, it -> it.seriesId }) { posterIndex, series ->
                            IptvPosterCard(
                                imageUrl = series.cover,
                                title = series.name,
                                onClick = { onOpenSeries(series) },
                                modifier = Modifier.width(120.dp),
                                focusRequester = if (posterIndex == 0) firstItemFocusRequester else null,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun VodScreen(
    modifier: Modifier = Modifier,
    onPlay: XtreamPlayHandler,
    onOpenMergedStreams: MergedStreamsHandler? = null,
) {
    IptvSettingsRepository.ensureLoaded()
    val pin by IptvSettingsRepository.pin.collectAsState()
    val sessionUnlocked by IptvSettingsRepository.sessionUnlocked.collectAsState()
    val lockAdult by IptvSettingsRepository.lockAdultCategories.collectAsState()
    val needsPin = pin.isNotEmpty() && !sessionUnlocked
    val adultHidden = lockAdult && !sessionUnlocked
    // TV remote (DPAD): focus the first poster of the first shelf when this
    // tab becomes active.
    val initialFocus = rememberTvInitialFocus()

    if (!XtreamConfig.isConfigured) {
        IptvNotConfigured()
        return
    }

    if (needsPin) {
        Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
            Text("IPTV", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(32.dp))
            Text("Locked")
        }
        MatrixPinDialog(
            title = "IPTV",
            onVerify = { entered -> entered == pin },
            onDismiss = {},
            onVerified = { IptvSettingsRepository.setSessionUnlocked(true) },
            dismissable = false,
        )
        return
    }

    var vodType by remember { mutableStateOf(VodType.Movies) }
    var vodCats by remember { mutableStateOf<List<XtreamCategory>?>(null) }
    var seriesCats by remember { mutableStateOf<List<XtreamCategory>?>(null) }
    var selectedCategory by remember { mutableStateOf<XtreamCategory?>(null) }
    var movies by remember { mutableStateOf<List<XtreamVodStream>?>(null) }
    var seriesList by remember { mutableStateOf<List<XtreamSeries>?>(null) }
    var selectedSeries by remember { mutableStateOf<XtreamSeries?>(null) }
    var seriesInfo by remember { mutableStateOf<XtreamSeriesInfo?>(null) }
    var query by remember { mutableStateOf("") }
    var globalQuery by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    // Matrix 3.1: an IMDb-id lookup is in flight for a tapped VOD title.
    var resolvingSources by remember { mutableStateOf(false) }
    // Full VOD catalog, loaded once in the background: powers the header
    // totals and the global search. Null while loading; a failure leaves it
    // null (totals/search stay unavailable, never guessed).
    var allMovies by remember { mutableStateOf<List<XtreamVodStream>?>(null) }
    var allSeries by remember { mutableStateOf<List<XtreamSeries>?>(null) }
    var allVodFailed by remember { mutableStateOf(false) }
    // Per-section shelf caches, keyed by category id, plus a status map keyed
    // by "Type:categoryId" (0 = loading, 1 = ok, 2 = failed).
    val movieSections = remember { mutableStateMapOf<String, List<XtreamVodStream>>() }
    val seriesSections = remember { mutableStateMapOf<String, List<XtreamSeries>>() }
    val sectionStatus = remember { mutableStateMapOf<String, Int>() }
    val scope = rememberCoroutineScope()

    // TV remote BACK steps out of series -> category -> shelves.
    BackHandler(enabled = selectedSeries != null || selectedCategory != null) {
        if (selectedSeries != null) {
            selectedSeries = null
            seriesInfo = null
        } else {
            selectedCategory = null
            query = ""
            globalQuery = ""
        }
    }

    fun loadCategories() {
        scope.launch {
            loading = true
            failed = false
            try {
                if (vodType == VodType.Movies) {
                    if (vodCats == null) vodCats = XtreamClient.vodCategories()
                } else {
                    if (seriesCats == null) seriesCats = XtreamClient.seriesCategories()
                }
            } catch (_: Exception) {
                failed = true
            }
            loading = false
        }
    }

    fun loadAllVod() {
        if ((allMovies != null && allSeries != null) || allVodFailed) return
        scope.launch {
            try {
                if (allMovies == null) allMovies = XtreamClient.vodStreamsAll()
                if (allSeries == null) allSeries = XtreamClient.seriesAll()
            } catch (_: Exception) {
                allVodFailed = true
            }
        }
    }

    fun loadItems(category: XtreamCategory) {
        scope.launch {
            loading = true
            failed = false
            try {
                if (vodType == VodType.Movies) {
                    movies = XtreamClient.vodStreams(category.categoryId)
                } else {
                    seriesList = XtreamClient.series(category.categoryId)
                }
            } catch (_: Exception) {
                failed = true
            }
            loading = false
        }
    }

    fun loadEpisodes(series: XtreamSeries) {
        scope.launch {
            loading = true
            failed = false
            try {
                seriesInfo = XtreamClient.seriesInfo(series.seriesId)
            } catch (_: Exception) {
                failed = true
            }
            loading = false
        }
    }

    fun loadSection(category: XtreamCategory) {
        val key = vodType.name + ":" + category.categoryId
        if (sectionStatus[key] == 1) return
        sectionStatus[key] = 0
        scope.launch {
            try {
                if (vodType == VodType.Movies) {
                    movieSections[category.categoryId] =
                        XtreamClient.vodStreams(category.categoryId)
                } else {
                    seriesSections[category.categoryId] =
                        XtreamClient.series(category.categoryId)
                }
                sectionStatus[key] = 1
            } catch (_: Exception) {
                sectionStatus[key] = 2
            }
        }
    }

    fun retry() {
        when {
            selectedSeries != null -> loadEpisodes(selectedSeries!!)
            selectedCategory != null -> loadItems(selectedCategory!!)
            else -> loadCategories()
        }
    }

    fun openCategory(category: XtreamCategory) {
        selectedCategory = category
        query = ""
        globalQuery = ""
        loadItems(category)
    }

    // Direct IPTV play, as before 3.1: the fallback when the merged panel
    // cannot be opened (no IMDb id resolved, or no panel handler).
    fun playMovieDirect(movie: XtreamVodStream) {
        onPlay(
            movie.name,
            movie.streamId,
            XtreamClient.movieStreamUrl(
                movie.streamId,
                movie.containerExtension,
            ),
            "movie",
            movie.streamIcon,
            null,
            null,
        )
    }

    fun playEpisodeDirect(
        series: XtreamSeries,
        season: Int?,
        episode: XtreamEpisode,
        episodeTitle: String,
    ) {
        onPlay(
            "${series.name} - $episodeTitle",
            episode.id,
            XtreamClient.episodeStreamUrl(
                episode.id,
                episode.containerExtension,
            ),
            "series",
            series.cover,
            season,
            episode.episodeNum,
        )
    }

    // Matrix 3.1: a VOD tap opens the merged streams panel (addon sources
    // first, this IPTV stream last) when an IMDb id resolves for the title;
    // otherwise it plays directly exactly as before.
    fun playMovie(movie: XtreamVodStream) {
        IptvSettingsRepository.addPlayedIptvKey(
            "iptv:${movie.streamId}",
            "iptv:${movie.streamId}",
        )
        val openMerged = onOpenMergedStreams
        if (openMerged == null) {
            playMovieDirect(movie)
            return
        }
        scope.launch {
            resolvingSources = true
            val cleanName = IptvTitleMatcher.cleanTitle(movie.name)
            val year = IptvTitleMatcher.parseYear(movie.name)
            val imdbId = IptvMergedStreams.resolveImdbId(cleanName, year, "movie")
            resolvingSources = false
            if (imdbId == null) {
                playMovieDirect(movie)
                return@launch
            }
            IptvMergedStreamHandoff.set(
                IptvStreamRef(
                    streamId = movie.streamId,
                    url = XtreamClient.movieStreamUrl(
                        movie.streamId,
                        movie.containerExtension,
                    ),
                    label = cleanName.ifBlank { movie.name },
                    contentType = "movie",
                    poster = movie.streamIcon,
                    season = null,
                    episode = null,
                )
            )
            openMerged(
                imdbId,
                "movie",
                cleanName.ifBlank { movie.name },
                movie.streamIcon,
                null,
                null,
                null,
                year?.toString(),
            )
        }
    }

    fun playEpisode(series: XtreamSeries, season: Int?, episode: XtreamEpisode) {
        val episodeTitle = episode.title?.takeIf { it.isNotBlank() }
            ?: "Episode ${episode.episodeNum ?: ""}"
        IptvSettingsRepository.addPlayedIptvKey(
            "iptv:${episode.id}",
            "iptv:${episode.id}",
        )
        val openMerged = onOpenMergedStreams
        if (openMerged == null) {
            playEpisodeDirect(series, season, episode, episodeTitle)
            return
        }
        scope.launch {
            resolvingSources = true
            val cleanName = IptvTitleMatcher.cleanTitle(series.name)
            val year = IptvTitleMatcher.parseYear(series.name)
            val imdbId = IptvMergedStreams.resolveImdbId(cleanName, year, "series")
            resolvingSources = false
            if (imdbId == null) {
                playEpisodeDirect(series, season, episode, episodeTitle)
                return@launch
            }
            IptvMergedStreamHandoff.set(
                IptvStreamRef(
                    streamId = episode.id,
                    url = XtreamClient.episodeStreamUrl(
                        episode.id,
                        episode.containerExtension,
                    ),
                    label = "S%02dE%02d · %s".format(
                        season ?: 0,
                        episode.episodeNum ?: 0,
                        episodeTitle,
                    ),
                    contentType = "series",
                    poster = series.cover,
                    season = season,
                    episode = episode.episodeNum,
                )
            )
            openMerged(
                imdbId,
                "series",
                cleanName.ifBlank { series.name },
                series.cover,
                season,
                episode.episodeNum,
                episodeTitle,
                year?.toString(),
            )
        }
    }

    fun openSeries(series: XtreamSeries) {
        selectedSeries = series
        seriesInfo = null
        loadEpisodes(series)
    }

    LaunchedEffect(Unit) {
        loadCategories()
        loadAllVod()
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "IPTV",
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = "${allMovies?.size ?: "…"} films • ${allSeries?.size ?: "…"} séries",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (resolvingSources) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Finding sources…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row {
            TextButton(
                onClick = {
                    if (vodType != VodType.Movies) {
                        vodType = VodType.Movies
                        selectedCategory = null
                        selectedSeries = null
                        query = ""
                        globalQuery = ""
                        loadCategories()
                    }
                },
            ) { Text("Movies") }
            TextButton(
                onClick = {
                    if (vodType != VodType.Series) {
                        vodType = VodType.Series
                        selectedCategory = null
                        selectedSeries = null
                        query = ""
                        globalQuery = ""
                        loadCategories()
                    }
                },
            ) { Text("Series") }
        }
        Spacer(modifier = Modifier.height(8.dp))
        when {
            loading -> IptvLoading()
            failed -> IptvError(onRetry = ::retry)
            selectedSeries != null -> {
                val series = selectedSeries!!
                TextButton(onClick = { selectedSeries = null; seriesInfo = null }) {
                    Text("Back")
                }
                val seasons = (seriesInfo?.episodes.orEmpty()).entries
                    .sortedBy { (season, _) -> season.toIntOrNull() ?: 0 }
                if (seasons.isEmpty()) {
                    IptvEmpty(message = "No episodes found", onRetry = { loadEpisodes(series) })
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        seasons.forEach { (season, episodes) ->
                            item(key = "season-$season") {
                                Text(
                                    text = "Season $season",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                )
                            }
                            items(episodes, key = { it.id }) { episode ->
                                val episodeTitle = episode.title?.takeIf { it.isNotBlank() }
                                    ?: "Episode ${episode.episodeNum ?: ""}"
                                IptvChannelRow(
                                    iconUrl = null,
                                    name = episodeTitle,
                                    infoLine = "Episode ${episode.episodeNum ?: ""}",
                                    onClick = {
                                        playEpisode(series, season.toIntOrNull(), episode)
                                    },
                                )
                            }
                        }
                    }
                }
            }
            selectedCategory != null -> {
                val category = selectedCategory!!
                TextButton(
                    onClick = { selectedCategory = null; query = ""; globalQuery = "" },
                ) {
                    Text("Back")
                }
                // Category header with its item count once loaded.
                val categoryCount = if (vodType == VodType.Movies) movies?.size else seriesList?.size
                Text(
                    text = if (categoryCount != null) {
                        "${category.categoryName} ($categoryCount)"
                    } else {
                        category.categoryName
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(8.dp))
                IptvSearchField(query = query, onQueryChange = { query = it })
                if (vodType == VodType.Movies) {
                    val shown = movies.orEmpty().filter {
                        query.isBlank() || it.name.contains(query, ignoreCase = true)
                    }
                    if (shown.isEmpty() && query.isBlank()) {
                        IptvEmpty(message = "No items found", onRetry = { loadItems(category) })
                    } else if (shown.isEmpty()) {
                        Text("No results")
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 120.dp),
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            items(shown, key = { it.streamId }) { movie ->
                                IptvPosterCard(
                                    imageUrl = movie.streamIcon,
                                    title = movie.name,
                                    onClick = { playMovie(movie) },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                } else {
                    val shown = seriesList.orEmpty().filter {
                        query.isBlank() || it.name.contains(query, ignoreCase = true)
                    }
                    if (shown.isEmpty() && query.isBlank()) {
                        IptvEmpty(message = "No items found", onRetry = { loadItems(category) })
                    } else if (shown.isEmpty()) {
                        Text("No results")
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 120.dp),
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            items(shown, key = { it.seriesId }) { series ->
                                IptvPosterCard(
                                    imageUrl = series.cover,
                                    title = series.name,
                                    onClick = { openSeries(series) },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }
            else -> {
                val list = if (vodType == VodType.Movies) {
                    vodCats.orEmpty()
                } else {
                    seriesCats.orEmpty()
                }.filterNot { adultHidden && IptvSettingsRepository.isAdultCategory(it.categoryName) }
                // Category-name lookups for adult-filtering global results.
                val vodCategoryNameById = vodCats.orEmpty()
                    .associate { it.categoryId to it.categoryName }
                val seriesCategoryNameById = seriesCats.orEmpty()
                    .associate { it.categoryId to it.categoryName }
                if (list.isEmpty() && globalQuery.isBlank()) {
                    IptvEmpty(message = "No items found", onRetry = { loadCategories() })
                } else {
                    Column(modifier = Modifier.weight(1f)) {
                        IptvSearchField(
                            query = globalQuery,
                            onQueryChange = { globalQuery = it },
                        )
                        if (globalQuery.isNotBlank()) {
                            when {
                                (allMovies == null || allSeries == null) && !allVodFailed -> Box(
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    contentAlignment = Alignment.Center,
                                ) { CircularProgressIndicator() }
                                allVodFailed -> Column {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Failed to load")
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = {
                                            allVodFailed = false
                                            loadAllVod()
                                        },
                                    ) { Text("Retry") }
                                }
                                else -> {
                                    // Global search: movies and series together,
                                    // by title substring, across all categories.
                                    val movieMatches = allMovies.orEmpty().filter { movie ->
                                        movie.name.contains(globalQuery, ignoreCase = true) &&
                                            !(adultHidden && vodCategoryNameById[movie.categoryId]
                                                ?.let { IptvSettingsRepository.isAdultCategory(it) } == true)
                                    }
                                    val seriesMatches = allSeries.orEmpty().filter { series ->
                                        series.name.contains(globalQuery, ignoreCase = true) &&
                                            !(adultHidden && seriesCategoryNameById[series.categoryId]
                                                ?.let { IptvSettingsRepository.isAdultCategory(it) } == true)
                                    }
                                    if (movieMatches.isEmpty() && seriesMatches.isEmpty()) {
                                        Text("No results")
                                    } else {
                                        LazyVerticalGrid(
                                            columns = GridCells.Adaptive(minSize = 120.dp),
                                            modifier = Modifier.weight(1f),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(16.dp),
                                        ) {
                                            items(movieMatches, key = { it.streamId }) { movie ->
                                                IptvPosterCard(
                                                    imageUrl = movie.streamIcon,
                                                    title = movie.name,
                                                    onClick = { playMovie(movie) },
                                                    modifier = Modifier.fillMaxWidth(),
                                                )
                                            }
                                            items(seriesMatches, key = { it.seriesId }) { series ->
                                                IptvPosterCard(
                                                    imageUrl = series.cover,
                                                    title = series.name,
                                                    onClick = { openSeries(series) },
                                                    modifier = Modifier.fillMaxWidth(),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            LazyColumn(modifier = Modifier.weight(1f)) {
                                itemsIndexed(list, key = { _, it -> it.categoryId }) { shelfIndex, category ->
                                    VodSectionShelf(
                                        category = category,
                                        vodType = vodType,
                                        status = sectionStatus[vodType.name + ":" + category.categoryId],
                                        movies = movieSections[category.categoryId],
                                        series = seriesSections[category.categoryId],
                                        onLoad = { loadSection(category) },
                                        onOpenCategory = { openCategory(category) },
                                        onPlayMovie = { movie -> playMovie(movie) },
                                        onOpenSeries = ::openSeries,
                                        firstItemFocusRequester = if (shelfIndex == 0) initialFocus else null,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
