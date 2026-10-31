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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.marzouki.datasaver.ui.theme.Brand
import com.marzouki.datasaver.ui.theme.LocalExtendedColors
import com.marzouki.datasaver.ui.theme.Spacing
import com.marzouki.datasaver.ui.theme.numeric

/**
 * Carte en verre dépoli : fond translucide sur les halos de [GlowBackground], liseré clair, sans ombre.
 * La bordure est retirée sur les fonds colorés (containers), où elle n'apporte rien.
 */
@Composable
fun DataKeeperCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    containerColor: Color = LocalExtendedColors.current.glass,
    contentColor: Color = if (containerColor == LocalExtendedColors.current.glass) {
        MaterialTheme.colorScheme.onSurface
    } else {
        contentColorFor(containerColor)
    },
    bordered: Boolean = containerColor == LocalExtendedColors.current.glass ||
        containerColor == MaterialTheme.colorScheme.surface,
    contentPadding: Dp = Spacing.cardPadding,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor)
    val elevation = CardDefaults.cardElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp, 0.dp)
    val borderColor = if (containerColor == LocalExtendedColors.current.glass) {
        LocalExtendedColors.current.glassBorder
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    val border = if (bordered) BorderStroke(1.dp, borderColor) else null
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

/**
 * Halos colorés (menthe, corail, indigo) peints derrière le contenu : ce sont eux qu'on devine
 * à travers les cartes en verre. Des dégradés radiaux, donc visibles sur toutes les versions d'Android.
 */
@Composable
fun Modifier.glowBackground(): Modifier {
    val strength = LocalExtendedColors.current.glowStrength
    return drawBehind {
        fun glow(color: Color, cx: Float, cy: Float, radius: Float, alpha: Float) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = alpha), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = radius,
                ),
                radius = radius,
                center = Offset(cx, cy),
            )
        }
        val r = size.minDimension * 0.75f
        glow(Brand.GlowMint, size.width * 0.05f, size.height * 0.28f, r, strength)
        glow(Brand.GlowCoral, size.width * 1.0f, size.height * 0.52f, r, strength * 0.7f)
        glow(Brand.GlowIndigo, size.width * 0.45f, size.height * 0.85f, r, strength * 0.8f)
    }
}

/** Fond bleu nuit des en-têtes, avec une lueur menthe en haut et corail en bas. */
fun Modifier.nightGlow(): Modifier = drawBehind {
    drawRect(Brand.Night)
    drawRect(
        Brush.radialGradient(
            colors = listOf(Brand.GlowMint.copy(alpha = 0.32f), Color.Transparent),
            center = Offset(size.width, 0f),
            radius = size.maxDimension * 0.9f,
        ),
    )
    drawRect(
        Brush.radialGradient(
            colors = listOf(Brand.GlowCoral.copy(alpha = 0.26f), Color.Transparent),
            center = Offset(0f, size.height * 1.1f),
            radius = size.maxDimension * 0.75f,
        ),
    )
}

/** Pastille d'icône : carré arrondi bleu nuit, pictogramme menthe. */
@Composable
fun IconChip(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    containerColor: Color = Brand.Night,
    contentColor: Color = Brand.GlowMint,
) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.32f))
            .background(containerColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(size * 0.58f))
    }
}

/** Titre de section précédé de sa pastille d'icône. */
@Composable
fun SectionTitle(text: String, icon: ImageVector, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconChip(icon)
        Spacer(Modifier.width(10.dp))
        Text(
            text,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize * 0.82f),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        action?.invoke()
    }
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
