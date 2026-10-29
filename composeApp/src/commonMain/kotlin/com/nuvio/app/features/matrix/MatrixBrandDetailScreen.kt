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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nuvio.app.core.ui.NuvioPosterCard
import com.nuvio.app.features.home.MetaPreview

private enum class BrandFilter(val label: String) {
    ALL("الكل"),
    MOVIES("أفلام"),
    SERIES("مسلسلات"),
}

/**
 * Showflix-style brand detail page: big brand logo, "اكتشف المجموعة",
 * filter chips (الكل/أفلام/مسلسلات), 2-column poster grid.
 */
@Composable
fun MatrixBrandDetailScreen(
    brand: MatrixBrand,
    onPosterClick: ((MetaPreview) -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) { MatrixSectionsRepository.ensureLoaded() }
    val catalogs by MatrixSectionsRepository.catalogs.collectAsState()
    var filter by remember { mutableStateOf(BrandFilter.ALL) }

    val movieItems = brand.movieCatalogId?.let { catalogs[it].orEmpty() }.orEmpty()
    val seriesItems = brand.seriesCatalogId?.let { catalogs[it].orEmpty() }.orEmpty()
    val items = when (filter) {
        BrandFilter.ALL -> movieItems + seriesItems
        BrandFilter.MOVIES -> movieItems
        BrandFilter.SERIES -> seriesItems
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "رجوع",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }

        // Big brand logo (asset where available, styled text otherwise).
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            val logoModel = brand.logoModel()
            if (logoModel != null) {
                AsyncImage(
                    model = logoModel,
                    contentDescription = brand.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
            } else {
                Text(
                    text = brand.name,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 44.sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Text(
            text = "اكتشف المجموعة",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 8.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            BrandFilter.entries.forEach { entry ->
                FilterChip(
                    selected = filter == entry,
                    onClick = { filter = entry },
                    label = { Text(entry.label) },
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(items, key = { "${it.type}:${it.id}" }) { item ->
                NuvioPosterCard(
                    title = item.name,
                    imageUrl = item.poster,
                    detailLine = item.releaseInfo,
                    onClick = onPosterClick?.let { { it(item) } },
                )
            }
        }
    }
}
