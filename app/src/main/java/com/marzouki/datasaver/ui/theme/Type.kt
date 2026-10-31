package com.marzouki.datasaver.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.marzouki.datasaver.R

/**
 * Readex Pro, latin et arabe dans la même police : titres, texte et chiffres (tabulaires partout,
 * pour que les colonnes du carnet restent alignées).
 */
val ReadexPro = FontFamily(
    Font(R.font.readex_pro_regular, FontWeight.Normal),
    Font(R.font.readex_pro_medium, FontWeight.Medium),
    Font(R.font.readex_pro_semibold, FontWeight.SemiBold),
    Font(R.font.readex_pro_bold, FontWeight.Bold),
)

private fun style(
    family: FontFamily,
    weight: FontWeight,
    size: TextUnit,
    lineHeight: TextUnit,
    letterSpacing: TextUnit = 0.sp,
) = TextStyle(
    fontFamily = family,
    fontWeight = weight,
    fontSize = size,
    lineHeight = lineHeight,
    letterSpacing = letterSpacing,
    fontFeatureSettings = "tnum",
)

private val Default = Typography()

val Typography = Typography(
    displayLarge = Default.displayLarge.copy(fontFamily = ReadexPro, fontWeight = FontWeight.Medium, letterSpacing = (-1).sp),
    displayMedium = Default.displayMedium.copy(fontFamily = ReadexPro, fontWeight = FontWeight.Medium, letterSpacing = (-1).sp),
    displaySmall = style(ReadexPro, FontWeight.Medium, 40.sp, 44.sp, (-1).sp),
    headlineLarge = style(ReadexPro, FontWeight.SemiBold, 34.sp, 36.sp, (-1).sp),
    headlineMedium = Default.headlineMedium.copy(fontFamily = ReadexPro, fontWeight = FontWeight.Bold),
    headlineSmall = style(ReadexPro, FontWeight.SemiBold, 26.sp, 32.sp, (-0.5).sp),
    titleLarge = style(ReadexPro, FontWeight.SemiBold, 22.sp, 28.sp),
    titleMedium = style(ReadexPro, FontWeight.SemiBold, 16.sp, 22.sp),
    titleSmall = style(ReadexPro, FontWeight.SemiBold, 14.sp, 20.sp),
    bodyLarge = style(ReadexPro, FontWeight.Normal, 16.sp, 24.sp),
    bodyMedium = style(ReadexPro, FontWeight.Normal, 14.sp, 20.sp),
    bodySmall = style(ReadexPro, FontWeight.Normal, 13.sp, 18.sp),
    labelLarge = style(ReadexPro, FontWeight.SemiBold, 15.sp, 20.sp),
    labelMedium = style(ReadexPro, FontWeight.SemiBold, 12.sp, 16.sp),
    labelSmall = Default.labelSmall.copy(fontFamily = ReadexPro, fontWeight = FontWeight.Medium),
)

/**
 * [Typography] sans espacement des lettres, pour l'arabe : ses lettres sont liées et Android ne les
 * espace pas, alors que la mise en page compte l'espacement négatif des titres. Le mot paraît plus
 * étroit qu'il n'est dessiné, et sa dernière lettre passe à la ligne (« استهلاك » puis « ي »).
 */
val RtlTypography = Typography.withoutLetterSpacing()

private fun Typography.withoutLetterSpacing(): Typography {
    fun TextStyle.flat() = copy(letterSpacing = 0.sp)
    return Typography(
        displayLarge = displayLarge.flat(),
        displayMedium = displayMedium.flat(),
        displaySmall = displaySmall.flat(),
        headlineLarge = headlineLarge.flat(),
        headlineMedium = headlineMedium.flat(),
        headlineSmall = headlineSmall.flat(),
        titleLarge = titleLarge.flat(),
        titleMedium = titleMedium.flat(),
        titleSmall = titleSmall.flat(),
        bodyLarge = bodyLarge.flat(),
        bodyMedium = bodyMedium.flat(),
        bodySmall = bodySmall.flat(),
        labelLarge = labelLarge.flat(),
        labelMedium = labelMedium.flat(),
        labelSmall = labelSmall.flat(),
    )
}

/** Chiffres tabulaires, pour un nombre placé dans un texte courant. */
fun TextStyle.numeric(): TextStyle = copy(fontFamily = ReadexPro, fontFeatureSettings = "tnum")
