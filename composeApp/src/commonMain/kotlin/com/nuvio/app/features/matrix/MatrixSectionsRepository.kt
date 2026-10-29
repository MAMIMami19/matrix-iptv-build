package com.nuvio.app.features.matrix

import com.nuvio.app.features.addons.httpGetText
import com.nuvio.app.features.home.MetaPreview
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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

/**
 * Fetches the matrix-sections virtual addon (manifest + catalog JSONs) over plain
 * HTTP and exposes per-catalog poster lists. Independent from the Stremio addon
 * system so the Movies/Series/Brand screens work without installing anything.
 */
object MatrixSectionsRepository {
    private const val BASE_URL =
        "https://raw.githubusercontent.com/MAMIMami19/matrix-sections/main"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _catalogs = MutableStateFlow<Map<String, List<MetaPreview>>>(emptyMap())
    val catalogs: StateFlow<Map<String, List<MetaPreview>>> = _catalogs.asStateFlow()

    private var manifestRequested = false

    fun ensureLoaded() {
        if (manifestRequested) return
        manifestRequested = true
        scope.launch {
            try {
                val manifestText = httpGetText("$BASE_URL/manifest.json")
                val manifest = matrixJson.decodeFromString<MatrixManifest>(manifestText)
                for (entry in manifest.catalogs) {
                    launch {
                        try {
                            val catText = httpGetText("$BASE_URL/catalog/${entry.type}/${entry.id}.json")
                            val catalog = matrixJson.decodeFromString<MatrixCatalog>(catText)
                            val previews = catalog.metas.map { m ->
                                MetaPreview(
                                    id = m.id,
                                    type = m.type.ifBlank { entry.type },
                                    name = m.name,
                                    poster = m.poster,
                                    description = m.description,
                                    genres = m.genres,
                                    releaseInfo = m.releaseInfo,
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
