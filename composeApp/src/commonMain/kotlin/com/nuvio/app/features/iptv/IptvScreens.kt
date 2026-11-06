package com.nuvio.app.features.iptv

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.rememberTvInitialFocus
import com.nuvio.app.core.ui.tvFocusRing
import coil3.compose.AsyncImage
import com.nuvio.app.features.profiles.PinEntryDialog
import com.nuvio.app.features.profiles.PinVerifyResult
import com.nuvio.app.features.watchprogress.WatchProgressRepository
import kotlinx.coroutines.launch

/**
 * Play callback: title, stream id, resolved stream URL, content type
 * ("tv", "movie", "series"), and the in-player list for this playback
 * (the tab's current list; empty when there is none).
 */
internal typealias XtreamPlayHandler = (
    title: String,
    streamId: Int,
    url: String,
    contentType: String,
    playlist: List<IptvPlayerListItem>,
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
private fun IptvSearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        label = { Text("Search") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
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
    focusRequester: androidx.compose.ui.focus.FocusRequester? = null,
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
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
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

@Composable
private fun IptvChip(
    label: String,
    onClick: () -> Unit,
    focusRequester: androidx.compose.ui.focus.FocusRequester? = null,
) {
    Box(
        modifier = Modifier.padding(end = 8.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .then(
                if (focusRequester != null) {
                    Modifier.focusRequester(focusRequester)
                } else {
                    Modifier
                },
            )
            .tvFocusRing(cornerRadius = 50.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            maxLines = 1,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
internal fun LiveTvScreen(
    modifier: Modifier = Modifier,
    isSelectedTab: Boolean = true,
    onPlay: XtreamPlayHandler,
) {
    IptvSettingsRepository.ensureLoaded()
    val pin by IptvSettingsRepository.pin.collectAsState()
    val sessionUnlocked by IptvSettingsRepository.sessionUnlocked.collectAsState()
    val lockAdult by IptvSettingsRepository.lockAdultCategories.collectAsState()
    val clearOnClose by IptvSettingsRepository.clearHistoryOnClose.collectAsState()
    val needsPin = pin.isNotEmpty() && !sessionUnlocked
    val adultHidden = lockAdult && !sessionUnlocked
    // TV remote (DPAD): focus the first group chip when this tab becomes active.
    val initialFocus = rememberTvInitialFocus()
    var wasSelected by remember { mutableStateOf(isSelectedTab) }

    // Clear-on-close: when the tab loses selection, wipe this session's IPTV
    // watch progress if the setting is on.
    LaunchedEffect(isSelectedTab, clearOnClose) {
        if (wasSelected && !isSelectedTab && clearOnClose) {
            IptvSettingsRepository.takePlayedIptvKeys().forEach { (videoId, parentMetaId) ->
                WatchProgressRepository.clearProgress(videoId, parentMetaId)
            }
        }
        wasSelected = isSelectedTab
    }

    if (needsPin) {
        Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
            Text("Live TV", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(32.dp))
            Text("Locked")
        }
        PinEntryDialog(
            profileName = "IPTV",
            onVerify = { entered -> PinVerifyResult(unlocked = entered == pin) },
            onDismiss = {},
            onVerified = { IptvSettingsRepository.setSessionUnlocked(true) },
        )
        return
    }

    var categories by remember { mutableStateOf<List<XtreamCategory>?>(null) }
    var selected by remember { mutableStateOf<XtreamCategory?>(null) }
    var channels by remember { mutableStateOf<List<XtreamLiveStream>?>(null) }
    var query by remember { mutableStateOf("") }
    var globalQuery by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    // Full catalog, loaded once in the background: powers the header total,
    // the per-group count badges and the global search. Null while loading;
    // a failure leaves it null (counts/search stay unavailable, never guessed).
    var allChannels by remember { mutableStateOf<List<XtreamLiveStream>?>(null) }
    var allChannelsFailed by remember { mutableStateOf(false) }
    val favorites by IptvSettingsRepository.favoriteChannels.collectAsState()
    val scope = rememberCoroutineScope()

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

    fun loadChannels(category: XtreamCategory) {
        scope.launch {
            loading = true
            failed = false
            try {
                channels = XtreamClient.liveStreams(category.categoryId)
            } catch (_: Exception) {
                failed = true
            }
            loading = false
        }
    }

    fun selectGroup(category: XtreamCategory) {
        selected = category
        channels = null
        query = ""
        globalQuery = ""
        loadChannels(category)
    }

    fun playChannel(channel: XtreamLiveStream, playlist: List<IptvPlayerListItem>) {
        IptvSettingsRepository.addPlayedIptvKey(
            "iptv:${channel.streamId}",
            "iptv:${channel.streamId}",
        )
        onPlay(
            channel.name,
            channel.streamId,
            XtreamClient.liveStreamUrl(channel.streamId),
            "tv",
            playlist,
        )
    }

    fun playFavorite(fav: FavoriteChannel, playlist: List<FavoriteChannel>) {
        IptvSettingsRepository.addPlayedIptvKey(
            "iptv:${fav.streamId}",
            "iptv:${fav.streamId}",
        )
        onPlay(
            fav.name,
            fav.streamId,
            XtreamClient.liveStreamUrl(fav.streamId),
            "tv",
            playlist.map { it.toPlayerListItem() },
        )
    }

    LaunchedEffect(Unit) {
        loadCategories()
        loadAllChannels()
    }

    val categoryNameById = categories.orEmpty()
        .associate { it.categoryId to it.categoryName }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "Live TV",
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = if (allChannels != null) {
                "${allChannels!!.size} channels"
            } else {
                "… channels"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))
        when {
            loading -> IptvLoading()
            failed -> IptvError(onRetry = {
                val current = selected
                if (current == null) loadCategories() else loadChannels(current)
            })
            selected == null -> {
                val list = categories.orEmpty()
                    .filterNot { adultHidden && IptvSettingsRepository.isAdultCategory(it.categoryName) }
                val groupCounts: Map<String, Int>? = allChannels
                    ?.groupBy { it.categoryId ?: "" }
                    ?.mapValues { (_, items) -> items.size }
                val visibleFavorites = favorites.filterNot {
                    adultHidden && it.groupName != null &&
                        IptvSettingsRepository.isAdultCategory(it.groupName)
                }
                if (list.isEmpty() && globalQuery.isBlank()) {
                    IptvEmpty(message = "No channels found", onRetry = { loadCategories() })
                } else {
                    Column(modifier = Modifier.weight(1f)) {
                        IptvSearchField(
                            query = globalQuery,
                            onQueryChange = { globalQuery = it },
                        )
                        if (globalQuery.isNotBlank()) {
                            when {
                                allChannels == null && !allChannelsFailed -> Box(
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    contentAlignment = Alignment.Center,
                                ) { CircularProgressIndicator() }
                                allChannelsFailed -> Column {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Failed to load")
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = {
                                            allChannelsFailed = false
                                            loadAllChannels()
                                        },
                                    ) { Text("Retry") }
                                }
                                else -> {
                                    val results = allChannels.orEmpty().filter { channel ->
                                        channel.name.contains(globalQuery, ignoreCase = true) &&
                                            !(adultHidden && categoryNameById[channel.categoryId]
                                                ?.let { IptvSettingsRepository.isAdultCategory(it) } == true)
                                    }
                                    if (results.isEmpty()) {
                                        Text("No results")
                                    } else {
                                        LazyColumn(modifier = Modifier.weight(1f)) {
                                            items(results, key = { it.streamId }) { channel ->
                                                IptvChannelRow(
                                                    iconUrl = channel.streamIcon,
                                                    name = channel.name,
                                                    infoLine = categoryNameById[channel.categoryId]
                                                        .orEmpty()
                                                        .ifBlank { "Pas d'informations" },
                                                    onClick = {
                                                        playChannel(
                                                            channel,
                                                            results.map {
                                                                it.toPlayerListItem(categoryNameById[it.categoryId])
                                                            },
                                                        )
                                                    },
                                                    isFavorite = favorites.any {
                                                        it.streamId == channel.streamId
                                                    },
                                                    onFavoriteToggle = {
                                                        IptvSettingsRepository.toggleFavoriteChannel(
                                                            FavoriteChannel(
                                                                streamId = channel.streamId,
                                                                name = channel.name,
                                                                iconUrl = channel.streamIcon,
                                                                groupName = categoryNameById[channel.categoryId],
                                                            ),
                                                        )
                                                    },
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            LazyColumn(modifier = Modifier.weight(1f)) {
                                item(key = "fav-header") {
                                    Text(
                                        text = "Favoris",
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                if (visibleFavorites.isEmpty()) {
                                    item(key = "fav-empty") {
                                        Text(
                                            text = "No favorites yet",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }
                                } else {
                                    items(
                                        visibleFavorites,
                                        key = { "fav-${it.streamId}" },
                                    ) { fav ->
                                        IptvChannelRow(
                                            iconUrl = fav.iconUrl,
                                            name = fav.name,
                                            infoLine = fav.groupName.orEmpty()
                                                .ifBlank { "Pas d'informations" },
                                            onClick = { playFavorite(fav, visibleFavorites) },
                                            isFavorite = true,
                                            onFavoriteToggle = {
                                                IptvSettingsRepository.toggleFavoriteChannel(fav)
                                            },
                                        )
                                    }
                                    item(key = "fav-spacer") {
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }
                                }
                                item(key = "groupes-header") {
                                    Text(
                                        text = "Groupes",
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyRow {
                                        itemsIndexed(list, key = { _, it -> it.categoryId }) { chipIndex, category ->
                                            IptvChip(
                                                label = category.categoryName,
                                                onClick = { selectGroup(category) },
                                                focusRequester = if (chipIndex == 0) initialFocus else null,
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Mes Groupes",
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                items(list, key = { it.categoryId }) { category ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth()
                                            .clickable { selectGroup(category) }
                                            .tvFocusRing(cornerRadius = 8.dp)
                                            .padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = category.categoryName,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f),
                                        )
                                        groupCounts?.get(category.categoryId)?.let { count ->
                                            Box(
                                                modifier = Modifier.clip(RoundedCornerShape(50))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                            ) {
                                                Text(
                                                    text = count.toString(),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        Text(
                                            text = ">",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                val category = selected!!
                TextButton(
                    onClick = { selected = null; channels = null; query = ""; globalQuery = "" },
                ) {
                    Text("Back")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = category.categoryName,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    val count = channels?.size
                    if (count != null) {
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = count.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                IptvSearchField(query = query, onQueryChange = { query = it })
                val shown = channels.orEmpty().filter {
                    query.isBlank() || it.name.contains(query, ignoreCase = true)
                }
                if (shown.isEmpty() && query.isBlank()) {
                    IptvEmpty(message = "No channels found", onRetry = { loadChannels(category) })
                } else if (shown.isEmpty()) {
                    Text("No results")
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(shown, key = { it.streamId }) { channel ->
                            IptvChannelRow(
                                iconUrl = channel.streamIcon,
                                name = channel.name,
                                infoLine = "Pas d'informations",
                                onClick = {
                                    playChannel(
                                        channel,
                                        shown.map { it.toPlayerListItem(category.categoryName) },
                                    )
                                },
                                isFavorite = favorites.any {
                                    it.streamId == channel.streamId
                                },
                                onFavoriteToggle = {
                                    IptvSettingsRepository.toggleFavoriteChannel(
                                        FavoriteChannel(
                                            streamId = channel.streamId,
                                            name = channel.name,
                                            iconUrl = channel.streamIcon,
                                            groupName = category.categoryName,
                                        ),
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
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
    onPlayMovie: (XtreamVodStream, List<XtreamVodStream>) -> Unit,
    onOpenSeries: (XtreamSeries) -> Unit,
    /**
     * TV remote (DPAD) entry point: when set, the first poster of this shelf
     * requests focus (pass it only for the first shelf of the tab).
     */
    firstItemFocusRequester: androidx.compose.ui.focus.FocusRequester? = null,
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
                                onClick = { onPlayMovie(movie, movies.orEmpty()) },
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
    isSelectedTab: Boolean = true,
    onPlay: XtreamPlayHandler,
) {
    IptvSettingsRepository.ensureLoaded()
    val pin by IptvSettingsRepository.pin.collectAsState()
    val sessionUnlocked by IptvSettingsRepository.sessionUnlocked.collectAsState()
    val lockAdult by IptvSettingsRepository.lockAdultCategories.collectAsState()
    val clearOnClose by IptvSettingsRepository.clearHistoryOnClose.collectAsState()
    val needsPin = pin.isNotEmpty() && !sessionUnlocked
    val adultHidden = lockAdult && !sessionUnlocked
    // TV remote (DPAD): focus the first poster of the first shelf when this
    // tab becomes active.
    val initialFocus = rememberTvInitialFocus()
    var wasSelected by remember { mutableStateOf(isSelectedTab) }

    LaunchedEffect(isSelectedTab, clearOnClose) {
        if (wasSelected && !isSelectedTab && clearOnClose) {
            IptvSettingsRepository.takePlayedIptvKeys().forEach { (videoId, parentMetaId) ->
                WatchProgressRepository.clearProgress(videoId, parentMetaId)
            }
        }
        wasSelected = isSelectedTab
    }

    if (needsPin) {
        Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
            Text("IPTV", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(32.dp))
            Text("Locked")
        }
        PinEntryDialog(
            profileName = "IPTV",
            onVerify = { entered -> PinVerifyResult(unlocked = entered == pin) },
            onDismiss = {},
            onVerified = { IptvSettingsRepository.setSessionUnlocked(true) },
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

    fun playMovie(movie: XtreamVodStream, playlist: List<XtreamVodStream>) {
        IptvSettingsRepository.addPlayedIptvKey(
            "iptv:${movie.streamId}",
            "iptv:${movie.streamId}",
        )
        onPlay(
            movie.name,
            movie.streamId,
            XtreamClient.movieStreamUrl(
                movie.streamId,
                movie.containerExtension,
            ),
            "movie",
            playlist.map { it.toPlayerListItem() },
        )
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
                // Full episode list for the in-player list (season order).
                val episodePlaylist: List<IptvPlayerListItem> = seasons.flatMap { (seasonKey, eps) ->
                    eps.map { ep ->
                        val epTitle = ep.title?.takeIf { it.isNotBlank() }
                            ?: "Episode ${ep.episodeNum ?: ""}"
                        ep.toPlayerListItem(series.name, epTitle, seasonKey.toIntOrNull())
                    }
                }
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
                                    IptvSettingsRepository.addPlayedIptvKey(
                                        "iptv:${episode.id}",
                                        "iptv:${episode.id}",
                                    )
                                    onPlay(
                                        "${series.name} - $episodeTitle",
                                        episode.id,
                                        XtreamClient.episodeStreamUrl(
                                            episode.id,
                                            episode.containerExtension,
                                        ),
                                        "series",
                                        episodePlaylist,
                                    )
                                })
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
                                    onClick = { playMovie(movie, shown) },
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
                                                    onClick = { playMovie(movie, movieMatches) },
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
                                        onPlayMovie = { movie, playlist -> playMovie(movie, playlist) },
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
