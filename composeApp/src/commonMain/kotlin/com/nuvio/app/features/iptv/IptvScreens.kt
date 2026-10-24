package com.nuvio.app.features.iptv

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
private fun IptvRow(iconUrl: String?, name: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!iconUrl.isNullOrBlank()) {
            AsyncImage(
                model = iconUrl,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
            )
            Spacer(modifier = Modifier.size(12.dp))
        }
        Text(
            text = name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
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
    val lockSettings by IptvSettingsRepository.lockSettings.collectAsState()
    val needsPin = pin.isNotEmpty() && !sessionUnlocked
    val adultHidden = lockAdult && !sessionUnlocked
    var showSettings by remember { mutableStateOf(false) }
    var showSettingsPin by remember { mutableStateOf(false) }
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

    if (showSettingsPin) {
        PinEntryDialog(
            profileName = "IPTV",
            onVerify = { entered -> PinVerifyResult(unlocked = entered == pin) },
            onDismiss = { showSettingsPin = false },
            onVerified = {
                IptvSettingsRepository.setSessionUnlocked(true)
                showSettingsPin = false
                showSettings = true
            },
        )
    }
    if (showSettings) {
        IptvSettingsScreen(onBack = { showSettings = false })
        return
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

    LaunchedEffect(Unit) { loadCategories() }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Live TV",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = {
                    if (lockSettings && pin.isNotEmpty() && !sessionUnlocked) {
                        showSettingsPin = true
                    } else {
                        showSettings = true
                    }
                },
            ) { Text("Settings") }
        }
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
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(list, key = { it.categoryId }) { category ->
                        IptvRow(iconUrl = null, name = category.categoryName) {
                            selected = category
                            channels = null
                            query = ""
                            loadChannels(category)
                        }
                    }
                }
            }
            else -> {
                val category = selected!!
                TextButton(onClick = { selected = null; channels = null; query = "" }) {
                    Text("Back")
                }
                IptvSearchField(query = query, onQueryChange = { query = it })
                val shown = channels.orEmpty().filter {
                    query.isBlank() || it.name.contains(query, ignoreCase = true)
                }
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(shown, key = { it.streamId }) { channel ->
                        IptvRow(iconUrl = channel.streamIcon, name = channel.name) {
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

private enum class VodType { Movies, Series }

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
    val lockSettings by IptvSettingsRepository.lockSettings.collectAsState()
    val needsPin = pin.isNotEmpty() && !sessionUnlocked
    val adultHidden = lockAdult && !sessionUnlocked
    var showSettings by remember { mutableStateOf(false) }
    var showSettingsPin by remember { mutableStateOf(false) }
    var wasSelected by remember { mutableStateOf(isSelectedTab) }

    LaunchedEffect(isSelectedTab, clearOnClose) {
        if (wasSelected && !isSelectedTab && clearOnClose) {
            IptvSettingsRepository.takePlayedIptvKeys().forEach { (videoId, parentMetaId) ->
                WatchProgressRepository.clearProgress(videoId, parentMetaId)
            }
        }
        wasSelected = isSelectedTab
    }

    if (showSettingsPin) {
        PinEntryDialog(
            profileName = "IPTV",
            onVerify = { entered -> PinVerifyResult(unlocked = entered == pin) },
            onDismiss = { showSettingsPin = false },
            onVerified = {
                IptvSettingsRepository.setSessionUnlocked(true)
                showSettingsPin = false
                showSettings = true
            },
        )
    }
    if (showSettings) {
        IptvSettingsScreen(onBack = { showSettings = false })
        return
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

    fun retry() {
        when {
            selectedSeries != null -> loadEpisodes(selectedSeries!!)
            selectedCategory != null -> loadItems(selectedCategory!!)
            else -> loadCategories()
        }
    }

    LaunchedEffect(Unit) { loadCategories() }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "IPTV",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = {
                    if (lockSettings && pin.isNotEmpty() && !sessionUnlocked) {
                        showSettingsPin = true
                    } else {
                        showSettings = true
                    }
                },
            ) { Text("Settings") }
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
                LazyColumn(modifier = Modifier.weight(1f)) {
                    seasons.forEach { (season, episodes) ->
                        item(key = "season-$season") {
                            Text(
                                text = "Season $season",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }
                        items(episodes, key = { it.id }) { episode ->
                            val episodeTitle = episode.title?.takeIf { it.isNotBlank() }
                                ?: "Episode ${episode.episodeNum ?: ""}"
                            IptvRow(iconUrl = null, name = episodeTitle) {
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
            selectedCategory != null -> {
                TextButton(onClick = { selectedCategory = null; query = "" }) {
                    Text("Back")
                }
                IptvSearchField(query = query, onQueryChange = { query = it })
                if (vodType == VodType.Movies) {
                    val shown = movies.orEmpty().filter {
                        query.isBlank() || it.name.contains(query, ignoreCase = true)
                    }
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(shown, key = { it.streamId }) { movie ->
                            IptvRow(iconUrl = movie.streamIcon, name = movie.name) {
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
                        }
                    }
                } else {
                    val shown = seriesList.orEmpty().filter {
                        query.isBlank() || it.name.contains(query, ignoreCase = true)
                    }
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(shown, key = { it.seriesId }) { series ->
                            IptvRow(iconUrl = series.cover, name = series.name) {
                                selectedSeries = series
                                seriesInfo = null
                                loadEpisodes(series)
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
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(list, key = { it.categoryId }) { category ->
                        IptvRow(iconUrl = null, name = category.categoryName) {
                            selectedCategory = category
                            query = ""
                            loadItems(category)
                        }
                    }
                }
            }
        }
    }
}
