package com.nuvio.app.features.matrix

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nuvio.app.core.ui.rememberTvInitialFocus
import com.nuvio.app.core.ui.tvFocusRing
import com.nuvio.app.features.home.MetaPreview

private enum class TrendingContentFilter(val label: String) {
    NEW_THIS_WEEK("جديد هذا الأسبوع"),
    TRENDING("الجميع يشاهد"),
    TOP_10("أفضل 10"),
}

private enum class TrendingTypeFilter(val label: String) {
    ALL("الكل"),
    MOVIES("أفلام"),
    SERIES("مسلسلات"),
}

/**
 * «جديد ورائج» (New & Trending) section page.
 * Filter chips + large backdrop-style cards with badges, metadata, and actions.
 */
@Composable
fun MatrixTrendingScreen(
    onPosterClick: ((MetaPreview) -> Unit)? = null,
    onPlayClick: ((MetaPreview) -> Unit)? = null,
    onMyListClick: ((MetaPreview) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) { MatrixSectionsRepository.ensureLoaded() }
    val catalogs by MatrixSectionsRepository.catalogs.collectAsState()
    // TV remote (DPAD): focus the first filter chip when this tab becomes active.
    val initialFocus = rememberTvInitialFocus()

    var contentFilter by remember { mutableStateOf(TrendingContentFilter.NEW_THIS_WEEK) }
    var typeFilter by remember { mutableStateOf(TrendingTypeFilter.ALL) }

    val items: List<MetaPreview> = remember(catalogs, contentFilter, typeFilter) {
        val movieCatalogId = when (contentFilter) {
            TrendingContentFilter.NEW_THIS_WEEK -> "mx_new_movie"
            TrendingContentFilter.TRENDING -> "mx_new_movie"
            TrendingContentFilter.TOP_10 -> "mx_top10_movie"
        }
        val seriesCatalogId = when (contentFilter) {
            TrendingContentFilter.NEW_THIS_WEEK -> "mx_new_series"
            TrendingContentFilter.TRENDING -> "mx_new_series"
            TrendingContentFilter.TOP_10 -> "mx_top10_series"
        }
        val movies = catalogs[movieCatalogId].orEmpty()
        val series = catalogs[seriesCatalogId].orEmpty()
        when (typeFilter) {
            TrendingTypeFilter.ALL -> movies + series
            TrendingTypeFilter.MOVIES -> movies
            TrendingTypeFilter.SERIES -> series
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "جديد ورائج",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        )

        // Content filter chips.
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 8.dp),
        ) {
            itemsIndexed(TrendingContentFilter.entries) { index, filter ->
                FilterChip(
                    selected = contentFilter == filter,
                    onClick = { contentFilter = filter },
                    label = { Text(filter.label) },
                    modifier = Modifier
                        .then(if (index == 0) Modifier.focusRequester(initialFocus) else Modifier)
                        .tvFocusRing(cornerRadius = 16.dp),
                )
            }
        }

        // Type filter chips.
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 12.dp),
        ) {
            items(TrendingTypeFilter.entries) { filter ->
                FilterChip(
                    selected = typeFilter == filter,
                    onClick = { typeFilter = filter },
                    label = { Text(filter.label) },
                    modifier = Modifier.tvFocusRing(cornerRadius = 16.dp),
                )
            }
        }

        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(items, key = { "${it.type}:${it.id}" }) { item ->
                    MatrixTrendingCard(
                        item = item,
                        isNew = contentFilter == TrendingContentFilter.NEW_THIS_WEEK,
                        onPlayClick = onPlayClick?.let { { it(item) } },
                        onDetailsClick = onPosterClick?.let { { it(item) } },
                        onMyListClick = onMyListClick?.let { { it(item) } },
                    )
                }
            }
        }
    }
}

@Composable
private fun MatrixTrendingCard(
    item: MetaPreview,
    isNew: Boolean,
    onPlayClick: (() -> Unit)?,
    onDetailsClick: (() -> Unit)?,
    onMyListClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        ),
        modifier = modifier.fillMaxWidth().tvFocusRing(cornerRadius = 16.dp),
        onClick = onDetailsClick ?: {},
    ) {
        Column {
            // Backdrop-style image (16:9 crop from poster).
            Box {
                AsyncImage(
                    model = item.poster,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                )
                // Badges row.
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (isNew) {
                        TrendingBadge(text = "جديد")
                    }
                    item.releaseInfo?.let { year ->
                        TrendingBadge(text = year)
                    }
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )

                // TMDB rating · type · year line.
                val metaLine = buildString {
                    append(if (item.type == "movie") "فيلم" else "مسلسل")
                    item.releaseInfo?.let { append(" · $it") }
                }
                Text(
                    text = metaLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp),
                )

                item.description?.takeIf { it.isNotBlank() }?.let { synopsis ->
                    Text(
                        text = synopsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        maxLines = 2,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }

                // Action buttons.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (onPlayClick != null) {
                        Button(onClick = onPlayClick) {
                            Text("تشغيل")
                        }
                    }
                    if (onDetailsClick != null) {
                        OutlinedButton(onClick = onDetailsClick) {
                            Text("التفاصيل")
                        }
                    }
                    if (onMyListClick != null) {
                        TextButton(onClick = onMyListClick) {
                            Text("+ قائمتي")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendingBadge(text: String) {
    androidx.compose.material3.Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.primary,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}
