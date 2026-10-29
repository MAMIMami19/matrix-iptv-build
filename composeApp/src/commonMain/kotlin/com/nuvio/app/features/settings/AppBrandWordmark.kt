package com.nuvio.app.features.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage

/**
 * Matrix IPTV brand wordmark. Loaded from androidApp assets (NOT composeResources —
 * new compose resources get no generated accessors in CI builds). The logo is used
 * as-is (colorful gradient M + white wordmark) for every theme.
 */
@Composable
internal fun AppBrandWordmark(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    icon: AppIconOption? = null,
) {
    AsyncImage(
        model = "file:///android_asset/matrix_wordmark.png",
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}
