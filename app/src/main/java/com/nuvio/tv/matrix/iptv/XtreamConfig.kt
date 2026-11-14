package com.nuvio.tv.matrix.iptv

import com.nuvio.tv.BuildConfig

/**
 * Matrix IPTV default Xtream line, compiled in from local.properties
 * (CI secrets NUVIO_XTREAM_SERVER / NUVIO_XTREAM_USER / NUVIO_XTREAM_PASS).
 *
 * Values are never logged. The embedded line is the user's own subscription;
 * builds containing it must never be forwarded outside his devices.
 */
internal object XtreamConfig {
    val SERVER: String = BuildConfig.XTREAM_SERVER
    val USERNAME: String = BuildConfig.XTREAM_USERNAME
    val PASSWORD: String = BuildConfig.XTREAM_PASSWORD

    /** True when a complete line was compiled in. */
    val isConfigured: Boolean
        get() = SERVER.isNotBlank() && USERNAME.isNotBlank() && PASSWORD.isNotBlank()
}
