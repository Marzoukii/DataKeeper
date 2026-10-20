package com.marzouki.datasaver.presentation.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.TextUnit
import androidx.glance.GlanceTheme
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.marzouki.datasaver.ui.theme.DarkColorScheme
import com.marzouki.datasaver.ui.theme.LightColorScheme

/** Même palette que l'appli, en clair et en sombre (pas de couleurs dynamiques). */
private val WidgetColors = ColorProviders(light = LightColorScheme, dark = DarkColorScheme)

@Composable
internal fun DataKeeperWidgetTheme(content: @Composable () -> Unit) {
    GlanceTheme(colors = WidgetColors, content = content)
}

/** Glance n'accepte que les polices système : sans-serif, en gras pour les chiffres. */
internal fun widgetText(
    color: ColorProvider,
    size: TextUnit,
    weight: FontWeight = FontWeight.Normal,
) = TextStyle(color = color, fontSize = size, fontWeight = weight, fontFamily = FontFamily.SansSerif)
