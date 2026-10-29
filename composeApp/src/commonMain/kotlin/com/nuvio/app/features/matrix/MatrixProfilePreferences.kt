package com.nuvio.app.features.matrix

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Showflix-style profile personalization options.
 * Watch languages and favorite genres curate the home rows.
 */

data class MatrixWatchLanguage(val code: String, val label: String)

val MATRIX_WATCH_LANGUAGES = listOf(
    MatrixWatchLanguage("all", "الكل"),
    MatrixWatchLanguage("fr", "الفرنسية"),
    MatrixWatchLanguage("ar", "العربية"),
    MatrixWatchLanguage("tr", "التركية"),
    MatrixWatchLanguage("es", "الإسبانية"),
)

data class MatrixGenre(val id: String, val label: String)

val MATRIX_GENRES = listOf(
    MatrixGenre("Action", "أكشن"),
    MatrixGenre("Comedy", "كوميديا"),
    MatrixGenre("Drama", "دراما"),
    MatrixGenre("Thriller", "تشويق"),
    MatrixGenre("Animation", "رسوم متحركة"),
    MatrixGenre("Documentary", "وثائقي"),
    MatrixGenre("Romance", "رومانسي"),
    MatrixGenre("Sci-Fi", "خيال علمي"),
)

/**
 * Brand IDs with primarily Arabic content, prioritized when Arabic is a
 * preferred watch language.
 */
val MATRIX_ARABIC_BRAND_IDS = setOf("mbc", "watchit", "yango")

/**
 * Brand IDs boosted when Animation is a favorite genre.
 */
val MATRIX_ANIMATION_BRAND_IDS = setOf("crunchyroll", "disney")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MatrixWatchLanguageChips(
    selected: List<String>,
    onSelectionChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = "لغات المشاهدة",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Text(
            text = "اختر ما يناسبك. يظل كل المحتوى متاحًا.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 8.dp),
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MATRIX_WATCH_LANGUAGES.forEach { lang ->
                val isSelected = if (lang.code == "all") {
                    selected.isEmpty() || selected.contains("all")
                } else {
                    selected.contains(lang.code)
                }
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        val newSelection = if (lang.code == "all") {
                            emptyList()
                        } else {
                            val current = selected.filter { it != "all" }.toMutableList()
                            if (current.contains(lang.code)) {
                                current.remove(lang.code)
                            } else {
                                current.add(lang.code)
                            }
                            current
                        }
                        onSelectionChange(newSelection)
                    },
                    label = { Text(lang.label) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MatrixGenreChips(
    selected: List<String>,
    onSelectionChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = "القصص التي تحبها",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Text(
            text = "اختياري. تتعلم الصفحة الرئيسية أيضًا مما تشاهده.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 8.dp),
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MATRIX_GENRES.forEach { genre ->
                val isSelected = selected.contains(genre.id)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        val newSelection = selected.toMutableList()
                        if (isSelected) {
                            newSelection.remove(genre.id)
                        } else {
                            newSelection.add(genre.id)
                        }
                        onSelectionChange(newSelection)
                    },
                    label = { Text(genre.label) },
                )
            }
        }
    }
}
