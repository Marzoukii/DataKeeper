package com.marzouki.datasaver.ui.theme

import androidx.compose.ui.graphics.Color

// Parcours violet : fond blanc cassé, cartes blanches arrondies, accent violet (actions, onglet
// actif, aujourd'hui), sarcelle pour le Wi-Fi (ce qu'on encourage), lavande pour la data mobile
// (ce qu'on surveille). Trois tons froids de la même famille.

/** Couleurs de marque, identiques en clair et en sombre. */
object Brand {
    val Accent = Color(0xFF5B3FD1)
    val OnAccent = Color(0xFFFFFFFF)
    /** Pastille lavande pâle : onglet actif, ligne d'aujourd'hui, pastilles d'icône. */
    val AccentTint = Color(0xFFECE8FC)
    val OnAccentTint = Color(0xFF3B2596)
    val AccentSoft = Color(0xFFC9BEF5)
    val Wifi = Color(0xFF0F9488)
    val Mobile = Color(0xFF9B7CF0)
}

// Thème clair
internal val LightPrimary = Color(0xFF5B3FD1)
internal val LightOnPrimary = Color(0xFFFFFFFF)
internal val LightPrimaryContainer = Color(0xFFECE8FC)
internal val LightOnPrimaryContainer = Color(0xFF3B2596)
internal val LightTertiary = Color(0xFF8466E8)
internal val LightOnTertiary = Color(0xFFFFFFFF)
internal val LightTertiaryContainer = Color(0xFFEFE9FE)
internal val LightOnTertiaryContainer = Color(0xFF3B2596)
internal val LightBackground = Color(0xFFF6F4F1)
internal val LightSurface = Color(0xFFFFFFFF)
internal val LightSurfaceVariant = Color(0xFFF1EDE8)
internal val LightOnSurface = Color(0xFF1C1B1A)
internal val LightOnSurfaceVariant = Color(0xFF6B6460)
internal val LightOutline = Color(0xFFC9C1B9)
internal val LightOutlineVariant = Color(0xFFECE7E1)
internal val LightError = Color(0xFFC62828)
internal val LightErrorContainer = Color(0xFFFDE2E1)
internal val LightOnErrorContainer = Color(0xFF7A1010)
internal val LightSuccess = Color(0xFF0F9488)
internal val LightSuccessContainer = Color(0xFFD5F3EF)

// Thème sombre
internal val DarkPrimary = Color(0xFFB3A4FF)
internal val DarkOnPrimary = Color(0xFF241466)
internal val DarkPrimaryContainer = Color(0xFF2D2552)
internal val DarkOnPrimaryContainer = Color(0xFFD9D0FF)
internal val DarkTertiary = Color(0xFFC4B5FD)
internal val DarkOnTertiary = Color(0xFF2A1A63)
internal val DarkTertiaryContainer = Color(0xFF2A2148)
internal val DarkOnTertiaryContainer = Color(0xFFE4DCFF)
internal val DarkBackground = Color(0xFF141312)
internal val DarkSurface = Color(0xFF1F1D1B)
internal val DarkSurfaceVariant = Color(0xFF2B2826)
internal val DarkOnSurface = Color(0xFFF1ECE8)
internal val DarkOnSurfaceVariant = Color(0xFFB5ADA7)
internal val DarkOutline = Color(0xFF5A534E)
internal val DarkOutlineVariant = Color(0xFF34302D)
internal val DarkError = Color(0xFFF26D6D)
internal val DarkErrorContainer = Color(0xFF4D1414)
internal val DarkOnErrorContainer = Color(0xFFFFDAD6)
internal val DarkSuccess = Color(0xFF5EE0D0)
internal val DarkSuccessContainer = Color(0xFF123532)

// Wi-Fi : sarcelle, porté par le rôle secondaire.
internal val LightSecondary = Color(0xFF0F9488)
internal val LightOnSecondary = Color(0xFFFFFFFF)
internal val LightSecondaryContainer = Color(0xFFD5F3EF)
internal val LightOnSecondaryContainer = Color(0xFF0B5A53)
internal val DarkSecondary = Color(0xFF5EE0D0)
internal val DarkOnSecondary = Color(0xFF00332E)
internal val DarkSecondaryContainer = Color(0xFF123532)
internal val DarkOnSecondaryContainer = Color(0xFFA6EEE4)
