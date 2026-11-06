package com.nuvio.app.features.matrix

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nuvio.app.core.ui.NuvioPosterCard
import com.nuvio.app.core.ui.tvFocusRing
import com.nuvio.app.features.home.MetaPreview
import kotlin.math.abs
import kotlin.math.min

/**
 * A Showflix-style horizontal poster row: Arabic section title + posters.
 * When [ranked] is true, posters render with big outlined rank numerals
 * (Top-10 style, supported by NuvioPosterCard).
 *
 * Posters render as a 3D cover-flow carousel in the "ART OF 2025"
 * motion-poster style: the poster nearest the viewport center is larger
 * than life, front-facing and perfectly sharp on a pure-black stage,
 * while neighbours lean away in 3D (rotationY), shrink, blur, fade
 * slightly, and dip down a touch, like a cylindrical carousel. The
 * transform is purely visual (graphicsLayer + image-only blur), so
 * tap-to-open-details and DPAD left/right focus keep working exactly as
 * before, and the violet TV focus ring stays sharp. Live TV / IPTV
 * channel rows do not use this composable, so they are unaffected.
 */
@Composable
fun MatrixSectionRow(
    title: String,
    items: List<MetaPreview>,
    onPosterClick: ((MetaPreview) -> Unit)?,
    modifier: Modifier = Modifier,
    ranked: Boolean = false,
    /**
     * TV remote (DPAD) entry point: when set, the first poster of this row
     * requests focus (pass it only for the first row of a tab).
     */
    firstItemFocusRequester: androidx.compose.ui.focus.FocusRequester? = null,
) {
    if (items.isEmpty()) return
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    Column(modifier = modifier.padding(bottom = 20.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
        )
        LazyRow(
            state = listState,
            // Pure-black stage behind the posters, like the reference.
            modifier = Modifier.background(Color.Black),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(items, key = { _, item -> "${item.type}:${item.id}" }) { index, item ->
                // Distance of this item's center from the viewport center,
                // normalized to roughly [-1, 1] (0 = centered). Computed from
                // the live layout info so it updates smoothly while scrolling.
                // Layout offsets are measured from the layout-start edge, so
                // flip the sign under RTL to keep the visual lean consistent.
                val layoutInfo = listState.layoutInfo
                val viewportSize =
                    (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset).coerceAtLeast(1)
                val viewportCenter =
                    (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
                val itemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
                val visualOffset = if (itemInfo != null) {
                    val itemCenter = itemInfo.offset + itemInfo.size / 2f
                    val normalized = (itemCenter - viewportCenter) / viewportSize * 2f
                    val directionSign =
                        if (layoutDirection == LayoutDirection.Ltr) 1f else -1f
                    (normalized * directionSign).coerceIn(-1.5f, 1.5f)
                } else {
                    0f
                }
                val distance = min(abs(visualOffset), 1f)
                // Side posters blur as they recede; the centered poster
                // stays perfectly sharp. Skip the blur entirely near the
                // center so the focused poster pays no blur cost on the
                // TV box (light version of the reference effect).
                val posterBlur = if (distance > 0.08f) {
                    (distance * CoverFlowMaxBlurDp).dp
                } else {
                    0.dp
                }
                Box(
                    modifier = Modifier.graphicsLayer {
                        // Outer edge recedes: right-of-center leans right/back,
                        // left-of-center leans left/back. Stronger lean than
                        // v2.2 so the side posters read as a tilted stage.
                        rotationY = visualOffset * CoverFlowMaxRotationY
                        // Center poster pops larger than life; sides shrink
                        // away from it.
                        val scale = CoverFlowCenterScale - distance * CoverFlowScaleDrop
                        scaleX = scale
                        scaleY = scale
                        alpha = 1f - distance * CoverFlowAlphaDrop
                        // Gentle cylindrical dip for off-center posters.
                        translationY = distance * CoverFlowDipDp * density.density
                        cameraDistance = CoverFlowCameraDistance * density.density
                        transformOrigin = TransformOrigin.Center
                    },
                ) {
                    NuvioPosterCard(
                        title = item.name,
                        imageUrl = item.poster,
                        detailLine = item.releaseInfo,
                        rankNumber = if (ranked) index + 1 else null,
                        onClick = onPosterClick?.let { { it(item) } },
                        focusRequester = if (index == 0) firstItemFocusRequester else null,
                        posterBlurRadius = posterBlur,
                    )
                }
            }
        }
    }
}

private const val CoverFlowMaxRotationY = 58f
private const val CoverFlowCenterScale = 1.08f
private const val CoverFlowScaleDrop = 0.30f
private const val CoverFlowAlphaDrop = 0.30f
private const val CoverFlowDipDp = 8f
private const val CoverFlowCameraDistance = 12f
private const val CoverFlowMaxBlurDp = 6f

/**
 * The "استكشف حسب العلامة" (explore by brand) chips row from the Showflix home.
 * Tapping a chip opens that brand's detail page.
 */
@Composable
fun MatrixBrandChipsRow(
    onBrandClick: (MatrixBrand) -> Unit,
    modifier: Modifier = Modifier,
    /**
     * TV remote (DPAD) entry point: when set, the first chip requests focus.
     */
    firstChipFocusRequester: androidx.compose.ui.focus.FocusRequester? = null,
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
                val logoModel = brand.logoModel()
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onBrandClick(brand) }
                        .then(
                            if (brand == MATRIX_BRANDS.firstOrNull() && firstChipFocusRequester != null) {
                                Modifier.focusRequester(firstChipFocusRequester)
                            } else {
                                Modifier
                            },
                        )
                        .tvFocusRing(cornerRadius = 20.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    ) {
                        if (logoModel != null) {
                            AsyncImage(
                                model = logoModel,
                                contentDescription = brand.name,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .height(26.dp)
                                    .widthIn(max = 110.dp),
                            )
                        } else {
                            Text(
                                text = brand.name,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}
