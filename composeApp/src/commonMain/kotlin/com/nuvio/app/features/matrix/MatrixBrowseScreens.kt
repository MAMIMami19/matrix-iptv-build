package com.nuvio.app.features.matrix

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.rememberTvInitialFocus
import com.nuvio.app.features.home.MetaPreview

private data class MatrixBrowseSection(
    val title: String,
    val catalogId: String,
    val ranked: Boolean = false,
)

private val MOVIE_SECTIONS: List<MatrixBrowseSection> = buildList {
    add(MatrixBrowseSection("وصل حديثا", "mx_new_movie"))
    add(MatrixBrowseSection("الأكثر مشاهدة", "mx_top10_movie", ranked = true))
    MATRIX_BRANDS.forEach { brand ->
        brand.movieCatalogId?.let { add(MatrixBrowseSection(brand.arabicTitle, it)) }
    }
}

private val SERIES_SECTIONS: List<MatrixBrowseSection> = buildList {
    add(MatrixBrowseSection("وصل حديثا", "mx_new_series"))
    add(MatrixBrowseSection("الأكثر مشاهدة", "mx_top10_series", ranked = true))
    MATRIX_BRANDS.forEach { brand ->
        brand.seriesCatalogId?.let { add(MatrixBrowseSection(brand.arabicTitle, it)) }
    }
}

@Composable
private fun MatrixBrowseScreen(
    sections: List<MatrixBrowseSection>,
    showBrandChips: Boolean,
    onPosterClick: ((MetaPreview) -> Unit)?,
    onBrandClick: ((MatrixBrand) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) { MatrixSectionsRepository.ensureLoaded() }
    val catalogs by MatrixSectionsRepository.catalogs.collectAsState()
    val profileState by com.nuvio.app.features.profiles.ProfileRepository.state.collectAsStateWithLifecycle()
    val activeProfile = profileState.activeProfile
    // TV remote (DPAD): focus the first interactive element when this tab
    // becomes active (first brand chip, else first poster of the first row).
    val initialFocus = rememberTvInitialFocus()

    val visibleSections = remember(catalogs, activeProfile) {
        val base = sections.mapNotNull { section ->
            val items = catalogs[section.catalogId].orEmpty()
            if (items.isEmpty()) null else section to items
        }
        // Matrix: curate row order by active profile preferences.
        // - Arabic watch language → Arabic-content brand rows first.
        // - Animation favorite → animation brand rows up.
        val watchLangs = activeProfile?.watchLanguages.orEmpty()
        val favGenres = activeProfile?.favoriteGenres.orEmpty()
        val prefersArabic = watchLangs.contains("ar")
        val prefersAnimation = favGenres.contains("Animation")
        if (!prefersArabic && !prefersAnimation) {
            base
        } else {
            val (priority, rest) = base.partition { (section, _) ->
                val brandId = MATRIX_BRANDS.firstOrNull { brand ->
                    brand.movieCatalogId == section.catalogId ||
                        brand.seriesCatalogId == section.catalogId
                }?.id
                (prefersArabic && brandId in MATRIX_ARABIC_BRAND_IDS) ||
                    (prefersAnimation && brandId in MATRIX_ANIMATION_BRAND_IDS)
            }
            priority + rest
        }
    }

    if (visibleSections.isEmpty() && !showBrandChips) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        if (showBrandChips && onBrandClick != null) {
            item(key = "matrix_brand_chips") {
                MatrixBrandChipsRow(
                    onBrandClick = onBrandClick,
                    modifier = Modifier.padding(top = 12.dp),
                    firstChipFocusRequester = initialFocus,
                )
            }
        }
        items(visibleSections, key = { (section, _) -> section.catalogId }) { (section, items) ->
            MatrixSectionRow(
                title = section.title,
                items = items,
                ranked = section.ranked,
                onPosterClick = onPosterClick,
                firstItemFocusRequester = if (!showBrandChips && section == visibleSections.firstOrNull()?.first) {
                    initialFocus
                } else {
                    null
                },
            )
        }
    }
}

/** The "أفلام" (Movies) tab: Showflix-style browse over the movie catalogs. */
@Composable
fun MatrixMoviesScreen(
    onPosterClick: ((MetaPreview) -> Unit)? = null,
    onBrandClick: ((MatrixBrand) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    MatrixBrowseScreen(
        sections = MOVIE_SECTIONS,
        showBrandChips = true,
        onPosterClick = onPosterClick,
        onBrandClick = onBrandClick,
        modifier = modifier,
    )
}

/** The "مسلسلات" (Series) tab: Showflix-style browse over the series catalogs. */
@Composable
fun MatrixSeriesScreen(
    onPosterClick: ((MetaPreview) -> Unit)? = null,
    onBrandClick: ((MatrixBrand) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    MatrixBrowseScreen(
        sections = SERIES_SECTIONS,
        showBrandChips = true,
        onPosterClick = onPosterClick,
        onBrandClick = onBrandClick,
        modifier = modifier,
    )
}
