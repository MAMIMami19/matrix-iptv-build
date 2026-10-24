package com.nuvio.app.core.ui

import androidx.compose.ui.graphics.Color

data class ThemeColorPalette(
    val secondary: Color,
    val secondaryVariant: Color,
    val accentGradient: List<Color> = listOf(secondary),
    val nativeAccentHex: String,
    val onSecondary: Color = Color.White,
    val onSecondaryVariant: Color = Color.White,
    val focusRing: Color,
    val focusBackground: Color,
    val background: Color = Color(0xFF0D0D0D),
    val backgroundElevated: Color = Color(0xFF1A1A1A),
    val backgroundCard: Color = Color(0xFF242424),
)

object ThemeColors {

    val Gold = ThemeColorPalette(
        secondary = Color(0xFFE8A91C),
        secondaryVariant = Color(0xFF9A6200),
        accentGradient = listOf(
            Color(0xFF8A5700),
            Color(0xFFE8A91C),
            Color(0xFFFFF1A8),
            Color(0xFFFFD45C),
            Color(0xFF9A6200),
        ),
        nativeAccentHex = "#FFD45C",
        onSecondary = Color(0xFF111111),
        onSecondaryVariant = Color.White,
        focusRing = Color(0xFFFFD45C),
        focusBackground = Color(0xFF3D2D1A),
        background = Color(0xFF0F0E0B),
        backgroundElevated = Color(0xFF1D1A14),
        backgroundCard = Color(0xFF262116),
    )

    val Jade = ThemeColorPalette(
        secondary = Color(0xFF22D37C),
        secondaryVariant = Color(0xFF0BBF9A),
        accentGradient = listOf(Color(0xFF7BF08D), Color(0xFF22D37C), Color(0xFF0BBF9A)),
        nativeAccentHex = "#7BF08D",
        onSecondary = Color(0xFF111111),
        onSecondaryVariant = Color(0xFF111111),
        focusRing = Color(0xFF7BF08D),
        focusBackground = Color(0xFF153A2C),
        background = Color(0xFF0B0F0D),
        backgroundElevated = Color(0xFF141D18),
        backgroundCard = Color(0xFF16251D),
    )

    val RoseGold = ThemeColorPalette(
        secondary = Color(0xFFEC70A9),
        secondaryVariant = Color(0xFFB75AFF),
        accentGradient = listOf(Color(0xFFB75AFF), Color(0xFFEC70A9), Color(0xFFFFB37A)),
        nativeAccentHex = "#FFB37A",
        onSecondary = Color(0xFF111111),
        onSecondaryVariant = Color.White,
        focusRing = Color(0xFFFFB37A),
        focusBackground = Color(0xFF442037),
        background = Color(0xFF100C0F),
        backgroundElevated = Color(0xFF1F161D),
        backgroundCard = Color(0xFF281A24),
    )

    val ArcticBlue = ThemeColorPalette(
        secondary = Color(0xFF3185F5),
        secondaryVariant = Color(0xFF4D55E8),
        accentGradient = listOf(Color(0xFF4DE3FF), Color(0xFF3185F5), Color(0xFF4D55E8)),
        nativeAccentHex = "#4DE3FF",
        onSecondary = Color.White,
        onSecondaryVariant = Color.White,
        focusRing = Color(0xFF4DE3FF),
        focusBackground = Color(0xFF172844),
        background = Color(0xFF0B0E14),
        backgroundElevated = Color(0xFF141A24),
        backgroundCard = Color(0xFF161E2A),
    )

    val Graphite = ThemeColorPalette(
        secondary = Color(0xFFAAB2BE),
        secondaryVariant = Color(0xFF687381),
        accentGradient = listOf(Color(0xFFF3F5F7), Color(0xFFAAB2BE), Color(0xFF687381)),
        nativeAccentHex = "#F3F5F7",
        onSecondary = Color(0xFF111111),
        onSecondaryVariant = Color(0xFFFFFFFF),
        focusRing = Color(0xFFF3F5F7),
        focusBackground = Color(0xFF30343A),
        background = Color(0xFF0C0D0F),
        backgroundElevated = Color(0xFF17191D),
        backgroundCard = Color(0xFF20242A),
    )

    val Crimson = ThemeColorPalette(
        secondary = Color(0xFFE53935),
        secondaryVariant = Color(0xFFC62828),
        nativeAccentHex = "#E53935",
        focusRing = Color(0xFFFF5252),
        focusBackground = Color(0xFF3D1A1A),
        background = Color(0xFF0D0D0D),
        backgroundElevated = Color(0xFF1A1A1A),
        backgroundCard = Color(0xFF241A1A),
    )

    val Ocean = ThemeColorPalette(
        secondary = Color(0xFF1E88E5),
        secondaryVariant = Color(0xFF1565C0),
        nativeAccentHex = "#1E88E5",
        focusRing = Color(0xFF42A5F5),
        focusBackground = Color(0xFF1A2D3D),
        background = Color(0xFF0D0D0F),
        backgroundElevated = Color(0xFF1A1A1E),
        backgroundCard = Color(0xFF1A1F24),
    )

    val Violet = ThemeColorPalette(
        secondary = Color(0xFF8E24AA),
        secondaryVariant = Color(0xFF6A1B9A),
        nativeAccentHex = "#8E24AA",
        focusRing = Color(0xFFAB47BC),
        focusBackground = Color(0xFF2D1A3D),
        background = Color(0xFF0D0D0F),
        backgroundElevated = Color(0xFF1A1A1E),
        backgroundCard = Color(0xFF1F1A24),
    )

    val Emerald = ThemeColorPalette(
        secondary = Color(0xFF43A047),
        secondaryVariant = Color(0xFF2E7D32),
        nativeAccentHex = "#43A047",
        focusRing = Color(0xFF66BB6A),
        focusBackground = Color(0xFF1A3D1E),
        background = Color(0xFF0D0D0D),
        backgroundElevated = Color(0xFF1A1A1A),
        backgroundCard = Color(0xFF1A241A),
    )

    val Amber = ThemeColorPalette(
        secondary = Color(0xFFFB8C00),
        secondaryVariant = Color(0xFFEF6C00),
        nativeAccentHex = "#FB8C00",
        focusRing = Color(0xFFFFA726),
        focusBackground = Color(0xFF3D2D1A),
        background = Color(0xFF0F0D0D),
        backgroundElevated = Color(0xFF1E1A1A),
        backgroundCard = Color(0xFF24201A),
    )

    val Rose = ThemeColorPalette(
        secondary = Color(0xFFD81B60),
        secondaryVariant = Color(0xFFC2185B),
        nativeAccentHex = "#D81B60",
        focusRing = Color(0xFFEC407A),
        focusBackground = Color(0xFF3D1A2D),
        background = Color(0xFF0D0D0D),
        backgroundElevated = Color(0xFF1A1A1A),
        backgroundCard = Color(0xFF241A1F),
    )

    val White = ThemeColorPalette(
        secondary = Color(0xFFF5F5F5),
        secondaryVariant = Color(0xFFE0E0E0),
        nativeAccentHex = "#F5F5F5",
        onSecondary = Color(0xFF111111),
        onSecondaryVariant = Color(0xFF111111),
        focusRing = Color(0xFFFFFFFF),
        focusBackground = Color(0xFF303030),
        background = Color(0xFF0D0D0D),
        backgroundElevated = Color(0xFF1A1A1A),
        backgroundCard = Color(0xFF222222),
    )

    val Showmax = ThemeColorPalette(
        secondary = Color(0xFFFF0E62),
        secondaryVariant = Color(0xFFFF7A1A),
        accentGradient = listOf(Color(0xFFFF0E62), Color(0xFFFF7A1A)),
        nativeAccentHex = "#FF0E62",
        onSecondary = Color.White,
        focusRing = Color(0xFFFF5C39),
        focusBackground = Color(0xFF2A1428),
        background = Color(0xFF07121E),
        backgroundElevated = Color(0xFF0D1B2B),
        backgroundCard = Color(0xFF131E30),
    )

    val Osn = ThemeColorPalette(
        secondary = Color(0xFFFF00D4),
        secondaryVariant = Color(0xFFC400A5),
        accentGradient = listOf(Color(0xFFFF00D4)),
        nativeAccentHex = "#FF00D4",
        onSecondary = Color.White,
        focusRing = Color(0xFFFF00D4),
        focusBackground = Color(0xFF2E0226),
        background = Color(0xFF000000),
        backgroundElevated = Color(0xFF141414),
        backgroundCard = Color(0xFF1F1F1F),
    )

    val Netflix = ThemeColorPalette(
        secondary = Color(0xFFE50914),
        secondaryVariant = Color(0xFFB00610),
        accentGradient = listOf(Color(0xFFE50914)),
        nativeAccentHex = "#E50914",
        onSecondary = Color.White,
        focusRing = Color(0xFFFFFFFF),
        focusBackground = Color(0xFF33110F),
        background = Color(0xFF141414),
        backgroundElevated = Color(0xFF1F1F1F),
        backgroundCard = Color(0xFF262626),
    )

    val AppleTv = ThemeColorPalette(
        secondary = Color(0xFF1480EE),
        secondaryVariant = Color(0xFF0A5CB8),
        accentGradient = listOf(Color(0xFF1480EE)),
        nativeAccentHex = "#1480EE",
        onSecondary = Color.White,
        focusRing = Color(0xFFFFFFFF),
        focusBackground = Color(0xFF0D2138),
        background = Color(0xFF000000),
        backgroundElevated = Color(0xFF1A1A1A),
        backgroundCard = Color(0xFF222222),
    )

    val Max = ThemeColorPalette(
        secondary = Color(0xFF5D22BD),
        secondaryVariant = Color(0xFF9D6BFF),
        accentGradient = listOf(Color(0xFF2F1D7D), Color(0xFF5D22BD), Color(0xFF9D6BFF)),
        nativeAccentHex = "#9D6BFF",
        onSecondary = Color.White,
        focusRing = Color(0xFFC4A8FF),
        focusBackground = Color(0xFF241447),
        background = Color(0xFF150A2E),
        backgroundElevated = Color(0xFF21123F),
        backgroundCard = Color(0xFF2A1850),
    )

    val Shahid = ThemeColorPalette(
        secondary = Color(0xFFF72C5B),
        secondaryVariant = Color(0xFFC72567),
        accentGradient = listOf(Color(0xFFF72C5B), Color(0xFFE91E63)),
        nativeAccentHex = "#F72C5B",
        onSecondary = Color.White,
        focusRing = Color(0xFFF72C5B),
        focusBackground = Color(0xFF2E1020),
        background = Color(0xFF101020),
        backgroundElevated = Color(0xFF1E242F),
        backgroundCard = Color(0xFF1C1C30),
    )

    val Paramount = ThemeColorPalette(
        secondary = Color(0xFF0652DF),
        secondaryVariant = Color(0xFF0064FF),
        accentGradient = listOf(Color(0xFF0652DF)),
        nativeAccentHex = "#0652DF",
        onSecondary = Color.White,
        focusRing = Color(0xFF4D9FFF),
        focusBackground = Color(0xFF0A1E42),
        background = Color(0xFF101010),
        backgroundElevated = Color(0xFF161616),
        backgroundCard = Color(0xFF1F1F1F),
    )

    val Disney = ThemeColorPalette(
        secondary = Color(0xFF1288E8),
        secondaryVariant = Color(0xFF0CC0DF),
        accentGradient = listOf(Color(0xFF113CCF), Color(0xFF1288E8), Color(0xFF0CC0DF)),
        nativeAccentHex = "#0CC0DF",
        onSecondary = Color.White,
        focusRing = Color(0xFFFFFFFF),
        focusBackground = Color(0xFF0E2A4A),
        background = Color(0xFF0C1B33),
        backgroundElevated = Color(0xFF13263F),
        backgroundCard = Color(0xFF1B3050),
    )

    val Starzplay = ThemeColorPalette(
        secondary = Color(0xFF17B8C9),
        secondaryVariant = Color(0xFF78F6F6),
        accentGradient = listOf(Color(0xFF17B8C9), Color(0xFF78F6F6)),
        nativeAccentHex = "#17B8C9",
        onSecondary = Color(0xFF062A30),
        focusRing = Color(0xFF78F6F6),
        focusBackground = Color(0xFF07303A),
        background = Color(0xFF000000),
        backgroundElevated = Color(0xFF121212),
        backgroundCard = Color(0xFF1A1A1A),
    )

    val Prime = ThemeColorPalette(
        secondary = Color(0xFF00A8E1),
        secondaryVariant = Color(0xFF0073A8),
        accentGradient = listOf(Color(0xFF00A8E1)),
        nativeAccentHex = "#00A8E1",
        onSecondary = Color.White,
        focusRing = Color(0xFF00A8E1),
        focusBackground = Color(0xFF0A2A38),
        background = Color(0xFF0F171E),
        backgroundElevated = Color(0xFF1A242F),
        backgroundCard = Color(0xFF232F3E),
    )

    val Canal = ThemeColorPalette(
        secondary = Color(0xFFF43F5E),
        secondaryVariant = Color(0xFFD8324E),
        accentGradient = listOf(Color(0xFFD8324E), Color(0xFFF43F5E)),
        nativeAccentHex = "#F43F5E",
        onSecondary = Color.White,
        focusRing = Color(0xFFF43F5E),
        focusBackground = Color(0xFF2E0E16),
        background = Color(0xFF000000),
        backgroundElevated = Color(0xFF1C1C1D),
        backgroundCard = Color(0xFF29292A),
    )

    fun getColorPalette(
        theme: AppTheme,
        customColors: CustomThemeColors = CustomThemeColors.Default,
    ): ThemeColorPalette = when (theme) {
        AppTheme.CUSTOM -> customColors.toColorPalette()
        AppTheme.GOLD -> Gold
        AppTheme.JADE -> Jade
        AppTheme.ROSE_GOLD -> RoseGold
        AppTheme.ARCTIC_BLUE -> ArcticBlue
        AppTheme.GRAPHITE -> Graphite
        AppTheme.CRIMSON -> Crimson
        AppTheme.OCEAN -> Ocean
        AppTheme.VIOLET -> Violet
        AppTheme.EMERALD -> Emerald
        AppTheme.AMBER -> Amber
        AppTheme.ROSE -> Rose
        AppTheme.WHITE -> White
        AppTheme.SHOWMAX -> Showmax
        AppTheme.OSN -> Osn
        AppTheme.NETFLIX -> Netflix
        AppTheme.APPLE_TV -> AppleTv
        AppTheme.MAX -> Max
        AppTheme.SHAHID -> Shahid
        AppTheme.PARAMOUNT -> Paramount
        AppTheme.DISNEY -> Disney
        AppTheme.STARZPLAY -> Starzplay
        AppTheme.PRIME -> Prime
        AppTheme.CANAL -> Canal
    }
}
