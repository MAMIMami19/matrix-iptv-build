package com.nuvio.tv.matrix.iptv

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

@Serializable
internal data class XtreamCategory(
    @SerialName("category_id") val categoryId: String,
    @SerialName("category_name") val categoryName: String,
)

@Serializable
internal data class XtreamLiveStream(
    @SerialName("stream_id") val streamId: Int,
    @SerialName("name") val name: String,
    @SerialName("stream_icon") val streamIcon: String? = null,
    @SerialName("category_id") val categoryId: String? = null,
)

@Serializable
internal data class XtreamVodStream(
    @SerialName("stream_id") val streamId: Int,
    @SerialName("name") val name: String,
    @SerialName("stream_icon") val streamIcon: String? = null,
    @SerialName("container_extension") val containerExtension: String? = null,
    @SerialName("category_id") val categoryId: String? = null,
)

@Serializable
internal data class XtreamSeries(
    @SerialName("series_id") val seriesId: Int,
    @SerialName("name") val name: String,
    @SerialName("cover") val cover: String? = null,
    @SerialName("category_id") val categoryId: String? = null,
)

@Serializable
internal data class XtreamEpisode(
    @SerialName("id") val id: Int,
    @SerialName("episode_num") val episodeNum: Int? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("container_extension") val containerExtension: String? = null,
)

@Serializable
internal data class XtreamSeriesInfo(
    @SerialName("episodes") val episodes: Map<String, List<XtreamEpisode>> = emptyMap(),
)

/**
 * Minimal Xtream Codes client for the embedded Matrix line.
 *
 * Ported from the 0.6.3-based Matrix build (Ktor -> OkHttp, the new base's
 * HTTP stack). Server + credentials come from the compiled-in [XtreamConfig].
 * They are never logged: error surfaces show generic text only, never
 * exception messages, because transport errors can embed request URLs.
 */
internal object XtreamClient {
    private val json = Json { ignoreUnknownKeys = true }
    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // Process-lifetime cache for the whole-catalog calls: the catalogs are
    // large (10k+ channels) and the IPTV tabs re-enter often. Per-category
    // calls stay fresh. A process restart refreshes everything.
    private val cache = mutableMapOf<String, List<*>>()

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T> cached(key: String, fetch: suspend () -> List<T>): List<T> {
        val hit = synchronized(cache) { cache[key] as? List<T> }
        if (hit != null) return hit
        val fresh = fetch()
        synchronized(cache) { cache[key] = fresh }
        return fresh
    }

    private fun apiUrl(action: String, vararg params: Pair<String, String>): String {
        val base = XtreamConfig.SERVER.trimEnd('/')
        val builder = "$base/player_api.php".toHttpUrl().newBuilder()
            .addQueryParameter("username", XtreamConfig.USERNAME)
            .addQueryParameter("password", XtreamConfig.PASSWORD)
            .addQueryParameter("action", action)
        params.forEach { (key, value) -> builder.addQueryParameter(key, value) }
        return builder.build().toString()
    }

    private suspend fun getText(action: String, vararg params: Pair<String, String>): String =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(apiUrl(action, *params))
                // A plain browser UA: some panels treat unknown clients
                // differently, and this can only help reachability.
                .header("User-Agent", "Mozilla/5.0")
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("Xtream request failed")
                }
                response.body?.string().orEmpty()
            }
        }

    suspend fun liveCategories(): List<XtreamCategory> =
        cached("live_categories") { json.decodeFromString(getText("get_live_categories")) }

    suspend fun liveStreams(categoryId: String): List<XtreamLiveStream> =
        json.decodeFromString(getText("get_live_streams", "category_id" to categoryId))

    /** All live streams across every group (no category filter). */
    suspend fun liveStreamsAll(): List<XtreamLiveStream> =
        cached("live_all") { json.decodeFromString(getText("get_live_streams")) }

    suspend fun vodCategories(): List<XtreamCategory> =
        cached("vod_categories") { json.decodeFromString(getText("get_vod_categories")) }

    suspend fun vodStreams(categoryId: String): List<XtreamVodStream> =
        json.decodeFromString(getText("get_vod_streams", "category_id" to categoryId))

    /** All movies across every VOD group (no category filter). */
    suspend fun vodStreamsAll(): List<XtreamVodStream> =
        cached("vod_all") { json.decodeFromString(getText("get_vod_streams")) }

    /**
     * Matrix 3.1: the already-cached whole VOD catalog, or null if it has
     * not been loaded yet. Never fetches: the merged streams panel must
     * not trigger a full catalog download on an addon title.
     */
    @Suppress("UNCHECKED_CAST")
    fun cachedVodStreams(): List<XtreamVodStream>? =
        synchronized(cache) { cache["vod_all"] as? List<XtreamVodStream> }

    /**
     * Matrix 3.4: the already-cached whole live catalog, or null if it
     * has not been loaded yet. Never fetches: the in-player panel must
     * not trigger a full catalog download on open.
     */
    @Suppress("UNCHECKED_CAST")
    fun cachedLiveStreams(): List<XtreamLiveStream>? =
        synchronized(cache) { cache["live_all"] as? List<XtreamLiveStream> }

    /** Same as [cachedVodStreams] for the whole series catalog. */
    @Suppress("UNCHECKED_CAST")
    fun cachedSeries(): List<XtreamSeries>? =
        synchronized(cache) { cache["series_all"] as? List<XtreamSeries> }

    suspend fun seriesCategories(): List<XtreamCategory> =
        cached("series_categories") { json.decodeFromString(getText("get_series_categories")) }

    suspend fun series(categoryId: String): List<XtreamSeries> =
        json.decodeFromString(getText("get_series", "category_id" to categoryId))

    /** All series across every series group (no category filter). */
    suspend fun seriesAll(): List<XtreamSeries> =
        cached("series_all") { json.decodeFromString(getText("get_series")) }

    suspend fun seriesInfo(seriesId: Int): XtreamSeriesInfo =
        json.decodeFromString(getText("get_series_info", "series_id" to seriesId.toString()))

    fun liveStreamUrl(streamId: Int): String =
        XtreamConfig.SERVER.trimEnd('/') +
            "/live/${XtreamConfig.USERNAME}/${XtreamConfig.PASSWORD}/$streamId.ts"

    fun movieStreamUrl(streamId: Int, containerExtension: String?): String =
        XtreamConfig.SERVER.trimEnd('/') +
            "/movie/${XtreamConfig.USERNAME}/${XtreamConfig.PASSWORD}/" +
            "$streamId.${containerExtension?.takeIf { it.isNotBlank() } ?: "mp4"}"

    fun episodeStreamUrl(episodeId: Int, containerExtension: String?): String =
        XtreamConfig.SERVER.trimEnd('/') +
            "/series/${XtreamConfig.USERNAME}/${XtreamConfig.PASSWORD}/" +
            "$episodeId.${containerExtension?.takeIf { it.isNotBlank() } ?: "mp4"}"
}
