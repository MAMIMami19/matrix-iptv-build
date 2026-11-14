package com.nuvio.tv.matrix.sections

import com.nuvio.tv.domain.model.ContentType
import com.nuvio.tv.domain.model.MetaPreview
import com.nuvio.tv.domain.model.PosterShape
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

@Serializable
private data class MatrixManifestCatalog(
    val type: String = "",
    val id: String = "",
    val name: String = "",
)

@Serializable
private data class MatrixManifest(
    val catalogs: List<MatrixManifestCatalog> = emptyList(),
)

@Serializable
private data class MatrixCatalogMeta(
    val id: String = "",
    val type: String = "movie",
    val name: String = "",
    val poster: String? = null,
    val description: String? = null,
    val genres: List<String> = emptyList(),
    val releaseInfo: String? = null,
)

@Serializable
private data class MatrixCatalog(
    val metas: List<MatrixCatalogMeta> = emptyList(),
)

private val matrixJson = Json { ignoreUnknownKeys = true }

/** A catalog entry from the matrix-sections manifest, in manifest order. */
data class MatrixCatalogInfo(
    val id: String,
    val name: String,
    val type: String,
)

/**
 * Fetches the matrix-sections virtual addon (manifest + catalog JSONs) over
 * plain HTTP and exposes per-catalog poster lists. Independent from the
 * Stremio addon system: the home injects these catalogs as its own rows.
 *
 * Ported from the 0.6.3-based Matrix build (Ktor -> OkHttp).
 * The served JSONs follow the Stremio catalog layout, so the standard
 * "see all" flow works for these rows too; detail resolution falls back to
 * the installed meta addons (e.g. Cinemeta) since matrix-sections serves no
 * /meta/ endpoint.
 */
object MatrixSectionsRepository {
    /** Public: the home rows use it as their addon base URL. */
    const val BASE_URL =
        "https://raw.githubusercontent.com/MAMIMami19/matrix-sections/main"

    /** Addon id used for the injected home rows. */
    const val ADDON_ID = "matrix-sections"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val _catalogs = MutableStateFlow<Map<String, List<MetaPreview>>>(emptyMap())
    val catalogs: StateFlow<Map<String, List<MetaPreview>>> = _catalogs.asStateFlow()

    private val _orderedCatalogs = MutableStateFlow<List<MatrixCatalogInfo>>(emptyList())
    /** Catalog ids + display names in manifest order (for home rows). */
    val orderedCatalogs: StateFlow<List<MatrixCatalogInfo>> = _orderedCatalogs.asStateFlow()

    private var manifestRequested = false

    private suspend fun httpGetText(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("Matrix sections request failed")
            response.body?.string().orEmpty()
        }
    }

    fun ensureLoaded() {
        if (manifestRequested) return
        manifestRequested = true
        scope.launch {
            try {
                val manifestText = httpGetText("$BASE_URL/manifest.json")
                val manifest = matrixJson.decodeFromString<MatrixManifest>(manifestText)
                _orderedCatalogs.value = manifest.catalogs.map { entry ->
                    MatrixCatalogInfo(id = entry.id, name = entry.name, type = entry.type)
                }
                for (entry in manifest.catalogs) {
                    launch {
                        try {
                            val catText = httpGetText("$BASE_URL/catalog/${entry.type}/${entry.id}.json")
                            val catalog = matrixJson.decodeFromString<MatrixCatalog>(catText)
                            val previews = catalog.metas.map { m ->
                                val resolvedType = m.type.ifBlank { entry.type }
                                MetaPreview(
                                    id = m.id,
                                    type = ContentType.fromString(resolvedType),
                                    rawType = resolvedType,
                                    name = m.name,
                                    poster = m.poster,
                                    posterShape = PosterShape.POSTER,
                                    background = null,
                                    logo = null,
                                    description = m.description,
                                    releaseInfo = m.releaseInfo,
                                    imdbRating = null,
                                    genres = m.genres,
                                    sourceAddonBaseUrl = BASE_URL,
                                )
                            }
                            _catalogs.value = _catalogs.value + (entry.id to previews)
                        } catch (_: Exception) {
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    fun catalog(catalogId: String): List<MetaPreview> =
        _catalogs.value[catalogId].orEmpty()
}
