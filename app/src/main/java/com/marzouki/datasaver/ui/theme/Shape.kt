package com.marzouki.datasaver.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Boutons, puces et barres de progression : CircleShape (pilule).
val DataKeeperShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Espacements (grille de 4 dp). */
object Spacing {
    val screenHorizontal = 16.dp
    /** Marge du titre d'écran (20 dp), en plus de [screenHorizontal]. */
    val titleExtra = 4.dp
    val betweenCards = 12.dp
    val cardPadding = 20.dp
}
