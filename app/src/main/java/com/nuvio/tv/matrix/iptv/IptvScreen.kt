package com.nuvio.tv.matrix.iptv

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.nuvio.tv.R
import com.nuvio.tv.matrix.MatrixTopBarHeight
import com.nuvio.tv.ui.theme.NuvioTheme

// Matrix 3.5: the Settings tab was REMOVED entirely (user order,
// «وهذي تاع الاعدادات في iptv نحيها طول»). Only Live TV and
// Films & Séries remain. IptvSettingsScreen.kt stays in the tree
// (unused) so the PIN/lock settings it managed keep compiling.
private enum class IptvTab { LiveTv, Vod }

/**
 * The IPTV destination: Live TV and Films & Séries (VOD) tabs over the
 * embedded Xtream lines, plus (3.5) a source switcher when backup lines
 * are compiled in.
 *
 * Clear-on-close runs when leaving this destination (not when switching
 * tabs): if the setting is on, this session's IPTV watch progress is removed
 * via [onClearProgress].
 */
@Composable
fun IptvScreen(
    onPlay: XtreamPlayHandler,
    onClearProgress: (List<Pair<String, String>>) -> Unit,
    modifier: Modifier = Modifier,
    // Matrix 3.1: open the merged streams panel for a VOD title.
    onOpenMergedStreams: MergedStreamsHandler? = null,
) {
    IptvSettingsRepository.ensureLoaded()
    var selectedTab by remember { mutableStateOf(IptvTab.LiveTv) }
    val initialFocus = rememberTvInitialFocus()
    // Matrix 3.5: the selected Xtream source (0 = default line).
    val sourceIndex by IptvSettingsRepository.selectedSourceIndex.collectAsState()
    val sources = XtreamConfig.sources

    DisposableEffect(Unit) {
        onDispose {
            if (IptvSettingsRepository.clearHistoryOnClose.value) {
                onClearProgress(IptvSettingsRepository.takePlayedIptvKeys())
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Matrix 3.2: the 3.1 top bar overlays the top of every root
        // route, and these tabs sat at top=16dp — under the bar. Offset
        // below it (bar height + the original 16dp gap); UP from the tabs
        // still reaches the top-bar nav.
        Row(modifier = Modifier.padding(start = 16.dp, top = MatrixTopBarHeight + 16.dp, end = 16.dp)) {
            IptvTabButton(
                label = "Live TV",
                selected = selectedTab == IptvTab.LiveTv,
                onClick = { selectedTab = IptvTab.LiveTv },
                focusRequester = initialFocus,
            )
            // Matrix 3.4: tabs from string resources — Arabic on the box
            // (was hardcoded French "Films & Séries" / "Réglages").
            IptvTabButton(
                label = stringResource(R.string.iptv_tab_vod),
                selected = selectedTab == IptvTab.Vod,
                onClick = { selectedTab = IptvTab.Vod },
            )
        }
        // Matrix 3.5: source switcher — the backup lines. Shown only
        // when at least one backup line is compiled in. UP from the
        // channel list lands here first, so switching lines when a
        // stream cuts is one UP + one OK away.
        if (sources.size > 1) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.iptv_source),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.width(12.dp))
                sources.forEachIndexed { index, _ ->
                    IptvTabButton(
                        label = iptvSourceLabel(index),
                        selected = sourceIndex == index,
                        onClick = { IptvSettingsRepository.setSelectedSourceIndex(index) },
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        // Key the tab content on the source: switching lines resets all
        // remembered catalog state (categories, channels, shelves) and
        // reloads everything from the newly selected line.
        key(sourceIndex) {
            when (selectedTab) {
                IptvTab.LiveTv -> LiveTvScreen(onPlay = onPlay)
                IptvTab.Vod -> VodScreen(onPlay = onPlay, onOpenMergedStreams = onOpenMergedStreams)
            }
        }
    }
}

/** Matrix 3.5: display label for a source — «الرئيسي» for 0, «بديل N» after. */
@Composable
private fun iptvSourceLabel(index: Int): String =
    if (index == 0) {
        stringResource(R.string.iptv_source_main)
    } else {
        stringResource(R.string.iptv_source_backup, index)
    }

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun IptvTabButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null,
) {
    // Matrix 3.5: tv-material3 Card with the theme's Gold focus
    // (FocusBackground + focusRing) — the same pattern as the
    // in-player panel rows, which verified visible on the box. The
    // old Row + tvFocusRing left the focused tab transparent.
    Card(
        onClick = onClick,
        modifier = Modifier.padding(end = 8.dp)
            .then(
                if (focusRequester != null) Modifier.focusRequester(focusRequester)
                else Modifier
            ),
        colors = CardDefaults.colors(
            containerColor = if (selected) TvFocusRingColor
            else MaterialTheme.colorScheme.surfaceVariant,
            focusedContainerColor = NuvioTheme.colors.FocusBackground,
        ),
        border = CardDefaults.border(
            focusedBorder = Border(
                border = NuvioTheme.focusRing.border(NuvioTheme.spacing.xxs),
                shape = RoundedCornerShape(50.dp),
            )
        ),
        scale = CardDefaults.scale(focusedScale = 1.04f),
        shape = CardDefaults.shape(shape = RoundedCornerShape(50.dp)),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
        )
    }
}
