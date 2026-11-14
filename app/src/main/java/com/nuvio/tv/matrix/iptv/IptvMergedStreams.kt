package com.nuvio.tv.matrix.iptv

import com.nuvio.tv.domain.model.ProxyHeaders
import com.nuvio.tv.domain.model.Stream
import com.nuvio.tv.domain.model.StreamBehaviorHints
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Matrix 3.1 — merged addon + IPTV streams panel (the Notflix idea).
 *
 * Addon titles and IPTV VOD titles meet in the same StreamScreen:
 * - An IPTV VOD tap resolves an IMDb id for the title and opens the
 *   StreamScreen with it; the exact IPTV stream is handed over in memory
 *   ([IptvMergedStreamHandoff]) and shown as the last "IPTV" group.
 * - An addon title's StreamScreen looks the title up in the already-cached
 *   Xtream catalog (title + year, episode level for series) and appends the
 *   matching IPTV stream as the last "IPTV" group when one exists.
 *
 * Order is always: addon sources first, IPTV last. Every lookup failure
 * returns null and the caller keeps the old behavior (direct IPTV play, or
 * the plain addon panel). No Xtream URL, server or credential is ever
 * logged here; the handoff lives in memory only and is never written to
 * navigation arguments (routes get logged).
 *
 * Title -> IMDb resolution uses the public keyless Cinemeta catalog search,
 * not TMDB: the app's TMDB key comes from local.properties and CI builds
 * ship without one, so a TMDB search would always fail on the box.
 */
internal const val IPTV_STREAM_GROUP_NAME = "IPTV"

/** The exact IPTV stream behind a VOD tap, handed to the StreamScreen. */
internal data class IptvStreamRef(
    val streamId: Int,
    val url: String,
    val label: String,
    val contentType: String,
    val poster: String?,
    val season: Int?,
    val episode: Int?,
) {
    fun toStream(): Stream = Stream(
        name = label,
        title = null,
        description = null,
        url = url,
        ytId = null,
        infoHash = null,
        fileIdx = null,
        externalUrl = null,
        behaviorHints = StreamBehaviorHints(
            notWebReady = null,
            bingeGroup = null,
            countryWhitelist = null,
            proxyHeaders = ProxyHeaders(
                // Same UA the IPTV direct-play path sends: the panel's
                // playback info takes its headers from here.
                request = mapOf("User-Agent" to "Mozilla/5.0"),
                response = null,
            ),
        ),
        addonName = IPTV_STREAM_GROUP_NAME,
        addonLogo = null,
    )
}

/**
 * One-shot, in-memory handoff from the IPTV VOD tab to the StreamScreen
 * ViewModel. Set immediately before navigating; consumed once when the
 * ViewModel is created. A process kill between the two just loses the
 * handoff — the title-match lookup is the fallback.
 */
internal object IptvMergedStreamHandoff {
    @Volatile
    private var pending: IptvStreamRef? = null

    fun set(ref: IptvStreamRef) {
        pending = ref
    }

    fun take(): IptvStreamRef? {
        val ref = pending
        pending = null
        return ref
    }
}

/**
 * Provider-name cleanup: Xtream names carry tags like "SUB-AR||", language
 * and quality markers, years and season markers. [cleanTitle] keeps the
 * bare title; [normalize] is the compare form for both sides of a match.
 */
internal object IptvTitleMatcher {
    private val YEAR_REGEX = Regex("""\b(19\d{2}|20\d{2})\b""")
    private val TRAILING_YEAR_REGEX = Regex("""\b(19\d{2}|20\d{2})\b\s*$""")
    private val SEASON_MARKER_REGEX =
        Regex("""\bs\d{1,2}(e\d{1,2})?\b|\bseason\s*\d+\b""", RegexOption.IGNORE_CASE)
    private val BRACKET_REGEX = Regex("""\[[^\]]*\]""")
    private val PAREN_REGEX = Regex("""\(([^)]*)\)""")

    // Standalone tokens that are provider tags, never part of a title.
    private val TAG_TOKENS = setOf(
        "sub", "sub-ar", "subar", "ar", "ara", "arabic",
        "fr", "fra", "french", "vostfr", "vf", "vo",
        "en", "eng", "english", "es", "esp", "de", "it", "tr", "hi", "hindi",
        "multi", "dual", "dubbed",
        "4k", "uhd", "fhd", "hd", "sd", "cam", "hdcam", "web-dl", "webdl",
        "bluray", "brrip", "x264", "x265", "hevc", "hdr", "dolby", "atmos",
        "imax", "3d",
    )

    fun parseYear(raw: String): Int? =
        YEAR_REGEX.find(raw)?.value?.toIntOrNull()

    fun cleanTitle(raw: String): String {
        var text = raw
        // "||" segments: keep the segments that are not pure tags.
        val segments = text.split("||").map { it.trim() }.filter { it.isNotEmpty() }
        val kept = segments.filter { segment ->
            val tokens = segment.split(Regex("[\\s\\-_/]+")).filter { it.isNotBlank() }
            tokens.isNotEmpty() && tokens.any { token ->
                val lower = token.lowercase().trim('[', ']', '(', ')', '.', ',')
                lower !in TAG_TOKENS && !YEAR_REGEX.matches(lower)
            }
        }
        text = if (kept.isNotEmpty()) kept.joinToString(" ") else raw
        // Drop bracketed [...] groups (quality/tags) and (...) groups that
        // are a year or a tag; keep other parentheticals.
        text = text.replace(BRACKET_REGEX, " ")
        text = text.replace(PAREN_REGEX) { match ->
            val inner = match.groupValues[1].trim()
            if (YEAR_REGEX.matches(inner) || inner.lowercase() in TAG_TOKENS) " " else "(${inner})"
        }
        // Season markers ("S01", "Season 2") are not part of a series title.
        text = text.replace(SEASON_MARKER_REGEX, " ")
        // A trailing year ("Title 2024") is metadata, not title — but a year
        // elsewhere ("2001: A Space Odyssey") is kept.
        text = text.replace(TRAILING_YEAR_REGEX, " ")
        val tokens = text.split(Regex("\\s+")).filter { it.isNotBlank() }
        val trimmed = tokens.dropLastWhile { token ->
            token.lowercase().trim('.', ',', '-', '_') in TAG_TOKENS
        }
        return trimmed.joinToString(" ").trim(' ', '-', ':', '|', '.')
    }

    /** Compare form: lowercase, no diacritics, punctuation -> spaces. */
    fun normalize(raw: String): String {
        val noDiacritics = java.text.Normalizer.normalize(raw, java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        return noDiacritics.lowercase()
            .replace(Regex("[^a-z0-9]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}

@Serializable
private data class CinemetaSearchResponse(
    @SerialName("metas") val metas: List<CinemetaSearchMeta> = emptyList(),
)

@Serializable
private data class CinemetaSearchMeta(
    @SerialName("id") val id: String = "",
    @SerialName("name") val name: String? = null,
    @SerialName("releaseInfo") val releaseInfo: String? = null,
)

internal object IptvMergedStreams {
    private const val CINEMETA_BASE = "https://v3-cinemeta.strem.io"
    private val json = Json { ignoreUnknownKeys = true }
    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    // Process-lifetime cache of resolved titles (successes only).
    private val imdbCache = ConcurrentHashMap<String, String>()

    /**
     * Resolve an IMDb id for a (cleaned) title via the public Cinemeta
     * catalog search. Strict: only an exact normalized-name match counts,
     * with a year match preferred when a year is known. Null on any
     * failure or no match — the caller falls back to direct IPTV play.
     */
    suspend fun resolveImdbId(title: String, year: Int?, type: String): String? =
        withContext(Dispatchers.IO) {
            val clean = IptvTitleMatcher.cleanTitle(title)
            val target = IptvTitleMatcher.normalize(clean)
            if (target.isBlank()) return@withContext null
            val mediaType =
                if (type.equals("series", ignoreCase = true) ||
                    type.equals("tv", ignoreCase = true)
                ) {
                    "series"
                } else {
                    "movie"
                }
            val cacheKey = "$mediaType|$target|${year ?: ""}"
            imdbCache[cacheKey]?.let { return@withContext it }
            try {
                val encoded = URLEncoder.encode(clean, "UTF-8").replace("+", "%20")
                val request = Request.Builder()
                    .url("$CINEMETA_BASE/catalog/$mediaType/top/search=$encoded.json")
                    .header("User-Agent", "Mozilla/5.0")
                    .build()
                val body = httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    response.body?.string().orEmpty()
                }
                if (body.isBlank()) return@withContext null
                val metas = json.decodeFromString<CinemetaSearchResponse>(body).metas
                val exact = metas.filter { meta ->
                    meta.id.startsWith("tt") &&
                        IptvTitleMatcher.normalize(meta.name.orEmpty()) == target
                }
                val best = exact.firstOrNull { meta ->
                    year != null && meta.releaseInfo?.toIntOrNull() == year
                } ?: exact.firstOrNull() ?: return@withContext null
                imdbCache[cacheKey] = best.id
                best.id
            } catch (_: Exception) {
                null
            }
        }

    /**
     * Find the IPTV VOD stream matching an addon title. Movies match on
     * title (+ year when both sides have one); series match at episode
     * level (series title + season/episode). Only the already-cached
     * catalog is searched — a StreamScreen open must never trigger a full
     * catalog download; the catalog warms when the IPTV tab is opened.
     * Null when there is no match, no cached catalog, or any failure.
     */
    suspend fun findMatchForTitle(
        title: String,
        year: Int?,
        type: String,
        season: Int?,
        episode: Int?,
    ): Stream? {
        val target = IptvTitleMatcher.normalize(title)
        if (target.isBlank()) return null
        return try {
            when (type.lowercase()) {
                "movie" -> {
                    val catalog = XtreamClient.cachedVodStreams() ?: return null
                    val match = catalog.firstOrNull { movie ->
                        if (IptvTitleMatcher.normalize(IptvTitleMatcher.cleanTitle(movie.name)) != target) {
                            return@firstOrNull false
                        }
                        val movieYear = IptvTitleMatcher.parseYear(movie.name)
                        year == null || movieYear == null || movieYear == year
                    } ?: catalog.firstOrNull { movie ->
                        IptvTitleMatcher.normalize(IptvTitleMatcher.cleanTitle(movie.name)) == target
                    } ?: return null
                    movieStream(match)
                }
                "series", "tv" -> {
                    val catalog = XtreamClient.cachedSeries() ?: return null
                    val series = catalog.firstOrNull { item ->
                        IptvTitleMatcher.normalize(IptvTitleMatcher.cleanTitle(item.name)) == target
                    } ?: return null
                    val seasonNum = season ?: return null
                    val episodeNum = episode ?: return null
                    val info = XtreamClient.seriesInfo(series.seriesId)
                    val ep = info.episodes[seasonNum.toString()]
                        ?.firstOrNull { it.episodeNum == episodeNum }
                        ?: return null
                    episodeStream(series, seasonNum, ep)
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun movieStream(movie: XtreamVodStream): Stream =
        IptvStreamRef(
            streamId = movie.streamId,
            url = XtreamClient.movieStreamUrl(movie.streamId, movie.containerExtension),
            label = IptvTitleMatcher.cleanTitle(movie.name).ifBlank { movie.name },
            contentType = "movie",
            poster = movie.streamIcon,
            season = null,
            episode = null,
        ).toStream()

    fun episodeStream(series: XtreamSeries, season: Int, episode: XtreamEpisode): Stream {
        val episodeTitle = episode.title?.takeIf { it.isNotBlank() }
            ?: "Episode ${episode.episodeNum ?: ""}"
        return IptvStreamRef(
            streamId = episode.id,
            url = XtreamClient.episodeStreamUrl(episode.id, episode.containerExtension),
            label = "S%02dE%02d · %s".format(season, episode.episodeNum ?: 0, episodeTitle),
            contentType = "series",
            poster = series.cover,
            season = season,
            episode = episode.episodeNum,
        ).toStream()
    }
}
