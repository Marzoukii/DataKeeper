package com.marzouki.datasaver.ui.theme

import androidx.compose.ui.graphics.Color

// Style clair et chaleureux : fond blanc cassé, cartes blanches arrondies, accent rouge corail
// (actions, onglet actif, aujourd'hui), vert pour le Wi-Fi (ce qu'on encourage), orange pour la data
// mobile (ce qu'on surveille).

/** Couleurs de marque, identiques en clair et en sombre. */
object Brand {
    val Accent = Color(0xFFD23B22)
    val OnAccent = Color(0xFFFFFFFF)
    /** Pastille rose : onglet actif, ligne d'aujourd'hui, pastilles d'icône. */
    val AccentTint = Color(0xFFFCE3DE)
    val OnAccentTint = Color(0xFF8E2614)
    val AccentSoft = Color(0xFFF6B8AC)
    val Wifi = Color(0xFF128265)
    val Mobile = Color(0xFFE07B2C)
}

// Thème clair
internal val LightPrimary = Color(0xFFD23B22)
internal val LightOnPrimary = Color(0xFFFFFFFF)
internal val LightPrimaryContainer = Color(0xFFFCE3DE)
internal val LightOnPrimaryContainer = Color(0xFF8E2614)
internal val LightTertiary = Color(0xFFC25E12)
internal val LightOnTertiary = Color(0xFFFFFFFF)
internal val LightTertiaryContainer = Color(0xFFFDEBDC)
internal val LightOnTertiaryContainer = Color(0xFF6B3108)
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
internal val LightSuccess = Color(0xFF128265)
internal val LightSuccessContainer = Color(0xFFDDF2EA)

// Thème sombre
internal val DarkPrimary = Color(0xFFFF7A63)
internal val DarkOnPrimary = Color(0xFF4A0E04)
internal val DarkPrimaryContainer = Color(0xFF4A1E16)
internal val DarkOnPrimaryContainer = Color(0xFFFFDAD3)
internal val DarkTertiary = Color(0xFFF2A15A)
internal val DarkOnTertiary = Color(0xFF3D1E05)
internal val DarkTertiaryContainer = Color(0xFF472A12)
internal val DarkOnTertiaryContainer = Color(0xFFFFDDBF)
internal val DarkBackground = Color(0xFF141312)
internal val DarkSurface = Color(0xFF1F1D1B)
internal val DarkSurfaceVariant = Color(0xFF2B2826)
internal val DarkOnSurface = Color(0xFFF1ECE8)
internal val DarkOnSurfaceVariant = Color(0xFFB5ADA7)
internal val DarkOutline = Color(0xFF5A534E)
internal val DarkOutlineVariant = Color(0xFF34302D)
internal val DarkError = Color(0xFFFF8A80)
internal val DarkErrorContainer = Color(0xFF4D1414)
internal val DarkOnErrorContainer = Color(0xFFFFDAD6)
internal val DarkSuccess = Color(0xFF4CC7A0)
internal val DarkSuccessContainer = Color(0xFF143B2F)

// Wi-Fi : vert, porté par le rôle secondaire.
internal val LightSecondary = Color(0xFF128265)
internal val LightOnSecondary = Color(0xFFFFFFFF)
internal val LightSecondaryContainer = Color(0xFFDDF2EA)
internal val LightOnSecondaryContainer = Color(0xFF07402F)
internal val DarkSecondary = Color(0xFF4CC7A0)
internal val DarkOnSecondary = Color(0xFF00382A)
internal val DarkSecondaryContainer = Color(0xFF143B2F)
internal val DarkOnSecondaryContainer = Color(0xFFBDEEDB)
