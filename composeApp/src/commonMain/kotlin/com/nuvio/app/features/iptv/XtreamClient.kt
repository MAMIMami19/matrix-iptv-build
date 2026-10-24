package com.nuvio.app.features.iptv

import com.nuvio.app.core.network.createApiHttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLBuilder
import io.ktor.http.path
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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
)

@Serializable
internal data class XtreamVodStream(
    @SerialName("stream_id") val streamId: Int,
    @SerialName("name") val name: String,
    @SerialName("stream_icon") val streamIcon: String? = null,
    @SerialName("container_extension") val containerExtension: String? = null,
)

@Serializable
internal data class XtreamSeries(
    @SerialName("series_id") val seriesId: Int,
    @SerialName("name") val name: String,
    @SerialName("cover") val cover: String? = null,
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
 * Minimal Xtream Codes client for the NOVA line.
 *
 * Server + credentials come from the generated [XtreamConfig] (GitHub Actions
 * secrets -> local.properties -> generated source). They are never logged:
 * error surfaces show generic text only, never exception messages, because
 * transport errors can embed request URLs.
 */
internal object XtreamClient {
    private val json = Json { ignoreUnknownKeys = true }
    private val httpClient by lazy { createApiHttpClient() }

    private fun apiUrl(action: String, vararg params: Pair<String, String>): String =
        URLBuilder(XtreamConfig.SERVER).apply {
            path("player_api.php")
            parameters.append("username", XtreamConfig.USERNAME)
            parameters.append("password", XtreamConfig.PASSWORD)
            parameters.append("action", action)
            params.forEach { (key, value) -> parameters.append(key, value) }
        }.buildString()

    private suspend fun getText(action: String, vararg params: Pair<String, String>): String =
        httpClient.get(apiUrl(action, *params)) {
            // A plain browser UA: some panels treat unknown clients
            // differently, and this can only help reachability.
            header("User-Agent", "Mozilla/5.0")
        }.bodyAsText()

    suspend fun liveCategories(): List<XtreamCategory> =
        json.decodeFromString(getText("get_live_categories"))

    suspend fun liveStreams(categoryId: String): List<XtreamLiveStream> =
        json.decodeFromString(getText("get_live_streams", "category_id" to categoryId))

    suspend fun vodCategories(): List<XtreamCategory> =
        json.decodeFromString(getText("get_vod_categories"))

    suspend fun vodStreams(categoryId: String): List<XtreamVodStream> =
        json.decodeFromString(getText("get_vod_streams", "category_id" to categoryId))

    suspend fun seriesCategories(): List<XtreamCategory> =
        json.decodeFromString(getText("get_series_categories"))

    suspend fun series(categoryId: String): List<XtreamSeries> =
        json.decodeFromString(getText("get_series", "category_id" to categoryId))

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
