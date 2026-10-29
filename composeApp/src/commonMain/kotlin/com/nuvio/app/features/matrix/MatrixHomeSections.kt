package com.nuvio.app.features.matrix

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.core.ui.NuvioPosterCard
import com.nuvio.app.features.home.MetaPreview

/**
 * A Showflix-style horizontal poster row: Arabic section title + posters.
 * When [ranked] is true, posters render with big outlined rank numerals
 * (Top-10 style, supported by NuvioPosterCard).
 */
@Composable
fun MatrixSectionRow(
    title: String,
    items: List<MetaPreview>,
    onPosterClick: ((MetaPreview) -> Unit)?,
    modifier: Modifier = Modifier,
    ranked: Boolean = false,
) {
    if (items.isEmpty()) return
    Column(modifier = modifier.padding(bottom = 20.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(items, key = { _, item -> "${item.type}:${item.id}" }) { index, item ->
                NuvioPosterCard(
                    title = item.name,
                    imageUrl = item.poster,
                    detailLine = item.releaseInfo,
                    rankNumber = if (ranked) index + 1 else null,
                    onClick = onPosterClick?.let { { it(item) } },
                )
            }
        }
    }
}

/**
 * The "استكشف حسب العلامة" (explore by brand) chips row from the Showflix home.
 * Tapping a chip opens that brand's detail page.
 */
@Composable
fun MatrixBrandChipsRow(
    onBrandClick: (MatrixBrand) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(bottom = 20.dp)) {
        Text(
            text = "استكشف حسب العلامة",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(MATRIX_BRANDS, key = { it.id }) { brand ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onBrandClick(brand) },
                ) {
                    Text(
                        text = brand.name,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}
