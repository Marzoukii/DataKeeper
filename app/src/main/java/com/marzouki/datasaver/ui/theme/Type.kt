package com.marzouki.datasaver.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.marzouki.datasaver.R

/** Titres et tous les chiffres (Go, Mo, %). */
val SpaceGrotesk = FontFamily(
    Font(R.font.space_grotesk_medium, FontWeight.Medium),
    Font(R.font.space_grotesk_semibold, FontWeight.SemiBold),
    Font(R.font.space_grotesk_bold, FontWeight.Bold),
)

/** Texte courant. */
val DmSans = FontFamily(
    Font(R.font.dm_sans_regular, FontWeight.Normal),
    Font(R.font.dm_sans_medium, FontWeight.Medium),
    Font(R.font.dm_sans_semibold, FontWeight.SemiBold),
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
)

private val Default = Typography()

val Typography = Typography(
    displayLarge = Default.displayLarge.copy(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold),
    displayMedium = Default.displayMedium.copy(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold),
    displaySmall = style(SpaceGrotesk, FontWeight.Bold, 40.sp, 44.sp, (-1).sp),
    headlineLarge = style(SpaceGrotesk, FontWeight.Bold, 34.sp, 36.sp, (-1).sp),
    headlineMedium = Default.headlineMedium.copy(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold),
    headlineSmall = style(SpaceGrotesk, FontWeight.Bold, 26.sp, 32.sp, (-0.5).sp),
    titleLarge = style(SpaceGrotesk, FontWeight.Bold, 22.sp, 28.sp),
    titleMedium = style(DmSans, FontWeight.SemiBold, 16.sp, 22.sp),
    titleSmall = style(DmSans, FontWeight.SemiBold, 14.sp, 20.sp),
    bodyLarge = style(DmSans, FontWeight.Normal, 16.sp, 24.sp),
    bodyMedium = style(DmSans, FontWeight.Normal, 14.sp, 20.sp),
    bodySmall = style(DmSans, FontWeight.Normal, 13.sp, 18.sp),
    labelLarge = style(DmSans, FontWeight.SemiBold, 15.sp, 20.sp),
    labelMedium = style(DmSans, FontWeight.SemiBold, 12.sp, 16.sp),
    labelSmall = Default.labelSmall.copy(fontFamily = DmSans, fontWeight = FontWeight.Medium),
)

/** Même style en Space Grotesk, pour un chiffre placé dans un texte en DM Sans. */
fun TextStyle.numeric(): TextStyle = copy(fontFamily = SpaceGrotesk)
