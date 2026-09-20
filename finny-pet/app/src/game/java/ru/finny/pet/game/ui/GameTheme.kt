package ru.finny.pet.game.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.finny.pet.R

/** Brand palette of the ЛЦТ-2026 template plus a few game accents. Fixed: the game has one look. */
object G {
    val purple = Color(0xFF520978)
    val purpleDeep = Color(0xFF310F53)
    val purpleLight = Color(0xFF8E1F8F)
    val magenta = Color(0xFFFF0053)
    val pink = Color(0xFFFFD6E4)
    val lavender = Color(0xFF8A83D1)
    val lavenderLight = Color(0xFFE4DFFF)
    val gold = Color(0xFFFFC94D)
    val goldDark = Color(0xFFD9A400)
    val green = Color(0xFF2EB86A)
    val greenDark = Color(0xFF1B7F4C)
    val red = Color(0xFFE63946)
    val sky = Color(0xFF6FB1E0)
    val ink = Color(0xFF1C1D22)
    val inkSoft = Color(0xFF5B5666)
    val paper = Color(0xFFFFFFFF)
    val paperTint = Color(0xFFF4F2F8)
    val scrim = Color(0x8A1C1D22)

    val brandGradient = Brush.linearGradient(listOf(purpleDeep, purple, purpleLight))
    val goldGradient = Brush.verticalGradient(listOf(Color(0xFFFFE08A), gold, goldDark))

    /** Semantic colours of the three budget directions. */
    val mandatory = red
    val optional = magenta
    val savings = green

    val radius = 24.dp
    val radiusSmall = 16.dp
}

@OptIn(ExperimentalTextApi::class)
private fun montserrat(weight: FontWeight) = Font(R.font.montserrat, weight, variationSettings = FontVariation.Settings(weight, FontStyle.Normal))
val Montserrat = FontFamily(montserrat(FontWeight.Normal), montserrat(FontWeight.Medium), montserrat(FontWeight.SemiBold), montserrat(FontWeight.Bold), montserrat(FontWeight.ExtraBold))

/** Big and bold: children read short phrases, not paragraphs (ТЗ 3.6: body ≥ 16 sp). */
private val GameTypography = Typography(
    displayMedium = TextStyle(fontFamily = Montserrat, fontSize = 44.sp, lineHeight = 50.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
    displaySmall = TextStyle(fontFamily = Montserrat, fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.ExtraBold),
    headlineMedium = TextStyle(fontFamily = Montserrat, fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.ExtraBold),
    headlineSmall = TextStyle(fontFamily = Montserrat, fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.ExtraBold),
    titleLarge = TextStyle(fontFamily = Montserrat, fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontFamily = Montserrat, fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold),
    titleSmall = TextStyle(fontFamily = Montserrat, fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold),
    bodyLarge = TextStyle(fontFamily = Montserrat, fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    bodyMedium = TextStyle(fontFamily = Montserrat, fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium),
    bodySmall = TextStyle(fontFamily = Montserrat, fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
    labelLarge = TextStyle(fontFamily = Montserrat, fontSize = 17.sp, lineHeight = 20.sp, fontWeight = FontWeight.ExtraBold),
    labelMedium = TextStyle(fontFamily = Montserrat, fontSize = 14.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold),
    labelSmall = TextStyle(fontFamily = Montserrat, fontSize = 12.sp, lineHeight = 14.sp, fontWeight = FontWeight.Bold),
)

@Composable
fun GameTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = G.purple, onPrimary = Color.White, primaryContainer = G.pink, onPrimaryContainer = G.purpleDeep,
            secondary = G.magenta, onSecondary = Color.White, secondaryContainer = G.pink, onSecondaryContainer = G.purpleDeep,
            tertiary = G.lavender, tertiaryContainer = G.lavenderLight,
            surface = G.paper, onSurface = G.ink, surfaceVariant = G.paperTint, onSurfaceVariant = G.inkSoft,
            background = G.paperTint, onBackground = G.ink, outline = Color(0xFFB9B3C4), error = G.red,
        ),
        typography = GameTypography,
        shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(G.radiusSmall), large = RoundedCornerShape(G.radius), extraLarge = RoundedCornerShape(32.dp)),
        content = content,
    )
}
