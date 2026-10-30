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
    MatrixBrand("netflix", "Netflix", "أعمال Netflix الأصلية", "mx_netflix_movie", "mx_netflix_series", "logo_netflix.png"),
    MatrixBrand("disney", "Disney+", "أعمال Disney+ الأصلية", "mx_disney_movie", "mx_disney_series", "logo_disney.png"),
    MatrixBrand("hbo", "HBO", "أعمال HBO الأصلية", "mx_hbo_movie", "mx_hbo_series", "logo_hbo.png"),
    MatrixBrand("apple", "Apple TV", "أعمال Apple TV الأصلية", "mx_apple_movie", "mx_apple_series", "logo_apple.png"),
    MatrixBrand("prime", "Prime Video", "أعمال Prime Video الأصلية", "mx_prime_movie", "mx_prime_series", "logo_prime.png"),
    MatrixBrand("hulu", "Hulu", "أعمال Hulu الأصلية", "mx_hulu_movie", "mx_hulu_series", "logo_hulu.png"),
    MatrixBrand("peacock", "Peacock", "أعمال Peacock الأصلية", "mx_peacock_movie", "mx_peacock_series", "logo_peacock.png"),
    MatrixBrand("mbc", "MBC", "أعمال MBC الأصلية", "mx_mbc_movie", "mx_mbc_series", "logo_mbc.png"),
    MatrixBrand("yango", "Yango Play", "أعمال Yango Play الأصلية", "mx_yango_movie", "mx_yango_series", "logo_yango.png"),
    MatrixBrand("watchit", "Watch It", "أعمال Watch It الأصلية", "mx_watchit_movie", "mx_watchit_series", "logo_watchit.png"),
    MatrixBrand("crunchyroll", "Crunchyroll", "أعمال Crunchyroll الأصلية", null, "mx_crunchyroll_series", "logo_crunchyroll.png"),
    MatrixBrand("sony", "Sony Pictures", "أعمال Sony الأصلية", "mx_sony_movie", null, "logo_sony.png"),
    MatrixBrand("universal", "Universal", "أعمال Universal الأصلية", "mx_universal_movie", null, "logo_universal.png"),
    MatrixBrand("warner", "Warner Bros", "أعمال Warner Bros الأصلية", "mx_warner_movie", "mx_warner_series", "logo_warner.png"),
    MatrixBrand("paramount", "Paramount", "أعمال Paramount الأصلية", "mx_paramount_movie", "mx_paramount_series", "logo_paramount.png"),
)

fun MatrixBrand.logoModel(): String? = logoAsset?.let { "file:///android_asset/$it" }
