package com.marzouki.datasaver.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * En-tête d'écran du parcours : ligne de contexte en petit, titre en grand dessous, posés à même
 * le fond ; une action ronde blanche à droite. Prévu pour une colonne à marge de 16 dp.
 */
@Composable
fun ScreenHeader(
    overline: String,
    title: String,
    modifier: Modifier = Modifier,
    overlineColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = 8.dp, top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                overline,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = overlineColor,
            )
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 28.sp, lineHeight = 34.sp),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        action?.invoke()
    }
}

/** En-tête des sous-écrans (réglages, partage de connexion) : bouton retour rond blanc, titre et sous-titre. */
@Composable
fun BackHeader(
    title: String,
    backDescription: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundAction(Icons.AutoMirrored.Rounded.ArrowBack, backDescription, onBack)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Bouton icône rond de l'en-tête : disque blanc à ombre douce. [primary] le remplit de violet (action principale). */
@Composable
fun HeaderIconButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit, primary: Boolean = false) {
    Box(Modifier.padding(start = 8.dp)) {
        RoundAction(icon, contentDescription, onClick, primary)
    }
}

@Composable
private fun RoundAction(icon: ImageVector, contentDescription: String, onClick: () -> Unit, primary: Boolean = false) {
    val container = if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val content = if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val shadowColor = if (primary) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.10f)
    Box(
        Modifier
            .size(48.dp)
            .padding(2.dp)
            .shadow(4.dp, CircleShape, ambientColor = shadowColor, spotColor = shadowColor)
            .clip(CircleShape)
            .background(container)
            .clickable(onClickLabel = contentDescription, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = content, modifier = Modifier.size(22.dp))
    }
}

@ThemePreviews
@Composable
private fun ScreenHeaderPreview() {
    PreviewSurface {
        ScreenHeader("Forfait Ooredoo · jour 20 sur 30", "Ma consommation") {
            HeaderIconButton(Icons.Rounded.Settings, "Réglages", onClick = {})
        }
    }
}
