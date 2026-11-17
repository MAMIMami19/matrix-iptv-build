package com.nuvio.tv.matrix.iptv

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nuvio.tv.R
import com.nuvio.tv.matrix.MatrixTopBarHeight

private enum class IptvTab { LiveTv, Vod, Settings }

/**
 * The IPTV destination: Live TV, Films & Séries (VOD) and Réglages tabs over
 * the embedded Xtream line.
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
            IptvTabButton(
                label = stringResource(R.string.iptv_tab_settings),
                selected = selectedTab == IptvTab.Settings,
                onClick = { selectedTab = IptvTab.Settings },
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        when (selectedTab) {
            IptvTab.LiveTv -> LiveTvScreen(onPlay = onPlay)
            IptvTab.Vod -> VodScreen(onPlay = onPlay, onOpenMergedStreams = onOpenMergedStreams)
            IptvTab.Settings -> IptvSettingsSection()
        }
    }
}

@Composable
private fun IptvTabButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null,
) {
    Box(
        modifier = Modifier.padding(end = 8.dp)
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) TvFocusRingColor
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick)
            .then(
                if (focusRequester != null) Modifier.focusRequester(focusRequester)
                else Modifier
            )
            .tvFocusRing(cornerRadius = 50.dp)
            .padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}
