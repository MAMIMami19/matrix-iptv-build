package com.nuvio.app.features.home.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.nuvio.app.core.ui.NuvioShelfSection
import com.nuvio.app.core.ui.NuvioViewAllPillSize
import com.nuvio.app.core.ui.PlatformLogoHeader
import com.nuvio.app.core.ui.rememberPosterCardStyleUiState
import com.nuvio.app.features.catalog.CatalogTarget
import com.nuvio.app.features.home.HomeCatalogSection
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.home.stableKey
import com.nuvio.app.features.watching.application.WatchingState

@Composable
fun HomeCatalogRowSection(
    section: HomeCatalogSection,
    modifier: Modifier = Modifier,
    entries: List<MetaPreview> = section.items,
    watchedKeys: Set<String> = emptySet(),
    fullyWatchedSeriesKeys: Set<String> = emptySet(),
    sectionPadding: Dp? = null,
    onViewAllClick: (() -> Unit)? = null,
    onPosterClick: ((MetaPreview) -> Unit)? = null,
    onPosterLongClick: ((MetaPreview) -> Unit)? = null,
) {
    if (sectionPadding != null) {
        HomeCatalogRowSectionContent(
            section = section,
            entries = entries,
            watchedKeys = watchedKeys,
            fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
            modifier = modifier.fillMaxWidth(),
            sectionPadding = sectionPadding,
            onViewAllClick = onViewAllClick,
            onPosterClick = onPosterClick,
            onPosterLongClick = onPosterLongClick,
        )
    } else {
        BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
            HomeCatalogRowSectionContent(
                section = section,
                entries = entries,
                watchedKeys = watchedKeys,
                fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                modifier = Modifier.fillMaxWidth(),
                sectionPadding = homeSectionHorizontalPaddingForWidth(maxWidth.value),
                onViewAllClick = onViewAllClick,
                onPosterClick = onPosterClick,
                onPosterLongClick = onPosterLongClick,
            )
        }
    }
}

@Composable
private fun HomeCatalogRowSectionContent(
    section: HomeCatalogSection,
    entries: List<MetaPreview>,
    watchedKeys: Set<String>,
    fullyWatchedSeriesKeys: Set<String>,
    modifier: Modifier,
    sectionPadding: Dp,
    onViewAllClick: (() -> Unit)?,
    onPosterClick: ((MetaPreview) -> Unit)?,
    onPosterLongClick: ((MetaPreview) -> Unit)?,
) {
    val posterCardStyle = rememberPosterCardStyleUiState()

    // The sections addon's trending catalogs render with big rank numbers
    // (1-based position in the catalog), like the reference Top-10 design.
    // Every other row renders exactly as before.
    val isRankedRow = (section.target as? CatalogTarget.Addon)?.catalogId in
        setOf("trending_movie", "trending_series")
    // Platform logo headers: the header image is a pure function of the
    // section's own catalog id — the same id that fetched this row's items —
    // so each logo sits above only its own platform's exclusives. Originals
    // catalogs carry a movie/series type suffix, hence prefix matching.
    // The logo replaces the platform-name text; the type suffix survives as
    // a small label underneath so movie and series rows stay
    // distinguishable. Rows not listed here (Top 10, trending, reality,
    // now showing/airing) keep their plain titles.
    val catalogId = (section.target as? CatalogTarget.Addon)?.catalogId
    val platformLogoAsset = when {
        catalogId == null -> null
        catalogId.startsWith("originals_canal") -> "logo_canal.jpg"
        catalogId.startsWith("originals_osn") -> "logo_osn.jpg"
        catalogId == "shahid_movie" || catalogId == "shahid_series" -> "logo_shahid.jpg"
        catalogId.startsWith("originals_disney") -> "logo_disney.jpg"
        catalogId.startsWith("originals_hulu") -> "logo_hulu.jpg"
        catalogId.startsWith("originals_apple") -> "logo_apple.jpg"
        catalogId.startsWith("originals_hbo") -> "logo_hbo.jpg"
        catalogId.startsWith("originals_prime") -> "logo_prime.jpg"
        catalogId.startsWith("originals_paramount") -> "logo_paramount.jpg"
        catalogId.startsWith("originals_netflix") -> "logo_netflix.jpg"
        catalogId.startsWith("originals_natgeo") -> "logo_natgeo.jpg"
        catalogId.startsWith("originals_showmax") -> "logo_showmax.jpg"
        catalogId.startsWith("originals_starz") -> "logo_starz.jpg"
        catalogId == "true_crime" -> "logo_id.jpg"
        else -> null
    }
    val isTrueCrimeRow = catalogId == "true_crime"
    val smallLabel = when {
        isTrueCrimeRow -> "تحقيقات وجرائم"
        (section.target as? CatalogTarget.Addon)?.contentType == "movie" -> "Movies - Films"
        (section.target as? CatalogTarget.Addon)?.contentType == "series" -> "Series - Séries"
        else -> null
    }
    val indexedEntries = entries.mapIndexed { index, item -> index to item }

    NuvioShelfSection(
        title = section.title,
        entries = indexedEntries,
        modifier = modifier,
        headerHorizontalPadding = sectionPadding,
        rowContentPadding = PaddingValues(horizontal = sectionPadding),
        onViewAllClick = onViewAllClick,
        viewAllPillSize = NuvioViewAllPillSize.Compact,
        headerContent = platformLogoAsset?.let { asset ->
            {
                PlatformLogoHeader(
                    assetFileName = asset,
                    contentDescription = section.title,
                    smallLabel = smallLabel,
                    onViewAllClick = onViewAllClick,
                )
            }
        },
        key = { indexed -> indexed.second.stableKey() },
    ) { (index, item) ->
        HomePosterCard(
            item = item,
            useLandscapeBackdropMode = posterCardStyle.catalogLandscapeModeEnabled,
            isWatched = WatchingState.isPosterWatched(
                watchedKeys = watchedKeys,
                item = item,
                fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
            ),
            onClick = onPosterClick?.let { { it(item) } },
            onLongClick = onPosterLongClick?.let { { it(item) } },
            rankNumber = if (isRankedRow) index + 1 else null,
        )
    }
}
