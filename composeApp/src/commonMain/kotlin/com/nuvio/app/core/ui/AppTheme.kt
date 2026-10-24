package com.nuvio.app.core.ui

import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.theme_amber
import nuvio.composeapp.generated.resources.theme_crimson
import nuvio.composeapp.generated.resources.theme_emerald
import nuvio.composeapp.generated.resources.theme_ocean
import nuvio.composeapp.generated.resources.theme_rose
import nuvio.composeapp.generated.resources.theme_violet
import nuvio.composeapp.generated.resources.theme_white
import nuvio.composeapp.generated.resources.theme_gold
import nuvio.composeapp.generated.resources.theme_jade
import nuvio.composeapp.generated.resources.theme_rose_gold
import nuvio.composeapp.generated.resources.theme_arctic_blue
import nuvio.composeapp.generated.resources.theme_graphite
import nuvio.composeapp.generated.resources.theme_custom
import org.jetbrains.compose.resources.StringResource

enum class AppTheme(val displayLabel: String? = null) {
    GOLD,
    JADE,
    ROSE_GOLD,
    ARCTIC_BLUE,
    GRAPHITE,
    CUSTOM,
    CRIMSON,
    OCEAN,
    VIOLET,
    EMERALD,
    AMBER,
    ROSE,
    WHITE,
    SHOWMAX("Showmax"),
    OSN("OSN+"),
    NETFLIX("Netflix"),
    APPLE_TV("Apple TV"),
    MAX("HBO Max"),
    SHAHID("MBC Shahid"),
    PARAMOUNT("Paramount+"),
    DISNEY("Disney+"),
    STARZPLAY("STARZPLAY"),
    PRIME("Prime Video"),
    CANAL("myCANAL"),
}

val AppTheme.labelRes: StringResource
    get() = when (this) {
        AppTheme.GOLD -> Res.string.theme_gold
        AppTheme.JADE -> Res.string.theme_jade
        AppTheme.ROSE_GOLD -> Res.string.theme_rose_gold
        AppTheme.ARCTIC_BLUE -> Res.string.theme_arctic_blue
        AppTheme.GRAPHITE -> Res.string.theme_graphite
        AppTheme.CUSTOM -> Res.string.theme_custom
        AppTheme.CRIMSON -> Res.string.theme_crimson
        AppTheme.OCEAN -> Res.string.theme_ocean
        AppTheme.VIOLET -> Res.string.theme_violet
        AppTheme.EMERALD -> Res.string.theme_emerald
        AppTheme.AMBER -> Res.string.theme_amber
        AppTheme.ROSE -> Res.string.theme_rose
        AppTheme.WHITE -> Res.string.theme_white
        AppTheme.SHOWMAX -> Res.string.theme_custom
        AppTheme.OSN -> Res.string.theme_custom
        AppTheme.NETFLIX -> Res.string.theme_custom
        AppTheme.APPLE_TV -> Res.string.theme_custom
        AppTheme.MAX -> Res.string.theme_custom
        AppTheme.SHAHID -> Res.string.theme_custom
        AppTheme.PARAMOUNT -> Res.string.theme_custom
        AppTheme.DISNEY -> Res.string.theme_custom
        AppTheme.STARZPLAY -> Res.string.theme_custom
        AppTheme.PRIME -> Res.string.theme_custom
        AppTheme.CANAL -> Res.string.theme_custom
    }
