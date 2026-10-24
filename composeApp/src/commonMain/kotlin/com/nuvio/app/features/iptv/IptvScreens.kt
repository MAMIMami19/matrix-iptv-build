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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nuvio.app.features.profiles.PinEntryDialog
import com.nuvio.app.features.profiles.PinVerifyResult
import com.nuvio.app.features.watchprogress.WatchProgressRepository
import kotlinx.coroutines.launch

/** Play callback: title, stream id, resolved stream URL, content type ("tv", "movie", "series"). */
internal typealias XtreamPlayHandler = (title: String, streamId: Int, url: String, contentType: String) -> Unit

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
) {
    Column(modifier = modifier.clickable(onClick = onClick)) {
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
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
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
    }
}

@Composable
private fun IptvChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier.padding(end = 8.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
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
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
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
        loadChannels(category)
    }

    LaunchedEffect(Unit) { loadCategories() }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "Live TV",
            style = MaterialTheme.typography.titleLarge,
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
                if (list.isEmpty()) {
                    IptvEmpty(message = "No channels found", onRetry = { loadCategories() })
                } else {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Groupes",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow {
                            items(list, key = { it.categoryId }) { category ->
                                IptvChip(label = category.categoryName) {
                                    selectGroup(category)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Mes Groupes",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(list, key = { it.categoryId }) { category ->
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                        .clickable { selectGroup(category) }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = category.categoryName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f),
                                    )
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
            else -> {
                val category = selected!!
                TextButton(onClick = { selected = null; channels = null; query = "" }) {
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
                            ) {
                                IptvSettingsRepository.addPlayedIptvKey(
                                    "iptv:${channel.streamId}",
                                    "iptv:${channel.streamId}",
                                )
                                onPlay(
                                    channel.name,
                                    channel.streamId,
                                    XtreamClient.liveStreamUrl(channel.streamId),
                                    "tv",
                                )
                            }
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
    onPlayMovie: (XtreamVodStream) -> Unit,
    onOpenSeries: (XtreamSeries) -> Unit,
) {
    LaunchedEffect(category.categoryId, vodType) { onLoad() }
    val shownMovies = if (vodType == VodType.Movies) movies.orEmpty().take(20) else emptyList()
    val shownSeries = if (vodType == VodType.Series) series.orEmpty().take(20) else emptyList()
    // Skip sections that loaded but are empty; loading/failed sections still
    // render their header so the user sees progress or can retry.
    if (status == 1 && shownMovies.isEmpty() && shownSeries.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Ajouts récents - ${category.categoryName}",
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
                        items(shownMovies, key = { it.streamId }) { movie ->
                            IptvPosterCard(
                                imageUrl = movie.streamIcon,
                                title = movie.name,
                                onClick = { onPlayMovie(movie) },
                                modifier = Modifier.width(120.dp),
                            )
                        }
                    } else {
                        items(shownSeries, key = { it.seriesId }) { series ->
                            IptvPosterCard(
                                imageUrl = series.cover,
                                title = series.name,
                                onClick = { onOpenSeries(series) },
                                modifier = Modifier.width(120.dp),
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
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
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
        loadItems(category)
    }

    fun playMovie(movie: XtreamVodStream) {
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
        )
    }

    fun openSeries(series: XtreamSeries) {
        selectedSeries = series
        seriesInfo = null
        loadEpisodes(series)
    }

    LaunchedEffect(Unit) { loadCategories() }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "IPTV",
            style = MaterialTheme.typography.titleLarge,
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
                                ) {
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
                                    )
                                }
                            }
                        }
                    }
                }
            }
            selectedCategory != null -> {
                val category = selectedCategory!!
                TextButton(onClick = { selectedCategory = null; query = "" }) {
                    Text("Back")
                }
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
                if (list.isEmpty()) {
                    IptvEmpty(message = "No items found", onRetry = { loadCategories() })
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(list, key = { it.categoryId }) { category ->
                            VodSectionShelf(
                                category = category,
                                vodType = vodType,
                                status = sectionStatus[vodType.name + ":" + category.categoryId],
                                movies = movieSections[category.categoryId],
                                series = seriesSections[category.categoryId],
                                onLoad = { loadSection(category) },
                                onOpenCategory = { openCategory(category) },
                                onPlayMovie = ::playMovie,
                                onOpenSeries = ::openSeries,
                            )
                        }
                    }
                }
            }
        }
    }
}
