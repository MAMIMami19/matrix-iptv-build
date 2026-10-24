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

enum class AppTheme {
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
    SHOWMAX,
    OSN,
    NETFLIX,
    APPLE_TV,
    MAX,
    SHAHID,
    PARAMOUNT,
    DISNEY,
    STARZPLAY,
    PRIME,
    CANAL,
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
        AppTheme.SHOWMAX -> Res.string.theme_showmax
        AppTheme.OSN -> Res.string.theme_osn
        AppTheme.NETFLIX -> Res.string.theme_netflix
        AppTheme.APPLE_TV -> Res.string.theme_apple_tv
        AppTheme.MAX -> Res.string.theme_max
        AppTheme.SHAHID -> Res.string.theme_shahid
        AppTheme.PARAMOUNT -> Res.string.theme_paramount
        AppTheme.DISNEY -> Res.string.theme_disney
        AppTheme.STARZPLAY -> Res.string.theme_starzplay
        AppTheme.PRIME -> Res.string.theme_prime
        AppTheme.CANAL -> Res.string.theme_canal
    }
