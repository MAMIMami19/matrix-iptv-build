package com.nuvio.tv.matrix.iptv

import com.nuvio.tv.BuildConfig

/**
 * One embedded Xtream line (server + credentials). Values come from
 * BuildConfig (CI secrets via local.properties) and are never logged.
 */
internal data class XtreamSource(
    val server: String,
    val username: String,
    val password: String,
) {
    val isConfigured: Boolean
        get() = server.isNotBlank() && username.isNotBlank() && password.isNotBlank()
}

/**
 * Matrix IPTV Xtream lines, compiled in from local.properties
 * (CI secrets NUVIO_XTREAM_SERVER / NUVIO_XTREAM_USER / NUVIO_XTREAM_PASS
 * for the default line, NUVIO_XTREAM2/3/4_* for the three 3.5 backups,
 * NUVIO_XTREAM5/6_* for the two 3.5.5 xsmart backups).
 *
 * Source 0 is the default line (the one 3.4 and earlier shipped); the
 * backups are ADDITIONAL sources the user switches to when a line cuts.
 * Values are never logged. The embedded lines are the user's own
 * subscriptions; builds containing them must never be forwarded outside
 * his devices.
 */
internal object XtreamConfig {
    /** The default line, kept for callers that predate multi-source. */
    val SERVER: String = BuildConfig.XTREAM_SERVER
    val USERNAME: String = BuildConfig.XTREAM_USERNAME
    val PASSWORD: String = BuildConfig.XTREAM_PASSWORD

    /** All complete lines, default first. Never empty when configured. */
    val sources: List<XtreamSource> = listOf(
        XtreamSource(
            BuildConfig.XTREAM_SERVER,
            BuildConfig.XTREAM_USERNAME,
            BuildConfig.XTREAM_PASSWORD,
        ),
        XtreamSource(
            BuildConfig.XTREAM2_SERVER,
            BuildConfig.XTREAM2_USERNAME,
            BuildConfig.XTREAM2_PASSWORD,
        ),
        XtreamSource(
            BuildConfig.XTREAM3_SERVER,
            BuildConfig.XTREAM3_USERNAME,
            BuildConfig.XTREAM3_PASSWORD,
        ),
        XtreamSource(
            BuildConfig.XTREAM4_SERVER,
            BuildConfig.XTREAM4_USERNAME,
            BuildConfig.XTREAM4_PASSWORD,
        ),
        XtreamSource(
            BuildConfig.XTREAM5_SERVER,
            BuildConfig.XTREAM5_USERNAME,
            BuildConfig.XTREAM5_PASSWORD,
        ),
        XtreamSource(
            BuildConfig.XTREAM6_SERVER,
            BuildConfig.XTREAM6_USERNAME,
            BuildConfig.XTREAM6_PASSWORD,
        ),
    ).filter { it.isConfigured }

    /** True when a complete default line was compiled in. */
    val isConfigured: Boolean
        get() = sources.isNotEmpty()

    /** True when at least one backup line was compiled in. */
    val hasBackups: Boolean
        get() = sources.size > 1
}
