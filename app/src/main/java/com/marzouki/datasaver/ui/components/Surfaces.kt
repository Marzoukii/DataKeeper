package com.marzouki.datasaver.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.marzouki.datasaver.ui.theme.Spacing
import com.marzouki.datasaver.ui.theme.numeric

/**
 * Carte plate du design : fond [containerColor], bordure 1 dp outlineVariant, sans ombre.
 * La bordure est retirée sur les fonds colorés (containers), où elle n'apporte rien.
 */
@Composable
fun DataKeeperCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(containerColor),
    bordered: Boolean = containerColor == MaterialTheme.colorScheme.surface,
    contentPadding: Dp = Spacing.cardPadding,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor)
    val elevation = CardDefaults.cardElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp, 0.dp)
    val border = if (bordered) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
    val inner: @Composable ColumnScope.() -> Unit = {
        Column(Modifier.padding(contentPadding), content = content)
    }
    if (onClick != null) {
        Card(onClick, modifier.fillMaxWidth(), shape = shape, colors = colors, elevation = elevation, border = border, content = inner)
    } else {
        Card(modifier.fillMaxWidth(), shape = shape, colors = colors, elevation = elevation, border = border, content = inner)
    }
}

/** Puce d'information non interactive (pilule). */
@Composable
fun InfoPill(
    text: String,
    containerColor: Color,
    modifier: Modifier = Modifier,
    contentColor: Color = contentColorFor(containerColor),
) {
    Surface(shape = CircleShape, color = containerColor, contentColor = contentColor, modifier = modifier) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge.numeric(),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

/** Fine barre de progression arrondie (4 dp par défaut). */
@Composable
fun ThinBar(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    height: Dp = 4.dp,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(trackColor),
    ) {
        if (fraction > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(height)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

/** Léger scintillement pour les cartes en cours de chargement. */
@Composable
fun Modifier.shimmer(): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "shimmerAlpha",
    )
    return graphicsLayer { this.alpha = alpha }
}

/** Carte fantôme affichée pendant le chargement. */
@Composable
fun PlaceholderCard(height: Dp, modifier: Modifier = Modifier, shape: Shape = MaterialTheme.shapes.extraLarge) {
    DataKeeperCard(modifier = modifier.shimmer(), shape = shape) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PlaceholderLine(Modifier.fillMaxWidth(0.5f))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(height)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            PlaceholderLine(Modifier.fillMaxWidth(0.7f))
        }
    }
}

@Composable
private fun PlaceholderLine(modifier: Modifier) {
    Box(
        modifier
            .height(14.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    )
}

@ThemePreviews
@Composable
private fun SurfacesPreview() {
    PreviewSurface {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DataKeeperCard {
                Text("Carte plate", style = MaterialTheme.typography.titleMedium)
                ThinBar(0.6f, MaterialTheme.colorScheme.tertiary, Modifier.padding(top = 8.dp))
            }
            InfoPill("1,8 Go restants", MaterialTheme.colorScheme.tertiaryContainer)
            PlaceholderCard(height = 80.dp)
        }
    }
}
