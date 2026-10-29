package com.nuvio.app.features.matrix

/**
 * The 15 platform brands from the Showflix reference, in the order they appear
 * in the "استكشف حسب العلامة" (explore by brand) row.
 */
data class MatrixBrand(
    val id: String,
    /** Short brand name shown on chips, e.g. "Netflix". */
    val name: String,
    /** Arabic row title, e.g. "أعمال Netflix الأصلية". */
    val arabicTitle: String,
    val movieCatalogId: String?,
    val seriesCatalogId: String?,
    /** androidApp asset file name for the brand logo, or null for text-only. */
    val logoAsset: String?,
)

val MATRIX_BRANDS: List<MatrixBrand> = listOf(
    MatrixBrand("netflix", "Netflix", "أعمال Netflix الأصلية", "mx_netflix_movie", "mx_netflix_series", "logo_netflix.jpg"),
    MatrixBrand("disney", "Disney+", "أعمال Disney+ الأصلية", "mx_disney_movie", "mx_disney_series", "logo_disney.jpg"),
    MatrixBrand("hbo", "HBO", "أعمال HBO الأصلية", "mx_hbo_movie", "mx_hbo_series", "logo_hbo.jpg"),
    MatrixBrand("apple", "Apple TV", "أعمال Apple TV الأصلية", "mx_apple_movie", "mx_apple_series", "logo_apple.jpg"),
    MatrixBrand("prime", "Prime Video", "أعمال Prime Video الأصلية", "mx_prime_movie", "mx_prime_series", "logo_prime.jpg"),
    MatrixBrand("hulu", "Hulu", "أعمال Hulu الأصلية", "mx_hulu_movie", "mx_hulu_series", "logo_hulu.jpg"),
    MatrixBrand("peacock", "Peacock", "أعمال Peacock الأصلية", "mx_peacock_movie", "mx_peacock_series", null),
    MatrixBrand("mbc", "MBC", "أعمال MBC الأصلية", "mx_mbc_movie", "mx_mbc_series", null),
    MatrixBrand("yango", "Yango Play", "أعمال Yango Play الأصلية", "mx_yango_movie", "mx_yango_series", null),
    MatrixBrand("watchit", "Watch It", "أعمال Watch It الأصلية", "mx_watchit_movie", "mx_watchit_series", null),
    MatrixBrand("crunchyroll", "Crunchyroll", "أعمال Crunchyroll الأصلية", null, "mx_crunchyroll_series", null),
    MatrixBrand("sony", "Sony Pictures", "أعمال Sony الأصلية", "mx_sony_movie", null, null),
    MatrixBrand("universal", "Universal", "أعمال Universal الأصلية", "mx_universal_movie", null, null),
    MatrixBrand("warner", "Warner Bros", "أعمال Warner Bros الأصلية", "mx_warner_movie", "mx_warner_series", null),
    MatrixBrand("paramount", "Paramount", "أعمال Paramount الأصلية", "mx_paramount_movie", "mx_paramount_series", "logo_paramount.jpg"),
)

fun MatrixBrand.logoModel(): String? = logoAsset?.let { "file:///android_asset/$it" }
